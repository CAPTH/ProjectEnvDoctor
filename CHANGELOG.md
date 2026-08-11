<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# ProjectEnvDoctor Changelog

## [Unreleased]

## [1.1.0] - 2026-08-11
### Added
- 完整实现 5 个环境检测器：JDK、Maven、Gradle、编码、Git规范
- 诊断结果弹窗（ResultDialog），表格三色分级展示
- 一键复制完整诊断报告到剪贴板
- DiagnoseResult 数据模型（DiagnoseLevel枚举 + DiagnoseResult单项 + DiagnoseSummary汇总）
- BaseChecker 统一检测器接口

### Fixed
- 修复中文Windows下Kotlin编译器GBK编码解析UTF-8源文件导致的语法错误
- 修复ToolbarDecorator和JBUI API使用错误

## [1.0.0] - 2026-08-11
### Added
- 项目基础框架搭建
- 右键菜单Action注册（ScanEnvAction）
- plugin.xml 插件配置
- Gradle IntelliJ Plugin 构建配置
