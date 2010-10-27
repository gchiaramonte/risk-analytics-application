package org.pillarone.riskanalytics.application.ui.main.action.workflow

import org.pillarone.riskanalytics.core.workflow.Status
import com.ulcjava.base.application.ULCTree
import org.pillarone.riskanalytics.application.ui.main.model.P1RATModel
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.user.UserManagement
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.UlcUtilities
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe


class StartWorkflowAction extends AbstractWorkflowAction {

    public StartWorkflowAction(String name, ULCTree tree, P1RATModel model) {
        super(name, tree, model);
    }

    public StartWorkflowAction(ULCTree tree, P1RATModel model) {
        super("StartWorkflow", tree, model);
    }

    void doActionPerformed(ActionEvent event) {
        DealLinkDialog dialog = new DealLinkDialog(UlcUtilities.getWindowAncestor(tree))
        Parameterization parameterization = getSelectedItem()
        Closure okAction = {
            ExceptionSafe.protect {
                if (!parameterization.isLoaded()) {
                    parameterization.load()
                }
                parameterization.dealId = dialog.dealSelectionModel.dealId
                super.doActionPerformed(event)
            }
        }
        if (parameterization.status == Status.NONE) {
            dialog.okAction = okAction
            dialog.show()
        } else {
            super.doActionPerformed(event)
        }

    }

    Status toStatus() {
        return Status.DATA_ENTRY
    }

    protected String requiredRole() {
        UserManagement.USER_ROLE
    }


}