package org.pillarone.riskanalytics.application.ui.comment.view

import org.pillarone.riskanalytics.core.FileConstants;


import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.FunctionComment

import be.devijver.wikipedia.Parser
import com.ulcjava.base.application.border.ULCTitledBorder
import com.ulcjava.base.application.util.Color
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.Font
import com.ulcjava.base.application.util.HTMLUtilities
import org.pillarone.riskanalytics.application.ui.base.model.AbstractCommentableItemModel
import org.pillarone.riskanalytics.application.ui.base.view.DownloadFilePane
import org.pillarone.riskanalytics.application.ui.base.view.FollowLinkPane
import org.pillarone.riskanalytics.application.ui.comment.action.EditCommentAction
import org.pillarone.riskanalytics.application.ui.comment.action.MakeVisibleAction
import org.pillarone.riskanalytics.application.ui.comment.action.RemoveCommentAction
import org.pillarone.riskanalytics.application.ui.result.model.ResultViewModel
import org.pillarone.riskanalytics.application.ui.util.DateFormatUtils
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.springframework.web.util.HtmlUtils
import com.ulcjava.base.application.*

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class CommentPane {
    ULCBoxPane content;
    FollowLinkPane label
    DownloadFilePane downloadFilePane
    ULCLabel tags
    ULCButton editButton
    ULCButton deleteButton
    ULCButton makeVisibleButton
    Comment comment
    String path
    int periodIndex
    EditCommentAction editCommentAction
    RemoveCommentAction removeCommentAction
    MakeVisibleAction makeVisibleAction
    protected AbstractCommentableItemModel model
    String searchText = null

    public CommentPane(AbstractCommentableItemModel model, Comment comment, String searchText = null) {
        this.model = model
        this.comment = comment
        if (searchText) this.searchText = searchText
        initComponents()
        layoutComponents()
    }


    protected void initComponents() {
        content = new ULCBoxPane(3, 2);
        content.setMinimumSize new Dimension(400, 100)
        content.name = "CommentPane"
        content.setBackground(Color.white);
        final ULCTitledBorder border = BorderFactory.createTitledBorder(getTitle());
        Font font = border.getTitleFont().deriveFont(Font.PLAIN)
        border.setTitleFont(font);
        content.setBorder(border);

        label = new FollowLinkPane();
        downloadFilePane = new DownloadFilePane(source: content)
        if (searchText) label.name = "foundText"
        label.setText getLabelText()
        downloadFilePane.setText(getCommentFiles())

        label.setFont(font);
        downloadFilePane.setFont(font)
        tags = new ULCLabel()
        tags.setText HTMLUtilities.convertToHtml(getTagsValue())
        editCommentAction = new EditCommentAction(comment)
        Closure enablingClosure = {-> return comment.tags.any {model instanceof ResultViewModel || it.name == NewCommentView.POST_LOCKING} || !model?.isReadOnly()}
        editCommentAction.enablingClosure = enablingClosure
        editButton = new ULCButton(editCommentAction)
        editButton.setContentAreaFilled false
        editButton.setBackground Color.white
        editButton.setOpaque false
        editButton.name = "editComment"
        removeCommentAction = new RemoveCommentAction(model, comment)
        removeCommentAction.enablingClosure = enablingClosure

        deleteButton = new ULCButton(removeCommentAction)
        deleteButton.setContentAreaFilled false
        editButton.setOpaque true
        deleteButton.name = "deleteComment"
        makeVisibleAction = new MakeVisibleAction(model, comment.path)
        makeVisibleButton = new ULCButton(makeVisibleAction)
        makeVisibleButton.setContentAreaFilled false
        makeVisibleButton.setBackground Color.white
        makeVisibleButton.setOpaque false
    }



    protected void layoutComponents() {
        content.add(ULCBoxPane.BOX_LEFT_TOP, tags);
        ULCBoxPane buttons = new ULCBoxPane(3, 1)
        buttons.add(makeVisibleButton)
        buttons.add(editButton)
        buttons.add(deleteButton)
        content.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCFiller())
        content.add(ULCBoxPane.BOX_RIGHT_TOP, buttons)
        content.add(3, ULCBoxPane.BOX_LEFT_TOP, label);
        content.add(3, ULCBoxPane.BOX_LEFT_TOP, downloadFilePane);
    }


    void addCommentListener(CommentListener listener) {
        editCommentAction.addCommentListener listener
        makeVisibleAction.addCommentListener listener
    }

    String getTagsValue() {
        int size = comment.getTags().size()
        StringBuilder sb = new StringBuilder(UIUtils.getText(this.class, "Tags") + ":")
        comment?.getTags()?.eachWithIndex {Tag tag, int index ->
            sb.append(tag?.getName())
            if (index < size - 1)
                sb.append(", ")
        }
        appendFunction(sb, comment)
        return sb.toString()
    }

    void appendFunction(StringBuilder sb, Comment comment) {
        if ((comment instanceof FunctionComment) && comment.function) {
            sb.append("<br>" + UIUtils.getText(CommentAndErrorView.class, "Function") + ": ")
            sb.append(comment.function)
        }
    }

    String getTitle() {
        String username = comment.user ? comment.user.username : ""
        StringBuilder sb = new StringBuilder(CommentAndErrorView.getDisplayPath(model, comment.getPath()))
        sb.append((comment.getPeriod() != -1) ? " P" + comment.getPeriod() : " " + UIUtils.getText(CommentAndErrorView.class, "forAllPeriods"))
        if (username != "")
            sb.append(" " + UIUtils.getText(CommentPane.class, "user") + ": " + username)
        sb.append(" " + DateFormatUtils.formatDetailed(comment.lastChange))
        return sb.toString()
    }

    private String getLabelText() {
        String text = comment.getText()
        if (searchText) {
            text = addHighlighting(text, searchText.split())
        }
        String wiki = null
        try {
            if (text) text = endLineToHtml(text)
            java.io.StringWriter writer = new java.io.StringWriter();
            (new Parser()).withVisitor(text, new HtmlVisitor(writer, null));
            wiki = writer.toString()
        } catch (Exception ex) {
            wiki = text
        }
        return HTMLUtilities.convertToHtml(HtmlUtils.htmlUnescape(wiki))
    }

    String getCommentFiles() {
        StringBuilder sb = new StringBuilder("<br>" + UIUtils.getText(NewCommentView, "addedFiles") + ":<br>")
        String url = FileConstants.COMMENT_FILE_DIRECTORY
        for (String file: comment.files) {
            sb.append("<a href='${url + File.separator + file}' >${file}</a><br>")
        }
        return sb.toString()
    }

    private String addHighlighting(String text, def words) {
        def found = []
        words.each {
            (text =~ /(?i)${it}/).each {def m ->
                found.add(m)
            }
        }
        found.each {
            text = text.replaceAll(it, "<span style=\"font-weight:bold;color:#006400\">${it}</span>")
        }
        return text
    }


    private String endLineToHtml(String text) {
        // \n causes hiding of links
        //workaround: replace all endline with html code
        return text.replaceAll("\n", "<br>")
    }


}
