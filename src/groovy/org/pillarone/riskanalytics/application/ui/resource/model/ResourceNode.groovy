package org.pillarone.riskanalytics.application.ui.resource.model

import com.ulcjava.base.application.ULCMenuItem
import com.ulcjava.base.application.ULCPopupMenu
import com.ulcjava.base.application.ULCTableTree
import grails.util.Holders
import org.pillarone.riskanalytics.application.UserContext
import org.pillarone.riskanalytics.application.ui.base.model.VersionedItemNode
import org.pillarone.riskanalytics.application.ui.main.action.*
import org.pillarone.riskanalytics.application.ui.main.action.workflow.StartWorkflowAction
import org.pillarone.riskanalytics.application.ui.main.view.item.ResourceUIItem
import org.pillarone.riskanalytics.core.workflow.Status

class ResourceNode extends VersionedItemNode {

    ResourceNode(ResourceUIItem abstractUIItem) {
        super(abstractUIItem, false)
    }

    @Override
    ResourceUIItem getAbstractUIItem() {
        super.itemNodeUIItem as ResourceUIItem
    }

    public Status getStatus() {
        this.itemNodeUIItem.item.status
    }

    @Override
    ULCPopupMenu getPopupMenu(ULCTableTree tree) {
        ULCPopupMenu parameterNodePopUpMenu = new ULCPopupMenu()
        parameterNodePopUpMenu.name = "resourceNodePopUpMenu"
        parameterNodePopUpMenu.add(new ULCMenuItem(new OpenItemAction(tree, this.itemNodeUIItem.mainModel)))
        parameterNodePopUpMenu.addSeparator()
        parameterNodePopUpMenu.add(new ULCMenuItem(new TagsAction(tree, this.itemNodeUIItem.mainModel)))
        parameterNodePopUpMenu.addSeparator()
        parameterNodePopUpMenu.add(new ULCMenuItem(new RenameAction(tree, this.itemNodeUIItem.mainModel)))
        parameterNodePopUpMenu.add(new ULCMenuItem(new SaveAsAction(tree, this.itemNodeUIItem.mainModel)))
        parameterNodePopUpMenu.add(new ULCMenuItem(new CreateNewMajorVersion(tree, this.itemNodeUIItem.mainModel)))
        if (UserContext.hasCurrentUser()) {
            def transactionsEnabled = Holders.grailsApplication.config.getProperty("transactionsEnabled")
            if (transactionsEnabled != null && transactionsEnabled) {
                parameterNodePopUpMenu.addSeparator()
                parameterNodePopUpMenu.add(new ULCMenuItem(new ChooseDealAction(tree, this.itemNodeUIItem.mainModel)))
                parameterNodePopUpMenu.add(new ULCMenuItem(new StartWorkflowAction(tree, this.itemNodeUIItem.mainModel)))
            }
        }
        parameterNodePopUpMenu.addSeparator()
        parameterNodePopUpMenu.add(new ULCMenuItem(new DeleteAction(tree, this.itemNodeUIItem.mainModel)))
        return parameterNodePopUpMenu
    }
}
