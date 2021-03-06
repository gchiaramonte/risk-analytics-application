package org.pillarone.riskanalytics.application.ui.base.model.modellingitem

import com.ulcjava.base.application.tabletree.AbstractTableTreeModel
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.base.model.ModellingTableTreeColumn
import org.pillarone.riskanalytics.application.ui.main.view.item.BatchUIItem
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.application.ui.result.model.SimulationNode
import org.pillarone.riskanalytics.application.ui.resulttemplate.model.ResultConfigurationNode
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.ModellingItemEvent
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.modellingitem.CacheItem
import org.pillarone.riskanalytics.core.search.CacheItemSearchService
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.SimulationProfile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.annotation.Resource

import static org.pillarone.riskanalytics.core.search.CacheItemEvent.EventType.*

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class NavigationTableTreeModel extends AbstractTableTreeModel implements ITableTreeModelWithValues {
    protected static final Log LOG = LogFactory.getLog(NavigationTableTreeModel)

    static
    final List<String> COLUMN_NAMES = ["Name", "State", "Tags", "TransactionName", "Owner", "LastUpdateBy", "Created", "LastModification"]
    @Autowired
    CacheItemSearchService cacheItemSearchService
    @Resource
    NavigationTableTreeBuilder navigationTableTreeBuilder
    private ModellingTableTreeColumn enumModellingTableTreeColumn
    Map columnValues = [:]
    int orderByColumn = -1
    boolean ascOrder

    public static int NAME = 0
    public static int STATE = 1
    public static int TAGS = 2
    public static int TRANSACTION_NAME = 3
    public static int OWNER = 4
    public static int LAST_UPDATER = 5
    public static int CREATION_DATE = 6
    public static int LAST_MODIFICATION_DATE = 7

    int columnCount = COLUMN_NAMES.size()

    FilterDefinition currentFilter = new FilterDefinition()

    NavigationTableTreeModel() {
        enumModellingTableTreeColumn = new ModellingTableTreeColumn()
    }

    @PostConstruct
    void initialize() {
        navigationTableTreeBuilder.registerTableTreeModel(this)
        navigationTableTreeBuilder.buildTreeNodes(filteredItems)
    }

    @PreDestroy
    void destroy() {
        navigationTableTreeBuilder.unregisterTableTreeModel()
    }

    @Override
    ITableTreeNode getRoot() {
        navigationTableTreeBuilder.root
    }

    List<ModellingItem> getFilteredItems() {
        ModellingItemFactory.getOrCreateModellingItems(cacheItemSearchService.search(currentFilter.toQuery()))
    }

    Object getValueAt(Object node, int i) {
        return getValue(node, i)
    }

    public def addNodeForItem(Model model) {
        navigationTableTreeBuilder.addNodeForItem(model)
    }

    public String getColumnName(int i) {
        return UIUtils.getText(NavigationTableTreeModel.class, COLUMN_NAMES[getColumnIndex(i)])
    }

    public String getColumnFilterName(int i) {
        return UIUtils.getText(NavigationTableTreeModel.class, COLUMN_NAMES[i])
    }

    protected int getColumnIndex(int column) {
        return column
    }

    public Object getChild(Object parent, int index) {
        return parent.getChildAt(index)
    }

    public int getChildCount(Object node) {
        return node.childCount
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0
    }

    public int getIndexOfChild(Object parent, Object child) {
        return parent.getIndex(child)
    }

    private Object getValue(ItemNode node, int columnIndex) {
        return enumModellingTableTreeColumn.getEnumModellingTableTreeColumnFor(columnIndex).getValue(node.itemNodeUIItem.item)
    }

    private Object getValue(def node, int columnIndex) {
        getColumnValue(node, columnIndex)
    }

    String getColumnValue(def node, int columnIndex) {
        return columnIndex == 0 ? node.getValueAt(columnIndex) : null
    }

    @Override
    void putValues(ItemNode node) {
        for (int column = 0; column < COLUMN_NAMES.size(); column++) {
            addColumnValue(node.itemNodeUIItem.item, node, column, getValue(node, column))
        }
    }

    private addColumnValue(Parameterization parameterization, ParameterizationNode node, int column, Object value) {
        if (columnValues[parameterization] == null) {
            columnValues[parameterization] = new Object[COLUMN_NAMES.size()]
        }
        columnValues[parameterization][column] = value
        node.values[column] = value
    }

    private addColumnValue(def item, SimulationNode node, int column, Object value) {
        node.values[column] = value
    }

    private addColumnValue(def item, ResultConfigurationNode node, int column, Object value) {
        node.values[column] = value
    }

    private addColumnValue(def item, BatchNode node, int column, Object value) {
        node.values[column] = value
    }

    private addColumnValue(def item, def node, int column, Object value) {
    }

    void updateTreeStructure(ModellingItemEvent event) {
        if (event.modellingItem instanceof SimulationProfile) {
            //profiles are not shown in the tree
            return
        }
        //only add nodes for items which are accepted by the current filter.
        //remove all nodes which are not accepted
        switch (event.eventType) {
            case ADDED:
                if (isAcceptedByCurrentFilter(event.cacheItem)) {
                    navigationTableTreeBuilder.addNodeForItem(event.modellingItem)
                }
                break
            case UPDATED:
                if (isAcceptedByCurrentFilter(event.cacheItem)) {
                    navigationTableTreeBuilder.itemChanged(event.modellingItem)
                } else {
                    navigationTableTreeBuilder.removeNodeForItem(event.modellingItem)
                }
                break
            case REMOVED:
                navigationTableTreeBuilder.removeNodeForItem(event.modellingItem)
                break
        }
    }

    boolean isAcceptedByCurrentFilter(CacheItem item) {
        return currentFilter.toQuery().every {
            it.accept(item)
        }
    }

    public void removeNodeForItem(BatchUIItem batchUIItem) {
        navigationTableTreeBuilder.removeNodeForItem batchUIItem
    }

    public void refresh() {
        ExceptionSafe.protect {
            refreshService()
            navigationTableTreeBuilder.removeAll()
            navigationTableTreeBuilder.buildTreeNodes(filteredItems)
            nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(root) as Object[]))
        }
    }

    private void refreshService() {
        cacheItemSearchService.refresh()
        ModellingItemFactory.clear()
    }

    public void order(int column, boolean asc) {
        LOG.info("Ordering on column: $column, ascending: $asc")
        orderByColumn = column
        ascOrder = asc
        navigationTableTreeBuilder.order(getComparator(column, asc))
    }

    private Comparator getComparator(int column, boolean ascOrder) {
        return { x, y -> ascOrder ? x.values[column] <=> y.values[column] : y.values[column] <=> x.values[column] } as Comparator
    }

    void filterTree(FilterDefinition filterDefinition) {
        LOG.debug("Apply filter definition start.")
        currentFilter = filterDefinition
        navigationTableTreeBuilder.removeNodesForItems(navigationTableTreeBuilder.modellingItems)
        navigationTableTreeBuilder.addNodesForItems(filteredItems)
        LOG.debug("Apply filter definition done.")
    }
}
