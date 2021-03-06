package org.pillarone.riskanalytics.application.ui.base.action

import com.canoo.ulc.community.ulcclipboard.server.ULCClipboard
import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.tabletree.ITableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.util.KeyStroke
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.util.LocaleResources

import java.text.NumberFormat

class TreeNodeCopier extends ResourceBasedAction {

    static String space = " "

    ULCTableTree rowHeaderTree
    ULCTableTree viewPortTree
    ITableTreeModel model
    List columnOrder
    boolean copyWithPath = false

    public TreeNodeCopier() {
        super("Copy")
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, false));
    }

    public TreeNodeCopier(boolean copyWithPath) {
        super(copyWithPath ? "CopyWithPath" : "Copy")
        if (!copyWithPath) {
            putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, false));
        }
        this.copyWithPath = copyWithPath
    }

    public void doActionPerformed(ActionEvent event) {
        List nodes = new ArrayList(rowHeaderTree.selectedPaths*.lastPathComponent)

        StringBuffer content = new StringBuffer()
        int columnCount = model.columnCount

        columnOrder = [0]

        content.append(writeHeader())
        writeData(content, nodes, columnCount)
    }

    void writeData(StringBuffer content, List nodes, int columnCount) {
        List distinctNodes = filterNodes(nodes)
        Collections.sort(distinctNodes, new TreeNodeComparator(model.root))
        distinctNodes?.each { ITableTreeNode node ->
            content.append(writeNode(node, columnCount))
        }
        writeToClipboard(content.toString())
    }

    protected void writeToClipboard(String content) {
        trace("Write to clipboard: $content")
        ULCClipboard.getClipboard().content = content
    }

    protected List filterNodes(final List selectedNodes) {
        List result = []
        selectedNodes.each {ITableTreeNode node ->
            if (!containsParentNode(node, selectedNodes)){
                result << node
            }
        }
        return result
    }

    protected boolean containsParentNode(ITableTreeNode node, List selectedNodes) {
        if (!node){
            return false
        }
        if (selectedNodes.contains(node.parent)) {
            return true
        }
        return containsParentNode(node.parent, selectedNodes)
    }

    protected String writeHeader() {
        StringBuffer line = new StringBuffer()
        if (copyWithPath)
            line << UIUtils.getText(TreeNodeCopier, "path") + "\t"
        line << rowHeaderTree.getColumnModel().getColumn(0).getHeaderValue()
        line << "\t"

        viewPortTree.getColumnModel().getColumns().each {
            line << it.getHeaderValue() << "\t"
            columnOrder << it.modelIndex
        }

        line.delete(line.size() - 1, line.size())

        line << "\n\n"

        return line.toString()
    }

    protected String writeNode(ITableTreeNode node, int columnCount) {
        StringBuffer line = new StringBuffer()
        appendNode(node, line, columnCount, 0)
        return line.toString()
    }

    protected def appendNode(ITableTreeNode node, StringBuffer line, int columnCount, int currentDepth) {
        List valueStrings = columnOrder.collect {columnIndex ->
            format(model.getValueAt(node, columnIndex))
        }

        if (copyWithPath)
            line.append(node.path.toString() + getNodeName(node, valueStrings) + "\t")

        currentDepth.times {
            line.append(space)
        }
        line.append(valueStrings.join("\t"))
        line.append("\n")

        currentDepth++
        node.childCount.times {
            appendNode(node.getChildAt(it), line, columnCount, currentDepth)
        }
    }

    protected String format(Object o) {
        if (o == null) {
            return ""
        }
        return o.toString()
    }

    protected String format(Number n) {
        if (n == null) {
            return ""
        }
        return copyFormat.format(n)
    }

    protected getCopyFormat() {
        NumberFormat format = NumberFormat.getInstance(LocaleResources.locale)
        format.setMaximumFractionDigits(10)
        format.groupingUsed = false
        return format
    }

    String getNodeName(def node, List valueStrings) {
        if (!valueStrings || valueStrings.size() < 2 || "".equals(valueStrings.get(1)) || !node.properties.keySet().contains("field"))
            return ""
        return ":" + node.field
    }
}