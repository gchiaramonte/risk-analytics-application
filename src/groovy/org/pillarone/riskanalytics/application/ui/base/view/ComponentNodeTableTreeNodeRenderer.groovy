package org.pillarone.riskanalytics.application.ui.base.view

import com.ulcjava.base.application.IRendererComponent
import com.ulcjava.base.application.ULCMenuItem
import com.ulcjava.base.application.ULCPopupMenu
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.tabletree.DefaultTableTreeCellRenderer
import com.ulcjava.base.application.util.Color
import com.ulcjava.base.application.util.Font
import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.application.ui.base.action.OpenComponentHelp
import org.pillarone.riskanalytics.application.ui.base.action.TreeExpander
import org.pillarone.riskanalytics.application.ui.base.action.TreeNodeDuplicator
import org.pillarone.riskanalytics.application.ui.base.action.TreeNodeRename
import org.pillarone.riskanalytics.application.ui.base.model.ComponentTableTreeNode
import org.pillarone.riskanalytics.application.ui.main.action.AddDynamicSubComponent
import org.pillarone.riskanalytics.application.ui.main.action.RemoveDynamicSubComponent
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationTableTreeNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationUtilities
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent

class ComponentNodeTableTreeNodeRenderer extends DefaultTableTreeCellRenderer {

    protected ULCPopupMenu addDynamicNodeMenu
    protected ULCPopupMenu removeDynamicNodeMenu
    protected ULCPopupMenu expandTreeMenu
    protected ULCPopupMenu expandTreeMenuWithHelp


    public ComponentNodeTableTreeNodeRenderer(tree, model) {
        addContextMenu(tree, model)

    }

    protected ULCMenuItem addContextMenu(tree, model) {
        OpenComponentHelp help = new OpenComponentHelp(tree.rowHeaderTableTree)

        addDynamicNodeMenu = new ULCPopupMenu()
        addDynamicNodeMenu.add(new ULCMenuItem(new AddDynamicSubComponent(tree.rowHeaderTableTree, model)))
        addDynamicNodeMenu.add(new ULCMenuItem(new TreeExpander(tree)))
        //todo remove the context menu item as long as the functionality is not available.
//        addDynamicNodeMenu.add(new ULCMenuItem(new TreeNodeCopier(rowHeaderTree: tree.getRowHeaderTableTree(), viewPortTree: tree.getViewPortTableTree(), model: model.treeModel)))
        addDynamicNodeMenu.addSeparator()
        addDynamicNodeMenu.add(new ULCMenuItem(help))
        addDynamicNodeMenu.name = "popup.expand"

        removeDynamicNodeMenu = new ULCPopupMenu()
        removeDynamicNodeMenu.add(new ULCMenuItem(new TreeExpander(tree)))
        //todo remove the context menu item as long as the functionality is not available.
//        removeDynamicNodeMenu.add(new ULCMenuItem(new TreeNodeCopier(rowHeaderTree: tree.getRowHeaderTableTree(), viewPortTree: tree.getViewPortTableTree(), model: model.treeModel)))
        removeDynamicNodeMenu.add(new ULCMenuItem(new TreeNodeDuplicator(tree.rowHeaderTableTree, model)))
        removeDynamicNodeMenu.add(new ULCMenuItem(new TreeNodeRename(tree.rowHeaderTableTree, model)))
        removeDynamicNodeMenu.addSeparator()
        removeDynamicNodeMenu.add(new ULCMenuItem(new RemoveDynamicSubComponent(tree.rowHeaderTableTree, model)))
        removeDynamicNodeMenu.addSeparator()
        removeDynamicNodeMenu.add(new ULCMenuItem(help))

        expandTreeMenu = new ULCPopupMenu()
        expandTreeMenu.name = "popup.expand"
        expandTreeMenu.add(new ULCMenuItem(new TreeExpander(tree)))
//        expandTreeMenu.add(new ULCMenuItem(new TreeNodeCopier(rowHeaderTree: tree.getRowHeaderTableTree(), viewPortTree: tree.getViewPortTableTree(), model: model.treeModel)))

        expandTreeMenuWithHelp = new ULCPopupMenu()
        expandTreeMenuWithHelp.name = "popup.expand"
        expandTreeMenuWithHelp.add(new ULCMenuItem(new TreeExpander(tree)))
//        expandTreeMenuWithHelp.add(new ULCMenuItem(new TreeNodeCopier(rowHeaderTree: tree.getRowHeaderTableTree(), viewPortTree: tree.getViewPortTableTree(), model: model.treeModel)))
        //expandTreeMenuWithHelp.addSeparator()
        expandTreeMenuWithHelp.add(new ULCMenuItem(help))
    }


    public IRendererComponent getTableTreeCellRendererComponent(ULCTableTree tableTree, Object value, boolean selected, boolean hasFocus, boolean expanded, boolean leaf, Object node) {
        IRendererComponent component = super.getTableTreeCellRendererComponent(tableTree, value, selected, hasFocus, expanded, leaf, node)
        setPopupMenu(component, node)
        customizeNode(component, node)
        return component

    }

    void setPopupMenu(IRendererComponent rendererComponent, def node) {
        rendererComponent.setComponentPopupMenu(node.leaf ? null : expandTreeMenu)
    }

    void customizeNode(IRendererComponent rendererComponent, def node) {
    }

    void customizeNode(IRendererComponent rendererComponent, ParameterizationTableTreeNode node) {
        if (node.errorMessage == null) {
            setForeground(Color.black)
            setToolTipText(null)
            setFont(getFont().deriveFont(Font.PLAIN))
        } else {
            setForeground(Color.red)
            setToolTipText(node.errorMessage)
            setFont(getFont().deriveFont(Font.BOLD))
        }
    }

    void setPopupMenu(IRendererComponent rendererComponent, ComponentTableTreeNode node) {
        if (node.component instanceof DynamicComposedComponent) {
            rendererComponent.setComponentPopupMenu(addDynamicNodeMenu)
        } else if (ComponentUtils.isDynamicComposedSubComponentNode(node)) {
            rendererComponent.setComponentPopupMenu(removeDynamicNodeMenu)
        } else {
            rendererComponent.setComponentPopupMenu(expandTreeMenuWithHelp)
        }
    }


}




class CompareComponentNodeTableTreeNodeRenderer extends ComponentNodeTableTreeNodeRenderer {

    public CompareComponentNodeTableTreeNodeRenderer(tree, model) {
        super(tree, model)
    }

    protected ULCMenuItem addContextMenu(Object tree, Object model) {
        OpenComponentHelp help = new OpenComponentHelp(tree.rowHeaderTableTree)

        addDynamicNodeMenu = new ULCPopupMenu()
        addDynamicNodeMenu.add(new ULCMenuItem(new TreeExpander(tree)))

        addDynamicNodeMenu.addSeparator()
        addDynamicNodeMenu.add(new ULCMenuItem(help))
        addDynamicNodeMenu.name = "popup.expand"

        removeDynamicNodeMenu = new ULCPopupMenu()
        removeDynamicNodeMenu.add(new ULCMenuItem(new TreeExpander(tree)))

        removeDynamicNodeMenu.addSeparator()
        removeDynamicNodeMenu.add(new ULCMenuItem(help))

        expandTreeMenu = new ULCPopupMenu()
        expandTreeMenu.name = "popup.expand"
        expandTreeMenu.add(new ULCMenuItem(new TreeExpander(tree)))

        expandTreeMenuWithHelp = new ULCPopupMenu()
        expandTreeMenuWithHelp.name = "popup.expand"
        expandTreeMenuWithHelp.add(new ULCMenuItem(new TreeExpander(tree)))

        expandTreeMenuWithHelp.add(new ULCMenuItem(help))
    }



    public IRendererComponent getTableTreeCellRendererComponent(ULCTableTree tableTree, Object value, boolean selected, boolean hasFocus, boolean expanded, boolean leaf, Object node) {
        setBackground(tableTree, node)
        IRendererComponent component = super.getTableTreeCellRendererComponent(tableTree, value, selected, hasFocus, expanded, leaf, node)
        setPopupMenu(component, node)
        return component

    }

    private void setBackground(ULCTableTree tableTree, Object node) {
        (tableTree.model.isDifferent(node)) ? setBackground(ParameterizationUtilities.ERROR_BG) : setBackground(Color.white)
    }
}

class CompareParameterizationRenderer extends DefaultTableTreeCellRenderer {

    public CompareParameterizationRenderer() {
    }

    public IRendererComponent getTableTreeCellRendererComponent(ULCTableTree tableTree, Object value, boolean selected, boolean hasFocus, boolean expanded, boolean leaf, Object node) {
        setBackground(tableTree, node)
        IRendererComponent component = super.getTableTreeCellRendererComponent(tableTree, value, selected, hasFocus, expanded, leaf, node)
        return component
    }

    private void setBackground(ULCTableTree tableTree, Object node) {
        (tableTree.model.isDifferent(node)) ? setBackground(ParameterizationUtilities.ERROR_BG) : setBackground(Color.white)
    }


}




class ComponentUtils {

    static boolean isDynamicComposedSubComponentNode(def node) {
        if (node instanceof ComponentTableTreeNode)
            return node.parent instanceof ComponentTableTreeNode && node.parent.component instanceof DynamicComposedComponent && node.parent.component.isDynamicSubComponent(node.component)
        return false
    }

    static boolean isDynamicComposedComponent(def node) {
        return (node instanceof ComponentTableTreeNode) && (node.component instanceof DynamicComposedComponent)
    }

    static String getSubComponentName(String name) {
        if (name.length() != 0) {
            List tokens = name.split(" ") as List
            if (tokens.get(0) != 'sub') {
                tokens.add(0, "sub")
            }
            name = StringUtils.uncapitalize(tokens.collect { StringUtils.capitalize(it)}.join(""))
        }
        return name
    }
}
