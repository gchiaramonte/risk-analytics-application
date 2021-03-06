package org.pillarone.riskanalytics.application.ui.base.model

import com.ulcjava.base.application.tabletree.DefaultMutableTableTreeNode
import com.ulcjava.base.application.tabletree.ITableTreeNode
import org.pillarone.riskanalytics.application.ui.main.view.item.AbstractUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.BatchUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchRootNode
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class TableTreeBuilderUtils {

    public static ModelNode findModelNode(DefaultMutableTableTreeNode root, ModellingItem item) {
        return findModelNode(root, item.modelClass.name)
    }

    public static ModelNode findModelNode(DefaultMutableTableTreeNode root, ModellingUIItem abstractUIItem) {
        return findModelNode(root, abstractUIItem.item.modelClass.name)
    }

    public static ModelNode findModelNode(DefaultMutableTableTreeNode root, String modelClassName) {
        ModelNode modelNode = null
        for (int i = 0; i < root.childCount && modelNode == null; i++) {
            def candidate = root.getChildAt(i)
            if (candidate instanceof ModelNode) { //could be batch root node
                if (candidate.itemClass.name.equals(modelClassName)) {
                    modelNode = candidate
                }
            }
        }
        return modelNode
    }

    public static BatchRootNode findBatchRootNode(DefaultMutableTableTreeNode root) {
        for (int i = 0; i < root.childCount; i++) {
            DefaultMutableTableTreeNode candidate = root.getChildAt(i)
            if (candidate instanceof BatchRootNode) {
                return candidate
            }
        }
        return null
    }


    public static ItemGroupNode findGroupNode(ModellingUIItem modellingUIItem, ModelNode modelNode) {
        return findGroupNode(modellingUIItem.item.class, modelNode)
    }

    public static ItemGroupNode findGroupNode(ModellingItem item, ModelNode modelNode) {
        return findGroupNode(item.class, modelNode)
    }

    public static ItemGroupNode findGroupNode(Class itemClass, ModelNode modelNode) {
        DefaultMutableTableTreeNode groupNode = null
        for (int i = 0; i < modelNode.childCount && groupNode == null; i++) {
            INavigationTreeNode childNode = modelNode.getChildAt(i) as INavigationTreeNode
            if (childNode.itemClass == itemClass) {
                groupNode = childNode as DefaultMutableTableTreeNode
            }
        }
        groupNode
    }

    public static ResourceGroupNode findResourceGroupNode(ITableTreeNode root) {
        for (int i = 0; i < root.childCount; i++) {
            ITableTreeNode childNode = root.getChildAt(i)
            if (childNode instanceof ResourceGroupNode) {
                return childNode
            }
        }
        return null
    }

    public static ResourceClassNode findResourceItemGroupNode(ResourceGroupNode node, Class resourceClass) {
        for (int i = 0; i < node.childCount; i++) {
            ResourceClassNode childNode = node.getChildAt(i)
            if (childNode.resourceClass == resourceClass) {
                return childNode
            }
        }
        return null
    }

    public static ItemGroupNode findGroupNode(Parameterization item, ModelNode modelNode) {
        ItemGroupNode groupNode = null
        for (int i = 0; i < modelNode.childCount && groupNode == null; i++) {
            ItemGroupNode childNode = modelNode.getChildAt(i)
            if (childNode.itemClass == Parameterization) {
                groupNode = childNode
            }
        }
        return groupNode
    }

    public static ITableTreeNode findNodeForItem(ITableTreeNode searchStartNode, Object item) {
        if (isEqual(item, searchStartNode)) {
            return searchStartNode
        }

        ITableTreeNode nodeForItem = null

        for (int i = 0; i < searchStartNode.childCount && nodeForItem == null; i++) {
            ITableTreeNode childNode = searchStartNode.getChildAt(i)
            nodeForItem = findNodeForItem(childNode, item)
        }

        return nodeForItem
    }

    private static isEqual(def item, def node) {
        false
    }

    private static isEqual(ModellingItem item, ItemNode node) {
        item.equals(node.itemNodeUIItem.item)
    }

    private static isEqual(BatchUIItem item, BatchNode node) {
        item.equals(node.itemNodeUIItem)
    }

    static List<ITableTreeNode> findAllNodesForItem(ITableTreeNode node, Object item) {
        List<ITableTreeNode> allNodes = []
        ITableTreeNode nodeForItem = null
        if (isEqual(item, node)) {
            allNodes << node
        } else {
            for (int i = 0; i < node.childCount && nodeForItem == null; i++) {
                def childNode = node.getChildAt(i)
                allNodes.addAll(findAllNodesForItem(childNode, item))
            }
        }
        return allNodes
    }

    public static AbstractUIItem findUIItemForItem(ITableTreeNode root, ModellingItem item) {
        ModelNode modelNode1 = findModelNode(root, item)
        if (modelNode1) {
            ItemGroupNode itemGroupNode = findGroupNode(item, modelNode1)
            if (itemGroupNode) {
                ItemNode node = findNodeForItem(itemGroupNode, item)
                if (node) {
                    return node.itemNodeUIItem
                }
            }
        }
        return null
    }

}
