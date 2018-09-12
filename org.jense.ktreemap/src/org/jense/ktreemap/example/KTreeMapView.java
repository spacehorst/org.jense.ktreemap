package org.jense.ktreemap.example;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedHashMap;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jense.ktreemap.ITreeMapColorProvider;
import org.jense.ktreemap.ITreeMapProvider;
import org.jense.ktreemap.KTreeMap;
import org.jense.ktreemap.SplitByNumber;
import org.jense.ktreemap.SplitBySlice;
import org.jense.ktreemap.SplitBySortedWeight;
import org.jense.ktreemap.SplitByWeight;
import org.jense.ktreemap.SplitSquarified;
import org.jense.ktreemap.SplitStrategy;
import org.jense.ktreemap.TreeMapNode;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class KTreeMapView extends ViewPart {
    private static final String ID_BUNDLE = "org.jense.ktreemap";

    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    private Action openXmlAction;
    private Action openTM3Action;
    private Action selectionChangedAction;
    private KTreeMap kTreeMap;
    private final LinkedHashMap<String, SplitStrategy> strategies = new LinkedHashMap<>();
    private final LinkedHashMap<String, ITreeMapColorProvider> colorProviders = new LinkedHashMap<>();
    private Combo cmbStrategy;
    private Combo cmbColorProvider;
    private Combo cmbTM3Weight;
    private Combo cmbTM3Value;
    private Composite legend;
    private XMLTreeMapProvider xmlProvider;
    private TM3TreeMapProvider tm3Provider;
    private BuilderTM3 builderTM3;
    private Group grpTM3Params;

    /*
     * The content provider class is responsible for providing objects to the
     * view. It can wrap existing objects in adapters or simply return objects
     * as-is. These objects may be sensitive to the current input of the view, or
     * ignore it and always show the same content (like Task List, for example).
     */

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent) {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setLayout(new FillLayout());

        TreeMapNode root = null;
        try {
            root = getDefaultRoot();
        } catch (ParseException e) {
            MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
                    .getActiveShell(), "Parse Error", e.getMessage());
            e.printStackTrace();
        }

        createLeftComp(sash);
        createKTreeMapComp(sash, root);
        createRightComp(sash);

        viewer.setInput(root);

        sash.setWeights(new int[] {15, 70, 15});

        makeActions();
        hookContextMenu();
        hookSelectionChangedAction();
        contributeToActionBars();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        // to dispose the color and the others resources :
        ResourceManager.dispose();
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    void updateColorProvider(Composite comp) {
        String key = cmbColorProvider.getText();
        ITreeMapColorProvider cp = colorProviders.get(key);
        kTreeMap.setColorProvider(cp);
        if (legend != null) {
            legend.dispose();
        }
        legend = cp.getLegend(comp, SWT.NONE);
        legend.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));
        comp.layout();
    }

    void updateStrategy() {
        String key = cmbStrategy.getText();
        SplitStrategy strat = strategies.get(key);
        kTreeMap.setStrategy(strat);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void createColorProviders(Composite comp) {
        createColorProviders();

        final Group grp = new Group(comp, SWT.NONE);
        grp.setText("Color Provider");
        grp.setLayout(new GridLayout());
        grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));

        cmbColorProvider = new Combo(grp, SWT.NONE);
        cmbColorProvider.removeAll();
        for (String key : colorProviders.keySet()) {
            cmbColorProvider.add(key);
        }
        cmbColorProvider.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));

        cmbColorProvider.select(0);

        cmbColorProvider.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // ignore
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateColorProvider(grp);
            }
        });

        // colorProvider choice :
        updateColorProvider(grp);
    }

    private void createColorProviders() {
        colorProviders.put("HSB linear", new HSBTreeMapColorProvider(kTreeMap,
                HSBTreeMapColorProvider.ColorDistributionTypes.Linear, Display
                        .getDefault().getSystemColor(SWT.COLOR_GREEN),
                Display.getDefault()
                        .getSystemColor(SWT.COLOR_RED)));
        colorProviders.put("HSB log", new HSBTreeMapColorProvider(kTreeMap,
                HSBTreeMapColorProvider.ColorDistributionTypes.Log, Display
                        .getDefault().getSystemColor(SWT.COLOR_GREEN),
                Display.getDefault()
                        .getSystemColor(SWT.COLOR_RED)));
        colorProviders.put("HSB SquareRoot", new HSBTreeMapColorProvider(
                kTreeMap, HSBTreeMapColorProvider.ColorDistributionTypes.SquareRoot,
                Display.getDefault().getSystemColor(SWT.COLOR_GREEN), Display
                        .getDefault().getSystemColor(SWT.COLOR_RED)));
        colorProviders.put("HSB CubicRoot", new HSBTreeMapColorProvider(
                kTreeMap, HSBTreeMapColorProvider.ColorDistributionTypes.CubicRoot,
                Display.getDefault().getSystemColor(SWT.COLOR_GREEN), Display
                        .getDefault().getSystemColor(SWT.COLOR_RED)));
        colorProviders.put("HSB exp", new HSBTreeMapColorProvider(kTreeMap,
                HSBTreeMapColorProvider.ColorDistributionTypes.Exp, Display
                        .getDefault().getSystemColor(SWT.COLOR_GREEN),
                Display.getDefault()
                        .getSystemColor(SWT.COLOR_RED)));
    }

    private void createKTreeMapComp(SashForm sash, TreeMapNode root) {
        kTreeMap = new KTreeMap(sash, SWT.NONE, root) {
            @Override
            protected void drawLabels(GC gc, TreeMapNode item) {
                gc.setFont(getFont());
                drawLeafLabel(gc, item);
            }

            private void drawLeafLabel(GC gc, TreeMapNode item) {
                if (item.isLeaf()) {
                    drawLabel(gc, item);
                } else {
                    for (TreeMapNode node : item.getChildren()) {
                        drawLeafLabel(gc, node);
                    }
                }
            }
        };
        kTreeMap.setTreeMapProvider(xmlProvider);
    }

    private void createLeftComp(SashForm sash) {
        Composite comp = new Composite(sash, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 2;
        layout.marginWidth = 2;
        comp.setLayout(layout);

        viewer = new TreeViewer(comp, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    private void createRightComp(SashForm sash) {
        Composite comp = new Composite(sash, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 2;
        layout.marginWidth = 2;
        comp.setLayout(layout);

        createStrategies(comp);
        createColorProviders(comp);
        createTM3Params(comp);
    }

    private void createTM3Params(Composite comp) {
        grpTM3Params = new Group(comp, SWT.NONE);
        grpTM3Params.setText("TM3 params");
        grpTM3Params.setLayout(new GridLayout());
        grpTM3Params.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));
        grpTM3Params.setVisible(false);

        cmbTM3Value = new Combo(grpTM3Params, SWT.NONE);
        cmbTM3Value.setToolTipText("Select the value field");
        cmbTM3Value.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));
        cmbTM3Value.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // ignore
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                Combo cmb = (Combo) e.getSource();
                String field = cmb.getText();
                TM3TreeMapProvider.setValueField(field);
                createColorProviders();
                updateColorProvider(cmbColorProvider.getParent());
                kTreeMap.redraw();
            }

        });

        cmbTM3Weight = new Combo(grpTM3Params, SWT.NONE);
        cmbTM3Weight.setToolTipText("Select the weight field");
        cmbTM3Weight.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));
        cmbTM3Weight.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // ignore
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                Combo cmb = (Combo) e.getSource();
                String field = cmb.getText();
                BuilderTM3.setFieldWeight(field);
                builderTM3.setWeights();
                kTreeMap.calculatePositions();
                kTreeMap.redraw();
            }

        });

    }

    private void createStrategies(Composite comp) {
        strategies.put("Squarified", new SplitSquarified());
        strategies.put("Sorted Weight", new SplitBySortedWeight());
        strategies.put("Weight", new SplitByWeight());
        strategies.put("Slice", new SplitBySlice());
        strategies.put("Equal Weight", new SplitByNumber());

        Group grp = new Group(comp, SWT.NONE);
        grp.setText("Strategy");
        grp.setLayout(new GridLayout());
        grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));

        cmbStrategy = new Combo(grp, SWT.NONE);
        cmbStrategy.removeAll();
        for (String key : strategies.keySet()) {
            cmbStrategy.add(key);
        }
        cmbStrategy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.VERTICAL_ALIGN_BEGINNING));

        cmbStrategy.select(0);

        cmbStrategy.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                // ignore
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateStrategy();
            }
        });

        // Strategy choice :
        updateStrategy();
    }

    private void fillContextMenu(IMenuManager manager) {
        TreeMapNode orig = kTreeMap.getDisplayedRoot();

        ITreeMapProvider provider = kTreeMap.getTreeMapProvider();

        TreeMapNode cursor = orig;

        // Separator
        String id = "separator";
        manager.add(new Separator(id));

        // Parents
        while (cursor.getParent() != null) {
            TreeMapNode parent = cursor.getParent();
            ZoomAction action = new ZoomAction(provider.getLabel(parent),
                    AbstractUIPlugin.imageDescriptorFromPlugin(ID_BUNDLE,
                            "icons/unzoom.gif"),
                    parent);
            manager.insertBefore(id, action);
            cursor = parent;
            id = action.getId();
        }

        // children
        cursor = orig;
        while (cursor.getChild(kTreeMap.getCursorPosition()) != null) {
            TreeMapNode child = cursor.getChild(kTreeMap.getCursorPosition());
            if ( !child.isLeaf()) {
                ZoomAction action = new ZoomAction(provider.getLabel(child),
                        AbstractUIPlugin.imageDescriptorFromPlugin(ID_BUNDLE,
                                "icons/zoom.gif"),
                        child);
                manager.add(action);
            }
            cursor = child;
        }
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(openXmlAction);
        manager.add(openTM3Action);
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(openXmlAction);
        manager.add(openTM3Action);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private TreeMapNode getDefaultRoot() throws ParseException {
        URL url = FileLocator.find(Platform.getBundle(ID_BUNDLE), new Path(
                "/TreeMap.xml"), null);
        try {
            url = FileLocator.toFileURL(url);
        } catch (IOException e) {
            MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
                    .getActiveShell(), "Error", e.getMessage());
            e.printStackTrace();
        }
        BuilderXML builder = new BuilderXML(new File(url.getPath()));

        xmlProvider = new XMLTreeMapProvider();
        tm3Provider = new TM3TreeMapProvider();

        return builder.getRoot();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                KTreeMapView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(kTreeMap);
        kTreeMap.setMenu(menu);
    }

    private void hookSelectionChangedAction() {
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                selectionChangedAction.run();
            }
        });
    }

    private void setTM3Fields() {
        String[] numberFields = TM3Bean.getNumberFields();
        String[] cmbValues = new String[numberFields.length + 1];
        cmbValues[0] = "";
        for (int i = 1; i < cmbValues.length; i++) {
            cmbValues[i] = numberFields[i - 1];
        }
        cmbTM3Weight.removeAll();
        cmbTM3Value.removeAll();
        for (int i = 0; i < cmbValues.length; i++) {
            String item = cmbValues[i];
            cmbTM3Weight.add(item);
            cmbTM3Value.add(item);
        }

    }

    private void makeActions() {
        openXmlAction = new OpenXMLAction();
        openTM3Action = new OpenTM3Action();

        selectionChangedAction = new Action() {
            @Override
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                if (obj instanceof TreeMapNode) {
                    TreeMapNode dest = ((TreeMapNode) obj).getParent();
                    if (dest == null) {
                        return;
                    }

                    kTreeMap.zoom(dest);
                    kTreeMap.redraw();

                }
            }
        };
    }

    class ViewContentProvider implements IStructuredContentProvider,
            ITreeContentProvider {

        @Override
        public void dispose() {
            /* ignore */}

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeMapNode) {
                return ((TreeMapNode) parent).getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeMapNode) {
                return ((TreeMapNode) child).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeMapNode) {
                return ! ((TreeMapNode) parent).isLeaf();
            }
            return false;
        }

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            /* ignore */
        }
    }

    class ViewLabelProvider extends LabelProvider {

        @Override
        public Image getImage(Object obj) {
            return null;
        }

        @Override
        public String getText(Object obj) {
            if (obj instanceof TreeMapNode) {
                TreeMapNode node = (TreeMapNode) obj;
                ITreeMapProvider provider = kTreeMap
                        .getTreeMapProvider();
                return provider.getLabel(node);
            }
            return obj.toString();
        }
    }

    private class OpenXMLAction extends Action {
        private final String[] EXTENTIONS = new String[] {"*.xml"};

        /**
         * Constructor
         */
        public OpenXMLAction() {
            super("Open XML File");
            setToolTipText(getText());
            setId(getText());
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.action.Action#getImageDescriptor()
         */
        @Override
        public ImageDescriptor getImageDescriptor() {
            return AbstractUIPlugin.imageDescriptorFromPlugin(ID_BUNDLE,
                    "icons/XMLFile.gif");
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
            Display display = PlatformUI.getWorkbench().getDisplay();
            final FileDialog dialog = new FileDialog(display.getActiveShell(),
                    SWT.OPEN);
            dialog.setFilterExtensions(EXTENTIONS);
            String path = dialog.open();

            if (path != null) {
                BuilderXML builder;
                try {
                    builder = new BuilderXML(new File(path));
                } catch (ParseException e) {
                    MessageDialog.openError(display.getActiveShell(), "Parse error", e
                            .getMessage());
                    return;
                }
                TreeMapNode root = builder.getRoot();
                kTreeMap.setTreeMapProvider(xmlProvider);
                kTreeMap.setRoot(root);
                viewer.setInput(root);
                createColorProviders();
                updateColorProvider(cmbColorProvider.getParent());
                grpTM3Params.setVisible(false);

            }
        }

    }

    private class OpenTM3Action extends Action {
        private final String[] EXTENTIONS = new String[] {"*.tm3"};

        /**
         * Constructor
         */
        public OpenTM3Action() {
            super("Open TM3 File");
            setToolTipText(getText());
            setId(getText());
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.action.Action#getImageDescriptor()
         */
        @Override
        public ImageDescriptor getImageDescriptor() {
            return AbstractUIPlugin.imageDescriptorFromPlugin(ID_BUNDLE,
                    "icons/TM3File.gif");
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
            Display display = PlatformUI.getWorkbench().getDisplay();
            final FileDialog dialog = new FileDialog(display.getActiveShell(),
                    SWT.OPEN);
            dialog.setFilterExtensions(EXTENTIONS);
            String path = dialog.open();

            if (path != null) {
                try {
                    builderTM3 = new BuilderTM3(new File(path));
                } catch (IOException e) {
                    MessageDialog.openError(display.getActiveShell(), "Parse error", e
                            .getMessage());
                    return;
                }
                TreeMapNode root = builderTM3.getRoot();
                kTreeMap.setTreeMapProvider(tm3Provider);
                kTreeMap.setRoot(root);
                viewer.setInput(root);
                createColorProviders();
                updateColorProvider(cmbColorProvider.getParent());
                // add tm3 fields
                setTM3Fields();
                grpTM3Params.setVisible(true);
            }
        }

    }

    private class ZoomAction extends Action {
        private final TreeMapNode node;

        /**
         * Constructor
         *
         * @param text text of the action
         * @param image image
         * @param node destination TreeMapNode of the zoom
         */
        public ZoomAction(String text, ImageDescriptor image, TreeMapNode node) {
            super(text, image);
            this.node = node;
            setId(text);
        }

        /*
         * (non-Javadoc)
         * @see javax.swing.Action#isEnabled()
         */
        @Override
        public boolean isEnabled() {
            return true;
        }

        /*
         * (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
            kTreeMap.zoom(node);
            kTreeMap.redraw();
        }

    }

}
