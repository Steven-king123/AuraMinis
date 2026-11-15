<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/brand/icon-dark.png">
    <img src="assets/brand/icon-light.png" alt="AuraMinis 图标" width="88" height="88">
  </picture>
</p>

<h1 align="center">AuraMinis</h1>

<p align="center">An unofficial enhanced Android AI assistant fork of OpenMinis.<br>Controllable memory, a customizable look, and comfortable reading.</p>

![AuraMinis 品牌横幅](assets/brand/banner.png)

**AuraMinis** 是基于 [OpenMinis](https://github.com/OpenMinis/OpenMinis) 的非官方 Android AI 助手增强分支。在保留上游 Agent 架构与本地工具底座的基础上，围绕日常使用打磨，提供**更可控的长期记忆、更自由的聊天外观，以及更舒适的阅读与交互体验**。

## 不止于聊天

从对话到执行，AuraMinis 延续了 OpenMinis 的移动端 Agent 与工具能力：

- **多模型与多提供商接入**：使用自己的模型配置与凭据，按需接入和切换不同模型。
- **设备上的 Linux 工作区**：无需获取手机 Root 权限的 Alpine Linux / PRoot 沙箱，支持安装软件包、运行脚本与处理文件，在 Android 系统及应用权限边界内运行。
- **浏览器与自动化交互**：访问网页、读取资料，借助工具完成操作。
- **Android 系统工具集成**：按授权使用日历、剪贴板、媒体等设备能力，衔接日常工作流。
- **Agent 生态**：延续上游的技能扩展（Skills）、文件工作区、持久记忆、MCP、子代理和定时任务能力。

## 为日常使用打磨

### 可控的长期记忆

在原版 `GLOBAL.md` 与每日日志之外，新增独立、可管理的结构化高级记忆：

- **分层保存与混合检索**：核心记忆保存长期信息，事件摘要记录经历与进展；结合关键词与向量检索，按相关性召回已保存的记忆。
- **召回由你控制**：分别选择总结模型与向量模型，调整相关性阈值和最多召回数量，没有合适结果时不强塞无关记忆。
- **管理与测试可视化**：查看、搜索、新增、编辑或删除记录，通过阈值测试观察候选分数及接受原因。
- **按需开启**：自动整理默认关闭；关闭高级记忆时收起相关入口，重新开启恢复，不清空配置与记录。

> 高级记忆检索已保存的记录，不会自动导入标准记忆或扫描全部历史聊天。[了解更多](docs/ADVANCED-MEMORY.md)。

### 自由定制的聊天外观

- **背景与主题遮罩**：设置全局聊天背景，调节遮罩强度并预览；背景既可固定，也可随键盘与输入框高度自动适配。
- **实时玻璃效果**：顶栏、输入区与浮动工具状态栏呈现通透的背景模糊与界面层次。
- **可调消息底色**：设置背景图片后，可统一调整用户消息、助手正文与展开思考区域的底色不透明度，文字本身保持不透明。

背景图片仅用于本地界面，不会作为附件发送给模型。

### 更舒适的阅读与交互

- **原生聊天与公式优化**：保留原生聊天列表，修复部分独立数学公式、列表内公式和流式输出中的解析问题。
- **更清爽的设置**：记忆模型支持按名称、ID 或提供商搜索；说明按需查看，保留必要的隐私、费用及危险操作确认。
- **减少重复开销**：优化界面渲染和状态观察，改善启动与键盘升降的交互体验。

## 典型使用场景

- **资料研究与整理**：访问网页收集资料，在 Linux 工作区中分析并生成结构化文件。
- **借助工具处理任务**：结合系统工具、脚本和 MCP 扩展处理文件转换、日程安排等实际操作，而不只给出文字步骤。
- **积累项目上下文**：保存长期偏好与阶段进展，随时检索、修正记忆或测试召回效果，减少重复交代背景。
- **阅读技术内容**：在喜欢的背景和界面风格中，阅读包含列表、表格与数学公式的长回答。

## 获取、文档与构建

AuraMinis 正式安装包正在准备中。可浏览[项目仓库](https://github.com/Steven-king123/OpenMinis)获取源码；构建前请确认所选分支包含需要的增强功能。

| 资源 | 说明 |
|---|---|
| [高级记忆使用指南](docs/ADVANCED-MEMORY.md) | 记忆类型、模型配置、召回与整理 |
| [完整构建说明](BUILDING.md) | Android 工具链与编译步骤 |
| [问题反馈](https://github.com/Steven-king123/OpenMinis/issues) | 提交问题或改进建议 |
| [MinisSkills](https://github.com/OpenMinis/MinisSkills) · [AwesomeMinis](https://github.com/OpenMinis/AwesomeMinis) | 上游技能与社区资源 |

## 隐私、致谢与许可证

**隐私与费用**：记忆记录与背景图片可保存在本机；聊天、总结和嵌入操作会按配置将相应文本发送至模型服务，可能产生费用。在设备上运行，不代表所有处理离线。

**上游归属**：感谢 [OpenMinis](https://github.com/OpenMinis/OpenMinis) 提供核心 Agent 架构与工具体系。官方网站：[openminis.app](https://openminis.app)。部分界面与交互设计参考了 [LastChat](https://github.com/Cocolalilal/LastChat)；相关增强在 OpenMinis 代码基础上实现，并使用相应开源组件，第三方代码及依赖的授权以各自许可证为准。

**许可证**：本项目沿用 [GNU GPL v3.0](LICENSE)。上游及第三方组件的版权与授权信息见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。
