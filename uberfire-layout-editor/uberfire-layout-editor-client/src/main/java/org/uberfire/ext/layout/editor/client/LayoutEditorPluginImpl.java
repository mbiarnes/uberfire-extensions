package org.uberfire.ext.layout.editor.client;

import java.util.List;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.mvp.UberView;
import org.uberfire.ext.editor.commons.client.file.SaveOperationService;
import org.uberfire.ext.layout.editor.api.LayoutServices;
import org.uberfire.ext.layout.editor.api.editor.LayoutTemplate;
import org.uberfire.ext.layout.editor.client.components.LayoutDragComponent;
import org.uberfire.ext.plugin.model.LayoutEditorModel;
import org.uberfire.ext.plugin.model.PluginType;
import org.uberfire.ext.plugin.service.PluginServices;
import org.uberfire.mvp.ParameterizedCommand;

@Dependent
public class LayoutEditorPluginImpl implements LayoutEditorPlugin {

    @Inject
    private LayoutEditorPresenter layoutEditorPresenter;

    @Inject
    private Caller<PluginServices> pluginServices;

    @Inject
    private Caller<LayoutServices> layoutServices;

    private String pluginName;

    private PluginType pluginType;

    private Path currentPath;

    private ParameterizedCommand<LayoutEditorModel> loadCallBack;

    private List<LayoutDragComponent> layoutDragComponents;

    @Override
    public void init(String layoutName, List<LayoutDragComponent> layoutDragComponents) {
        this.pluginName = layoutName;
        this.layoutDragComponents = layoutDragComponents;
        layoutEditorPresenter.setupDndPallete(layoutDragComponents);
    }

    @Override
    public Widget asWidget() {
        final UberView<LayoutEditorPresenter> view = layoutEditorPresenter.getView();
        return view.asWidget();
    }

    @Override
    public void loadLayout(LayoutTemplate layoutTemplate) {
        layoutEditorPresenter.loadLayout(layoutTemplate);
    }

    @Override
    public void loadDefaultLayout(String layoutName) {
        layoutEditorPresenter.loadDefaultLayout(layoutName);
    }

    @Override
    public LayoutTemplate getLayout() {
        return layoutEditorPresenter.getLayout();
    }

    @Override
    public void addLayoutProperty(String key, String value) {
        layoutEditorPresenter.addLayoutProperty(key, value);
    }

    @Override
    public String getLayoutProperty(String key) {
        return layoutEditorPresenter.getLayoutProperty(key);
    }

    @Override
    public void load(final PluginType pluginType,
            final Path currentPath,
            final ParameterizedCommand<LayoutEditorModel> loadCallBack) {
        this.pluginType = pluginType;
        this.currentPath = currentPath;
        this.loadCallBack = loadCallBack;
        pluginServices.call( new RemoteCallback<LayoutEditorModel>() {
            @Override
            public void callback( final LayoutEditorModel model ) {

                layoutServices.call( new RemoteCallback<LayoutTemplate>() {
                    @Override
                    public void callback( final LayoutTemplate layoutTemplate ) {
                        if (layoutTemplate != null) {
                            layoutEditorPresenter.loadLayout(layoutTemplate);
                            loadCallBack.execute( getLayoutContent( currentPath, model.getLayoutEditorModel() ) );
                        } else {
                            layoutEditorPresenter.loadDefaultLayout(pluginName);
                        }

                    }
                } ).convertLayoutFromString( model.getLayoutEditorModel() );

            }
        } ).getLayoutEditor( currentPath, pluginType );
    }

    @Override
    public void save(final Path path, final RemoteCallback<Path> saveSuccessCallback) {

        layoutServices.call( new RemoteCallback<String>() {
            @Override
            public void callback( final String model ) {
                savePlugin( model, path, saveSuccessCallback );
            }
        } ).convertLayoutToString(layoutEditorPresenter.getLayout());

    }

    private void savePlugin(final String model, final Path path, final RemoteCallback<Path> saveSuccessCallback ) {

        new SaveOperationService().save(path, new ParameterizedCommand<String>() {
            @Override
            public void execute( final String commitMessage ) {
                pluginServices.call( saveSuccessCallback ).saveLayout( getLayoutContent( path, model ),
                        commitMessage );
            }
        });
    }

    private LayoutEditorModel getLayoutContent(Path currentPath, String model) {
        return new LayoutEditorModel(pluginName, pluginType, currentPath, model);
    }
}