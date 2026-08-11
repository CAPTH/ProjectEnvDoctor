package com.doctor.checker

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

/**
 * JDK环境检测器
 * 功能：校验项目SDK版本、对比构建文件声明Java版本，检测版本不匹配问题
 * 兼容 Maven(pom.xml) / Gradle(build.gradle/build.gradle.kts)
 */
class JdkChecker : BaseChecker {

    override fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult> {
        val resultList = mutableListOf<DiagnoseResult>()
        val projectRootManager = ProjectRootManager.getInstance(project)
        val projectSdk: Sdk? = projectRootManager.projectSdk

        // 1. 判断项目是否未配置SDK
        if (projectSdk == null) {
            resultList.add(
                DiagnoseResult(
                    level = DiagnoseLevel.ERROR,
                    title = "项目未配置SDK",
                    description = "当前项目没有指定项目SDK，会导致代码大量爆红、编译失败",
                    solution = "文件 → 项目结构 → 项目 → 项目SDK，选择已配置的JDK"
                )
            )
            return resultList
        }

        val rawSdkVersion = projectSdk.versionString ?: "未知版本"
        val sdkVersionText = normalizeVersionString(rawSdkVersion)

        resultList.add(
            DiagnoseResult(
                level = DiagnoseLevel.OK,
                title = "项目SDK配置正常",
                description = "当前项目SDK：$sdkVersionText",
                solution = ""
            )
        )

        // 2. 读取构建文件声明的Java版本
        val declaredJavaVersion = findDeclaredJavaVersion(projectRoot)
        if (declaredJavaVersion == null) {
            resultList.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "未识别构建配置中的Java版本",
                    description = "未能从pom.xml / build.gradle读取到目标Java版本，无法进行版本比对",
                    solution = "检查构建配置文件是否存在，确认声明java版本字段"
                )
            )
        } else {
            // 简易版本匹配判断（简化逻辑，不做复杂版本解析，符合无PSI方案）
            val sdkMajor = extractMajorVersion(sdkVersionText)
            val buildMajor = extractMajorVersion(declaredJavaVersion)
            val sdkMajorText = sdkMajor?.toString() ?: "unknown"
            val buildMajorText = buildMajor?.toString() ?: "unknown"

            if (sdkMajor != null && buildMajor != null && sdkMajor < buildMajor) {
                resultList.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.ERROR,
                        title = "JDK版本不兼容",
                        description = "构建配置要求Java " + buildMajorText + "，当前项目SDK仅为Java " + sdkMajorText,
                        solution = "文件 → 项目结构 → 项目，升级项目SDK至Java " + buildMajorText + " 及以上版本"
                    )
                )
            } else if (sdkMajor != null && buildMajor != null && sdkMajor > buildMajor) {
                resultList.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.WARN,
                        title = "SDK版本高于构建目标版本",
                        description = "项目SDK为Java " + sdkMajorText + "，构建配置目标为Java " + buildMajorText + "，可能存在字节码兼容或Preview API风险",
                        solution = "确认构建配置中targetCompatibility或maven.compiler.target与团队约定一致"
                    )
                )
            } else {
                resultList.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.OK,
                        title = "JDK版本匹配校验通过",
                        description = "项目SDK版本满足构建文件声明的Java " + buildMajorText + " 要求",
                        solution = ""
                    )
                )
            }
        }

        return resultList
    }

    /**
     * 在项目根目录查找构建文件，提取声明的Java版本
     * 支持 pom.xml、build.gradle、build.gradle.kts
     */
    private fun findDeclaredJavaVersion(root: VirtualFile): String? {
        // pom.xml maven
        val pomFile = root.findChild("pom.xml")
        if (pomFile != null && pomFile.isValid) {
            val content = stripXmlComments(String(pomFile.contentsToByteArray(), Charsets.UTF_8))
            // 按优先级匹配多种Java版本声明
            val pomRegexList = listOf(
                Regex("<java.version>(.*?)</java.version>"),
                Regex("<maven.compiler.source>(.*?)</maven.compiler.source>"),
                Regex("<maven.compiler.target>(.*?)</maven.compiler.target>"),
                Regex("<maven.compiler.release>(.*?)</maven.compiler.release>")
            )
            for (r in pomRegexList) {
                val match = r.find(content)
                if (match != null) {
                    return match.groupValues[1].trim()
                }
            }
        }

        // build.gradle groovy
        val gradleFile = root.findChild("build.gradle")
        if (gradleFile != null && gradleFile.isValid) {
            val content = stripCodeComments(String(gradleFile.contentsToByteArray(), Charsets.UTF_8))
            val regexList = listOf(
                Regex("sourceCompatibility\\s*=\\s*['\"]?([^'\"\\s]+)"),
                Regex("targetCompatibility\\s*=\\s*['\"]?([^'\"\\s]+)"),
                Regex("jvmTarget\\s*=\\s*['\"]([^'\"]+)['\"]"),
                Regex("JavaLanguageVersion\\.of\\(['\"]?([^'\")]+)['\"]?\\)")
            )
            for (r in regexList) {
                val match = r.find(content)
                if (match != null) return match.groupValues[1].trim()
            }
        }

        // build.gradle.kts kotlin dsl
        val gradleKts = root.findChild("build.gradle.kts")
        if (gradleKts != null && gradleKts.isValid) {
            val content = stripCodeComments(String(gradleKts.contentsToByteArray(), Charsets.UTF_8))
            val regexList = listOf(
                Regex("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_([^\\s]+)"),
                Regex("targetCompatibility\\s*=\\s*JavaVersion\\.VERSION_([^\\s]+)"),
                Regex("jvmTarget\\s*=\\s*\"([^\"]+)\""),
                Regex("JavaLanguageVersion\\.of\\(['\"]?([^'\")]+)['\"]?\\)")
            )
            for (r in regexList) {
                val match = r.find(content)
                if (match != null) {
                    val raw = match.groupValues[1].replace("_", ".").trim()
                    // 兜底：转换后的值必须能提取出有效主版本号
                    if (extractMajorVersion(raw) != null) {
                        return raw
                    }
                }
            }
        }

        return null
    }

    /**
     * 去除 XML 注释 <!-- ... -->
     */
    private fun stripXmlComments(content: String): String {
        return Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(content, "")
    }

    /**
     * 去除代码注释：单行注释和多行注释
     */
    private fun stripCodeComments(content: String): String {
        var result = content
        // 去除多行块注释
        result = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(result, "")
        // 去除单行注释
        result = Regex("//[^\\n]*").replace(result, "")
        return result
    }

    /**
     * 规范化版本字符串
     * 从各种 JDK 返回格式中提取纯数字版本号
     * "17.0.8" → "17.0.8" ; "java version \"17.0.8\"" → "17.0.8" ; "17.0.8+7" → "17.0.8"
     */
    private fun normalizeVersionString(raw: String): String {
        // 匹配第一个形如 数字.数字 或 数字 的部分
        val match = Regex("(\\d+(?:[._]\\d+)+)").find(raw)
        return match?.groupValues?.get(1)?.replace("_", ".") ?: raw.trim()
    }

    /**
     * 简易提取主版本号
     * "17.0.10" → 17 ; "1.8.0" → 8
     */
    private fun extractMajorVersion(versionStr: String): Int? {
        return when {
            versionStr.startsWith("1.") -> {
                val numStr = versionStr.split(".")[1]
                numStr.toIntOrNull()
            }
            else -> {
                val firstPart = versionStr.split(".")[0]
                firstPart.toIntOrNull()
            }
        }
    }
}