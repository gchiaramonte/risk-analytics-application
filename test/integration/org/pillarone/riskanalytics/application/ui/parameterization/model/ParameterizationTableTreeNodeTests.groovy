package org.pillarone.riskanalytics.application.ui.parameterization.model

import models.application.ApplicationModel
import models.core.CoreModel
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.parameterization.SimpleMultiDimensionalParameter
import org.pillarone.riskanalytics.application.ui.base.model.ComponentTableTreeNode
import org.pillarone.riskanalytics.application.example.component.ExampleParameterComponent
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent
import org.pillarone.riskanalytics.core.example.component.ExampleOutputComponent
import org.pillarone.riskanalytics.core.example.marker.ITestComponentMarker
import org.pillarone.riskanalytics.core.example.parameter.ExampleEnum
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.parameterization.ConstrainedString
import org.pillarone.riskanalytics.core.parameterization.ParameterInjector
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolderFactory
import org.pillarone.riskanalytics.core.components.ComponentUtils

class ParameterizationTableTreeNodeTests extends GroovyTestCase {

    void setUp() {
        LocaleResources.setTestMode()
    }

    void tearDown() {
        LocaleResources.clearTestMode()
    }

    void testGetValueAt() {
        Model model = new CoreModel()

        List parameters = new ArrayList()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 1)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 2)
        def node = ParameterizationNodeFactory.getNode(parameters, model)

        assertEquals ComponentUtils.getNormalizedName("paramName"), node.getValueAt(0)
        assertEquals 1, node.getValueAt(1)
        assertEquals 2, node.getValueAt(2)

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 1.2)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 2.2)
        node = ParameterizationNodeFactory.getNode(parameters, model)

        assertEquals ComponentUtils.getNormalizedName("paramName"), node.getValueAt(0)
        assertEquals 1.2, node.getValueAt(1)
        assertEquals 2.2, node.getValueAt(2)

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 'text1')
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 'text2')
        node = ParameterizationNodeFactory.getNode(parameters, model)

        assertEquals ComponentUtils.getNormalizedName("paramName"), node.getValueAt(0)
        assertEquals 'text1', node.getValueAt(1)
        assertEquals 'text2', node.getValueAt(2)

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, ExampleEnum.FIRST_VALUE)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, ExampleEnum.SECOND_VALUE)
        node = ParameterizationNodeFactory.getNode(parameters, model)

        assertEquals ComponentUtils.getNormalizedName("paramName"), node.getValueAt(0)
        assertEquals 'FIRST_VALUE', node.getValueAt(1)
        assertEquals 'SECOND_VALUE', node.getValueAt(2)
    }

    void testSetValueAt() {
        Model model = new CoreModel()

        List parameters = new ArrayList()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 1)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 2)
        def node = ParameterizationNodeFactory.getNode(parameters, model)

        node.setValueAt(3, 1)
        node.setValueAt(4, 2)
        assertEquals 3, parameters[0].businessObject
        assertEquals 4, parameters[1].businessObject

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 1.2)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 2.2)
        node = ParameterizationNodeFactory.getNode(parameters, model)

        node.setValueAt(3.3, 1)
        node.setValueAt(4.4, 2)
        assertEquals 3.3, parameters[0].businessObject
        assertEquals 4.4, parameters[1].businessObject

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, 'text1')
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, 'text2')
        node = ParameterizationNodeFactory.getNode(parameters, model)

        node.setValueAt('text3', 1)
        node.setValueAt('text4', 2)
        assertEquals 'text3', parameters[0].businessObject
        assertEquals 'text4', parameters[1].businessObject

        parameters.clear()
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 0, ExampleEnum.FIRST_VALUE)
        parameters << ParameterHolderFactory.getHolder('path:to:paramName', 1, ExampleEnum.SECOND_VALUE)
        node = ParameterizationNodeFactory.getNode(parameters, model)
        node.parent = new ComponentTableTreeNode(new ExampleInputOutputComponent(), 'propName')

        node.setValueAt('SECOND_VALUE', 1)
        node.setValueAt('FIRST_VALUE', 2)
        assertEquals ExampleEnum.SECOND_VALUE, parameters[0].businessObject
        assertEquals ExampleEnum.FIRST_VALUE, parameters[1].businessObject
    }

    void testNullValues_PMO353() {
        def mdp = new SimpleMultiDimensionalParameter([1, 2, 3])
        def parameters = []
        parameters << null
        parameters << ParameterHolderFactory.getHolder('testPath', 0, mdp)

        def node = ParameterizationNodeFactory.getNode(parameters, new CoreModel())

        assertNull node.getValueAt(1)
        assertNotNull node.getValueAt(2)
    }

    void testConstrainedStringNode() {
        new ParameterizationImportService().compareFilesAndWriteToDB(["ApplicationParameters"])
        Model model = new ApplicationModel()
        model.init()
        model.injectComponentNames()

        Parameterization p = new Parameterization("ApplicationParameters")
        p.load()

        new ParameterInjector(p.toConfigObject()).injectConfiguration(model)

        ConstrainedStringParameterizationTableTreeNode node = ParameterizationNodeFactory.getNode([ParameterHolderFactory.getHolder("path", 0, new ConstrainedString(ITestComponentMarker, "Component Default"))], model)
        node.setParent(new ComponentTableTreeNode(null, "name"))

        //Test PMO-555: node must be editable and the value not null if it's initialized with a component default
        assertTrue node.isCellEditable(1)
        assertNotNull node.getValueAt(1)

        //ART-83
        String stringValue = node.parameter[0].businessObject.stringValue
        assertTrue node.values.contains(node.nameToNormalized.get(stringValue))

        assertNotNull node
        assertEquals 5, node.getValues().size()

        node.addComponent(new ExampleOutputComponent(name: "newExampleOutputComponent"))

        assertEquals 6, node.getValues().size()

        node.addComponent(new ExampleParameterComponent(name: "newExampleParameterComponent"))
        //Test PMO-540: check marker class when adding components
        assertEquals 6, node.getValues().size()
    }

    void testConstrainedStringNode_PMO1562() {
        new ParameterizationImportService().compareFilesAndWriteToDB(["ApplicationParameters"])
        Model model = new ApplicationModel()

        ConstrainedStringParameterizationTableTreeNode node = ParameterizationNodeFactory.getNode([ParameterHolderFactory.getHolder("path", 0, new ConstrainedString(ITestComponentMarker, "Component Default"))], model)
        node.setParent(new ComponentTableTreeNode(null, "name"))

        assertEquals "", node.parameter[0].businessObject.stringValue

        ExampleOutputComponent component = new ExampleOutputComponent(name: "example1")
        node.addComponent(component)

        assertEquals "example1", node.parameter[0].businessObject.stringValue

        ExampleOutputComponent component2 = new ExampleOutputComponent(name: "example2")
        node.addComponent(component2)
        assertEquals "example1", node.parameter[0].businessObject.stringValue

        node.setValueAt(ComponentUtils.getNormalizedName("example2"), 1)
        assertEquals "example2", node.parameter[0].businessObject.stringValue

        node.removeComponent(component2)
        assertEquals "example1", node.parameter[0].businessObject.stringValue

        node.removeComponent(component)
        assertEquals "", node.parameter[0].businessObject.stringValue


    }

}
