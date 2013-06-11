package org.pillarone.riskanalytics.application.ui.batch.action

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.batch.model.BatchDataTableModel
import org.pillarone.riskanalytics.core.output.SimulationRun

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
abstract class BatchSimulationSelectionAction extends ResourceBasedAction {
    protected BatchDataTableModel model

    public BatchSimulationSelectionAction(String actionName) {
        super(actionName);
    }

    SimulationRun getSelectedSimulationRun() {
        return model?.selectedRun
    }

}
