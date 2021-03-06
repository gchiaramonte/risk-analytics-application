package org.pillarone.riskanalytics.application.ui.comment

import com.canoo.ulc.community.ulcclipboard.server.ULCClipboard
import com.ulcjava.base.application.ULCFrame
import com.ulcjava.testframework.operator.*
import models.core.CoreModel
import org.pillarone.riskanalytics.application.AbstractSimpleFunctionalTest
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.comment.view.NewCommentView
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterViewModel
import org.pillarone.riskanalytics.application.ui.parameterization.view.ParameterView
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.ModelStructureDAO
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.fileimport.ModelStructureImportService
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.simulation.item.ModelStructure
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.EnumTagType

import java.awt.event.InputEvent

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class AddNewCommentTests extends AbstractSimpleFunctionalTest {

    Parameterization parameterization

    protected void doStart() {
        new ParameterizationImportService().compareFilesAndWriteToDB(['Core'])
        new ModelStructureImportService().compareFilesAndWriteToDB(['Core'])

        ModellingItemFactory.clear()

        ULCFrame frame = new ULCFrame("test")
        frame.defaultCloseOperation = ULCFrame.TERMINATE_ON_CLOSE
        frame.name = "test"
        CoreModel model = new CoreModel()
        model.initComponents()

        def dao = ParameterizationDAO.findByName('CoreParameters')
        parameterization = ModellingItemFactory.getParameterization(dao)
        parameterization.load()
        dao = ModelStructureDAO.findByModelClassName(model.class.name)
        ModelStructure structure = ModellingItemFactory.getModelStructure(dao)
        structure.load()
        ParameterViewModel parameterViewModel = new ParameterViewModel(model, parameterization, structure)
        parameterViewModel.setReadOnly(readOnly)
        ParameterView parameterView = new ParameterView(parameterViewModel)
        frame.setContentPane(parameterView.content)
        ULCClipboard.install()
        UIUtils.setRootPane(frame)
        frame.visible = true
        NewCommentView.allTags*.delete(flush: true)
        new Tag(name: NewCommentView.POST_LOCKING).save(flush: true)
        new Tag(name: "TAG1", tagType: EnumTagType.COMMENT).save(flush: true)
    }

    void testAddNewComment() {
        assertEquals 0, parameterization.comments.size()
        ULCFrameOperator frameOperator = new ULCFrameOperator(new ComponentByNameChooser("test"))
        ULCTableTreeOperator componentTree = new ULCTableTreeOperator(frameOperator, new ComponentByNameChooser("parameterTreeRowHeader"))

        componentTree.doCollapseRow(1)
        componentTree.clickOnCell(1, 0, 1, InputEvent.BUTTON1_MASK)
        componentTree.clickOnCell(1, 0, 1, InputEvent.BUTTON3_MASK)

        ULCTabbedPaneOperator tabbedPaneOperator = new ULCTabbedPaneOperator(frameOperator, new ComponentByNameChooser('commentAndErrorPane'))
        assertNotNull tabbedPaneOperator
        assertEquals 1, tabbedPaneOperator.getComponentCount()

        ULCPopupMenuOperator popupMenuOperator = new ULCPopupMenuOperator(frameOperator, new ComponentByNameChooser("popup.expand"))
        ULCMenuItemOperator expandItem = new ULCMenuItemOperator(popupMenuOperator, "Add comment")
        assertNotNull expandItem
        expandItem.clickMouse()

        assertEquals 2, tabbedPaneOperator.getComponentCount()
        assertEquals 1, tabbedPaneOperator.getSelectedIndex()

        ULCListOperator tags = new ULCListOperator(frameOperator, new ComponentByNameChooser('tagsList'))
        assertEquals 1, tags.getItemCount()

        ULCTextAreaOperator textAreaOperator = new ULCTextAreaOperator(frameOperator, new ComponentByNameChooser('newCommentText'))
        assertNotNull textAreaOperator
        textAreaOperator.typeText('Comment')

        ULCButtonOperator buttonOperator = new ULCButtonOperator(frameOperator, new ComponentByNameChooser('saveNewComment'))
        assertNotNull buttonOperator
        buttonOperator.getFocus()
        buttonOperator.clickMouse()

        assertEquals 1, parameterization.comments.size()
        assertEquals 'Comment', parameterization.comments.get(0).text
        assertEquals 1, tabbedPaneOperator.getComponentCount()

    }

    protected boolean isReadOnly() {
        return false
    }
}
