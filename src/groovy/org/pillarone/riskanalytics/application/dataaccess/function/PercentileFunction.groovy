package org.pillarone.riskanalytics.application.dataaccess.function

import org.pillarone.riskanalytics.application.ui.result.model.ResultTableTreeNode
import org.pillarone.riskanalytics.core.dataaccess.ResultAccessor
import org.pillarone.riskanalytics.core.output.PostSimulationCalculation
import org.pillarone.riskanalytics.core.output.QuantilePerspective
import org.pillarone.riskanalytics.core.output.SimulationRun

class PercentileFunction extends AbstractQuantilePerspectiveBasedFunction<Double> {

    public static final String PERCENTILE = "Percentile"

    private double percentile

    PercentileFunction(double percentile, QuantilePerspective quantilePerspective) {
        super(quantilePerspective)
        this.percentile = percentile
    }

    Double getParameter() {
        return percentile
    }

    String getName() {
        return PERCENTILE
    }

    double evaluateResult(SimulationRun simulationRun, int periodIndex, ResultTableTreeNode node) {
        return ResultAccessor.getPercentile(simulationRun, periodIndex, node.path, node.collector, node.field, percentile, quantilePerspective)
    }

    @Override
    String getKeyFigureName() {
        return quantilePerspective == QuantilePerspective.LOSS ? PostSimulationCalculation.PERCENTILE : PostSimulationCalculation.PERCENTILE_PROFIT
    }


}
