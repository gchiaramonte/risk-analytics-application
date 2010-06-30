package org.pillarone.riskanalytics.application.ui.simulation.view.impl

import org.pillarone.riskanalytics.application.ui.simulation.model.impl.SimulationActionsPaneModel
import org.pillarone.riskanalytics.core.simulation.SimulationState
import org.pillarone.riskanalytics.core.simulation.engine.actions.IterationAction
import org.pillarone.riskanalytics.core.simulation.engine.actions.PeriodAction
import org.pillarone.riskanalytics.core.simulation.engine.actions.SimulationAction
import static org.pillarone.riskanalytics.core.simulation.SimulationState.*
import org.pillarone.riskanalytics.core.simulation.engine.*

class MockSimulationRunner extends SimulationRunner {

    int count = 0
    boolean cancelled = false
    boolean stopped = false

    private List states = [INITIALIZING, RUNNING, RUNNING, RUNNING, SAVING_RESULTS, POST_SIMULATION_CALCULATIONS, FINISHED]
    private List progress = [0, 33, 66, 99, 0, 50, 100]
    private List dates = [null, new GregorianCalendar(2010, 6, 5).time, new GregorianCalendar(2010, 6, 5).time, new GregorianCalendar(2010, 6, 5).time, null, new GregorianCalendar(2010, 6, 5).time, new GregorianCalendar(2010, 6, 5).time]

    Date getEstimatedSimulationEnd() {
        return dates[count]
    }

    int getProgress() {
        return progress[count]
    }

    SimulationState getSimulationState() {
        if (cancelled) return CANCELED
        if (stopped) return STOPPED
        return states[count]
    }

    void start() {
    }

    MockSimulationRunner next() {
        count++
        return this
    }

    void cancel() {
        cancelled = true
    }

    void stop() {
        stopped = true
    }

    public static SimulationRunner createRunner() {

        PeriodScope periodScope = new PeriodScope()
        IterationScope iterationScope = new IterationScope(periodScope: periodScope)
        SimulationScope simulationScope = new SimulationScope(iterationScope: iterationScope)

        PeriodAction periodAction = new PeriodAction(periodScope: periodScope, model: simulationScope.model)
        IterationAction iterationAction = new IterationAction(periodAction: periodAction, iterationScope: iterationScope)
        SimulationAction simulationAction = new SimulationAction(iterationAction: iterationAction, simulationScope: simulationScope)

        SimulationRunner runner = new MockSimulationRunner()
        runner.simulationAction = simulationAction
        runner.currentScope = simulationScope

        return runner
    }


}

class TestActionPaneModel extends SimulationActionsPaneModel {

    def TestActionPaneModel(provider) {
        super(provider);
    }

    void runSimulation() {
        runner = MockSimulationRunner.createRunner()
        RunSimulationService.getService().runSimulation(runner, new SimulationConfiguration(simulation: simulation, outputStrategy: outputStrategy))
        notifySimulationStart()
    }


}
