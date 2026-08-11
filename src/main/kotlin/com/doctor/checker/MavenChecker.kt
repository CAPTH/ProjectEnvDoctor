package com.doctor.checker

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Maven环境检测器
 * 检测内容：pom.xml存在性、文件非空校验、简易仓库识别
 * 读取本地maven settings.xml 仓库路径
 * 不使用PSI，纯文本正则解析
 */
class MavenChecker : BaseChecker {

    override fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult> {
        val results = mutableListOf<DiagnoseResult>()
        val pomFile = projectRoot.findChild("pom.xml")

        // 1. 判断是否Maven项目
        if (pomFile == null || !pomFile.isValid) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.OK,
                    title = "非Maven项目",
                    description = "根目录未找到 pom.xml，跳过Maven相关检测",
                    solution = ""
                )
            )
            return results
        }

        results.add(
            DiagnoseResult(
                level = DiagnoseLevel.OK,
                title = "检测到Maven项目",
                description = "存在 pom.xml 构建文件",
                solution = ""
            )
        )

        // 2. pom.xml 文件内容校验
        val pomContent = String(pomFile.contentsToByteArray(), Charsets.UTF_8).trim()
        if (pomContent.isEmpty()) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.ERROR,
                    title = "pom.xml 文件为空",
                    description = "pom.xml 无任何内容，无法进行Maven构建",
                    solution = "补全Maven基础pom模板，检查文件是否误创建"
                )
            )
        } else if (!pomContent.contains("<project")) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.ERROR,
                    title = "pom.xml 格式异常",
                    description = "文件未找到根标签 <project>，不是合法Maven配置文件",
                    solution = "核对pom.xml文件完整性，确认xml标签完整"
                )
            )
        }

        // 3. 尝试读取用户目录默认 settings.xml
        val userHome = System.getProperty("user.home")
        val defaultSettings = File(userHome, ".m2/settings.xml")
        if (!defaultSettings.exists()) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "未找到用户级 settings.xml",
                    description = "~/.m2/settings.xml 不存在，将使用Maven默认中央仓库",
                    solution = "可创建settings.xml配置国内镜像加速依赖下载"
                )
            )
        } else {
            val settingsText = defaultSettings.readText(Charsets.UTF_8)
            // 简单判断是否配置镜像
            val hasMirror = Regex("<mirror>").containsMatchIn(settingsText)
            if (!hasMirror) {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.WARN,
                        title = "settings.xml 未配置镜像源",
                        description = "未检测到<mirror>配置，拉取依赖速度可能较慢",
                        solution = "在settings.xml中添加阿里云等国内Maven镜像"
                    )
                )
            } else {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.OK,
                        title = "settings.xml 已配置镜像",
                        description = "检测到镜像配置，依赖下载速度理论更佳",
                        solution = ""
                    )
                )
            }
        }

        return results
    }
}