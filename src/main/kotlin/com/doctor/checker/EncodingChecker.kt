package com.doctor.checker

import com.doctor.model.DiagnoseLevel
import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 文件编码检测器
 * 读取 .idea/encodings.xml 判断项目全局编码配置
 * 规范建议统一使用 UTF-8，防止中文乱码
 * 不使用PSI，纯文件读取 + 正则解析
 */
class EncodingChecker : BaseChecker {

    override fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult> {
        val results = mutableListOf<DiagnoseResult>()

        val ideaDir = projectRoot.findChild(".idea")
        if (ideaDir == null || !ideaDir.isValid) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "缺失 .idea 配置目录",
                    description = "未找到.idea文件夹，无法读取IDE编码配置",
                    solution = "使用IDEA打开项目，自动生成.idea目录"
                )
            )
            return results
        }

        val encodingsFile = ideaDir.findChild("encodings.xml")
        if (encodingsFile == null || !encodingsFile.isValid) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "缺少 encodings.xml",
                    description = ".idea目录下不存在encodings.xml，编码采用IDEA全局默认设置",
                    solution = "文件 → 设置 → 编辑器 → 文件编码，统一设置为UTF-8并保存"
                )
            )
            return results
        }

        val content = String(encodingsFile.contentsToByteArray(), Charsets.UTF_8)
        // 查找全局项目编码配置
        val globalEncodingRegex = Regex("<project\\s+encoding=\"(.*?)\"")
        val match = globalEncodingRegex.find(content)

        if (match == null) {
            results.add(
                DiagnoseResult(
                    level = DiagnoseLevel.WARN,
                    title = "未配置项目全局编码",
                    description = "encodings.xml 未声明项目编码，存在编码混乱风险",
                    solution = "打开文件编码设置，勾选透明-native-to-ascii转换，全部设置UTF-8"
                )
            )
        } else {
            val encodeName = match.groupValues[1].uppercase()
            if (encodeName == "UTF-8") {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.OK,
                        title = "项目全局编码 UTF-8",
                        description = "encodings.xml 配置项目编码为UTF-8，规范标准",
                        solution = ""
                    )
                )
            } else {
                results.add(
                    DiagnoseResult(
                        level = DiagnoseLevel.ERROR,
                        title = "项目编码非UTF-8",
                        description = "当前项目编码配置为 " + encodeName + "，容易引发中文乱码",
                        solution = "设置 → 编辑器 → 文件编码，将项目编码修改为 UTF-8"
                    )
                )
            }
        }

        return results
    }
}