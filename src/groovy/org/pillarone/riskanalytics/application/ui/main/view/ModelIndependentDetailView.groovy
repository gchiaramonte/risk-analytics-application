package org.pillarone.riskanalytics.application.ui.main.view

import com.canoo.ulc.detachabletabbedpane.server.ULCDetachableTabbedPane
import com.ulcjava.base.application.ULCComponent
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.finished.FinishedSimulationView
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.queue.RealTimeLoggingView
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.queue.SimulationQueueView
import org.pillarone.riskanalytics.application.ui.upload.finished.view.FinishedUploadsView
import org.pillarone.riskanalytics.application.ui.upload.queue.view.UploadQueueView
import org.pillarone.riskanalytics.application.ui.util.IResourceBundleResolver
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.Resource

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class ModelIndependentDetailView {

    private static final String SIMULATION_QUEUE_TAB_NAME_KEY = "simulationQueueTab"
    private static final String SIMULATION_LOGGING_TAB_NAME_KEY = 'simulationLoggingTab'
    private static final String FINISHED_SIMULATION_TAB_NAME_KEY = "finishedSimulationsTab"
    private static final String FINISHED_UPLOADS_TAB_NAME_KEY = "finishedUploadsTab"
    private static final String UPLOAD_QUEUE_TAB_NAME_KEY = "uploadQueueTab"

    private ULCDetachableTabbedPane tabbedPane
    @Resource
    SimulationQueueView simulationQueueView

    @Resource
    UploadQueueView uploadQueueView

    @Resource
    FinishedSimulationView finishedSimulationView

    @Resource
    FinishedUploadsView finishedUploadsView

    @Resource
    RealTimeLoggingView realTimeLoggingView

    @Resource
    IResourceBundleResolver resourceBundleResolver

    @PostConstruct
    void initialize() {
        tabbedPane = new ULCDetachableTabbedPane()
        String simulationQueueTabName = getStringFromResourceBundle(SIMULATION_QUEUE_TAB_NAME_KEY)
        String finishedSimulationsTabName = getStringFromResourceBundle(FINISHED_SIMULATION_TAB_NAME_KEY)
        String finishedUploadsTabName = getStringFromResourceBundle(FINISHED_UPLOADS_TAB_NAME_KEY)
        String simulationLoggingTabName = getStringFromResourceBundle(SIMULATION_LOGGING_TAB_NAME_KEY)
        String uploadQueueTabName = getStringFromResourceBundle(UPLOAD_QUEUE_TAB_NAME_KEY)
        tabbedPane.addTab(simulationQueueTabName, simulationQueueView.content)
        tabbedPane.setCloseableTab(tabbedPane.indexOfTab(simulationQueueTabName), false)

        tabbedPane.addTab(finishedSimulationsTabName, finishedSimulationView.content)
        tabbedPane.setCloseableTab(tabbedPane.indexOfTab(finishedSimulationsTabName), false)

        tabbedPane.addTab(simulationLoggingTabName, realTimeLoggingView.content)
        tabbedPane.setCloseableTab(tabbedPane.indexOfTab(simulationLoggingTabName), false)

        tabbedPane.addTab(uploadQueueTabName, uploadQueueView.content)
        tabbedPane.setCloseableTab(tabbedPane.indexOfTab(uploadQueueTabName), false)

        tabbedPane.addTab(finishedUploadsTabName, finishedUploadsView.content)
        tabbedPane.setCloseableTab(tabbedPane.indexOfTab(finishedUploadsTabName), false)
    }

    ULCComponent getContent() {
        return tabbedPane
    }

    private String getStringFromResourceBundle(String key) {
        resourceBundleResolver.getText(ModelIndependentDetailView, key)
    }

}
