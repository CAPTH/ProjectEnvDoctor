package com.doctor.checker

import com.doctor.model.DiagnoseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 所有检测器统一父接口
 */
interface BaseChecker {
    /**
     * 执行检测
     * @param project 当前项目对象
     * @param projectRoot 项目根目录虚拟文件
     * @return 检测结果集合
     */
    fun check(project: Project, projectRoot: VirtualFile): List<DiagnoseResult>
}