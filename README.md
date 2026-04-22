# PlainApp - 通过浏览器管理手机

PlainApp 是一个开源的 Android 应用，允许用户通过 Web 浏览器安全地管理手机内容。

## 功能特性

- 📁 **文件管理** - 浏览和管理手机文件系统
- 🎵 **媒体管理** - 查看和管理照片、视频、音乐
- 👤 **联系人** - 查看和管理联系人
- 💬 **短信** - 查看和管理短信
- 📱 **应用管理** - 查看已安装应用，支持APK导出
- 📞 **通话记录** - 查看通话记录
- 🔔 **通知中心** - 实时查看手机通知
- 📺 **屏幕镜像** - 将手机屏幕镜像到浏览器

## 构建状态

![Android CI](https://github.com/plainhub/plain-app/workflows/Android%20CI/badge.svg)

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
git clone https://github.com/plainhub/plain-app.git

# 进入项目目录
cd plain-app

# 构建Release APK
./gradlew assembleRelease

# APK输出位置
# app/build/outputs/apk/release/app-release.apk
```

### 自动构建

每次推送到 main 分支时，GitHub Actions 会自动构建 Release APK。

## 许可证

AGPL-3.0