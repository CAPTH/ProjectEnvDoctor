package com.doctor.model

/**
 * 诊断等级枚举
 * 对应三类结果：正常 / 警告 / 错误
 */
enum class DiagnoseLevel(
    val icon: String,
    val displayName: String
) {
    OK("🟢", "正常"),
    WARN("🟡", "警告"),
    ERROR("🔴", "错误");
}

/**
 * 单项环境检测结果
 * @param level 诊断等级 OK/WARN/ERROR
 * @param title 检测标题（例：JDK版本匹配校验）
 * @param description 问题详细描述
 * @param solution 可直接复制的修复方案，无方案传空字符串
 */
data class DiagnoseResult(
    val level: DiagnoseLevel,
    val title: String,
    val description: String,
    val solution: String
) {
    /**
     * 格式化输出文本，用于弹窗展示/日志导出
     */
    fun formatText(): String {
        return buildString {
            append("${level.icon}【${level.displayName}】$title\n")
            append("描述：$description\n")
            if (solution.isNotBlank()) {
                append("💡修复方案：$solution\n")
            }
            appendLine()
        }
    }
}

/**
 * 一次完整诊断汇总结果
 * 收集所有检测器返回的单项 DiagnoseResult
 */
data class DiagnoseSummary(
    val itemList: MutableList<DiagnoseResult> = mutableListOf()
) {
    /**
     * 添加一条检测结果
     */
    fun addResult(result: DiagnoseResult) {
        itemList.add(result)
    }

    /**
     * 批量添加多条结果
     */
    fun addAllResults(results: Collection<DiagnoseResult>) {
        itemList.addAll(results)
    }

    /**
     * 获取完整格式化报告文本
     */
    fun getFullReport(): String {
        if (itemList.isEmpty()) {
            return "未检测到任何项目环境信息！"
        }
        return buildString {
            appendLine("===== 项目环境诊断报告 =====")
            appendLine()
            itemList.forEach { append(it.formatText()) }
        }
    }

    // 快捷筛选方法，后续UI可用来分类统计
    fun getErrorItems(): List<DiagnoseResult> = itemList.filter { it.level == DiagnoseLevel.ERROR }
    fun getWarnItems(): List<DiagnoseResult> = itemList.filter { it.level == DiagnoseLevel.WARN }
    fun getOkItems(): List<DiagnoseResult> = itemList.filter { it.level == DiagnoseLevel.OK }
}