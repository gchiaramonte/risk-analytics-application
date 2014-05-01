package org.pillarone.riskanalytics.application.ui.batch.view.action

import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tree.TreePath
import grails.util.Holders
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.base.model.TableTreeBuilderUtils
import org.pillarone.riskanalytics.application.ui.batch.view.BatchView
import org.pillarone.riskanalytics.application.ui.main.view.SelectionTreeView

class FindParameterizationsInTreeAction extends ResourceBasedAction {
    private final BatchView batchView

    FindParameterizationsInTreeAction(BatchView batchView) {
        super('FindParameterizationsInTree')
        this.batchView = batchView
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        List<ItemNode> nodes = (batchView.selectedBatchRowInfos.parameterization.collect {
            TableTreeBuilderUtils.findNodeForItem(selectionTreeView.root, it)
        } - [null]) as List<ItemNode>
        if (nodes) {
            TreePath[] paths = nodes.collect {
                new TreePath(DefaultTableTreeModel.getPathToRoot(it) as Object[])
            }
            TreePath[] parents = nodes.collect {
                new TreePath(DefaultTableTreeModel.getPathToRoot(it.parent) as Object[])
            }
            selectionTreeView.selectionTree.collapseAll()
            selectionTreeView.selectionTree.expandPaths(parents, false)
            selectionTreeView.selectionTree.selectionModel.selectionPaths = paths
        }
    }

    SelectionTreeView getSelectionTreeView() {
        Holders.grailsApplication.mainContext.getBean('selectionTreeView', SelectionTreeView)
    }

    @Override
    boolean isEnabled() {
        batchView.selectedBatchRowInfos.size() > 0
    }
}