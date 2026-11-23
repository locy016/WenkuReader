# libmobi 的源码与替换

libmobi 由 Bartek Fabiszewski 等贡献者开发，以 LGPL-3.0-or-later 提供。发布包中的 libmobi 使用上游对应版本的源码，源码收录在本版本 [app/src/main/cpp/libmobi](../app/src/main/cpp/libmobi) 中，许可正文为其 [COPYING](../app/src/main/cpp/libmobi/COPYING) 以及 [GPL v3](GPL-3.0.txt)。

Android 的 JNI 库 libnative-lib.so 通过动态链接依赖 SONAME 为 libmobi.so 的独立库。可以按下面的流程替换 ABI 兼容的修改版，重打包并使用自己的密钥安装。在 LGPL 适用范围内，允许替换该库、为调试修改版而进行逆向工程，并运行修改后的组合。

## 从源码生成库

取「关于 → 本版本源码」所指的版本标签。需要 Android NDK、Python 3.10 或更新版，以及 Android SDK Build Tools。下面以 PowerShell 和 ARM64 为例。$ndk 和 $tools 指向本机工具目录，$mobiSource 指向修改后的 libmobi 源码目录。省略 --source 时使用本仓库源码。

~~~powershell
python scripts/libmobi/build.py --ndk $ndk --abi arm64-v8a --source $mobiSource --output out/mobi-arm64/libmobi.so
~~~

生成库位于 out/mobi-arm64/libmobi.so。保持库名、SONAME、公开和 JNI 使用的符号兼容。构建 32 位库时使用独立输出目录及 --abi armeabi-v7a。兼容 Android 的 16 KiB 页大小要求同时检查 ELF 加载段和 APK 内未压缩的库对齐。

## 替换、对齐并自行签名

以下示例替换 ARM64。需要同时替换 32 位版本时，再传入一个 --library armeabi-v7a=路径；其余 ABI 保留原库。输入可以是已签名的发布 APK。脚本只生成新的未签名文件，应用资料和原 APK 保持原样。$releaseApk 指向发布包，$myKeystore 指向自己的密钥库。

~~~powershell
python scripts/libmobi/repackage.py $releaseApk out/libmobi-unsigned.apk --library arm64-v8a=out/mobi-arm64/libmobi.so
& "$tools/zipalign" -P 16 -f 4 out/libmobi-unsigned.apk out/libmobi-aligned.apk
& "$tools/apksigner" sign --ks $myKeystore --out out/libmobi-signed.apk out/libmobi-aligned.apk
& "$tools/zipalign" -c -P 16 4 out/libmobi-signed.apk
& "$tools/apksigner" verify --verbose --print-certs out/libmobi-signed.apk
~~~

也可以用自己的密钥给从整个仓库重建的 APK 签名，见 [开发手册](../DEVELOPMENT.md)。Android 覆盖安装要求签名匹配。用已有正式版以外的证书时，请在干净的设备或独立模拟器中安装修改版，避免影响原有书库和数据。

发布前以实际对应版本验证：在干净模拟器安装独立签名的 APK，导入并打开 MOBI 样本，检查修改库确实被加载且可以阅读。分别记录各 ABI 的验证范围。
