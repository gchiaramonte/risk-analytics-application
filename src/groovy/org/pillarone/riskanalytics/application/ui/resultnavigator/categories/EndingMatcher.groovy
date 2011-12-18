package org.pillarone.riskanalytics.application.ui.resultnavigator.categories

/**
 * @author martin.melchior
 */
class EndingMatcher implements ICategoryMatcher {
    static final String NAME = "ByEnding"
    String prefix

    EndingMatcher(String prefix) {
        this.prefix = prefix
    }

    String getName() {
        return NAME
    }

    boolean isMatch(String path) {
        int index = path.indexOf(prefix)
        return index>=0 && index+prefix.length() < path.length()-1
    }

    String getMatch(String path) {
        int index = path.indexOf(prefix)+prefix.length()
        if (index>=0 && index < path.length()-1) {
            return path[index..-1]
        }
        return null
    }
}