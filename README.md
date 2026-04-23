# 手机NAS (MobileNAS) - 通过浏览器管理手机

手机NAS是一个开源的Android应用，允许用户通过Web浏览器安全地管理手机内容。

## 功能特性

- 🌐 **内置浏览器** - 通过手机本地服务器提供Web浏览体验
- 📥 **下载管理** - 支持文件下载、进度跟踪、下载历史
- ⚙️ **自定义设置** - 可配置主页、搜索引擎、下载路径等
- 🔒 **隐私模式** - 支持无痕浏览
- 📱 **本地服务器** - 内嵌NanoHTTPD服务器，多端口自动回退

## 构建状态

[![Android CI](https://github.com/2182977liu-bit/MobileNAS/actions/workflows/android-build.yml/badge.svg)](https://github.com/2182977liu-bit/MobileNAS/actions)

## 如何使用

1. 安装应用到手机
2. 打开应用并启动服务器
3. 在浏览器中访问显示的地址
4. 开始管理手机内容

## 技术栈

- **语言**: Kotlin
- **最低SDK**: 21 (Android 5.0)
- **目标SDK**: 34 (Android 14)
- **依赖**: OkHttp, NanoHTTPD, Kotlin Coroutines

## 构建说明

### 本地构建

```bash
# 克隆仓库
git clone https://github.com/2182977liu-bit/MobileNAS.git

# 进入项目目录
cd MobileNAS

# 构建Release APK
./gradlew assembleRelease

# APK输出位置
# app/build/outputs/apk/release/app-release.apk
```

### 自动构建

每次推送到 main 分支时，GitHub Actions 会自动构建 Release APK。

## 许可证

AGPL-3.0

---

## 更新日志

### v1.0.0 (2026-04-23)

#### 重构
- 项目更名为「手机NAS (MobileNAS)」
- 包名从 `com.example.plainapp` 变更为 `com.mobilenas.app`
- Application 类从 `PlainAppApplication` 重命名为 `MobileNASApplication`

#### 修复
- 添加缺失的 Gradle Wrapper 文件（gradlew、gradlew.bat、gradle-wrapper.jar）
- 创建缺失的 MainActivity.kt（Manifest 中声明的启动 Activity）
- 创建缺失的应用图标资源（mipmap-hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi）
- 修复 BrowserHttpHandler 错误继承 NanoHTTPD（2.3.1 无对应构造函数），改为普通类
- 修复 SimpleWebServer 调用 NanoHTTPD 2.3.1 中不存在的 API（awaitShutdown、onShutdown、listeningPort）
- 修复 suspend 函数在非协程环境中直接调用的问题（使用 runBlocking 包装）
- 添加 BrowserManager.kt 中缺失的 URLDecoder import

#### 新功能
- 初始化项目，包含浏览器功能和 GitHub Actions 自动构建
