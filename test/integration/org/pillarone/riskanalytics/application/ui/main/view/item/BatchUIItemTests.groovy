package org.pillarone.riskanalytics.application.ui.main.view.item

import models.core.CoreModel
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.fileimport.FileImportService
import org.pillarone.riskanalytics.core.output.OutputStrategy
import org.pillarone.riskanalytics.core.output.ResultConfigurationDAO
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.SimulationState
import org.pillarone.riskanalytics.core.simulation.item.Batch
import org.springframework.transaction.TransactionStatus

/**
 * @author fouad.jaada@intuitive-collaboration.com
 *
 */
class BatchUIItemTests extends AbstractUIItemTest {

    @Override
    AbstractUIItem createUIItem() {
        FileImportService.importModelsIfNeeded(["Core"])

        LocaleResources.testMode = true
        SimulationRun run
        BatchRun batchRun = null
        BatchRun.withTransaction { TransactionStatus status ->
            batchRun = new BatchRun(name: "test")
            batchRun.save(flush: true)
            ParameterizationDAO dao = ParameterizationDAO.list()[0]
            ResultConfigurationDAO configurationDAO = ResultConfigurationDAO.list()[0]
            run = new SimulationRun(name: "run")
            run.parameterization = dao
            run.resultConfiguration = configurationDAO
            run.model = CoreModel.name
            run.periodCount = 2
            run.iterations = 5
            run.randomSeed = 0
            run.strategy = OutputStrategy.NO_OUTPUT
            run.simulationState = SimulationState.NOT_RUNNING
            run.save(flush: true)
            batchRun.addToSimulationRuns(run)
            batchRun.save(flush: true)
        }
        Batch batch = new Batch(batchRun.name)
        batch.load()
        RiskAnalyticsMainModel mainModel = new RiskAnalyticsMainModel()
        new BatchUIItem(batch)
    }


    public void testView() {
        //todo fja
        //        assertEquals 1, tableOperator.rowCount
        //        assertEquals 8, tableOperator.columnCount
        //
        //        assertEquals "run", tableOperator.getValueAt(0, 0)
        //        assertEquals "CoreModel", tableOperator.getValueAt(0, 1)
        //        assertEquals "CoreAlternativeParameters v1", tableOperator.getValueAt(0, 2)
        //        assertEquals "CoreResultConfiguration v1", tableOperator.getValueAt(0, 3)
        //        assertEquals "2/5", tableOperator.getValueAt(0, 4)
        //        assertEquals 0, tableOperator.getValueAt(0, 5)
        //        assertEquals "No output", tableOperator.getValueAt(0, 6)
        //        assertEquals "not running", tableOperator.getValueAt(0, 7)
    }
}
