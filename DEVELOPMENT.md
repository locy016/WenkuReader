# 文库阅读开发、验收与 USB 发布

文库阅读面向 Android 手机和平板。本手册记录书库、阅读器和发布流程的维护入口，以及正式签名、模拟器验收和 USB 升级步骤。

## 修改入口

下表以文库阅读开发工作树为准。仍在开发中的文件以源码路径标记。

| 工作内容 | 主要入口 |
| --- | --- |
| 应用版本、发布变体、APK 命名与原生库校验 | [app/build.gradle.kts](app/build.gradle.kts) |
| 书库分组、搜索展开、文件导入、续读和筛选 | [UnifiedLibraryScreen.kt](app/src/main/java/com/aryan/reader/UnifiedLibraryScreen.kt)、[UnifiedLibrarySections.kt](app/src/main/java/com/aryan/reader/UnifiedLibrarySections.kt) |
| 圆角、描边和书库底部按钮 | `app/src/main/java/com/aryan/reader/LibraryAppearance.kt` |
| 封面、类别和开书动画 | [ThemedBookCover.kt](app/src/main/java/com/aryan/reader/ThemedBookCover.kt)、`app/src/main/java/com/aryan/reader/LibraryBookCategory.kt`、`app/src/main/java/com/aryan/reader/LibraryBookOpening.kt` |
| 中文 TXT 章节识别和目录标题 | `app/src/main/java/com/aryan/reader/epub/ChineseTxtChapters.kt`、`app/src/main/java/com/aryan/reader/epubreader/TxtChapterTitles.kt` |
| 书籍打开、恢复进度和阅读器路由 | [MainViewModel.kt](app/src/main/java/com/aryan/reader/MainViewModel.kt)、[AppNavigation.kt](app/src/main/java/com/aryan/reader/AppNavigation.kt) |
| Android 启动图标 | `app/src/main/res/mipmap-anydpi-v26/wenku_launcher.xml` 引用彩色前景、背景和单色资源，预览见 `branding/wenku-reader` |

Android 端的运行标识为 `app.juanzhou.reader`，代码命名空间为 `com.aryan.reader`。产品显示名和 APK 文件名使用「文库阅读」与 `WenkuReader`。覆盖升级依赖应用 ID、签名和递增的 `versionCode`，品牌更新时一起检查这三项。

## 产品和设计经验

- **书库首页**直接提供本地导入和阅读。保留类别分组，网格按五列排布并显示封面，列表集中显示书名、作者、格式、标签及阅读进度。首页整理时一并核对空书库、搜索结果、分类标题和窄屏转行。导入、搜索和分类集中在书库，历史上的独立书架、侧菜单和不可用书源已从产品入口移除。
- **按钮**沿用 4 dp 小圆角。状态组从左至右为全部、正在阅读、已阅、未读，使用藏书、书签、对勾、合上的书四种图标。当前状态用主题色底标出，纯图标入口提供中文无障碍名称。底部独立一行，左侧为「导入文件」，右侧为使用打开书本图标的「继续阅读」，两项均显示文字。
- **搜索**默认收起为标题栏图标。打开后输入书名或作者，已有正文搜索能力按选项启用。键盘收起时保留输入与结果，退出搜索时恢复书库。
- **封面**以浅色纸面、薄书脊、页边和投影表达装帧。自动封面按书籍固定分配浅色渐变，同组书籍也可呈现多种色调。书名放在上半部，作者放在下方。长中文标题以近似等长的行优先排版。书籍有自带封面或文档预览图时保留原图。
- **开书与续读**点击书封时，从实际来源封面做短过渡，加载沿用已有路径。书库和正文分阶段淡出、淡入，避免两层字同时可见。遇到打开慢，分别观察首开、重复打开、首个可读页面和续读定位。改动加载策略时一并验证旧版导入数据和阅读位置。
- **阅读状态**正文支持翻页。隐藏系统顶部状态栏，底部以真实章节名替代泛化的 Part 标签。唤出上下工具栏时隐藏底部提示，顶部和底部工具栏及章节区用适度透明度。去掉 FPS 和「推理分段」提示时，保留真实章节顺序、目录条目和跳转位置。

## 验收安排

1. 用模拟器和合成样本验证改动。书库界面调整核对截图、点击和受影响的手机／平板宽度。图标替换只需核对显示、可读名称和对应动作。
2. 涉及阅读器时，选取相关 TXT、EPUB、PDF 样本做短流程验证。用户安排真机人工复核时，可在已安装的正式版上使用平板 `EBook` 里的《完美世界》和《东周列国志》，覆盖目录顺序、目录跳转、退出再打开后的续读。真实原书和设备截图留在仓库外。模拟夹具、截图和临时 APK 可放在项目父目录的 `验证/` 下。
3. 分别记录构建成功、模拟器行为验证、USB 安装和人工复核。设备即将离线且用户要求先部署时，先执行已具备发布条件的覆盖安装，剩余阅读或视觉复核单独记录。

## Android 构建

正式包使用 `ossRelease`。当前仓库固定使用 JDK 21、Gradle Wrapper 9.3.1、`compileSdk 37`、`targetSdk 36` 和 NDK `28.2.13676358`。变更工具链前以 Gradle 配置中的值为准。版本号和开发版后缀统一在 [app/build.gradle.kts](app/build.gradle.kts) 修改。

```powershell
.\gradlew.bat :app:assembleOssRelease
```

Gradle 将重命名后的包放在 `app/build/outputs/renamed-apk/ossRelease/WenkuReader-v<版本>-release.apk`。以签名验证结果判定它是否可用于正式安装。

### Windows 离线构建经验

本机依赖缓存完整时，可以从 `D:\DevTools\cache\juanzhou-workspace` 构建。它是指向源码目录的 ASCII 路径联接，用于兼容部分原生工具的路径处理。同一工作树只跑一个正式打包任务。以下是本机 0.1.x 发布使用过的离线参数，开始前设置好 `JAVA_HOME` 与 `ANDROID_HOME`。

```powershell
Set-Location -LiteralPath 'D:\DevTools\cache\juanzhou-workspace'
$nativeDir = 'D:\DevTools\cache\episteme-native-v1.0.59-oss'
& 'D:\Gradle\gradle-9.3.1\bin\gradle.bat' :app:assembleOssRelease --offline --max-workers 6 `
    --console=plain --no-configuration-cache `
    -x :shared:kmpPartiallyResolvedDependenciesChecker `
    '-Pkotlin.daemon.jvmargs=-Xmx5g' '-Pandroid.overridePathCheck=true' `
    "-PwenkuReader.prebuiltNativeDir=$nativeDir" '-PwenkuReader.useVerifiedReleaseNative=true'
```

依赖未缓存时从常规构建开始。如果已有缓存依赖本机镜像，沿用对应的 `--init-script` 参数。离线例子中的 `-x` 只用于本机已经解析共享依赖的情况。跨模块或依赖改动使用正常检查。预编译原生库来自固定的 Episteme v1.0.59 OSS ARM 产物，Gradle 对规定的库和 SHA-256 逐项校验。修改原生代码时重新构建并校验所需 ABI。

模拟器使用能运行当前 ARM 原生库的系统镜像。Windows 上遇到 SwiftShader 的 QEMU 进程退出时，可用同一 AVD 的宿主 GPU 后端复验，结合 Windows 事件和 Android 日志区分宿主崩溃与应用崩溃。在已安装正式签名的模拟器上，用同签名的候选包覆盖可以保留测试样本。

## 版本源码和发布许可

1. 将版本号及版本代码同步到 Gradle。构建生成「本版本源码」链接，目标为公开 GitHub 仓库的版本标签 v<versionName>。签名前核对版本、许可和原生库来源，模拟器验收通过后将标签和提交一起推送到 main。分发前实际打开该版本链接，确认公开可读且标签与构建对应。已经分发的版本标签保持指向原提交。
2. 正式包应提供对应源码及构建说明。核对 APK 中 assets/legal 的 AGPL、LGPL、GPL 正文、第三方源码和构建实际解析出的 Android 依赖清单。原生依赖发生变化时更新 [第三方源码清单](licenses/THIRD-PARTY-SOURCES.txt)，保留随组件分发的版权声明和许可，检查固定的 Rust 依赖清单。
3. libmobi 采用独立动态库。参照 [libmobi 替换说明](licenses/RELINKING.md) 验证替换、重新签名并安装的流程。记录正式包的 SHA-256、源码标签及提交号，并与 APK 一起交付版本源码链接。
4. 公开仓库存放对应源码、构建和许可材料。APK、签名密钥、本地配置、测试书籍和测试记录按本机发布流程保存。

## 正式签名和 USB 升级

1. 使用既有正式密钥签名。密钥和密码通过本机安全存储提供，签名后用 `apksigner verify --verbose --print-certs` 校验签名，用 `aapt2 dump badging` 校验 `app.juanzhou.reader`、目标版本、递增的版本代码和发行包属性。现有正式版的签名证书 SHA-256 为 `1fad1b1aea4485f4aa5c0373c245ec506d3fe3a26aeb6b7d9ce2573119db2680`。记录已验收 APK 的文件 SHA-256，部署时使用同一产物。
2. 用 `adb devices -l` 识别 USB 设备。联想平板型号为 `TB322FC`，指令用 `-s` 指定当次查到的序列号。安装前读取当前版本，遇到签名冲突或版本冲突时保留现有应用，先查明原因。
3. 使用覆盖安装。下面的变量分别指 ADB 路径、联想平板序列号和已验收的签名 APK 路径。

   ```powershell
   & $adb -s $tabletSerial install --no-incremental -r $signedApk
   if ($LASTEXITCODE -ne 0) { throw 'USB 安装失败' }
   & $adb -s $tabletSerial shell dumpsys package app.juanzhou.reader |
       Select-String 'versionName=|versionCode='
   ```

4. 确认安装返回 `Success`，设备版本与目标一致。根据部署指示做简短启动或界面复核。正式产物归档到项目父目录的 `release/<版本>/文库阅读-<版本>.apk`，核对归档副本的文件哈希。

### 书库和进度的处理

- 覆盖安装默认保留应用数据。验收时需要区分“系统确认覆盖成功、应用数据目录保留”和“实际核对书库及章节续读”。对于后者，打开代表性书籍核对定位即可。
- 「从书库移除」是项目内的可恢复操作，可从设置恢复书籍及进度。删除原文件或重置进度属于另外的操作。
- 当前正式版没有独立的本地书库及进度重置入口。若用户另外授权清除应用存储，先说明会一并清除设置、书签、批注和仅保存在应用内的导入副本。操作前后核对平板 `EBook` 原文件目录，启动应用确认状态。重新授权本地文件夹会触发扫描，新加入的书籍与重置前的进度分别记录。
