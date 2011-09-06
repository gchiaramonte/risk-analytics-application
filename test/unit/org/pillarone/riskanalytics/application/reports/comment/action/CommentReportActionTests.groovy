package org.pillarone.riskanalytics.application.reports.comment.action

import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment
import com.ulcjava.base.application.ULCComponent

import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.parameter.comment.CommentDAO
import org.joda.time.DateTime
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.pillarone.riskanalytics.application.ui.util.DateFormatUtils
import org.pillarone.riskanalytics.application.reports.bean.PropertyValuePairBean
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.reports.ReportHelper
import org.pillarone.riskanalytics.application.reports.AbstractReportActionTests
import org.pillarone.riskanalytics.application.reports.gira.action.GiraSubReportTests

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class CommentReportActionTests extends AbstractReportActionTests {

    void testGeneratePDF() {
        CommentReportAction reportAction = new CommentReportAction(null)
        File testExportFile = File.createTempFile("test", ".pdf")
        reportAction.metaClass.getFileName = {->
            return testExportFile.getAbsolutePath()
        }
        reportAction.metaClass.getComments = {->
            List list = []
            for (int i = 0; i < 10; i++) {
                list << getComment(i)
            }
            return list
        }
        reportAction.metaClass.saveReport = {def output, String fileName, ULCComponent component ->
//            File f = new File("E:/downloads/reports/" + fileName)
            FileOutputStream fos = new FileOutputStream(testExportFile)
            fos.write(output)
        }

        reportAction.metaClass.addCommentData = {Comment comment, Collection currentValues ->
            String boxTitle = comment.path + " P" + String.valueOf(comment.period) + " testUser" + " " + DateFormatUtils.formatDetailed(comment.lastChange)
            String tags = comment.getTags().join(", ")
            String addedFiles = "Attachments: " + comment.getFiles().join(", ")
            currentValues << ["boxTitle": boxTitle, "tags": tags, "addedFiles": addedFiles, "text": comment.getText()]

        }

        reportAction.metaClass.getReport = {->
            Map params = new HashMap()
            JRBeanCollectionDataSource collectionDataSource = reportAction.getCollectionDataSource()
            params["comments"] = collectionDataSource
            params["title"] = "Parameterization comments"
            params["footer"] = "sample report generated by PillarOne by testUser,"
            params["infos"] = createItemSettingsDataSource()
            params["currentUser"] = "testUser"
            params["itemInfo"] = "Parameterization Info"
            params["_file"] = "CommentReport"
            params["SUBREPORT_DIR"] = ReportHelper.getReportFolder()
            params["Comment"] = "Comment"
            params["p1Icon"] = getClass().getResource(UIUtils.ICON_DIRECTORY + "application.png")
            params["p1Logo"] = getClass().getResource(UIUtils.ICON_DIRECTORY + "pdf-reports-header.png")
            return ReportHelper.getReportOutputStream(params, collectionDataSource).toByteArray()
        }

        //todo fja: it doesn't work on the cruise,
        // test will be moved to report plugin
//        reportAction.doActionPerformed(null)
//        verifyExport(testExportFile)
    }

    static Comment getComment(int index) {
        String text = getText(index)
        CommentDAO dao = new CommentDAO(comment: text, path: "org.riskanalytics.jasper.report.test.pdf.export.path${index}", periodIndex: index, timeStamp: new DateTime())
        Comment comment = new Comment(dao)
        comment.metaClass.getTags = {->
            [new Tag(name: "tag1"), new Tag(name: "tag2")]
        }
        comment.metaClass.getFiles = {->
            ["file1", "file2"]
        }
        return comment
    }

    static String getText(int index) {
//        StringBuilder sb = new StringBuilder()
//        for (int i = 0; i < 60; i++) {
//            sb.append("Text$index ")
//            if (i % 9 == 0)
//                sb.append("\n")
//        }
//
//        return sb.toString()
        return "afa afa afa afa afa afa afa"
    }


}
