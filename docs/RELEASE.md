# AuraMinis 正式版

## 安装与升级

下载入口：[GitHub Releases](https://github.com/Steven-king123/AuraMinis/releases)。首个独立正式版本为 **1.0.0 / versionCode 44**，包名 `io.github.stevenking123.auraminis`，要求 Android 8.0+、ARM64。

它是基于 OpenMinis 的非官方 Fork，**不覆盖官方版或旧 Minis Dev，也不自动继承其数据**。以后同一包名的升级需保持相同签名，更新前建议备份。当前关闭应用内自动检查、手动检查和 APK 自安装，请从本仓库 Release 手动获取安装包。

先保留旧版。需要迁移时，在旧应用导出应用内备份，再由 AuraMinis 显式导入；不要直接复制旧包的加密偏好。跨包恢复尚未完成实机验收，不能保证所有配置和凭据完整迁移，也不应以卸载旧版为迁移前提。

背景图片、完整 Linux 环境、Tailscale 节点身份及部分网络/备份目的地设置不应假定会迁移。通知、悬浮窗、无障碍、Shizuku、外部目录等授权需按新应用重新配置。请勿公开上传含凭据或私人内容的备份。

保留 `minis://` 资源协议以兼容已有消息。与其他 Minis 版本并装时，外部隐式链接可能弹出应用选择器；不要在两个应用同时发起使用相同 localhost 回调端口的 OAuth 登录。

## 构建

完整工具链、原生依赖及 rootfs 准备请先阅读 [BUILDING.md](../BUILDING.md)。不能仅下载 Android 源码就假设 Linux 发行输入已齐全。

原生输入就绪后：

```sh
cd src/android
# 本次构建使用 JDK17、SDK36、NDK28.0.13004108、CMake3.22.1。
# 多版本NDK环境可在独立工作区显式设置 ndkVersion，勿改用户全局配置。
./gradlew --no-daemon --max-workers=1 :app:assembleGithubRelease
```

Release 已启用 R8，`DEBUG` / `DEV_TOOLS` 为 false；没有额外声称已开启资源 shrink。输出位于 `app/build/outputs/apk/github/release/app-github-release-unsigned.apk`。不要使用历史 legacy 输出目录中的旧 APK。

Release 不再用 debug key 自动签名。分发前需用自己长期保存的私钥签署，验证签名并保留备份；**自行签名的包不能覆盖维护者同包名但不同签名的版本**。维护者证书 SHA-256：

```text
326c93f23a79713dfe6b4d8f2174b637c3493020ee3053eb3102c51a3d4e6bcb
```

私钥、密码和服务器配置不在仓库中。`scripts/configure_server_dev.py` 与 `configure_wallpaper_test.py` 保留用于历史开发构建，**不适用于当前 AuraMinis 正式工作区，不要执行**。debug/perf 仍是开发变体，不是正式分发或升级产物。

构建后核验包名、版本、Manifest、签名、沙箱与原生库，归档本批 `mapping.txt` 和 native symbols。R8 配置应包含 Go 绑定及 VAD 回调 keep；不以关闭混淆或全局忽略警告代替兼容检查。

## 图标资源

保留自动、浅色、深色选择和 Android 主题图标。前景与背景独立，主体来自已定稿的折带 A 设计。启动器可能缓存图标，切换不一定立即刷新。

母稿及预览位于 `assets/brand/android/`，原定稿图为 `assets/brand/approved-icon-pair.png`。使用 Pillow 的生成脚本 `scripts/make_auraminis_icons.py` 可重建图像层；XML 与单色资源已提交，需同步检查，不把预览中的圆角烘焙进自适应前景。

## 1.0.0 验证状态

- Release 完整编译、R8 优化和签名完成；143 项定向测试、22 个测试类通过，0失败、错误或跳过；不是全仓或设备测试。另有11项离线身份、品牌与图标检查。
- APK 的独立包名、版本、显示名、提供器、启动别名、不可调试标志及v2/v3签名已检查。
- 14个原生载荷与 rootfs 已检查。rootfs 可以以 `.tar` 形式入包，内含521个条目和 BusyBox。合并 R8 配置含 Go/VAD 的保留规则。
- ARM64库的 ELF 载入段为16KiB对齐，32位PRoot辅助加载器为4KiB；**尚未完成16KiB页设备实测认证**。
- 尚未完成此独立包的实机启动、沙箱、工具、语音、图标切换、登录、记忆及跨包备份恢复验收。此前开发版的正向性能反馈不能代替这一验收。

反馈入口指向本 Fork；不预填上游邮箱，也不自动上传崩溃日志。发现问题时请附版本、复现步骤和脱敏材料。
