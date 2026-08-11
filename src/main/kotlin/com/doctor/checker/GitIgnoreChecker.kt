package com.doctor.checker

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * .gitignore 规范检测器
 * 检测项目根目录.gitignore是否存在
 * 校验是否包含Java/IDEA项目通用忽略规则
 * 不使用PSI，纯文本读取匹配
 */
class GitIgnoreChecker : BaseChecker {

    /**
     * IDEA + Java项目推荐必须忽略的规则列表
     */
    private val requiredRules = listOf(
        ".idea/",
        "*.iml",
        "target/",
        "build/",
        ".gradle/",
        "*.class",
        ".DS_Store"
    )

    override fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult> {
        val results = mutableListOf<DiagnoseResult>()
        val gitIgnoreFile = projectRoot.findChild(".gitignore")

        // 1. 判断文件是否存在
        if (gitIgnoreFile == null || !gitIgnoreFile.isValid) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "项目缺少 .gitignore 文件",
                    description = "无.gitignore配置，容易误提交IDE配置、编译产物等垃圾文件",
                    solution = "项目根目录新建.gitignore，添加 .idea/ target/ build/ *.iml 等规则"
                )
            )
            return results
        }

        results.add(
            DiagnoseResult(
                level = DiagnoseLevel.OK,
                title = "存在 .gitignore 文件",
                description = "项目根目录已创建.gitignore",
                solution = ""
            )
        )

        // 读取文件内容
        val content = String(gitIgnoreFile.contentsToByteArray(), Charsets.UTF_8)
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        // 校验缺失的忽略规则
        val missingRules = requiredRules.filter { rule ->
            lines.none { line -> line == rule || line.trimEnd('/') == rule.trimEnd('/') }
        }

        if (missingRules.isNotEmpty()) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = ".gitignore 规则不全",
                    description = "缺失常用忽略项：${missingRules.joinToString(", ")}",
                    solution = "将缺失规则追加至.gitignore文件末尾"
                )
            )
        } else {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.OK,
                    title = ".gitignore 基础规则完整",
                    description = "已包含IDEA、Maven/Gradle通用忽略配置",
                    solution = ""
                )
            )
        }

        return results
    }
}