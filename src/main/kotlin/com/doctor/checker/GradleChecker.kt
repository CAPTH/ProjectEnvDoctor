package com.doctor.checker

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Gradle环境检测器
 * 检测内容：build.gradle / build.gradle.kts、settings.gradle 是否存在
 * 简易读取配置，判断Gradle项目基础完整性
 * 不使用PSI，直接读取文件原始文本正则匹配
 */
class GradleChecker : BaseChecker {

    override fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult> {
        val results = mutableListOf<DiagnoseResult>()

        val buildGradle = projectRoot.findChild("build.gradle")
        val buildGradleKts = projectRoot.findChild("build.gradle.kts")
        val settingsGradle = projectRoot.findChild("settings.gradle")
        val settingsGradleKts = projectRoot.findChild("settings.gradle.kts")

        val isGradleProject = (buildGradle != null && buildGradle.isValid)
                || (buildGradleKts != null && buildGradleKts.isValid)

        // 1. 判断是否为Gradle项目
        if (!isGradleProject) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.OK,
                    title = "非Gradle项目",
                    description = "项目根目录未找到 build.gradle / build.gradle.kts，跳过Gradle检测",
                    solution = ""
                )
            )
            return results
        }

        results.add(
            DiagnoseResult(
                level = DiagnoseLevel.OK,
                title = "检测到Gradle构建文件",
                description = "识别为Gradle项目",
                solution = ""
            )
        )

        // 2. 检查settings.gradle是否存在
        val hasSettings = (settingsGradle != null && settingsGradle.isValid)
                || (settingsGradleKts != null && settingsGradleKts.isValid)
        if (!hasSettings) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "缺失 settings.gradle",
                    description = "Gradle项目建议存在settings.gradle，用于配置项目名称、子模块",
                    solution = "在项目根目录新建 settings.gradle，填入 rootProject.name = '项目名'"
                )
            )
        }

        // 3. 读取build脚本内容简单校验（空文件警告）
        val mainBuildFile = buildGradle ?: buildGradleKts
        mainBuildFile?.let { file ->
            val content = String(file.contentsToByteArray(), Charsets.UTF_8).trim()
            if (content.isEmpty()) {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.ERROR,
                        title = "Gradle构建文件为空",
                        description = "${file.name} 文件内容为空，会导致构建失败",
                        solution = "补充Gradle基础构建配置，或确认文件是否创建失误"
                    )
                )
            }

            // 简易检测是否声明仓库（无远程仓库警告）
            // 同时检查 build 文件和 settings 文件中的仓库声明
            val settingsFile = settingsGradle ?: settingsGradleKts
            var settingsContent = ""
            settingsFile?.let { sf ->
                if (sf.isValid) {
                    settingsContent = String(sf.contentsToByteArray(), Charsets.UTF_8)
                }
            }
            val combinedContent = content + "\n" + settingsContent
            val hasRepo = Regex("mavenCentral|maven\\s*\\(|google\\s*\\(").containsMatchIn(combinedContent)
            if (!hasRepo) {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.WARN,
                        title = "未识别到依赖仓库配置",
                        description = "构建脚本中未找到mavenCentral等仓库声明，可能无法下载依赖",
                        solution = "在 repositories {} 块内添加 mavenCentral()"
                    )
                )
            }
        }

        return results
    }
}