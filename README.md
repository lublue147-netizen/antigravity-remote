# 🚀 Antigravity Remote

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Version-v1.0.7-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />
</p>

<p align="center">
  <a href="https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.7/app-release.apk">
    <img src="https://img.shields.io/badge/📥%20下载-最新%20v1.0.7%20Release%20APK-1A73E8?style=for-the-badge&logo=android&logoColor=white" />
  </a>
</p>

A native and containerized Android client for remotely controlling **Google Antigravity** AI coding agents. Monitor, chat with, and manage your AI sessions directly from your Android phone or tablet.

一款专为 **Google Antigravity** AI 编码助手打造的 Android 远程控制客户端，支持双模操控（官方网页版全屏容器 + 自建 WebSocket 原生面板）。

---

## 🌟 核心特性 (Features)

### 1. 🌐 官方网页版全屏沉浸容器 (Web Remote Container)
- **多账号自动选择与持久记忆 (v1.0.7 重磅优化)**：
  - **自动选择上次账号**：自动侦测并记录每次会话与浏览使用的 Google 账号（`/u/0/`、`/u/1/`、`/u/2/`...）。重新打开应用时**自动直达上次使用的账号**，告别每次反复手动重选的麻烦！
  - **防止误重置**：修复 Compose 界面刷新与键盘弹出时 WebView 错误重载至默认根路径的问题，保持当前工作区与账号会话稳定。
  - **便捷多账号切换面板**：悬浮菜单直观展示当前账号角标（如 `u/1`），支持一键秒切至 `u/0` / `u/1` / `u/2` / `u/3`、唤起 Google 官方账号选择器或新增账号。
  - **Cookie 深度落盘**：集成 Activity 生命周期与页面路由监听，实时调用 `CookieManager.flush()` 确保 Google OAuth 凭证与选择器状态安全持久化。
- **零黑边自适应**：自动注入 Viewport 与 CSS，将桌面版 Antigravity 界面满屏拉伸铺满移动设备屏幕，彻底消除 16:9 桌面版在长屏手机上的上下大黑边。
- **全能文件下载支持**：
  - **原生 HTTP/HTTPS 下载**：自动附加 Google OAuth 登录 Cookie 和 User-Agent，接管系统 `DownloadManager` 下载任务。
  - **SPA Blob / Data 内存流下载**：深度拦截前端动态生成的 `blob:` 和 `data:` 导出文件（如代码 Zip 包、diff 补丁、导出文档），桥接 Native 并保存至系统 Downloads 目录。
  - **状态栏完成通知**：下载成功后自动推送系统通知与 Toast，点击即可直接查看或打开文件。
- **新窗口与文件上传支持**：支持 `target="_blank"` 新标签拦截与 `<input type="file">` 唤起手机系统相册/文件管理器。
- **状态栏安全避让**：保留顶部系统状态栏与前置挖孔/刘海屏安全间距，左上角 `←` 返回按钮、工作区名称与会话标题完整露出，100% 轻松点击。
- **软键盘智能避让与滚屏**：配置 `adjustResize` 与 Compose `.imePadding()`，键盘弹出时自动平滑顶起，并自动将当前正在输入的文本框推至可视安全区。
- **Google OAuth 登录兼容**：内置持久化 Cookie 管理，模拟 Chrome Mobile UA 并剥离 WebView 阻断标识，无缝支持 Google 账号授权登录。
- **全手势与悬浮菜单**：支持系统边缘侧滑返回、硬件返回键后退，并配备半透明快捷悬浮球（刷新、后退、切换账号、修改链接、切换模式），打字时智能自动隐藏。

### 2. ⚡ 原生 WebSocket 控制面板 (Native Dashboard)
- **实时会话流**：通过 WebSocket 与 Antigravity 后端服务双向通信，实时推送 Agent 状态与消息流。
- **决策审批**：针对 Agent 请求确认的操作，提供一键批准（Approve）与拒绝（Reject）原生交互。
- **状态通知**：后台任务提醒与震动通知。

### 3. 🔐 统一永久签名密钥 (Unified Permanent Keystore)
- 内置固化项目签名证书 `app/release.jks`（有效期至 2054 年）。
- **支持直接覆盖安装升级**，绝不再报签名冲突错误。

---

## 📥 下载安装 (Download)

访问 [Releases 页面](https://github.com/lublue147-netizen/antigravity-remote/releases) 获取最新安装包：

| 版本 | 文件类型 | 说明 | 下载直链 |
| :--- | :--- | :--- | :--- |
| **v1.0.7** | **Release (正式版)** | 推荐使用，新增多账号自动选择与记忆、快捷账号切换，签名兼容 | [下载 app-release.apk](https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.7/app-release.apk) |
| **v1.0.7** | **Debug (调试版)** | 包含调试日志输出 | [下载 app-debug.apk](https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.7/app-debug.apk) |

---

## 🚀 使用指南 (Getting Started)

### 方式一：连接官方网页版（最简单、最推荐）
1. 在电脑端浏览器打开正在运行的 Antigravity 远程会话，复制会话链接（形如：`https://antigravity.google.com/u/1/r/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx?p=...`）；
2. 打开手机 App，点击「配置会话链接」或在「设置」中粘贴该链接；
3. 点击「加载运行」，登录您的 Google 账号后即可使用；
4. 拥有多个 Google 账号的用户，App 会**自动记忆并锁定您最后使用的账号**，再次进入无需重新选择！

### 方式二：连接自建 WebSocket 代理服务
1. 启动支持 Antigravity Remote 的桥接代理服务（如 [OmniAntigravityRemoteChat](https://github.com/diegosouzapw/OmniAntigravityRemoteChat) 或自定义代理）；
2. 在 App「设置」页输入服务地址（例如 `192.168.1.100:3000`）及可选的 Auth Token；
3. 保存后切换至「会话列表」并点击「连接」，即可使用纯原生 Compose 面板。

---

## 🏗️ 技术架构 (Architecture)

```
com.antigravity.remote/
├── data/
│   ├── local/          # DataStore 首选项持久化 (URL, 多账号记忆, 主题, 沉浸模式)
│   ├── model/          # 数据模型 (AgentSession, ChatMessage, ConnectionState)
│   ├── remote/         # WebSocket 通信服务
│   └── repository/     # 统一 Repository 仓库层
├── di/                 # Dagger Hilt 依赖注入模块
├── util/
│   ├── AccountUrlHelper.kt       # 多账号 URL 路由解析与切换工具
│   ├── DownloadHelper.kt         # 系统下载管理器与文件导出辅助
│   └── BlobDownloadInterface.kt  # Blob / Base64 数据流桥接接口
└── ui/
    ├── navigation/     # Jetpack Compose Navigation (包含沉浸式全屏与安全避让)
    ├── screen/
    │   ├── WebRemoteScreen.kt  # 官方网页版全屏容器 (多账号自动恢复 + 自适应拉伸 + 悬浮菜单)
    │   ├── DashboardScreen.kt  # 原生会话列表与连接状态
    │   ├── ChatScreen.kt       # 原生消息流与决策审批
    │   └── SettingsScreen.kt   # 应用设置与双模配置 (多账号自动记忆开关)
    ├── theme/          # Material 3 深色/浅色自适应主题
    └── viewmodel/      # MainViewModel 业务状态流
```

---

## 📦 云端自动化构建 (CI/CD)

本项目配置了完整的 **GitHub Actions** 自动化流水线（`.github/workflows/build-release.yml`）：
- **触发条件**：推送至 `main` 分支或创建 `v*` 语义化版本 Tag；
- **产物分发**：构建完成后自动上传 Debug/Release APK Artifact，并直接发布至 GitHub Releases。
