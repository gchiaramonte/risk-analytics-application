package org.pillarone.riskanalytics.application.ui.result.model

import com.ulcjava.base.application.tabletree.ITableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModelStructure
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.output.*

class DeterministicResultViewModel extends AbstractResultViewModel {

    DeterministicResultViewModel(Model model, ModelStructure structure, Simulation simulation) {
        super(model, simulation, structure)
    }

    protected ITableTreeModel getResultTreeTableModel(Model model, Parameterization parameterization, simulationRun, ITableTreeNode treeRoot, ConfigObject results) {
        return new DeterministicResultTableTreeModel(treeRoot, simulationRun, parameterization, results)
    }

    @Override
    protected Map<String, ICollectingModeStrategy> obtainsCollectors(SimulationRun simulationRun, List allPaths) {
        return ResultViewUtils.obtainsCollectors(simulationRun, allPaths)
    }

    @Override
    protected ConfigObject initPostSimulationCalculations(SimulationRun simulationRun) {
        ConfigObject results = new ConfigObject()

        List<Object[]> calculations = PostSimulationCalculation.executeQuery("SELECT period, path.pathName, field.fieldName, result FROM org.pillarone.riskanalytics.core.output.PostSimulationCalculation " +
                " WHERE run.id = ?", [simulationRun.id])
        for (Object[] psc in calculations) {
            Map periodMap = results[psc[0].toString()]
            Map pathMap = periodMap[psc[1]]
            Map fieldMap = pathMap[psc[2]]
            fieldMap[PostSimulationCalculation.MEAN] = psc[3]
        }

        return results
    }

    @Override
    protected void recreateAllColumns() {
        //TODO
    }
}
