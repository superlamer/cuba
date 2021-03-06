/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.cuba.web.toolkit.ui.client.button;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ui.VButton;

public class CubaButtonWidget extends VButton {

    public CubaButtonWidget() {
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (BrowserInfo.get().isIE9()) {
            int type = DOM.eventGetType(event);
            switch (type) {
                case Event.ONMOUSEOVER:
                    if (isEnabled() && !getStyleName().contains("ie9-hover")) {
                        addStyleName("ie9-hover");
                    }
                    break;
                case Event.ONMOUSEOUT:
                    if (getStyleName().contains("ie9-hover")) {
                        removeStyleName("ie9-hover");
                    }
                    break;
            }
        }

        super.onBrowserEvent(event);
    }

    @Override
    public void onClick(ClickEvent event) {
        if (BrowserInfo.get().isIE() && BrowserInfo.get().getIEVersion() >= 11) {
            // fix focusing of button-wrap in IE11
            setFocus(true);
        }

        super.onClick(event);
    }

    @Override
    protected boolean handleKeyboardEvents(Event event) {
        int type = DOM.eventGetType(event);
        // Synthesize clicks based on keyboard events AFTER the normal key
        // handling.
        if ((event.getTypeInt() & Event.KEYEVENTS) != 0) {
            switch (type) {
                case Event.ONKEYDOWN:
                    // Button should not react on shortcuts with ENTER and SPACE
                    if (isShortcut(event)
                            && (event.getKeyCode() == KeyCodes.KEY_ENTER
                            || event.getKeyCode() == KeyCodes.KEY_SPACE)) {
                        return true;
                    }
                    // Stop propagation when the user starts pressing a button that
                    // we are handling to prevent actions from getting triggered
                    if (event.getKeyCode() == KeyCodes.KEY_SPACE) {
                        isFocusing = true;
                        event.preventDefault();
                        event.stopPropagation();
                        return true;
                    } else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                        isFocusing = true;
                        event.stopPropagation();
                        return true;
                    }
                    break;

                // CAUTION IE sometimes does not generate ONKEYPRESS for ENTER, so we override default Vaadin behavior
                case Event.ONKEYUP:
                    if (isFocusing) {
                        if (event.getKeyCode() == KeyCodes.KEY_SPACE) {
                            isFocusing = false;
                            onClick();
                            event.stopPropagation();
                            event.preventDefault();
                            return true;
                        } else if (event.getKeyCode() == KeyCodes.KEY_ENTER) {
                            isFocusing = false;
                            onClick();
                            event.stopPropagation();
                            event.preventDefault();
                            return true;
                        }
                    } else if (isShortcut(event)
                            && (event.getKeyCode() == KeyCodes.KEY_ENTER
                            || event.getKeyCode() == KeyCodes.KEY_SPACE)) {
                        return true;
                    }
                    break;
                case Event.ONKEYPRESS: {
                    // Button should not react on shortcuts with ENTER and SPACE
                    if (isShortcut(event)
                            && (event.getKeyCode() == KeyCodes.KEY_ENTER
                            || event.getKeyCode() == KeyCodes.KEY_SPACE)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected boolean isShortcut(Event event) {
        return event.getShiftKey() || event.getAltKey() || event.getCtrlKey() || event.getMetaKey();
    }
}