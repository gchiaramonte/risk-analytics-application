package org.pillarone.riskanalytics.application.ui.base.model.modellingitem

import com.google.common.eventbus.Subscribe
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.event.ITableTreeModelListener
import com.ulcjava.base.application.event.TableTreeModelEvent
import com.ulcjava.base.application.tabletree.IMutableTableTreeNode
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.development.DevelopmentRunnerContainerServices
import com.ulcjava.base.server.ULCSession
import models.application.ApplicationModel
import org.joda.time.DateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.PollingSupport
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.main.eventbus.RiskAnalyticsEventBus
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.application.ui.search.ModellingItemCache
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.ModellingItemEvent
import org.pillarone.riskanalytics.application.ui.search.UlcCacheItemEventHandler
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.example.model.EmptyModel
import org.pillarone.riskanalytics.core.fileimport.FileImportService
import org.pillarone.riskanalytics.core.model.registry.ModelRegistry
import org.pillarone.riskanalytics.core.output.ResultConfigurationDAO
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.parameter.ParameterizationTag
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.search.CacheItemSearchService
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.workflow.Status

import static org.junit.Assert.*

class NavigationTableTreeModelTests {

    private NavigationTableTreeModel model
    private RiskAnalyticsEventBus riskAnalyticsEventBus
    private TestModelListener modelListener
    private TestPollingSupport testPollingSupport

    CacheItemSearchService cacheItemSearchService

    private ULCSession currentSession = new ULCSession('', new DevelopmentRunnerContainerServices())

    @Before
    void setUp() {
        new ULCSession.DefaultCurrentSessionRegistry().currentSession = currentSession
        cacheItemSearchService.cleanUp()
        cacheItemSearchService.init()

        ModellingItemFactory.clear()
        LocaleResources.testMode = true
        FileImportService.importModelsIfNeeded(['Application'])
        ModelRegistry.instance.clear()
        ModelRegistry.instance.addModel(ApplicationModel)
        newParameterization('Parametrization X', '1')
        newParameterization('Parametrization X', '1.2')
        newParameterization('Parametrization X', '1.3')
        newParameterization('Parametrization X', '1.4')
        newParameterization('Parametrization X', '1.4.1')
        newParameterization('Parametrization X', '2')
        newParameterization('Parametrization X', '3')
        newParameterization('Parametrization X', '4')
        newParameterization('Parametrization X', '5')
        newParameterization('Parametrization X', '6')
        newParameterization('Parametrization X', '7')
        newParameterization('Parametrization X', '8')
        newParameterization('Parametrization X', '9')
        newParameterization('Parametrization X', '10')
        newParameterization('Parametrization X', '11')
        cacheItemSearchService.refresh()
        riskAnalyticsEventBus = new RiskAnalyticsEventBus()
        NavigationTableTreeBuilder builder = new NavigationTableTreeBuilder()
        model = new NavigationTableTreeModel(cacheItemSearchService: cacheItemSearchService, navigationTableTreeBuilder: builder)
        model.initialize()
        modelListener = new TestModelListener()
        model.addTableTreeModelListener(modelListener)

        testPollingSupport = new TestPollingSupport()
        UlcCacheItemEventHandler queue = new UlcCacheItemEventHandler(cacheItemSearchService: cacheItemSearchService, pollingSupport: testPollingSupport)
        queue.init()
        ModellingItemCache modellingItemCache = new ModellingItemCache(ulcCacheItemEventHandler: queue, riskAnalyticsEventBus: riskAnalyticsEventBus)
        modellingItemCache.initialize()
        riskAnalyticsEventBus.register(this)
    }

    @Subscribe
    void updateModel(ModellingItemEvent event) {
        model.updateTreeStructure(event)
    }

    private void newParameterization(String name, String version) {
        ParameterizationDAO.withNewSession {
            new ParameterizationDAO(name: name, itemVersion: version,
                    modelClassName: 'models.application.ApplicationModel', periodCount: 1,
                    status: Status.NONE, creationDate: new DateTime(), modificationDate: new DateTime()).save(flush: true)
        }
    }

    @After
    void tearDown() {
        LocaleResources.testMode = false
        ModelRegistry.instance.clear()
        ModellingItemFactory.clear()
        riskAnalyticsEventBus.unregister(this)
        new ULCSession.DefaultCurrentSessionRegistry().currentSession = null
    }

    @Test
    void testColumnCount() {
        assert 8 == model.columnCount
    }

    @Test
    void testTreeStructure() {
        IMutableTableTreeNode root = model.root as IMutableTableTreeNode
        assert root
        assert 3 == root.childCount
    }

    @Test
    void testSimpleParamStructureWithTenNodes() {
        IMutableTableTreeNode modelNode = model.root.getChildAt(0) as IMutableTableTreeNode
        IMutableTableTreeNode paramsNode = modelNode.getChildAt(0) as IMutableTableTreeNode

        assertEquals 2, paramsNode.childCount
        ItemNode v11Node = paramsNode.getChildAt(1)
        assertEquals 'Parametrization X', v11Node.name

        assertEquals '11', v11Node.itemNodeUIItem.item.versionNumber.toString()
        assertEquals 10, v11Node.childCount

        ItemNode v1Node = v11Node.getChildAt(9)
        assertEquals '1', v1Node.itemNodeUIItem.item.versionNumber.toString()
        assertEquals 3, v1Node.childCount

        ItemNode v14Node = v1Node.getChildAt(0)
        assertEquals '1.4', v14Node.itemNodeUIItem.item.versionNumber.toString()
        assertEquals 1, v14Node.childCount

        ItemNode v141Node = v14Node.getChildAt(0)
        assertEquals '1.4.1', v141Node.itemNodeUIItem.item.versionNumber.toString()
    }

    @Test
    void testUpdateTreeStructure() {
        ParameterizationDAO.withNewSession {
            ParameterizationDAO parameterizationDAO = new ParameterizationDAO(name: 'Parametrization X', itemVersion: '12', modelClassName: 'models.application.ApplicationModel', periodCount: 1, status: Status.NONE)
            parameterizationDAO.save(flush: true)
        }
        testPollingSupport.poll()
        IMutableTableTreeNode modelNode = model.root.getChildAt(0) as IMutableTableTreeNode
        IMutableTableTreeNode paramsNode = modelNode.getChildAt(0) as IMutableTableTreeNode
        IMutableTableTreeNode resultsNode = modelNode.getChildAt(2) as IMutableTableTreeNode
        assertEquals 2, paramsNode.childCount
        ItemNode v12Node = paramsNode.getChildAt(1)
        assertEquals 'Parametrization X', v12Node.name

        assertEquals '12', v12Node.itemNodeUIItem.item.versionNumber.toString()

        ParameterizationDAO.withNewSession {
            ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndItemVersion('Parametrization X', '12')
            parameterizationDAO.status = Status.IN_REVIEW
            parameterizationDAO.save(flush: true)
        }
        testPollingSupport.poll()
        assertEquals(Status.IN_REVIEW.displayName, model.getValueAt(v12Node, 1))

        ParameterizationDAO.withNewSession {
            ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndItemVersion('Parametrization X', '12')
            parameterizationDAO.delete(flush: true)
        }
        testPollingSupport.poll()
        assertEquals(2, paramsNode.childCount)
        def v11Node = paramsNode.getChildAt(1)
        assertEquals '11', v11Node.itemNodeUIItem.item.versionNumber.toString()
        SimulationRun.withNewSession {
            SimulationRun run = new SimulationRun()
            run.parameterization = ParameterizationDAO.list()[0]
            run.resultConfiguration = ResultConfigurationDAO.list()[0]
            run.name = 'TestRun'
            run.startTime = new DateTime()
            run.endTime = new DateTime()
            run.model = 'models.application.ApplicationModel'
            run.save(flush: true)
        }
        testPollingSupport.poll()
        assertEquals(1, resultsNode.childCount)

    }

    @Test
    void testGetValueAt() {
        ITableTreeNode applicationNode = model.root.getChildAt(0)
        IMutableTableTreeNode paramsNode = applicationNode.getChildAt(0) as IMutableTableTreeNode
        assertEquals('Application', model.getValueAt(applicationNode, 0))
        assertEquals('Parameterization', model.getValueAt(paramsNode, 0))
        assertEquals('ApplicationParameters v1', model.getValueAt(paramsNode.getChildAt(0), 0))
    }

    @Test
    void testModel() {
        model.addNodeForItem(new EmptyModel())
        IMutableTableTreeNode modelNode = model.root.getChildAt(1) as IMutableTableTreeNode
        assertEquals('Empty', model.getValueAt(modelNode, 0))
    }

    @Test
    void testRefresh() {
        IMutableTableTreeNode oldModelNode = model.root.getChildAt(0) as IMutableTableTreeNode
        IMutableTableTreeNode oldParamsNode = oldModelNode.getChildAt(0) as IMutableTableTreeNode
        model.refresh()
        IMutableTableTreeNode newModelNode = model.root.getChildAt(0) as IMutableTableTreeNode
        IMutableTableTreeNode newParamsNode = newModelNode.getChildAt(0) as IMutableTableTreeNode

        assertNotSame(oldModelNode, newModelNode)
        assertNotSame(oldParamsNode, newParamsNode)
    }

    @Test
    void testUpdateP14nNodes() {
        IMutableTableTreeNode modelNode = getNodeByName(model.root, 'Application') as IMutableTableTreeNode
        ParameterizationNode paramsNode = getNodeByName(modelNode.getChildAt(0), 'Parametrization X v11') as ParameterizationNode
        ModellingItem paramsItem = paramsNode.itemNodeUIItem.item

        SimulationRun.withNewSession {
            SimulationRun run = new SimulationRun()
            ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndModelClassNameAndItemVersion('Parametrization X', 'models.application.ApplicationModel', '11')
            run.parameterization = parameterizationDAO
            run.resultConfiguration = ResultConfigurationDAO.list()[0]
            run.name = 'TestRun1'
            run.startTime = new DateTime()
            run.endTime = new DateTime()
            run.model = 'models.application.ApplicationModel'
            run.save(flush: true)
        }

        testPollingSupport.poll()
        // expect one nodeStructure changed on simulation node
        assert 1 == modelListener.nodeStructureChangedEvents.size()
        modelListener.reset()

        SimulationRun.withNewSession {
            ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndModelClassNameAndItemVersion('Parametrization X', 'models.application.ApplicationModel', '11')
            SimulationRun run = new SimulationRun()
            run.parameterization = parameterizationDAO
            run.resultConfiguration = ResultConfigurationDAO.list()[0]
            run.name = 'TestRun2'
            run.startTime = new DateTime()
            run.endTime = new DateTime()
            run.model = 'models.application.ApplicationModel'
            run.save(flush: true)
        }
        testPollingSupport.poll()
        assert 1 == modelListener.nodeInsertedEvents.size()
        modelListener.reset()

        ParameterizationDAO.withNewSession {
            ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndModelClassNameAndItemVersion('Parametrization X', 'models.application.ApplicationModel', '11')
            parameterizationDAO.addToTags(new ParameterizationTag(parameterizationDAO: parameterizationDAO, tag: Tag.list()[0]))
            parameterizationDAO.save(flush: true)
        }
        testPollingSupport.poll()
        assert 3 == modelListener.nodeChangedEvents.size()

        //assert that tree contains the simulation nodes and the child nodes.
        ParameterizationNode paramsNode2 = getNodeByName(modelNode.getChildAt(0), 'Parametrization X v11') as ParameterizationNode
        assert paramsItem.is(paramsNode2.itemNodeUIItem.item)
        IMutableTableTreeNode resultsNode = modelNode.getChildAt(2) as IMutableTableTreeNode
        assert 2 == resultsNode.childCount
        ParameterizationNode simulationParamsNode1 = resultsNode.getChildAt(0).getChildAt(0) as ParameterizationNode
        ParameterizationNode simulationParamsNode2 = resultsNode.getChildAt(1).getChildAt(0) as ParameterizationNode
        paramsNode2.values.each { k, v ->
            assert v == simulationParamsNode1.values[k]
            assert v == simulationParamsNode2.values[k]

        }
    }

    private ITableTreeNode getNodeByName(ITableTreeNode node, String name) {
        ITableTreeNode result = null
        if (node.getValueAt(0).toString().equals(name)) {
            result = node
        } else {
            for (int i = 0; i < node.childCount && result == null; i++) {
                result = getNodeByName(node.getChildAt(i), name)
            }
        }
        return result
    }

    @Test
    void testUnknownModelClass() {
        ParameterizationDAO.withNewSession {
            ParameterizationDAO parameterizationDAO = new ParameterizationDAO(name: 'Parametrization X', itemVersion: '12', modelClassName: 'java.lang.Object', periodCount: 1, status: Status.NONE)
            parameterizationDAO.save(flush: true)
        }
        testPollingSupport.poll()
        assert 0 == modelListener.nodeChangedEvents.size()
        assert 0 == modelListener.nodeStructureChangedEvents.size()
    }

    @Test
    void testItemInstanceIdentity() {
        newParameterization('Parametrization X', '12')
        ParameterizationDAO parameterizationDAO = ParameterizationDAO.findByNameAndModelClassNameAndItemVersion('Parametrization X', 'models.application.ApplicationModel', '12')
        testPollingSupport.poll()
        IMutableTableTreeNode modelNode = getNodeByName(model.root, 'Application') as IMutableTableTreeNode
        ParameterizationNode paramsNode = getNodeByName(modelNode.getChildAt(0), 'Parametrization X v12') as ParameterizationNode
        assertNotNull(paramsNode)
        assertNotNull(ModellingItemFactory.getItemInstances()[ModellingItemFactory.key(Parameterization, parameterizationDAO.id)])
        Parameterization cachedItem = ModellingItemFactory.getParameterization(parameterizationDAO)
        assertTrue(cachedItem.is(paramsNode.itemNodeUIItem.item))
    }
}


class TestSubscriber {

    @Subscribe
    void onEvent(ModellingItemEvent event) {

    }

}

class TestModelListener implements ITableTreeModelListener {
    List<TableTreeModelEvent> nodeChangedEvents = []
    List<TableTreeModelEvent> structureChangedEvents = []
    List<TableTreeModelEvent> nodeStructureChangedEvents = []
    List<TableTreeModelEvent> nodeInsertedEvents = []
    List<TableTreeModelEvent> nodeRemovedEvents = []

    void reset() {
        nodeChangedEvents.clear()
        structureChangedEvents.clear()
        nodeStructureChangedEvents.clear()
        nodeInsertedEvents.clear()
        nodeRemovedEvents.clear()
    }

    @Override
    void tableTreeStructureChanged(TableTreeModelEvent event) {
        structureChangedEvents << event
    }

    @Override
    void tableTreeNodeStructureChanged(TableTreeModelEvent event) {
        nodeStructureChangedEvents << event
    }

    @Override
    void tableTreeNodesInserted(TableTreeModelEvent event) {
        nodeInsertedEvents << event
    }

    @Override
    void tableTreeNodesRemoved(TableTreeModelEvent event) {
        nodeRemovedEvents << event
    }

    @Override
    void tableTreeNodesChanged(TableTreeModelEvent event) {
        nodeChangedEvents << event
    }
}

class TestPollingSupport extends PollingSupport {
    private final List<IActionListener> listeners = []

    @Override
    void addActionListener(IActionListener listener) {
        listeners.add(listener)
    }

    @Override
    void removeActionListener(IActionListener listener) {
        listeners.remove(listener)
    }

    void poll() {
        listeners.each {
            it.actionPerformed(null)
        }
    }
}