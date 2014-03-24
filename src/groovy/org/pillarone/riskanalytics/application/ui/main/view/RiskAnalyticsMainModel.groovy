package org.pillarone.riskanalytics.application.ui.main.view
import com.ulcjava.applicationframework.application.ApplicationContext
import groovy.beans.Bindable
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.base.model.AbstractModellingModel
import org.pillarone.riskanalytics.application.ui.base.model.AbstractPresentationModel
import org.pillarone.riskanalytics.application.ui.base.model.IModelChangedListener
import org.pillarone.riskanalytics.application.ui.base.model.modellingitem.ModellingInformationTableTreeModel
import org.pillarone.riskanalytics.application.ui.base.view.IModelItemChangeListener
import org.pillarone.riskanalytics.application.ui.batch.model.BatchTableListener
import org.pillarone.riskanalytics.application.ui.main.model.IRiskAnalyticsModelListener
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.application.ui.simulation.model.INewSimulationListener
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.BatchListener
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.SimulationConfigurationModel
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.BatchRunSimulationRun
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.model.registry.IModelRegistryListener
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Simulation

class RiskAnalyticsMainModel extends AbstractPresentationModel implements IModelRegistryListener {

    Map<AbstractUIItem, Object> viewModelsInUse
    ModellingInformationTableTreeModel navigationTableTreeModel
    def switchActions = []
    private List<IRiskAnalyticsModelListener> modelListeners = []
    private List<BatchTableListener> batchTableListeners = []
    private List<IModelItemChangeListener> modelItemListeners = []
    private List<INewSimulationListener> newSimulationListeners = []
    //selectedItem needs to be updated when tabs are changed etc.
    @Bindable
    AbstractUIItem currentItem

    static final Log LOG = LogFactory.getLog(RiskAnalyticsMainModel)

    ApplicationContext ulcApplicationContext

    public RiskAnalyticsMainModel() {
        viewModelsInUse = [:]
        navigationTableTreeModel = ModellingInformationTableTreeModel.getInstance(this)
        navigationTableTreeModel.buildTreeNodes()
    }

    public RiskAnalyticsMainModel(ModellingInformationTableTreeModel navigationTableTreeModel) {
        viewModelsInUse = [:]
        this.navigationTableTreeModel = navigationTableTreeModel
    }

    void saveAllOpenItems() {
        viewModelsInUse.keySet().each { AbstractUIItem item ->
            item.save()
        }
    }

    void modelAdded(Class modelClass) {
        navigationTableTreeModel.addNodeForItem(modelClass.newInstance() as Model)
    }

    public void removeItems(Model selectedModel, List<AbstractUIItem> modellingItems) {
        closeItems(selectedModel, modellingItems)
        try {
            for (AbstractUIItem item : modellingItems) {
                item.remove()
                item.delete()
            }
        } catch (Exception ex) {
            LOG.error "Deleting Item Failed: ${ex}"
        }
        fireModelChanged()
    }

    public AbstractModellingModel getViewModel(AbstractUIItem item) {
        if (viewModelsInUse.containsKey(item)) {
            return viewModelsInUse[item] as AbstractModellingModel
        }
        return item.viewModel as AbstractModellingModel
    }


    public void openItem(Model model, AbstractUIItem item) {
        if (!item.loaded)
            item.load()
        notifyOpenDetailView(model, item)
    }

    public void closeItem(Model model, AbstractUIItem abstractUIItem) {
        notifyCloseDetailView(model, abstractUIItem)
        unregisterModel(abstractUIItem)
        abstractUIItem.removeAllModellingItemChangeListener()
    }


    private void closeItems(Model selectedModel, List<AbstractUIItem> items) {
        for (AbstractUIItem item : items) {
            closeItem(selectedModel, item)
        }
    }

    public void addModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemListeners << listener
    }

    public void removeModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemListeners.remove(listener)
    }

    public void fireModelItemChanged() {
        modelItemListeners.each { IModelItemChangeListener listener -> listener.modelItemChanged() }
    }

    public void addBatchTableListener(BatchTableListener batchTableListener) {
        batchTableListeners << batchTableListener
    }

    public void fireRowAdded(BatchRunSimulationRun addedRun) {
        batchTableListeners.each { BatchTableListener batchTableListener -> batchTableListener.fireRowAdded(addedRun) }
    }

    public void fireRowDeleted(Simulation item) {
        batchTableListeners.each { BatchTableListener batchTableListener ->
            batchTableListener.fireRowDeleted(item.simulationRun)
        }
    }

    public void registerModel(AbstractUIItem item, def model) {
        viewModelsInUse[item] = model
        if (model instanceof IModelChangedListener) {
            addModelChangedListener(model)
        }
    }

    private def unregisterModel(AbstractUIItem item) {
        def viewModel = viewModelsInUse.remove(item)
        if (viewModel != null) {
            if (viewModel instanceof SimulationConfigurationModel) {
                viewModel.actionsPaneModel.removeSimulationListener(this)
                removeModelChangedListener(viewModel.settingsPaneModel)
                removeModelChangedListener(viewModel.actionsPaneModel)
                removeNewSimulationListener(viewModel)
            }
            if (viewModel instanceof IModelChangedListener) {
                removeModelChangedListener(viewModel)
            }
        }
    }

    void addModelListener(IRiskAnalyticsModelListener listener) {
        if (!modelListeners.contains(listener))
            modelListeners << listener
    }

    void notifyOpenDetailView(Model model, AbstractUIItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.openDetailView(model, item)
        }
    }

    void notifyOpenDetailView(Model model, ModellingItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.openDetailView(model, item)
        }
    }

    void notifyCloseDetailView(Model model, AbstractUIItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.closeDetailView(model, item)
        }
    }

    void notifyChangedDetailView(Model model, AbstractUIItem item) {
        setCurrentItem(item)
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.changedDetailView(model, item)
        }
    }

    void notifyChangedWindowTitle(AbstractUIItem abstractUIItem) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.windowTitle = abstractUIItem
        }
    }

    public void addNewSimulationListener(INewSimulationListener newSimulationListener) {
        newSimulationListeners << newSimulationListener
    }

    public void removeNewSimulationListener(INewSimulationListener newSimulationListener) {
        newSimulationListeners.remove(newSimulationListener)
    }

    public void fireNewSimulation(Simulation simulation) {
        newSimulationListeners.each { INewSimulationListener newSimulationListener ->
            newSimulationListener.newSimulation(simulation)
        }
    }

    public void setCurrentItem(AbstractUIItem currentItem) {
        this.currentItem = (currentItem instanceof BatchUIItem) ? null : currentItem
        switchActions.each {
            boolean b = (this.currentItem instanceof ParameterizationUIItem) || (this.currentItem instanceof ResultUIItem)
            it.setEnabled(b)
            it.selected = b
        }
        notifyChangedWindowTitle(currentItem)
    }

    ModellingUIItem getAbstractUIItem(ModellingItem modellingItem) {
        ModellingUIItem item = null
        viewModelsInUse.keySet().findAll { it instanceof ModellingUIItem }.each { ModellingUIItem openedUIItem ->
            if (modellingItem.class == openedUIItem.item.class && modellingItem.id == openedUIItem.item.id) {
                item = openedUIItem
            }
        }
        return item
    }

    /**
     * insert new batch node to the mainTree, created by editing a new simulation
     * @param newBatchRun
     */
    public void addBatch(BatchRun newBatchRun) {
        navigationTableTreeModel.addNodeForItem(new BatchUIItem(this, newBatchRun))
        viewModelsInUse.each { k, v ->
            if (v instanceof BatchListener)
                v.newBatchAdded(newBatchRun)
        }
    }

    boolean isItemOpen(AbstractUIItem item) {
        viewModelsInUse.containsKey(item)
    }

}
