# 文库阅读 · WenkuReader

<img src="branding/wenku-reader/launcher-preview.png" width="80" alt="文库阅读图标" />

文库阅读是一款面向 Android 手机和平板的本地阅读应用。把设备里的小说、电子书、文档和漫画导入书库，即可分类查找、离线阅读，并在下次打开时回到上次的位置。

## APP 功能

| 使用场景 | 已实现的功能 |
| --- | --- |
| 导入内容 | 从系统文件选择器导入文件，或授权本地文件夹加入书库。支持 TXT、EPUB、PDF、MOBI／AZW／AZW3／PRC、FB2、Markdown、HTML、Word（DOCX／DOC／DOT）、ODT／FODT、PPTX，以及 CBZ／CBR／CB7／CBT 漫画。 |
| 整理书库 | 按书名或作者搜索，按类别分组，在五列网格和紧凑列表之间切换。TXT 和 EPUB 分别成组，格式标签显示在封面左下角。可按全部、正在阅读、已阅、未读筛选，手动调整分类，从设置恢复已移除的书籍。 |
| 中文小说与电子书 | 识别常见中文 TXT 章节标题，按目录跳转。排版类内容可切换翻页和纵向滚动，调整字体、字号、行距和阅读主题。 |
| PDF 与固定版式文件 | 支持缩放、跳页和通过目录定位。带文字层的 PDF 支持文内搜索，适用文件可使用书签、标注等阅读工具。 |
| 朗读 | 支持在适用的文本和电子书中使用设备语音引擎朗读，可设置声音与读音替换。 |
| 继续阅读 | 记录书籍阅读位置，可从书库的续读按钮回到最近阅读的内容，也可重新打开指定书籍接续。 |

## 源码与开发

- 「设置 → 关于 → 本版本源码」打开对应 Git 标签。0.1.9 的源码见 [v0.1.9](https://github.com/locy016/WenkuReader/tree/v0.1.9)。
- 需要 JDK 21 和对应的 Android SDK 及 NDK。正式构建任务为 `:app:assembleOssRelease`，环境配置、模拟器验收及 USB 升级步骤见 [开发与发布手册](DEVELOPMENT.md)。
- 功能建议和问题可通过 [GitHub Issues](https://github.com/locy016/WenkuReader/issues) 反馈。开发约定见 [项目约束](AGENTS.md)。

## 许可与致谢

文库阅读按 GNU Affero General Public License v3.0 only（AGPL-3.0-only）提供。使用、修改和再分发的条件，以及担保条款，见 [完整许可证](LICENSE)。APK 的「关于 → 许可证」也提供离线许可正文、第三方源码清单和依赖来源。

**修改声明（2026-11-23，文库阅读 0.1.9）** 文库阅读包含 Episteme OSS（© 2026 Episteme）的代码，起点为上游 v1.0.59-oss 的 e3008fd4。文库阅读修改了应用标识和图标、书库交互、中文 TXT 章节识别、阅读界面及 Android 构建和发布流程。上游版权声明及第三方组件各自的许可继续适用。感谢上游作者和贡献者。[上游版本](https://github.com/Aryan-Raj3112/episteme/releases/tag/v1.0.59-oss) · [第三方源码清单](licenses/THIRD-PARTY-SOURCES.txt) · [libmobi 重链接说明](licenses/RELINKING.md)
