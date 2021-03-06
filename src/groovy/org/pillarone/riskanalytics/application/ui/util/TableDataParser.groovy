package org.pillarone.riskanalytics.application.ui.util

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.ui.base.action.CopyPasteColumnMapping
import org.pillarone.riskanalytics.application.ui.base.action.TablePasterHelper
import org.pillarone.riskanalytics.application.util.LocaleResources

@CompileStatic
public class TableDataParser {

    Locale locale
    String columnSeparator = '\t'
    String lineSeparator = '\n'

    CopyPasteColumnMapping columnMapping


    List parseTableData(String stringData, int offset = 0) {
        if (locale == null) {
            locale = LocaleResources.locale
        }
        TablePasterHelper pasterHelper = new TablePasterHelper(locale, stringData)

        String[] lineStrings = stringData.split(lineSeparator)
        List lines = new ArrayList(lineStrings.length)
        lineStrings.each { String lineString ->
            String[] entries = lineString.split(columnSeparator)
            List line = []
            entries.eachWithIndex { String value, int index ->
                line << pasterHelper.fromString(value, columnMapping.getColumnType(index + offset))
            }
            lines << line
        }

        return lines
    }
}