package com.doctor.action

import com.doctor.checker.BaseChecker
import com.doctor.checker.EncodingChecker
import com.doctor.checker.GradleChecker
import com.doctor.checker.GitIgnoreChecker
import com.doctor.checker.JdkChecker
import com.doctor.checker.MavenChecker
import com.doctor.model.DiagnoseSummary
import com.doctor.ui.ResultDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 菜单动作：扫描项目环境诊断
 * 适配新版 IntelliJ Platform，移除废弃API
 */
class ScanEnvAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.getData(CommonDataKeys.PROJECT)
        if (project == null) return

        // 获取项目根目录：从选中的文件或项目文件推导
        val selectedFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val projectRoot: VirtualFile? = findProjectRoot(project, selectedFile)
        if (projectRoot == null) return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "正在诊断项目环境...", false) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.text = "执行各项环境检测器"

                val checkers: List<BaseChecker> = listOf(
                    JdkChecker(),
                    MavenChecker(),
                    GradleChecker(),
                    GitIgnoreChecker(),
                    EncodingChecker()
                )

                val summary = DiagnoseSummary()
                val total = checkers.size
                checkers.forEachIndexed { index, checker ->
                    indicator.checkCanceled()
                    indicator.text = "正在检测: " + checker.javaClass.simpleName + " (" + (index + 1) + "/" + total + ")"
                    indicator.fraction = index.toDouble() / total
                    try {
                        val resultList = checker.check(project, projectRoot)
                        summary.addAllResults(resultList)
                    } catch (ex: Exception) {
                        // 单个检测器异常不影响其他检测器
                    }
                }
                indicator.fraction = 1.0

                ApplicationManager.getApplication().invokeLater {
                    ResultDialog(summary).show()
                }
            }
        })
    }

    override fun update(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        e.presentation.isEnabledAndVisible = project != null
    }

    /**
     * 推导项目根目录
     * 优先从选中文件向上查找含 .idea 的目录，兜底用 project.projectFile
     */
    private fun findProjectRoot(project: Project, selectedFile: VirtualFile?): VirtualFile? {
        // 从选中文件向上查找
        var current: VirtualFile? = selectedFile
        while (current != null) {
            if (current.isDirectory && current.findChild(".idea") != null) {
                return current
            }
            current = current.parent
        }
        // 兜底：project.projectFile 通常在 .idea 目录下，其 parent 的 parent 是项目根
        val projectFile = project.projectFile
        if (projectFile != null) {
            val ideaDir = projectFile.parent
            if (ideaDir != null && ideaDir.name == ".idea") {
                return ideaDir.parent
            }
            return projectFile.parent
        }
        return null
    }
}
