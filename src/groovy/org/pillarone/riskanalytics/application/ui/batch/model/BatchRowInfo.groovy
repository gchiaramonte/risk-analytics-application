package org.pillarone.riskanalytics.application.ui.batch.model

import com.google.common.base.Preconditions
import groovy.transform.CompileStatic
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.PeriodType
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.simulation.item.SimulationProfile

import static org.pillarone.riskanalytics.core.simulation.SimulationState.FINISHED

@CompileStatic
class BatchRowInfo {
    private final Parameterization parameterization
    private SimulationProfile simulationProfile
    private Simulation simulation
    String durationAsString

    BatchRowInfo(Parameterization parameterization) {
        this.parameterization = Preconditions.checkNotNull(parameterization)
    }

    String getName() {
        simulation ? simulation.nameAndVersion : parameterization.nameAndVersion
    }

    String getModelName() {
        parameterization.modelClass.simpleName
    }

    Class getModelClass() {
        parameterization.modelClass
    }

    String getTemplateName() {
        simulationProfile?.template?.nameAndVersion ?: ''
    }

    String getPeriodIterationAsString() {
        if (simulation) {
            return "${simulation.periodCount}/${simulation.numberOfIterations}"
        }
        if (simulationProfile) {
            return "${parameterization.periodCount}/${simulationProfile.numberOfIterations}"
        }
        return ''
    }

    String getRandomSeed() {
        simulation ? simulation.randomSeed : simulationProfile?.randomSeed ?: ''
    }

    String getSimulationStateAsString() {
        simulation ? simulation.simulationState : ''
    }

    Parameterization getParameterization() {
        return parameterization
    }

    String getParameterizationVersion() {
        parameterization.versionNumber.toString()
    }

    boolean isFinished() {
        simulation?.simulationState == FINISHED
    }

    boolean isValid() {
        simulationProfile || simulation
    }

    Simulation getSimulation() {
        return simulation
    }

    ResultConfiguration getTemplate() {
        if (simulation) {
            return simulation.template
        }
        if (simulationProfile) {
            return simulationProfile.template
        }
        return null
    }

    void setSimulationProfile(SimulationProfile simulationProfile) {
        this.simulationProfile = simulationProfile
    }

    private void updateDurationFromSimulation() {
        DateTime start = simulation?.start
        DateTime end = simulation?.end
        if (start && end) {
            Period period = new Period(start, end, PeriodType.minutes());
            durationAsString = "${period.minutes} min"
        }
    }

    void setSimulation(Simulation simulation) {
        this.simulation = simulation
        updateDurationFromSimulation()
    }
}
