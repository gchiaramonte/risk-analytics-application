package org.pillarone.riskanalytics.application.ui.base.model.modellingitem

import com.ulcjava.base.application.tabletree.DefaultMutableTableTreeNode
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.IMutableTableTreeNode
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.base.model.*
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchRootNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.application.ui.resource.model.ResourceNode
import org.pillarone.riskanalytics.application.ui.result.model.SimulationNode
import org.pillarone.riskanalytics.application.ui.resulttemplate.model.ResultConfigurationNode
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.components.IResource
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.model.registry.IModelRegistryListener
import org.pillarone.riskanalytics.core.model.registry.ModelRegistry
import org.pillarone.riskanalytics.core.simulation.item.*
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import static org.pillarone.riskanalytics.application.ui.base.model.TableTreeBuilderUtils.*

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class NavigationTableTreeBuilder implements IModelRegistryListener {
    static final int PARAMETERIZATION_NODE_INDEX = 0
    static final int RESULT_CONFIGURATION_NODE_INDEX = 1
    static final int SIMULATION_NODE_INDEX = 2
    static Log LOG = LogFactory.getLog(NavigationTableTreeBuilder)
    final DefaultMutableTableTreeNode root
    private ITableTreeModelWithValues tableTreeModelWithValues
    private boolean resourceNodeVisible

    public NavigationTableTreeBuilder() {
        root = new DefaultMutableTableTreeNode("root")
    }

    @PostConstruct
    void initialize() {
        ModelRegistry.instance.addListener(this)
    }

    @PreDestroy
    void close() {
        ModelRegistry.instance.removeListener(this)
    }

    void registerTableTreeModel(ITableTreeModelWithValues tableTreeModel) {
        this.tableTreeModelWithValues = tableTreeModel
    }

    void unregisterTableTreeModel() {
        this.tableTreeModelWithValues = null
    }

    void buildTreeNodes(List<ModellingItem> modellingItems) {
        buildResourcesNodes(modellingItems)
        buildBatchNodes(modellingItems)
        buildModelNodes(modellingItems)
    }

    public List<ModellingItem> getModellingItems() {
        return internalGetModellingItems(root, [])
    }

    protected List<ModellingItem> internalGetModellingItems(ITableTreeNode currentNode, List<ModellingItem> list) {
        if (currentNode instanceof ItemNode) {
            Object item = currentNode.itemNodeUIItem.item
            if ((item instanceof ParametrizedItem) || (item instanceof ResultConfiguration) || (item instanceof Batch)) {
                list << (item as ModellingItem)
            }
            if (item instanceof Simulation) {
                return
            }
        }

        for (int i = 0; i < currentNode.childCount; i++) {
            internalGetModellingItems(currentNode.getChildAt(i), list)
        }
        return list
    }

    private buildModelNodes(List<ModellingItem> items) {
        getAllModelClasses().each { Class<Model> modelClass ->
            List<ModellingItem> itemsForModel = items.findAll { it.modelClass == modelClass }
            Map groupedItems = itemsForModel.groupBy { it.class.name }
            Model model = modelClass.newInstance()
            model.init()
            DefaultMutableTableTreeNode modelNode = getModelNode(model)
            DefaultMutableTableTreeNode parametrisationsNode = modelNode.getChildAt(PARAMETERIZATION_NODE_INDEX) as DefaultMutableTableTreeNode
            DefaultMutableTableTreeNode resultConfigurationsNode = modelNode.getChildAt(RESULT_CONFIGURATION_NODE_INDEX) as DefaultMutableTableTreeNode
            DefaultMutableTableTreeNode simulationsNode = modelNode.getChildAt(SIMULATION_NODE_INDEX) as DefaultMutableTableTreeNode
            addToNode(parametrisationsNode, groupedItems[Parameterization.name])
            addToNode(resultConfigurationsNode, groupedItems[ResultConfiguration.name])
            addSimulationsToNode(simulationsNode, groupedItems[Simulation.name])
            root.insert(modelNode, root.childCount - (resourceNodeVisible ? 2 : 1))
        }

    }

    private addSimulationsToNode(DefaultMutableTableTreeNode simulationsNode, List<Simulation> simulations) {
        simulationsNode.leaf = simulations?.size() == 0
        simulations.each {
            if (!(it.parameterization && it.template)) {
                LOG.error("simulation ${it.dump()} must have a parameterization and a result template. Will not add it to the tree")
                return
            }
            if (!(it.start && it.end)) {
                LOG.error("simulation ${it.dump()} must have start and end date. Will not add it to the tree")
                return
            }
            simulationsNode.add(createNode(it))
        }
    }

    private addToNode(DefaultMutableTableTreeNode node, List<ModellingItem> items) {
        if (items) {
            sortByName(items)
            getItemMap(items, false).values().each {
                node.add(createItemNodes(it))
            }
            getItemMap(items, true).values().each {
                node.add(createItemNodes(it))
            }
        }
    }

    private void sortByName(List items) {
        items.sort { a, b -> a.name.compareToIgnoreCase(b.name) }
    }

    private buildBatchNodes(List<ModellingItem> items) {
        root.add(createBatchNode(items))
    }

    private buildResourcesNodes(List<ModellingItem> items) {
        def resourceClasses = getAllResourceClasses()
        if (!resourceClasses.isEmpty()) {
            resourceNodeVisible = true
            ResourceGroupNode resourceGroupNode = new ResourceGroupNode("Resources")
            resourceClasses.each { Class resourceClass ->

                ResourceClassNode resourceNode = new ResourceClassNode(UIUtils.getText(NavigationTableTreeModel.class, resourceClass.simpleName), resourceClass)
                List<ModellingItem> resourceItems = items.findAll { ModellingItem item -> item instanceof Resource && item.modelClass == resourceClass }
                getItemMap(resourceItems, false).values().each {
                    resourceNode.add(createItemNodes(it))
                }
                resourceGroupNode.add(resourceNode)
            }
            root.add(resourceGroupNode)
        }
    }

    //legacy  - only used for model node insertions.
    private createModelNode(Model model) {
        Class modelClass = model.class

        model.init()
        ITableTreeNode modelNode = getModelNode(model)
        DefaultMutableTableTreeNode parametrisationsNode = modelNode.getChildAt(PARAMETERIZATION_NODE_INDEX) as DefaultMutableTableTreeNode
        DefaultMutableTableTreeNode resultConfigurationsNode = modelNode.getChildAt(RESULT_CONFIGURATION_NODE_INDEX) as DefaultMutableTableTreeNode
        DefaultMutableTableTreeNode simulationsNode = modelNode.getChildAt(SIMULATION_NODE_INDEX) as DefaultMutableTableTreeNode

        getItemMap(getItemsForModel(modelClass, Parameterization), false).values().each { List<Parameterization> it ->
            parametrisationsNode.add(createItemNodes(it))
        }
        getItemMap(getItemsForModel(modelClass, Parameterization), true).values().each { List<Parameterization> it ->
            parametrisationsNode.add(createItemNodes(it))
        }

        getItemMap(getItemsForModel(modelClass, ResultConfiguration), false).values().each {
            resultConfigurationsNode.add(createItemNodes(it))
        }

        addSimulationsToNode(simulationsNode, getItemsForModel(modelClass, Simulation))
        root.insert(modelNode, root.childCount - (resourceNodeVisible ? 2 : 1))
    }

    //legacy  - only used for mode node insertions.
    private <T> List<T> getItemsForModel(Class modelClass, Class<T> clazz) {
        switch (clazz) {
            case Resource: return ModellingItemFactory.getResources(modelClass)
            case Parameterization: return ModellingItemFactory.getParameterizationsForModel(modelClass)
            case ResultConfiguration: return ModellingItemFactory.getResultConfigurationsForModel(modelClass)
            case Simulation: return ModellingItemFactory.getActiveSimulationsForModel(modelClass)
            default: return []
        }
    }

    private List getAllModelClasses() {
        return ModelRegistry.instance.allModelClasses.toList()
    }

    private List<Class> getAllResourceClasses() {
        if (!(Holders.config?.includedResources instanceof List)) {
            LOG.info("Please note that there are no resource classes defined in the config.groovy file")
            return []
        }
        List acceptedResources = Holders.config.includedResources
        List<Class> classes = []
        acceptedResources.each { Class resource ->
            if (IResource.isAssignableFrom(resource)) {
                classes << resource
                LOG.info "Provided class ${resource} added to resource classes."
            } else {
                LOG.warn "Provided class ${resource} does not implement IResource interface."
            }
        }

        return classes
    }

    private DefaultMutableTableTreeNode getModelNode(Model model) {
        DefaultMutableTableTreeNode modelNode = null

        for (int i = 0; i < root.childCount && modelNode == null; i++) {
            ITableTreeNode candidate = root.getChildAt(i)
            if (candidate instanceof ItemNode && candidate.getItemClass() == model.class) {
                modelNode = candidate
            }
        }

        if (modelNode == null) {
            modelNode = new ModelNode(model)
            DefaultMutableTableTreeNode parameterizationsNode = new ItemGroupNode(UIUtils.getText(NavigationTableTreeModel.class, "Parameterization"), Parameterization)
            DefaultMutableTableTreeNode resultConfigurationsNode = new ItemGroupNode(UIUtils.getText(NavigationTableTreeModel.class, "ResultTemplates"), ResultConfiguration)
            DefaultMutableTableTreeNode simulationsNode = new ItemGroupNode(UIUtils.getText(NavigationTableTreeModel.class, "Results"), Simulation)
            modelNode.add(parameterizationsNode)
            modelNode.add(resultConfigurationsNode)
            modelNode.add(simulationsNode)
        }

        return modelNode
    }

    private Map getItemMap(items, boolean workflow) {
        Map<String, List> map = [:]
        items = items.findAll {
            workflow ? it.versionNumber.toString().startsWith("R") : !it.versionNumber.toString().startsWith("R")
        }
        items.each {
            List list = map.get(it.name)
            if (!list) {
                list = []
                list.add(it)
                map.put(it.name, list)
            } else {
                list.add(it)
            }
        }
        map
    }

    private ITableTreeNode createItemNodes(List items) {
        List tree = []
        tree.addAll(items)
        tree.sort { a, b -> b.versionNumber <=> a.versionNumber }

        IMutableTableTreeNode root = createNode(tree.first())
        tree.remove(tree.first())
        root.leaf = tree.empty

        List secondLevelNodes = tree.findAll { it.versionNumber.level == 1 }
        secondLevelNodes.each {
            IMutableTableTreeNode node = createNode(it)
            createSubNodes(tree, node)
            root.add(node)
        }

        root
    }

    private void createSubNodes(List tree, ItemNode node) {
        def currentLevelNodes = tree.findAll { ModellingItem it ->
            it.versionNumber.isDirectChildVersionOf(node.versionNumber)
        }
        node.leaf = currentLevelNodes.size() == 0
        currentLevelNodes.each {
            ItemNode newNode = createNode(it)
            node.add(newNode)
            createSubNodes(tree, newNode)
        }
    }

    public void order(Comparator comparator) {
        // Model nodes are direct children of root node
        // But Batches and Resources nodes (non-Model nodes) are direct children of root
        //
        root.childCount.times { childIndex ->
            def topLevelNode = root.getChildAt(childIndex)
            if (topLevelNode instanceof ModelNode) {
                DefaultMutableTableTreeNode parameterizationGroupNode = topLevelNode.getChildAt(PARAMETERIZATION_NODE_INDEX) as DefaultMutableTableTreeNode
                orderParameterizations(comparator, parameterizationGroupNode)

                DefaultMutableTableTreeNode simulationGroupNode = topLevelNode.getChildAt(SIMULATION_NODE_INDEX) as DefaultMutableTableTreeNode
                orderSimulations(comparator, simulationGroupNode)
            }
        }
    }

    private void orderParameterizations(Comparator comparator, DefaultMutableTableTreeNode parameterizationGroupNode) {
        List<ParameterizationNode> paramNodes = []
        parameterizationGroupNode.childCount.times { int nodeIndex ->
            ParameterizationNode node = parameterizationGroupNode.getChildAt(nodeIndex) as ParameterizationNode
            paramNodes << node
        }
        parameterizationGroupNode.removeAllChildren()
//        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parameterizationGroupNode) as Object[]))
        paramNodes.sort(comparator)
        paramNodes.each {
            parameterizationGroupNode.add(it)
        }
        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parameterizationGroupNode) as Object[]))
    }

    private void orderSimulations(Comparator comparator, DefaultMutableTableTreeNode simulationGroupNode) {
        List<SimulationNode> simNodes = []
        simulationGroupNode.childCount.times { int nodeIndex ->
            SimulationNode node = simulationGroupNode.getChildAt(nodeIndex) as SimulationNode
            simNodes << node
        }
        simulationGroupNode.removeAllChildren()
//        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(simulationGroupNode) as Object[]))
        simNodes.sort(comparator)
        simNodes.each {
            simulationGroupNode.add(it)
        }
        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(simulationGroupNode) as Object[]))
    }

    @Override
    void modelAdded(Class modelClass) {
        addNodeForItem(modelClass.newInstance() as Model)
    }

    public DefaultMutableTableTreeNode addNodeForItem(Model model, boolean notifyStructureChanged = true) {
        createModelNode(model)
        if (notifyStructureChanged) {
            //TODO (db) check with Michi about the node structure changed'. Maybe we could use NodesWhereInserted, but somehow the index screws up.
            this.tableTreeModelWithValues.nodeStructureChanged(new TreePath(root))
        }
        return root
    }

    public DefaultMutableTableTreeNode addNodeForItem(Simulation item, boolean notifyStructureChanged = true) {
        if (item.end) {
            ModelNode modelNode = findModelNode(root, item)
            LOG.debug("Adding simulation to modelNode: $modelNode")
            if (modelNode) {
                DefaultMutableTableTreeNode groupNode = findGroupNode(item, modelNode)
                LOG.debug("Group node : ${groupNode.name}")
                groupNode.leaf = false
                insertNodeInto(createNode(item), groupNode, notifyStructureChanged)
                return groupNode
            }
        }
        return null
    }

    public DefaultMutableTableTreeNode addNodeForItem(ModellingItem modellingItem, boolean notifyStructureChanged = true) {
        ModellingUIItem modellingUIItem = UIItemFactory.createItem(modellingItem)
        addNodeForUIItem(modellingUIItem, notifyStructureChanged)
    }

    DefaultMutableTableTreeNode addNodeForItem(BatchUIItem batchRun, boolean notifyStructureChanged = true) {
        ITableTreeNode groupNode = findBatchRootNode(root)
        insertNodeInto(createNode(batchRun), groupNode, notifyStructureChanged)
        return groupNode
    }

    private DefaultMutableTableTreeNode addNodeForUIItem(BatchUIItem batchRun, boolean notifyStructureChanged) {
        ITableTreeNode groupNode = findBatchRootNode(root)
        insertNodeInto(createNode(batchRun), groupNode, notifyStructureChanged)
        return groupNode
    }

    private DefaultMutableTableTreeNode addNodeForUIItem(ModellingUIItem modellingUIItem, boolean notifyStructureChanged) {
        ModelNode modelNode = findModelNode(root, modellingUIItem)
        LOG.debug("Model node : $modelNode")
        if (modelNode != null) { //item in db, but not enabled in Config
            ITableTreeNode groupNode = findGroupNode(modellingUIItem, modelNode)
            LOG.debug("Group node: $groupNode")
            createAndInsertItemNode(groupNode, modellingUIItem, notifyStructureChanged)
            return groupNode
        }
        return null
    }

    private DefaultMutableTableTreeNode addNodeForUIItem(ResourceUIItem modellingUIItem, boolean notifyStructureChanged) {
        ResourceGroupNode resourceGroup = findResourceGroupNode(root)
        if (resourceGroup) {
            ITableTreeNode itemGroupNode = findResourceItemGroupNode(resourceGroup, modellingUIItem.item.modelClass)
            createAndInsertItemNode(itemGroupNode, modellingUIItem, notifyStructureChanged)
            return itemGroupNode
        }
        return null
    }

    public void itemChanged(ModellingItem item) {
        ModelNode modelNode = findModelNode(root, item)
        LOG.debug("Modify item $item on modelNode: $modelNode")
        if (modelNode) {
            ITableTreeNode itemGroupNode = findGroupNode(item, modelNode)
            LOG.debug("GroupNode $itemGroupNode")
            itemNodeChanged(itemGroupNode, item)
        }
    }

    public void itemChanged(Batch item) {
        BatchRootNode batchRootNode = findBatchRootNode(root)
        itemNodeChanged(batchRootNode, item)
    }

    public void itemChanged(Parameterization item) {
        ModelNode modelNode = findModelNode(root, item)
        if (modelNode) {
            ITableTreeNode itemGroupNode = findGroupNode(item, modelNode)
            itemNodeChanged(itemGroupNode, item)
            ITableTreeNode simulationGroupNode = modelNode.getChildAt(SIMULATION_NODE_INDEX)
            findAllNodesForItem(simulationGroupNode, item).each {
                updateValues(it)
            }
        }
    }

    public void itemChanged(ResultConfiguration item) {
        ModelNode modelNode = findModelNode(root, item)
        if (modelNode) {
            ITableTreeNode itemGroupNode = findGroupNode(item, modelNode)
            itemNodeChanged(itemGroupNode, item)
            ITableTreeNode simulationGroupNode = modelNode.getChildAt(RESULT_CONFIGURATION_NODE_INDEX)
            itemNodeChanged(simulationGroupNode, item)
        }
    }

    private void itemNodeChanged(ITableTreeNode itemGroupNode, ModellingItem item) {
        ItemNode itemNode = findNodeForItem(itemGroupNode, item)
        LOG.debug("Node for item. ${itemNode?.name}")
        if (itemNode) {
            updateValues(itemNode)
        }
    }

    private void updateValues(ItemNode itemNode) {
        tableTreeModelWithValues.putValues(itemNode)
        tableTreeModelWithValues.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(itemNode) as Object[]))
    }

    private void itemNodeChanged(ITableTreeNode itemGroupNode, Simulation item) {
        ItemNode itemNode = findNodeForItem(itemGroupNode, item)
        if (!itemNode) {
            addNodeForItem(item, true)
        } else {
            updateValues(itemNode)
        }
    }

    public void itemChanged(Resource item) {
        ITableTreeNode itemGroupNode = findResourceItemGroupNode(findResourceGroupNode(root), item.modelClass)
        itemNodeChanged(itemGroupNode, item)
    }

    public void removeAllGroupNodeChildren(ItemGroupNode groupNode) {
        groupNode.removeAllChildren()
        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(groupNode) as Object[]))
    }

    public void removeNodeForItem(ModellingUIItem modellingUIItem) {
        ModelNode modelNode = findModelNode(root, modellingUIItem)
        if (modelNode) {
            ITableTreeNode groupNode = findGroupNode(modellingUIItem, modelNode)
            def itemNode = findNodeForItem(groupNode, modellingUIItem.item)
            if (itemNode) {
                removeItemNode(itemNode, true)
            }
        } else {
            LOG.warn("ModelNode not found for ModellingUIItem: $modellingUIItem")
        }
    }

    public void removeNodeForItem(ModellingItem modellingItem) {
        removeNodeForItem(UIItemFactory.createItem(modellingItem))
    }

    public void addNodesForItems(List<ModellingItem> items) {
        Set<DefaultMutableTableTreeNode> parentNodes = new HashSet<DefaultMutableTableTreeNode>()
        items.each {
            DefaultMutableTableTreeNode parent = addNodeForItem(it, false)
            if (parent) {
                parentNodes.add(parent)
            }
        }
        parentNodes.each {
            tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(it) as Object[]))
        }

    }

    public void removeNodesForItems(List<ModellingItem> items) {
        Set<DefaultMutableTableTreeNode> parentNodes = new HashSet<DefaultMutableTableTreeNode>()
        items.each {
            ITableTreeNode node = null
            ITableTreeNode groupNode = internalFindGroupNode(it)
            if (groupNode) {
                node = findNodeForItem(groupNode, it)
            }
            if (!node) {
                LOG.debug("No node found for modelling item $it")
            } else {
                parentNodes.add(removeItemNode(node, false))
            }

        }
        parentNodes.each {
            tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(it) as Object[]))
        }
    }

    private ITableTreeNode internalFindGroupNode(ModellingItem item) {
        ModelNode modelNode = findModelNode(root, item)
        return modelNode ? findGroupNode(item, modelNode) : null
    }

    private ITableTreeNode internalFindGroupNode(Resource item) {
        return findResourceItemGroupNode(findResourceGroupNode(root), item.modelClass)
    }

    private ITableTreeNode internalFindGroupNode(Batch item) {
        findBatchRootNode(root)
    }

    public void removeNodeForItem(ResourceUIItem modellingUIItem) {
        ITableTreeNode itemGroupNode = findResourceItemGroupNode(findResourceGroupNode(root), modellingUIItem.item.modelClass)
        ITableTreeNode itemNode = findNodeForItem(itemGroupNode, modellingUIItem.item)
        if (itemNode) {
            removeItemNode(itemNode, true)
        }
    }

    private DefaultMutableTableTreeNode removeItemNode(DefaultMutableTableTreeNode itemNode, boolean notifyStructureChanged) {
        if (itemNode instanceof SimulationNode) {
            itemNode.removeAllChildren()
        } else {
            if (itemNode.childCount > 0) {
                DefaultMutableTableTreeNode parent = itemNode.parent
                int childIndex = parent.getIndex(itemNode)
                IMutableTableTreeNode firstChild = itemNode.getChildAt(0)
                parent.insert(firstChild, childIndex)
                def children = []
                for (int i = 0; i < itemNode.childCount; i++) {
                    children << itemNode.getChildAt(i)
                }
                if (children.size() > 0) {
                    firstChild.leaf = false
                }
                children.each {
                    firstChild.add(it)
                }
                itemNode.removeAllChildren()
            }
        }
        return removeNodeFromParent(itemNode, notifyStructureChanged)
    }

    private DefaultMutableTableTreeNode removeNodeFromParent(DefaultMutableTableTreeNode itemNode, boolean notifyStructureChanged) {
        DefaultMutableTableTreeNode parent = itemNode.getParent() as DefaultMutableTableTreeNode
        int childIndex = parent.getIndex(itemNode)
        parent.remove(childIndex)
        if (notifyStructureChanged) {
            tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]))
        }
        return parent
    }


    public void removeNodeForItem(BatchUIItem batchUIItem) {
        ITableTreeNode groupNode = findBatchRootNode(root)
        ITableTreeNode itemNode = findNodeForItem(groupNode, batchUIItem)
        if (itemNode) {
            removeNodeFromParent(itemNode, true)
        }
    }

    private createAndInsertItemNode(DefaultMutableTableTreeNode node, ModellingUIItem modellingUIItem, boolean notifyStructureChanged) {
        boolean parameterNameFound = false
        for (int i = 0; i < node.childCount; i++) {
            if (isMatchingParent(node.getChildAt(i).itemNodeUIItem, modellingUIItem)) {
                parameterNameFound = true
                DefaultMutableTableTreeNode newNode = createNode(modellingUIItem)
                DefaultMutableTableTreeNode childNode = node.getChildAt(i) as DefaultMutableTableTreeNode
                LOG.debug("Item with previous version already in tree. ${childNode}")
                if (modellingUIItem.versionable && modellingUIItem.item.versionNumber.level > 1) {
                    insertSubversionItemNode(childNode, newNode, notifyStructureChanged)
                } else {
                    def children = []
                    childNode.childCount.times {
                        children << childNode.getChildAt(it)
                    }
                    children.each { newNode.add(it) }
                    childNode.removeAllChildren()
                    childNode.leaf = true
                    node.remove(i)
                    if (childNode.itemNodeUIItem.isVersionable() && childNode.itemNodeUIItem.item.versionNumber.level == 1) {
                        newNode.insert(childNode, 0)
                    } else {
                        insertSubversionItemNode(newNode, childNode, notifyStructureChanged)
                    }
                    node.insert(newNode, i)
                    if (notifyStructureChanged) {
                        //TODO (db) check with Michi about the node structure changed'. Maybe we could use NodesWhereInserted, but somehow the index screws up.
                        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(node) as Object[]))
                    }
                    return
                }
            }
        }

        if (!parameterNameFound) {
            LOG.debug("Add new item ${modellingUIItem}")
            def newNode = createNode(modellingUIItem)
            newNode.leaf = true
            node.leaf = false
            insertNodeInto(newNode, node, notifyStructureChanged)
        }
    }

    private createAndInsertItemNode(DefaultMutableTableTreeNode node, BatchUIItem batchUIItem, boolean notifyStructureChanged) {
        DefaultMutableTableTreeNode newNode = createNode(batchUIItem)
        node.add(newNode)
    }

    private boolean isMatchingParent(AbstractUIItem currentItem, AbstractUIItem itemToAdd) {
        return currentItem.name == itemToAdd.name
    }

    private boolean isMatchingParent(ParameterizationUIItem currentItem, ParameterizationUIItem itemToAdd) {
        if (currentItem.item.versionNumber.isWorkflow()) {
            if (!(itemToAdd.item.versionNumber.isWorkflow())) {
                return false
            }
        } else {
            if (itemToAdd.item.versionNumber.isWorkflow()) {
                return false
            }
        }
        return currentItem.name == itemToAdd.name
    }

    private void insertSubversionItemNode(DefaultMutableTableTreeNode node, DefaultMutableTableTreeNode newItemNode, boolean notifyStructureChanged) {
        node.childCount.times {
            DefaultMutableTableTreeNode childNode = node.getChildAt(it)
            if (newItemNode.itemNodeUIItem.isVersionable() && newItemNode.itemNodeUIItem.item.versionNumber.toString().startsWith(childNode.itemNodeUIItem.item.versionNumber.toString())) {
                if (newItemNode.itemNodeUIItem.item.versionNumber.isDirectChildVersionOf(childNode.itemNodeUIItem.item.versionNumber)) {
                    childNode.leaf = false
                    newItemNode.leaf = true
                    childNode.add(newItemNode)
                    if (notifyStructureChanged) {
                        tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(childNode) as Object[]))
                    }
                } else {
                    insertSubversionItemNode(childNode, newItemNode, notifyStructureChanged)
                }
            }
        }
    }

    private DefaultMutableTableTreeNode createNode(String name) {
        new DefaultMutableTableTreeNode(name)
    }

    private DefaultMutableTableTreeNode createNode(Parameterization item) {
        return createNode(new ParameterizationUIItem(item))
    }

    private DefaultMutableTableTreeNode createNode(ParameterizationUIItem parameterizationUIItem) {
        ParameterizationNode node = new ParameterizationNode(parameterizationUIItem)
        tableTreeModelWithValues.putValues(node)
        return node
    }

    private DefaultMutableTableTreeNode createNode(ResultConfiguration item) {
        return createNode(new ResultConfigurationUIItem(item))
    }

    private DefaultMutableTableTreeNode createNode(ResultConfigurationUIItem resultConfigurationUIItem) {
        ResultConfigurationNode node = new ResultConfigurationNode(resultConfigurationUIItem)
        tableTreeModelWithValues.putValues(node)
        return node
    }

    private DefaultMutableTableTreeNode createNode(Resource item) {
        return createNode(new ResourceUIItem(item))
    }

    private DefaultMutableTableTreeNode createNode(ResourceUIItem item) {
        ResourceNode node = new ResourceNode(item)
        tableTreeModelWithValues.putValues(node)
        return node
    }

    private DefaultMutableTableTreeNode createNode(BatchUIItem batchUIItem) {
        BatchNode node = new BatchNode(batchUIItem)
        tableTreeModelWithValues.putValues(node)
        return node
    }

    private DefaultMutableTableTreeNode createNode(Batch batch) {
        return new BatchNode(new BatchUIItem(batch))
    }

    private DefaultMutableTableTreeNode createNode(Simulation item) {
        Model selectedModelInstance = item.modelClass?.newInstance() as Model
        SimulationNode node = new SimulationNode(UIItemFactory.createItem(item))
        DefaultMutableTableTreeNode paramsNode = createNode(item.parameterization)
        paramsNode.leaf = true
        DefaultMutableTableTreeNode templateNode = createNode(item.template)
        templateNode.leaf = true
        node.add(paramsNode)
        node.add(templateNode)
        tableTreeModelWithValues.putValues(node)
        return node
    }

    private DefaultMutableTableTreeNode createBatchNode(List<ModellingItem> items) {
        BatchRootNode batchesNode = new BatchRootNode("Batches")
        List<Batch> batches = items.findAll { it instanceof Batch } as List<Batch>
        batches.each { Batch batch ->
            batchesNode.add(createNode(batch))
        }
        return batchesNode
    }

    private void insertNodeInto(DefaultMutableTableTreeNode newNode, DefaultMutableTableTreeNode parent, boolean notifyStructureChanged) {
        List<ModellingUIItem> children = []
        parent.childCount.times { i ->
            children << parent.getChildAt(i)
        }
        children << newNode
        sortByName(children)
        int newIndex = children.indexOf(newNode)

        parent.insert(newNode, newIndex)
        LOG.debug("Node ${newNode} added at index $newIndex to parent: $parent")
        if (notifyStructureChanged) {
            if (parent.childCount == 1) {
                tableTreeModelWithValues.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]))
            } else {
                tableTreeModelWithValues.nodesWereInserted(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]), [newIndex] as int[])
            }
        }
    }

    void removeAll() {
        root.removeAllChildren()
    }
}
