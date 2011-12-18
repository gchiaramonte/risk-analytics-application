package org.pillarone.riskanalytics.application.ui.resultnavigator.categories


/**
 * @author martin.melchior
 */
class CategoryUtils {

    static String getWordByMatchedEnclosing(String path, String prefix, String suffix) {
        return new EnclosingMatcher(prefix, suffix).getMatch(path)
    }

    static String getWordByUniqueMatch(String path, List<String> allMembers) {
        return new SingleValueFromListMatcher(allMembers).getMatch(path)
    }

    static String getWordAtEnd(String path, String endPrefix) {
        return new EndingMatcher(endPrefix).getMatch(path)
    }

    static String getWordAtStart(String path, String startSuffix) {
        int index = path.indexOf(startSuffix)
        if (index>=0) {
            return path[0..startSuffix]
        }
        return null
    }

    public static List<String> parseList(String str) {
        String[] array = str.split(",")
        List<String> values = []
        for (String x : array) {
            x = x.trim()
            values.add(x)
        }
        return values
    }

    static String writeList(List<String> list) {
        StringBuilder builder = new StringBuilder()
        for (String value : list) {
            builder.append( value + " , ")
        }
        String value = builder.toString()
        value = value.trim()
        if (value.endsWith(",")) {
            value = value[0..-2]
            value.trim()
        }
        return value
    }
}