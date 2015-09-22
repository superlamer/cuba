/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.sys;

import com.google.common.base.Charsets;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.app.WebStatisticsAccumulator;
import com.haulmont.cuba.web.auth.RequestContext;
import com.vaadin.server.*;
import com.vaadin.shared.ApplicationConstants;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Main CUBA web-application servlet
 *
 * @author artamonov
 * @version $Id$
 */
public class CubaApplicationServlet extends VaadinServlet {

    private static final long serialVersionUID = -8701539520754293569L;

    public static final String FROM_HTML_REDIRECT_PARAM = "fromCubaHtmlRedirect";

    private Logger log = LoggerFactory.getLogger(CubaApplicationServlet.class);

    protected WebConfig webConfig;

    protected WebStatisticsAccumulator statisticsCounter;

    protected volatile ClassLoader classLoader;

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        CubaVaadinServletService service = new CubaVaadinServletService(this, deploymentConfiguration);
        if (classLoader != null) {
            service.setClassLoader(classLoader);
        }
        service.init();
        return service;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        Configuration configuration = AppBeans.get(Configuration.NAME);
        webConfig = configuration.getConfig(WebConfig.class);
        statisticsCounter = AppBeans.get(WebStatisticsAccumulator.class);

        super.init(servletConfig);
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();

        getService().addSessionInitListener(event -> {
            CubaBootstrapListener bootstrapListener = AppBeans.get(CubaBootstrapListener.NAME);

            event.getSession().addBootstrapListener(bootstrapListener);
        });
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
        int sessionExpirationTimeout = webConfig.getHttpSessionExpirationTimeoutSec();
        int sessionPingPeriod = sessionExpirationTimeout / 3;

        if (sessionPingPeriod > 0) {
            // configure Vaadin heartbeat according to web config
            initParameters.setProperty(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL, String.valueOf(sessionPingPeriod));
        }

        return super.createDeploymentConfiguration(initParameters);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String contextName = request.getContextPath().length() == 0 ? "" : request.getContextPath().substring(1);

        if (request.getParameter("restartApp") != null) {
            request.getSession().invalidate();
            response.sendRedirect(requestURI);
            return;
        }

        String[] uriParts = requestURI.split("/");
        String action = null;

        if (uriParts.length > 0) {
            String lastPart = uriParts[uriParts.length - 1];

            if (webConfig.getLoginAction().equals(lastPart) || webConfig.getLinkHandlerActions().contains(lastPart)) {
                action = lastPart;
            }
        }

        boolean needRedirect = action != null;
        if (needRedirect) {
            if (webConfig.getUseRedirectWithBlankPageForLinkAction() &&
                    action != null &&
                    request.getParameter(FROM_HTML_REDIRECT_PARAM) == null) {
                redirectWithBlankHtmlPage(request, response);
            } else {
                redirectToApp(request, response, contextName, uriParts, action);
            }
        } else {
            serviceAppRequest(request, response);
        }
    }

    protected void redirectWithBlankHtmlPage(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        final BufferedWriter page = new BufferedWriter(new OutputStreamWriter(
                response.getOutputStream(), "UTF-8"));

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(request.getRequestURI());
        Map<String, String[]> parameterMap = request.getParameterMap();
        stringBuilder.append("?");
        stringBuilder.append(FROM_HTML_REDIRECT_PARAM);
        stringBuilder.append("=true");

        for (String key : parameterMap.keySet()) {
            for (String value : parameterMap.get(key)) {
                stringBuilder.append("&");
                stringBuilder.append(key);
                stringBuilder.append("=");
                stringBuilder.append(value);
            }
        }
        String url = stringBuilder.toString();

        page.write(String.format(IOUtils.toString(CubaApplicationServlet.class.getResourceAsStream("redirect-page-template.html"),
                Charsets.UTF_8.name()), url, url, url));
        page.close();
    }

    protected void redirectToApp(HttpServletRequest request, HttpServletResponse response,
                                 String contextName, String[] uriParts, String action) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < uriParts.length; i++) {
            sb.append(uriParts[i]);
            if (uriParts[i].equals(contextName)) {
                break;
            }
            if (i < uriParts.length - 1)
                sb.append("/");
        }

        HttpSession httpSession = request.getSession();
        if (action != null) {
            httpSession.setAttribute(AppUI.LAST_REQUEST_ACTION_ATTR, action);
        }
        if (request.getParameterNames().hasMoreElements()) {
            Map<String, String> params = new HashMap<>();
            Enumeration parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String name = (String) parameterNames.nextElement();
                if (!FROM_HTML_REDIRECT_PARAM.equals(name)) {
                    params.put(name, request.getParameter(name));
                }
            }
            httpSession.setAttribute(AppUI.LAST_REQUEST_PARAMS_ATTR, params);
        }

        statisticsCounter.incWebRequestsCount();
        log.debug("Redirect to application " + httpSession.getId());
        response.addCookie(new Cookie("JSESSIONID", httpSession.getId()));
        response.sendRedirect(sb.toString());
    }

    protected void serviceAppRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestContext.create(request, response);
        statisticsCounter.incWebRequestsCount();

        long startTs = System.currentTimeMillis();

        try {
            super.service(request, response);
        } finally {
            RequestContext.destroy();
        }

        if (hasPathPrefix(request, ApplicationConstants.UIDL_PATH + '/')) {
            long t = System.currentTimeMillis() - startTs;
            if (t > (webConfig.getLogLongRequestsThresholdSec() * 1000)) {
                log.warn(String.format("Too long request processing [%d ms]: ip=%s, url=%s",
                        t, request.getRemoteAddr(), request.getRequestURI()));
            }
        }
    }

    protected boolean hasPathPrefix(HttpServletRequest request, String prefix) {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            return false;
        }

        if (!prefix.startsWith("/")) {
            prefix = '/' + prefix;
        }

        return pathInfo.startsWith(prefix);
    }
}