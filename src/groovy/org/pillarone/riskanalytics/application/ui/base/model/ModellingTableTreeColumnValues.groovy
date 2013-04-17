package org.pillarone.riskanalytics.application.ui.base.model

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.core.parameter.ParameterizationTag
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.joda.time.DateTime

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class ModellingTableTreeColumnValues {

    public static List getValues(Map columnValues, int columnIndex) {
        Set values = new TreeSet()
        columnValues?.each {Parameterization parameterization, List value ->
            if (value[columnIndex]) {
                addValue(values, value[columnIndex]);
            }
        }
        return values as List
    }

    public static List getTagsValues() {
        Set values = new TreeSet()
        ParameterizationTag.withTransaction {status ->
            Collection all = ParameterizationTag.findAll()
            all.each {ParameterizationTag parametrizationTag ->
                String tagName = parametrizationTag.tag.name
                values.add(tagName)
            }
        }
        return values as List
    }

    private static void addValue(Set values, DateTime value) {
        values.add(ModellingInformationTableTreeModel.simpleDateFormat.print(value))
    }

    private static void addValue(Set values, Object value) {
        values.add(value);
    }


}
