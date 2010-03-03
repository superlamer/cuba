package com.haulmont.cuba.gui.components;

import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.PropertyDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.security.entity.EntityOp;
import com.haulmont.cuba.security.global.UserSession;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaClass;

import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;

abstract class ListActionsHelper<T extends List> {
    protected IFrame frame;
    protected T component;
    protected UserSession userSession;
    protected MetaClass metaClass;

    protected java.util.List<Listener> listeners;

    ListActionsHelper(IFrame frame, T component) {
        if (component == null) {
            throw new IllegalStateException("Component cannot be null");
        }
        this.frame = frame;
        this.component = component;
        userSession = UserSessionClient.getUserSession();
        metaClass = component.getDatasource().getMetaClass();
        listeners = new ArrayList<Listener>();
    }

    public Action createCreateAction() {
        return createCreateAction(new ValueProvider() {
            public Map<String, Object> getValues() {
                return Collections.emptyMap();
            }

            public Map<String, Object> getParameters() {
                return Collections.emptyMap();
            }
        }, WindowManager.OpenType.THIS_TAB);
    }

    public Action createCreateAction(final WindowManager.OpenType openType) {
        return createCreateAction(new ValueProvider() {
            public Map<String, Object> getValues() {
                return Collections.emptyMap();
            }

            public Map<String, Object> getParameters() {
                return Collections.emptyMap();
            }
        }, openType);
    }

    public Action createCreateAction(final ValueProvider valueProvider) {
        return createCreateAction(valueProvider, WindowManager.OpenType.THIS_TAB);
    }

    public abstract Action createCreateAction(final ValueProvider valueProvider, final WindowManager.OpenType openType);

    public Action createEditAction() {
        return createEditAction(WindowManager.OpenType.THIS_TAB);
    }

    public Action createEditAction(final WindowManager.OpenType openType) {
        return createEditAction(openType, Collections.EMPTY_MAP);
    }

    public Action createEditAction(final WindowManager.OpenType openType, Map<String, Object> params) {
        final AbstractAction action = new EditAction("edit", openType, params);
        ListActionsHelper.this.component.addAction(action);

        return action;
    }

    public Action createRefreshAction() {
        Action action = new RefreshAction();
        component.addAction(action);
        return action;
    }

    public Action createRemoveAction() {
        return createRemoveAction(true);
    }

    public Action createRemoveAction(final boolean autocommit) {
        Action action = new RemoveAction(autocommit);
        component.addAction(action);
        return action;
    }

    public Action createFilterApplyAction(final String componentId) {
        Action action = new FilterApplyAction();
        ((Button) frame.getComponent(componentId)).setAction(action);
        return action;
    }

    public Action createFilterClearAction(final String componentId, final String containerName) {
        Action action = new FilterClearAction(containerName);
        ((Button) frame.getComponent(componentId)).setAction(action);
        return action;
    }

    public Action createAddAction(final Window.Lookup.Handler handler, final WindowManager.OpenType openType) {
        return createAddAction(handler, openType, Collections.<String, Object>emptyMap());
    }

    public Action createAddAction(final Window.Lookup.Handler handler) {
        return createAddAction(handler, WindowManager.OpenType.THIS_TAB);
    }

    public Action createAddAction(final Window.Lookup.Handler handler, String windowId) {
        return createAddAction(handler, WindowManager.OpenType.THIS_TAB,
                Collections.<String, Object>emptyMap(), null, windowId);
    }

    public Action createAddAction(final Window.Lookup.Handler handler, final Map<String, Object> params) {
        return createAddAction(handler, WindowManager.OpenType.THIS_TAB, params);
    }

    public Action createAddAction(final Window.Lookup.Handler handler, final WindowManager.OpenType openType, final Map<String, Object> params) {
        return createAddAction(handler, openType, params, null);
    }

    public Action createAddAction(final Window.Lookup.Handler handler, final WindowManager.OpenType openType,
                                  final Map<String, Object> params, final String captionKey) {
        return createAddAction(handler, openType, params, captionKey, null);
    }

    public Action createAddAction(final Window.Lookup.Handler handler, final WindowManager.OpenType openType,
                                  final Map<String, Object> params, final String captionKey, String windowId) {
        AbstractAction action = new AddAction(captionKey, handler, openType, params, windowId);
        component.addAction(action);
        return action;
    }

    protected void fireCreateEvent(Entity entity) {
        for (Listener listener: listeners) {
            listener.entityCreated(entity);
        }
    }

    protected void fireEditEvent(Entity entity) {
        for (Listener listener: listeners) {
            listener.entityEdited(entity);
        }
    }

    protected void fireRemoveEvent(Set<Entity> entities) {
        for (Listener listener: listeners) {
            listener.entityRemoved(entities);
        }
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    protected class EditAction extends AbstractAction {
        private final WindowManager.OpenType openType;

        private Map<String, Object> params;

        public EditAction(String id, WindowManager.OpenType openType) {
            super(id);
            this.openType = openType;
            this.params = Collections.EMPTY_MAP;
        }

        public EditAction(String id, WindowManager.OpenType openType, Map<String, Object> params) {
            super(id);
            this.openType = openType;
            this.params = params;
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            if (userSession.isEntityOpPermitted(metaClass, EntityOp.UPDATE))
                return MessageProvider.getMessage(messagesPackage, "actions.Edit");
            else
                return MessageProvider.getMessage(messagesPackage, "actions.View");
        }

        public void actionPerform(Component component) {
            final Set selected = ListActionsHelper.this.component.getSelected();
            if (selected.size() == 1) {
                final CollectionDatasource datasource = ListActionsHelper.this.component.getDatasource();
                final String windowID = datasource.getMetaClass().getName() + ".edit";

                Datasource parentDs = null;
                if (datasource instanceof PropertyDatasource) {
                    MetaProperty metaProperty = ((PropertyDatasource) datasource).getProperty();
                    if (metaProperty.getType().equals(MetaProperty.Type.AGGREGATION)) {
                        parentDs = datasource;
                    }
                }
                final Datasource pDs = parentDs;

                final Window window = frame.openEditor(windowID, datasource.getItem(), openType, params, parentDs);

                window.addListener(new Window.CloseListener() {
                    public void windowClosed(String actionId) {
                        if (Window.COMMIT_ACTION_ID.equals(actionId) && window instanceof Window.Editor) {
                            Object item = ((Window.Editor) window).getItem();
                            if (item instanceof Entity) {
                                if (pDs == null) {
                                    datasource.updateItem((Entity) item);
                                }
                                fireEditEvent((Entity) item);
                            }
                        }
                    }
                });
            }
        }
    }

    public static interface Listener {
        void entityCreated(Entity entity);
        void entityEdited(Entity entity);
        void entityRemoved(Set<Entity> entity);
    }

    private class RefreshAction extends AbstractAction {
        public RefreshAction() {
            super("refresh");
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            return MessageProvider.getMessage(messagesPackage, "actions.Refresh");
        }

        public void actionPerform(Component component) {
            ListActionsHelper.this.component.getDatasource().refresh();
        }
    }

    private class RemoveAction extends AbstractAction {
        private final boolean autocommit;

        public RemoveAction(boolean autocommit) {
            super("remove");
            this.autocommit = autocommit;
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            return MessageProvider.getMessage(messagesPackage, "actions.Remove");
        }

        public boolean isEnabled() {
            return super.isEnabled() && userSession.isEntityOpPermitted(metaClass, EntityOp.DELETE);
        }

        public void actionPerform(Component component) {
            final Set selected = ListActionsHelper.this.component.getSelected();
            if (!selected.isEmpty()) {
                final String messagesPackage = AppConfig.getInstance().getMessagesPack();
                frame.showOptionDialog(
                        MessageProvider.getMessage(messagesPackage, "dialogs.Confirmation"),
                        MessageProvider.getMessage(messagesPackage, "dialogs.Confirmation.Remove"),
                        IFrame.MessageType.CONFIRMATION,
                        new Action[]{new AbstractAction("ok") {
                            public String getCaption() {
                                return MessageProvider.getMessage(messagesPackage, "actions.Ok");
                            }

                            public boolean isEnabled() {
                                return true;
                            }

                            @Override
                            public String getIcon() {
                                return "icons/ok.png";
                            }

                            public void actionPerform(Component component) {
                                @SuppressWarnings({"unchecked"})
                                final CollectionDatasource ds = ListActionsHelper.this.component.getDatasource();
                                for (Object item : selected) {
                                    ds.removeItem((Entity) item);
                                }

                                fireRemoveEvent(selected);

                                if (autocommit) {
                                    try {
                                        ds.commit();
                                    } catch (RuntimeException e) {
                                        ds.refresh();
                                        throw e;
                                    }
                                }
                            }
                        }, new AbstractAction("cancel") {
                            public String getCaption() {
                                return MessageProvider.getMessage(messagesPackage, "actions.Cancel");
                            }

                            public boolean isEnabled() {
                                return true;
                            }

                            @Override
                            public String getIcon() {
                                return "icons/cancel.png";
                            }

                            public void actionPerform(Component component) {
                            }
                        }});
            }
        }
    }

    private class FilterApplyAction extends AbstractAction {
        public FilterApplyAction() {
            super("apply");
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            return MessageProvider.getMessage(messagesPackage, "actions.Apply");
        }

        public void actionPerform(Component component) {
            ListActionsHelper.this.component.getDatasource().refresh();
        }
    }

    private class FilterClearAction extends AbstractAction {
        private final String containerName;

        public FilterClearAction(String containerName) {
            super("clear");
            this.containerName = containerName;
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            return MessageProvider.getMessage(messagesPackage, "actions.Clear");
        }

        public void actionPerform(Component component) {
            Component.Container container = ListActionsHelper.this.frame.getComponent(containerName);
            ComponentsHelper.walkComponents(container,
                    new ComponentVisitor() {
                        public void visit(Component component, String name) {
                            if (component instanceof Field) {
                                ((Field) component).setValue(null);
                            }
                        }
                    }
            );
        }
    }

    private class AddAction extends AbstractAction {
        private final String captionKey;
        private final Window.Lookup.Handler handler;
        private final WindowManager.OpenType openType;
        private final Map<String, Object> params;
        private String windowId; 

        public AddAction(String captionKey, Window.Lookup.Handler handler, WindowManager.OpenType openType, Map<String, Object> params, String windowId) {
            super("add");
            this.captionKey = captionKey;
            this.handler = handler;
            this.openType = openType;
            this.params = params;
            this.windowId = windowId;
        }

        public String getCaption() {
            final String messagesPackage = AppConfig.getInstance().getMessagesPack();
            return MessageProvider.getMessage(messagesPackage, captionKey == null ? "actions.Add" : captionKey);
        }

        public void actionPerform(Component component) {
            final CollectionDatasource datasource = ListActionsHelper.this.component.getDatasource();
            final String winID = windowId == null ? datasource.getMetaClass().getName() + ".browse" : windowId;

            frame.openLookup(winID, handler, openType, params);
        }
    }
}
