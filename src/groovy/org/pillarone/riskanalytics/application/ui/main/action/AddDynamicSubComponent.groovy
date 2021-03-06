package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.base.view.DynamicComponentNameDialog
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterViewModel
import org.pillarone.riskanalytics.application.ui.util.ComponentUtils
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.NonUniqueComponentNameException

class AddDynamicSubComponent extends ResourceBasedAction {

    def tree
    ParameterViewModel model

    public AddDynamicSubComponent(def tree, ParameterViewModel model) {
        super("AddDynamicSubComponent")
        this.tree = tree
        this.model = model
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, true));
    }


    public void doActionPerformed(ActionEvent event) {
        if (model.paramterTableTreeModel.readOnly) return
        def node = tree.selectedPath.lastPathComponent
        if (!node || !ComponentUtils.isDynamicComposedComponent(node)) return;

        if (isNotEditable(node)) {
            return
        }
        DynamicComponentNameDialog dialog = new DynamicComponentNameDialog(UlcUtilities.getWindowAncestor(tree))
        dialog.title = UIUtils.getText(this.class, "newDynamicSubComponent") + ": " + (node ? node.getDisplayName() : "dynamic component")
        dialog.okAction = {
            ExceptionSafe.protect {
                Component component = node.component.createDefaultSubComponent()
                String name = dialog.nameInput.text.trim()
                name = ComponentUtils.getSubComponentName(name)

                if (name.length() == 0 || !StringUtils.isAlphanumericSpace(name)) {
                    ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tree), "IllegalSubComponentName")
                    alert.show()
                    return
                }
                try {
                    component.name = name
                    model.parametrizedItem.addComponent([ComponentUtils.removeModelFromPath(node.path, model.model), name].join(":"), component)
                } catch (NonUniqueComponentNameException e) {
                    ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tree), "UniqueSubComponent")
                    alert.show()
                }
            }
        }
        dialog.show()
    }

    @Override
    String logContent() {
        "Adding component with name:"
    }

    public boolean isEnabled() {
        return super.isEnabled() && !model.paramterTableTreeModel.readOnly;
    }

    private boolean isNotEditable(def node) {
        def model = model.paramterTableTreeModel
        model.readOnly || !model.isNodeInEditablePaths(node)
    }
}
