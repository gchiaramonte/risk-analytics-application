package org.pillarone.riskanalytics.application.ui.resultnavigator.categories

import org.pillarone.riskanalytics.application.ui.resultnavigator.model.OutputElement

/**
 * @author martin.melchior
 */
class ConditionalAssignment implements ICategoryResolver {
    static final String NAME = "ByCondition"
    ICategoryResolver condition
    String value

    ConditionalAssignment(String value, ICategoryResolver condition) {
        this.value = value
        this.condition = condition
    }

    String getName() {
        return NAME
    }

    boolean isResolvable(OutputElement element) {
        return condition.isResolvable(element)
    }

    String getResolvedValue(OutputElement element) {
        return condition.isResolvable(element) ? value : null
    }

    boolean createTemplatePath(OutputElement element, String category) {
        return false
    }
}
