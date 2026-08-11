# ProjectEnvDoctor
IDEA 插件：项目环境一键检测工具

## 📖 项目简介
ProjectEnvDoctor 是基于 IntelliJ Platform 使用 Kotlin 开发的 IDEA 插件，提供**项目视图右键一键检测项目环境**功能，快速采集项目 SDK、构建工具、编码配置等信息，快速排查项目环境异常。

支持 IDEA 2025.3+
开发语言：Kotlin
当前版本：V1.1

## ✨ 功能特性
- 项目树右键菜单：【项目环境一键检测】
- 自动绑定当前激活项目
- 环境信息采集
  - 项目 SDK/JDK 版本检测与兼容性校验
  - Maven 项目检测（pom.xml 完整性、settings.xml 镜像配置）
  - Gradle 项目检测（构建文件完整性、仓库配置）
  - 文件编码检测（.idea/encodings.xml UTF-8 校验）
  - Git 规范检测（.gitignore 存在性与规则完整性）
- 诊断弹窗三色分级展示（正常/警告/错误），每条附带修复方案
- 支持一键复制完整诊断报告
- 同时兼容 IDEA 社区版 & 旗舰版

## ️ 开发环境
- 构建工具：Gradle IntelliJ Plugin
- Gradle DSL：Kotlin DSL（build.gradle.kts）
- 开发IDE：IntelliJ IDEA
- **Plugin SDK：由 gradle intellijPlatform 自动下载，无需本地手动配置**
- JDK 要求：**JDK 17（IntelliJ Platform 2025.3 最低要求JDK17）**
> ⚠️ 重要提醒：
> 运行 `runIde` / `buildPlugin` 任务必须使用 JDK17，不要使用高版本JDK，容易出现平台兼容性异常。

##  项目目录结构
```
ProjectEnvDoctor/
├── src/main/
│   ├── kotlin/com/doctor/
│   │   ├── action/
│   │   │   └── ScanEnvAction.kt          # 右键菜单Action，串联所有检测器
│   │   ├── checker/                      # 环境检测器
│   │   │   ├── BaseChecker.kt            # 检测器统一接口
│   │   │   ├── JdkChecker.kt             # JDK/SDK版本检测
│   │   │   ├── MavenChecker.kt           # Maven项目检测
│   │   │   ├── GradleChecker.kt          # Gradle项目检测
│   │   │   ├── EncodingChecker.kt        # 文件编码检测
│   │   │   └── GitIgnoreChecker.kt       # .gitignore规范检测
│   │   ├── model/
│   │   │   └── DiagnoseResult.kt         # 诊断结果数据模型
│   │   ── ui/
│   │       └── ResultDialog.kt           # 诊断结果弹窗UI
│   └── resources/
│       ── META-INF/
│           └── plugin.xml                # 插件扩展点、Action注册配置
├── build.gradle.kts                      # Gradle构建脚本
├── gradle.properties                     # Gradle配置（含编码设置）
├── CHANGELOG.md                          # 版本变更日志
├── README.md                             # 项目文档
└── 项目开发文档.md                       # 项目设计与开发文档
```

## 🚀 本地调试运行
1. IDEA 打开项目根目录，等待Gradle同步完成
2. 使用 JDK 17 作为项目Gradle JVM
3. 执行 Gradle Task：`intellijPlatform -> runIde`
4. 启动沙箱IDE，导入测试项目
5. 左侧项目树目录/文件右键，点击【项目环境一键检测】

## 📦 插件打包
执行打包命令：
```shell
./gradlew buildPlugin
```
输出目录：`build/distributions/ProjectEnvDoctor-xxx.zip`

使用方式：
1. IDEA → 插件 → 从磁盘安装插件，选择zip文件本地测试
2. 打包产物可用于上传 JetBrains Marketplace 发布

## ⚙️ plugin.xml 关键配置说明
```xml
<actions>
    <group id="ProjectEnvDoctor.ProjectPopupGroup"
           text="项目环境工具"
           popup="false">
        <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
        <action id="com.doctor.action.ScanEnvAction"
                class="com.doctor.action.ScanEnvAction"
                text="项目环境一键检测"
                description="扫描当前项目环境信息">
        </action>
    </group>
</actions>
```
- `ProjectViewPopupMenu`：项目树右键菜单
- `anchor="last"`：放置在右键菜单底部
- group 标签必须配置 `text` 属性，否则菜单会被IDE隐藏

## 📋 代码开发规范
1. 所有 `AnAction` 重写 `getActionUpdateThread()` 返回 `ActionUpdateThread.BGT`，规避EDT线程警告
2. **禁止在 `update()` 方法读取虚拟文件、访问项目数据**，只做启用/可见性判断
3. 所有UI弹窗逻辑统一放在 `actionPerformed`，杜绝阻塞UI线程
4. 新增/修改的源码文件头部添加注释说明功能
5. group标签必须配置text属性，否则分组菜单会被IDE隐藏
6. 中文Windows环境下，字符串中变量拼接使用 `+` 拼接而非 `$` 插值，避免Kotlin编译器GBK编码解析异常

## ️ 踩坑记录
1. plugin.xml 中 `<group>` 缺少 text 属性 → 右键菜单直接消失
2. Action未指定 `ActionUpdateThread.BGT` → IDE日志持续输出EDT线程违规警告
3. `update()` 中读取项目、文件对象 → 性能警告、潜在卡顿
4. Gradle JVM版本不匹配，使用过高JDK导致沙箱启动失败
5. 不要在UI线程执行耗时环境扫描逻辑
6. **中文Windows下Kotlin编译器编码问题**：UTF-8源文件中 `$variable` 紧跟中文字符时，编译器以GBK读取导致多字节序列误解析，报 `Syntax error: Expecting ')'`。解决方案：使用字符串拼接 `"..." + var + "..."` 替代插值
7. `ToolbarDecorator` 无 `setAddButtonText()` 方法，`JBUI.emptyInsets()` 不接受参数且返回类型非 `Border`

## 📄 License
MIT License
