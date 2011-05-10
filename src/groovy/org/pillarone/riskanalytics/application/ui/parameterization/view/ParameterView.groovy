package org.pillarone.riskanalytics.application.ui.parameterization.view

import com.canoo.ulc.community.fixedcolumntabletree.server.ULCFixedColumnTableTree
import com.canoo.ulc.detachabletabbedpane.server.ITabListener
import com.canoo.ulc.detachabletabbedpane.server.TabEvent
import com.canoo.ulc.detachabletabbedpane.server.ULCCloseableTabbedPane
import com.ulcjava.base.application.event.ISelectionChangedListener
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.event.SelectionChangedEvent
import com.ulcjava.base.application.tabletree.ITableTreeCellEditor
import com.ulcjava.base.application.tabletree.ITableTreeCellRenderer
import com.ulcjava.base.application.tabletree.ULCTableTreeColumn
import com.ulcjava.base.application.tree.TreePath
import com.ulcjava.base.application.util.KeyStroke
import org.pillarone.riskanalytics.application.ui.base.view.AbstractModellingTreeView
import org.pillarone.riskanalytics.application.ui.base.view.ComponentNodeTableTreeNodeRenderer
import org.pillarone.riskanalytics.application.ui.base.view.DelegatingCellEditor
import org.pillarone.riskanalytics.application.ui.base.view.DelegatingCellRenderer
import org.pillarone.riskanalytics.application.ui.comment.action.InsertCommentAction
import org.pillarone.riskanalytics.application.ui.comment.action.ShowCommentsAction
import org.pillarone.riskanalytics.application.ui.comment.model.CommentFilter
import org.pillarone.riskanalytics.application.ui.comment.view.CommentAndErrorView
import org.pillarone.riskanalytics.application.ui.comment.view.NavigationListener
import org.pillarone.riskanalytics.application.ui.main.action.AddDynamicSubComponent
import org.pillarone.riskanalytics.application.ui.main.action.RemoveDynamicSubComponent
import org.pillarone.riskanalytics.application.ui.parameterization.action.MultiDimensionalTabStarter
import org.pillarone.riskanalytics.application.ui.util.DataTypeFactory
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.simulation.item.IModellingItemChangeListener
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import com.ulcjava.base.application.*
import org.pillarone.riskanalytics.application.ui.base.action.*
import org.pillarone.riskanalytics.application.ui.parameterization.model.*

class ParameterView extends AbstractModellingTreeView implements NavigationListener {

    ULCTabbedPane tabbedPane
    CommentAndErrorView commentAndErrorView
    ULCSplitPane splitPane
    static double DIVIDER = 0.65
    static double NO_DIVIDER = 1.0
    def commentFilters

    ParameterView(ParameterViewModel model) {
        super(model)
        model.addNavigationListener this
        commentFilters = [:]
    }

    protected void initTree() {

        def treeModel = model.treeModel

        int treeWidth = UIUtils.calculateTreeWidth(treeModel.root)
        def columnsWidths = Math.max(UIUtils.calculateColumnWidth(treeModel.root, 1) + 10, 150)

        tree = new ULCFixedColumnTableTree(model.treeModel, 1, ([treeWidth] + [columnsWidths] * model.periodCount) as int[])


        tree.viewPortTableTree.name = "parameterTreeContent"
        tree.viewPortTableTree.columnModel.getColumns().eachWithIndex {ULCTableTreeColumn it, int index ->

            it.setCellEditor(new DelegatingCellEditor(createEditorConfiguration()))
            it.setCellRenderer(new DelegatingCellRenderer(createRendererConfiguration(index + 1, tree.viewPortTableTree)))
            it.setHeaderRenderer(new CenteredHeaderRenderer())
        }
        ComponentNodeTableTreeNodeRenderer renderer = new ComponentNodeTableTreeNodeRenderer(tree, model, commentAndErrorView)


        tree.rowHeaderTableTree.columnModel.getColumns().each {ULCTableTreeColumn it ->
            it.setCellRenderer(renderer)
            it.setHeaderRenderer(new CenteredHeaderRenderer())
        }

        tree.rowHeaderTableTree.name = "parameterTreeRowHeader"
        tree.rowHeaderTableTree.columnModel.getColumn(0).headerValue = "Name"
        tree.cellSelectionEnabled = true
        // TODO (Mar 20, 2009, msh): Identified this as cause for PMO-240 (expand behaviour).

        tree.viewPortTableTree.addActionListener(new MultiDimensionalTabStarter(this))


        tree.getRowHeaderTableTree().expandPaths([new TreePath([model.treeModel.root] as Object[])] as TreePath[], false);
        commentAndErrorView.tableTree = tree
        new SelectionTracker(tree)
    }

    private Map createEditorConfiguration() {
        DefaultCellEditor defaultEditor = new DefaultCellEditor(new ULCTextField());
        DefaultCellEditor doubleEditor = new BasicCellEditor(DataTypeFactory.getDoubleDataTypeForEdit());
        DefaultCellEditor integerEditor = new BasicCellEditor(DataTypeFactory.getIntegerDataTypeForEdit());
        DefaultCellEditor dateEditor = new BasicCellEditor(DataTypeFactory.getDateDataType());

        ComboBoxCellComponent comboBoxEditor = new ComboBoxCellComponent();
        CheckBoxCellComponent checkBoxEditor = new CheckBoxCellComponent();

        Map editors = new HashMap<Class, ITableTreeCellEditor>();
        editors.put(SimpleValueParameterizationTableTreeNode.class,
                defaultEditor);
        editors.put(DoubleTableTreeNode.class,
                doubleEditor);
        editors.put(BooleanTableTreeNode.class, checkBoxEditor);
        editors.put(IntegerTableTreeNode.class,
                integerEditor);
        editors.put(DateParameterizationTableTreeNode.class,
                dateEditor);
        editors.put(EnumParameterizationTableTreeNode.class,
                comboBoxEditor);
        editors.put(ParameterizationClassifierTableTreeNode.class,
                comboBoxEditor);
        editors.put(ConstrainedStringParameterizationTableTreeNode.class,
                comboBoxEditor);

        return editors
    }

    private Map createRendererConfiguration(int columnIndex, ULCTableTree tree) {
        BasicCellRenderer defaultRenderer = new BasicCellRenderer(columnIndex);
        MultiDimensionalCellRenderer mdpRenderer = new MultiDimensionalCellRenderer(columnIndex);
        BasicCellRenderer doubleRenderer = new BasicCellRenderer(columnIndex, DataTypeFactory.getDoubleDataTypeForNonEdit());
        BasicCellRenderer integerRenderer = new BasicCellRenderer(columnIndex, DataTypeFactory.getIntegerDataTypeForNonEdit());
        BasicCellRenderer dateRenderer = new BasicCellRenderer(columnIndex, DataTypeFactory.getDateDataType());
        ComboBoxCellComponent comboBoxRenderer = new ComboBoxCellComponent();
        CheckBoxCellComponent checkBoxRenderer = new CheckBoxCellComponent();

        ULCPopupMenu menu = new ULCPopupMenu();
        ULCPopupMenu mdpMenu = new ULCPopupMenu();
        mdpMenu.add(new ULCMenuItem(new OpenMDPAction(tree)))

        TableTreeCopier copier = new TableTreeCopier();
        copier.setTable(tree);
        menu.add(new ULCMenuItem(copier));
        mdpMenu.add(new ULCMenuItem(copier));
        TreeNodePaster paster = new TreeNodePaster();
        paster.setTree(tree);
        menu.add(new ULCMenuItem(paster));
        mdpMenu.add(new ULCMenuItem(paster));
        InsertCommentAction insertComment = new InsertCommentAction(tree, (columnIndex - 1) % model.periodCount)
        insertComment.addCommentListener commentAndErrorView
        ShowCommentsAction showCommentsAction = new ShowCommentsAction(tree, (columnIndex - 1) % model.periodCount, false)
        showCommentsAction.addCommentListener commentAndErrorView

        mdpMenu.addSeparator()
        mdpMenu.add(new ULCMenuItem(insertComment))
        mdpMenu.add(new ULCMenuItem(showCommentsAction))

        menu.addSeparator()
        menu.add(new ULCMenuItem(insertComment))
        menu.add(new ULCMenuItem(showCommentsAction))

        defaultRenderer.setMenu(menu)
        doubleRenderer.setMenu(menu)
        integerRenderer.setMenu(menu)
        dateRenderer.setMenu(menu)
        initComboBox(comboBoxRenderer, menu);
        initCheckBox(checkBoxRenderer, menu);
        mdpRenderer.setMenu(mdpMenu)

        Map renderers = new HashMap<Class, ITableTreeCellRenderer>();
        renderers.put(SimpleValueParameterizationTableTreeNode.class,
                defaultRenderer);
        renderers.put(DoubleTableTreeNode.class,
                doubleRenderer);
        renderers.put(BooleanTableTreeNode.class, checkBoxRenderer);
        renderers.put(IntegerTableTreeNode.class,
                integerRenderer);
        renderers.put(DateParameterizationTableTreeNode.class,
                dateRenderer);
        renderers.put(EnumParameterizationTableTreeNode.class,
                comboBoxRenderer);
        renderers.put(ParameterizationClassifierTableTreeNode.class,
                comboBoxRenderer);
        renderers.put(ConstrainedStringParameterizationTableTreeNode.class,
                comboBoxRenderer);
        renderers.put(MultiDimensionalParameterizationTableTreeNode.class,
                mdpRenderer);

        return renderers
    }

    private void initRenderer(ULCLabel renderer, ULCPopupMenu menu) {
        renderer.setHorizontalAlignment(ULCLabel.RIGHT);
        renderer.setComponentPopupMenu(menu);
    }

    private void initComboBox(ULCComboBox renderer, ULCPopupMenu menu) {
        renderer.setComponentPopupMenu(menu);
    }

    private void initCheckBox(ULCCheckBox renderer, ULCPopupMenu menu) {
        renderer.setComponentPopupMenu(menu);
    }

    protected void initComponents() {
        commentAndErrorView = new CommentAndErrorView(model)
        tabbedPane = new ULCCloseableTabbedPane(name: 'tabbedPane')
        tabbedPane.tabPlacement = ULCTabbedPane.TOP
        tabbedPane.addTabListener([tabClosing: {TabEvent event ->
            event.getClosableTabbedPane().closeCloseableTab(event.getTabClosingIndex())
            event.getClosableTabbedPane().selectedIndex = 0
        }] as ITabListener)
        tabbedPane.addSelectionChangedListener([selectionChanged: { SelectionChangedEvent event ->
            if (commentFilters) {
                ULCTabbedPane tabbedPane = (ULCTabbedPane) event.getSource();
                int selection = tabbedPane.getSelectedIndex();
                model?.tabbedPaneChanged(commentFilters[selection])
                showHiddenCommentToolBar(selection)
            }
        }] as ISelectionChangedListener)


        super.initComponents();
        attachListeners()
        updateErrorVisualization(model.item)
    }

    //TODO: remove listener after closing view

    protected void attachListeners() {
        def rowHeaderTree = tree.getRowHeaderTableTree()
        rowHeaderTree.registerKeyboardAction(new RemoveDynamicSubComponent(tree.rowHeaderTableTree, model), KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true), ULCComponent.WHEN_FOCUSED)
        rowHeaderTree.registerKeyboardAction(new AddDynamicSubComponent(tree.rowHeaderTableTree, model), KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, true), ULCComponent.WHEN_FOCUSED)
        rowHeaderTree.registerKeyboardAction(new TreeNodeRename(tree.rowHeaderTableTree, model), KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, true), ULCComponent.WHEN_FOCUSED)
        rowHeaderTree.registerKeyboardAction(new TreeExpander(tree), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK, false), ULCComponent.WHEN_FOCUSED)
        rowHeaderTree.registerKeyboardAction(new TreeCollapser(tree), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK, false), ULCComponent.WHEN_FOCUSED)
        rowHeaderTree.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK))
        rowHeaderTree.registerKeyboardAction(ctrlaction, KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), ULCComponent.WHEN_FOCUSED)

        def parameterization = model.getItem() as Parameterization
        parameterization.addModellingItemChangeListener([itemSaved: {item ->},
                itemChanged: {Parameterization item ->
                    updateErrorVisualization(item)
                }] as IModellingItemChangeListener)

        Closure closeSplitPane = {->
            int count = ((ULCCloseableTabbedPane) splitPane.getBottomComponent()).getTabCount()
            if (count == 1) {
                splitPane.setDividerLocation(NO_DIVIDER)
            }
        }
        commentAndErrorView.addPopupMenuListener(closeSplitPane)
    }

    protected void updateErrorVisualization(Parameterization item) {
        commentAndErrorView.updateErrorVisualization item
    }

    protected ULCContainer layoutContent(ULCContainer content) {
        ULCBoxPane contentPane = new ULCBoxPane(1, 1)
        splitPane = new ULCSplitPane(ULCSplitPane.VERTICAL_SPLIT)
        splitPane.oneTouchExpandable = true
        splitPane.setResizeWeight(1)
        splitPane.setDividerSize(10)

        splitPane.setDividerLocation(DIVIDER)
        contentPane.add(ULCBoxPane.BOX_EXPAND_EXPAND, splitPane)
        tabbedPane.removeAll()
        tabbedPane.addTab(model.treeModel.root.name, UIUtils.getIcon("treeview-active.png"), content)
        tabbedPane.setCloseableTab(0, false)
        splitPane.add(tabbedPane);
        splitPane.add(commentAndErrorView.tabbedPane)
        return splitPane
    }


    public void removeTabs() {
        int count = tabbedPane.getTabCount()
        for (int i = count - 1; i > 1; i--) {
            tabbedPane.closeCloseableTab(i)
        }
        tree.viewPortTableTree.getActionListeners().each {
            if (it instanceof MultiDimensionalTabStarter) {
                it.openTabs = [:]
            }
        }
    }

    public void showHiddenComments() {
        if ((NO_DIVIDER - splitPane.getDividerLocationRelative()) < 0.1)
            splitPane.setDividerLocation(DIVIDER)
        else
            splitPane.setDividerLocation(NO_DIVIDER)
    }

    public void showComments() {
        if ((NO_DIVIDER - splitPane.getDividerLocationRelative()) < 0.1) {
            splitPane.setDividerLocation(DIVIDER)
        }
    }

    public void addCommentFilter(int tabbedPaneIndex, CommentFilter filter) {
        commentFilters[tabbedPaneIndex] = filter
        model?.tabbedPaneChanged(filter)
        showHiddenCommentToolBar(tabbedPaneIndex)
    }

    private void showHiddenCommentToolBar(int tabbedPaneIndex) {
        commentAndErrorView.commentSearchPane.setVisible(tabbedPaneIndex <= 1)
    }

}





