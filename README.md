# 手机NAS (MobileNAS) - 通过浏览器管理手机

手机NAS是一个开源的Android应用，允许用户通过同一WiFi下的Web浏览器远程管理手机内容。

## 功能特性

### 📁 文件管理
- 浏览手机内部存储和SD卡
- 文件下载、上传、删除、重命名
- 创建新目录
- 多存储挂载点支持

### 📸 媒体浏览
- 图片网格浏览，点击查看大图
- 视频列表浏览和下载
- 音频列表浏览和下载
- 分页加载，支持大量媒体文件

### 📱 应用管理
- 查看所有已安装应用
- 显示应用图标、名称、版本号
- 一键导出APK文件

### 📋 设备信息
- 设备型号、品牌、Android版本
- 电池电量、充电状态、健康度
- 存储空间使用情况（进度条展示）
- WiFi名称、IP地址、服务器端口

### 🔔 通知推送
- 实时推送手机通知到浏览器
- 通过WebSocket实时传输

### 🔍 mDNS 发现
- 局域网内自动注册服务
- 支持通过主机名访问

### 🌐 Web 管理界面
- Vue.js 3 响应式前端
- 侧边栏导航，移动端汉堡菜单
- 深色主题，现代UI设计
- 支持桌面和移动浏览器

### ⚙️ 系统特性
- Ktor 嵌入式Web服务器（替代NanoHTTPD）
- Room 数据库持久化
- WebSocket 实时通信
- 前台服务保证后台运行
- 多端口自动回退（8080→8081→8082→8090→3000）

## 构建状态

[![Android CI](https://github.com/2182977liu-bit/MobileNAS/actions/workflows/android-build.yml/badge.svg)](https://github.com/2182977liu-bit/MobileNAS/actions)

## 如何使用

1. 下载最新APK并安装到手机
2. 打开应用，点击「启动服务器」
3. 确保手机和电脑在同一WiFi下
4. 在浏览器中访问应用显示的地址（如 `http://192.168.1.100:8080`）
5. 开始管理你的手机！

## API 接口

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/device/info` | GET | 设备信息 |
| `/api/files?path=` | GET | 文件列表 |
| `/api/files/download?path=` | GET | 下载文件 |
| `/api/files/delete` | POST | 删除文件 |
| `/api/files/rename` | POST | 重命名文件 |
| `/api/files/mkdir` | POST | 创建目录 |
| `/api/files/mounts` | GET | 存储挂载点 |
| `/api/media/images` | GET | 图片列表 |
| `/api/media/videos` | GET | 视频列表 |
| `/api/media/audio` | GET | 音频列表 |
| `/api/media/file?path=` | GET | 媒体文件 |
| `/api/apps` | GET | 已安装应用 |
| `/api/apps/export?package=` | GET | 导出APK |
| `/api/downloads` | GET | 下载列表 |
| `/api/settings` | GET | 设置 |
| `/api/settings` | PUT | 更新设置 |
| `/ws` | WebSocket | 实时事件 |

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin |
| Web服务器 | Ktor 2.3.7 (Netty) |
| 数据库 | Room 2.6.1 |
| 序列化 | Kotlinx Serialization |
| HTTP客户端 | OkHttp 4.12.0 |
| 前端 | Vue 3 + Vue Router 4 |
| 实时通信 | WebSocket |
| 服务发现 | mDNS (NsdManager) |
| 最低SDK | 21 (Android 5.0) |
| 目标SDK | 34 (Android 14) |
| Gradle | 8.5 |
| AGP | 8.2.2 |

## 构建说明

```bash
git clone https://github.com/2182977liu-bit/MobileNAS.git
cd MobileNAS
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/mobilenas-1.0.0.apk
```

每次推送到 main 分支时，GitHub Actions 会自动构建 Release APK。

## 许可证

AGPL-3.0

---

## 更新日志

### v1.1.0 (2026-04-24)

#### 新功能
- **文件管理API** - 目录浏览、文件下载、删除、重命名、创建目录、存储挂载点查询
- **设备信息API** - 设备型号、电池状态、存储空间、网络信息
- **媒体浏览API** - 图片/视频/音频分页查询，通过MediaStore获取
- **应用管理API** - 已安装应用列表、APK导出
- **通知推送** - NotificationListenerService实时推送通知到浏览器
- **mDNS服务发现** - 局域网内自动注册，支持主机名发现
- **Vue.js前端** - 完整的Web管理界面，包含7个页面：
  - 首页仪表盘（设备信息卡片+快捷入口）
  - 文件管理（面包屑导航+文件操作）
  - 图片浏览（缩略图网格+大图查看）
  - 视频浏览（列表+下载）
  - 音频浏览（列表+下载）
  - 应用管理（搜索+图标+APK导出）
  - 设备信息（5大分区详情展示）
- **WebSocket通知事件** - 支持推送通知事件到前端

#### 重构
- Web服务器从NanoHTTPD迁移到Ktor 2.3.7 (Netty)
- 引入Room数据库替代文件存储
- 引入Kotlinx Serialization替代手写JSON
- 添加Foreground Service保证后台运行
- 添加前台服务通知（显示服务器地址+停止按钮）
- 项目结构重组（data/server/service/ui/util包）

### v1.0.0 (2026-04-23)

#### 重构
- 项目更名为「手机NAS (MobileNAS)」
- 包名从 `com.example.plainapp` 变更为 `com.mobilenas.app`

#### 修复
- 添加缺失的Gradle Wrapper文件
- 创建缺失的MainActivity.kt和应用图标
- 修复NanoHTTPD API兼容性问题
- 修复suspend函数调用问题
- 添加缺失的URLDecoder import

#### 新功能
- 初始化项目，包含浏览器功能和GitHub Actions自动构建
