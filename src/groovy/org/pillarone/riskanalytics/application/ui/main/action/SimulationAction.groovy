package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCTree
import com.ulcjava.base.application.event.ActionEvent
import org.pillarone.riskanalytics.application.ui.main.model.P1RATModel
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class SimulationAction extends SelectionTreeAction {

    Log LOG = LogFactory.getLog(SimulationAction)

    public SimulationAction(ULCTree tree, P1RATModel model) {
        super("RunSimulation", tree, model)
    }

    public void doActionPerformed(ActionEvent event) {
        Model selectedModel = getSelectedModel()
        if (selectedModel) {
            Object selectedItem = getSelectedItem()
            Simulation simulation = new Simulation("Simulation")
            simulation.parameterization = selectedItem instanceof Parameterization ? selectedItem : null
            simulation.template = selectedItem instanceof ResultConfiguration ? selectedItem : null
            model.openItem(selectedModel, simulation)
        }
        else {
            LOG.debug("No selected model found. Action cancelled.")
        }
    }

}
