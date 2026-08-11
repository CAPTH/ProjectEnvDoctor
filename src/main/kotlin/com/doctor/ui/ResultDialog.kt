package com.doctor.ui

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.doctor.model.DiagnoseSummary
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * 诊断结果弹窗
 * 接收 DiagnoseSummary，表格分级展示正常/警告/错误
 * 支持一键复制完整报告文本
 */
class ResultDialog(
    private val summary: DiagnoseSummary
) : DialogWrapper(true) {

    private lateinit var table: JBTable
    private lateinit var tableModel: DefaultTableModel

    companion object {
        // 表格列定义
        private val COLUMN_NAMES = arrayOf("等级", "标题", "描述", "修复方案")
        // 颜色定义
        private val COLOR_OK = Color(0, 130, 0)
        private val COLOR_WARN = Color(204, 120, 0)
        private val COLOR_ERROR = Color(190, 0, 0)
    }

    init {
        title = "项目环境诊断报告"
        setSize(1000, 550)
        init()
    }

    override fun createCenterPanel(): JComponent {
        tableModel = object : DefaultTableModel(COLUMN_NAMES, 0) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }

        table = JBTable(tableModel).apply {
            rowHeight = 32
            autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            setDefaultRenderer(Any::class.java, ResultCellRenderer())
        }

        // 填充数据
        fillTableData()

        // 设置列宽比例：等级窄、标题中等、描述和修复方案宽
        val columnModel = table.columnModel
        columnModel.getColumn(0).preferredWidth = 50   // 等级
        columnModel.getColumn(0).maxWidth = 60
        columnModel.getColumn(1).preferredWidth = 150  // 标题
        columnModel.getColumn(1).maxWidth = 200
        columnModel.getColumn(2).preferredWidth = 320  // 描述
        columnModel.getColumn(3).preferredWidth = 320  // 修复方案

        val decorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { copyReport() }
            .disableRemoveAction()
            .disableUpDownActions()

        val panel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(decorator.createPanel(), BorderLayout.CENTER)
        }
        return panel
    }

    /**
     * 将诊断数据载入表格
     */
    private fun fillTableData() {
        tableModel.rowCount = 0
        summary.itemList.forEach { item ->
            val levelText = when (item.level) {
                DiagnoseLevel.OK -> "正常"
                DiagnoseLevel.WARN -> "警告"
                DiagnoseLevel.ERROR -> "错误"
            }
            val row = arrayOf(levelText, item.title, item.description, item.solution)
            tableModel.addRow(row)
            // 把DiagnoseLevel附加到行对象，供Renderer读取颜色
            table.putClientProperty("row_${tableModel.rowCount - 1}", item.level)
        }
    }

    /**
     * 一键复制全部报告文本到剪贴板
     */
    private fun copyReport() {
        val reportText = summary.getFullReport()
        val selection = StringSelection(reportText)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        JOptionPane.showMessageDialog(window, "诊断报告已复制到剪贴板！")
    }

    /**
     * 单元格渲染器：根据等级渲染文字颜色
     */
    private inner class ResultCellRenderer : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(
            table: JTable,
            value: Any?,
            selected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ) {
            val level = table.getClientProperty("row_$row") as? DiagnoseLevel
            val textAttr = when (level) {
                DiagnoseLevel.OK -> SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN, COLOR_OK
                )
                DiagnoseLevel.WARN -> SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN, COLOR_WARN
                )
                DiagnoseLevel.ERROR -> SimpleTextAttributes(
                    SimpleTextAttributes.STYLE_PLAIN, COLOR_ERROR
                )
                else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
            }
            append(value?.toString() ?: "", textAttr)
        }
    }

    // 只保留关闭按钮，移除默认OK/Cancel
    override fun createActions(): Array<Action> = emptyArray()
}