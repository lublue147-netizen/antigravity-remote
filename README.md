# 🚀 Antigravity Remote

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Version-v1.0.15-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" />
</p>

<p align="center">
  <a href="https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.15/app-release.apk">
    <img src="https://img.shields.io/badge/📥%20下载-最新%20v1.0.15%20Release%20APK-1A73E8?style=for-the-badge&logo=android&logoColor=white" />
  </a>
</p>

A native and containerized Android client for remotely controlling **Google Antigravity** AI coding agents. Monitor, chat with, and manage your AI sessions directly from your Android phone or tablet.

一款专为 **Google Antigravity** AI 编码助手打造的 Android 远程控制客户端，支持双模操控（官方网页版全屏容器 + 自建 WebSocket 原生面板）。

---

## 🌟 核心特性 (Features)

### 1. 🪟 Windows / Linux 双平台远程实例剪贴板与原生粘贴终极兼容 (v1.0.15 重磅)
- **原生上下文菜单长按粘贴保底拦截 (Native Context Menu Interceptor)**：重写 `WebView.onTextContextMenuItem(android.R.id.paste)`，在用户通过系统长按浮窗选择「粘贴」后执行 120ms 状态检测。若 Windows 实例因 Remote Clipboard Provider 劫持或 `preventDefault` 吞没粘贴事件，系统自动无缝补发通用文本穿透注入，彻底根治 Windows 远程实例下主对话输入框长按粘贴无响应的痼疾。
- **DOM Capture 捕获阶段防吞监听器**：在 DOM 最顶层 `paste` 事件捕获阶段进行监听，即便前端框架或远端服务尝试阻止默认行为，也能感知并在内容未变动时将手机剪贴板内容精准填入。
- **React 16+ 受控组件 Prototype Setter 穿透**：通过调用 `HTMLTextAreaElement.prototype` 原始 setter 与 `document.execCommand('insertText')`，突破 React 虚拟 DOM 状态追踪限制，输入后立即触发 `input` 与 `change` 事件，确保发送按钮立即激活且撤销/重做栈完整保留。
- **完整 Clipboard API 规范模拟**：补齐 `navigator.clipboard.read()`（返回 `ClipboardItem`）与 `navigator.clipboard.write()`，完美适配 Antigravity 多模态截图与文件粘贴检测逻辑。

### 2. 📋 剪贴板复制粘贴全链路修复与增强 (Clipboard Bridge & Smart Actions) (v1.0.14)
- **异步剪贴板 API 原生桥接 (Navigator Clipboard Polyfill)**：突破 Android WebView 缺乏 `clipboard-read` 权限弹窗导致 `navigator.clipboard.readText()` 报错拒绝的限制；在 `onPageStarted` 与 `onPageFinished` 阶段注入原生 `ClipboardManager` 桥接，使 Monaco Editor、代码块复制按钮及网页内各类复制粘贴功能无缝运作。
- **选区与输入双功能悬浮胶囊 (Smart Selection & Paste Bubble)**：
  - 选中文本时，选区上方弹出「📋 复制所选文字」胶囊气泡，点按即拷；
  - 聚焦输入框或富文本编辑器且系统剪贴板有内容时，浮动气泡自动呈现「📥 粘贴剪贴板」快捷按钮；
  - 移除 touchstart/contextmenu 中误清选区的 `removeAllRanges()`，恢复 200ms 防抖等待选区稳定计算，避免气泡瞬闪消失。
- **悬浮菜单 (FAB) 快捷按键兜底**：在半透明悬浮球菜单中新增「复制所选文字」与「粘贴到输入框」，直接穿透至当前激活元素或提示词输入框，提供 100% 稳定的复制粘贴保障。
- **表格链接原生轻触直达**：点击表格内部任意超链接直接弹出原生菜单（复制链接文字、复制链接地址、外部浏览器打开），表格提取操作更流畅。

### 2. 🔋 前台发热与耗电深度治理 (Foreground Thermal & Battery Optimization) (v1.0.13)
- **根除 DOM Mutation 监听与侵入式重绘**：彻底移除后台轮询与 `MutationObserver` 按钮注入，改用零开销的原生事件委托（`Event Delegation`）拦截链接长按、双击与点击事件；消除 AI 流式输出打字时 Chromium 渲染引擎死循环，前台 CPU 占用归零。
- **60Hz 动态锁频节电**：针对 Android 高刷屏（90Hz/120Hz/144Hz）显式将 Activity 刷新率限制在 60Hz，彻底解决因光标闪烁导致屏幕和 GPU 持续锁死在 120Hz 的高额耗电。
- **消除 Monaco 编辑器与 xterm 终端重排干扰**：精简视口自适应规则为 `#root, #app, main` 顶层节点，全面隔离代码编辑器容器，杜绝键盘输入和代码滚动时的全页递归 Layout 计算。
- **Jetpack Compose 状态解耦与 IME 节流**：剥离顶层 URL 重组监听，并将软键盘监听重构为 `WindowInsets.isImeVisible`，消除输入法升降过程中的高频逐帧重组。
- **批量原子持久化与 I/O 降频**：新增 DataStore 单次事务批量更新，停用 SPA 频繁导航过程中的同步 `CookieManager.flush()` 磁盘阻塞。
- **悬浮菜单一键提取表格链接**：在 FAB 悬浮菜单中新增「提取表格链接」按需查询工具，纯按需执行，不带来任何后台常驻开销。

### 2. 🔄 应用内检测更新与多版本选择更新 (In-App Updates & Version Picker) (v1.0.12)
- **智能检测最新版本**：内置 GitHub Releases API 通信模块，支持在【设置】界面手动点击「检查最新版本」或在悬浮球菜单中随时触发；支持开机/启动静默检测，检测到新版本主动弹出升级卡片。
- **选择更新方式**：
  - **应用内直接下载安装**：调用系统 `DownloadManager` 高速下载并注册广播监听，下载完成后自动呼起 Android 系统应用安装器进行无缝覆盖升级。
  - **浏览器直接下载**：一键唤起系统默认浏览器下载 APK，应对特殊网络环境。
  - **GitHub Release 官方主页**：随时跳转查看网页版发布详情。
- **多版本自由选择更新 (Version Picker)**：提供「选择版本更新」面板，实时拉取并展示 GitHub 所有历史发布版本（包含版本号、发布日期、体积大小及当前安装状态），支持自由查看任一版本的更新日志并选择安装该版本。
- **配置持久化**：支持自由开关「启动时自动检查更新」，首选项实时存储于 DataStore。

### 2. 🌐 官方网页版全屏沉浸容器 (Web Remote Container)
- **全面发热与续航功耗优化 (v1.0.11 重磅)**：
  - **MutationObserver 节流防抖与递归自变动过滤**：为 DOM 监听器增加 350ms 防抖节流，内部变动打标隔离，跳过自身插入的工具栏/按钮节点，彻底根除高频流式输出与连续 DOM 变动引发的强制重排（Forced Reflow）与 CPU 满载发烫。
  - **单元格状态标记跳过 (Cell Caching)**：为已处理的单元格添加 `data-ag-linkified` 缓存标记，增量式扫描未处理单元格，大幅缩短单次扫描耗时。
  - **全生命周期休眠机制 (Lifecycle Timers Control)**：深度集成 `LifecycleEventObserver`，当 App 退至后台、锁屏或切换至其他标签页时，自动触发 `webView.onPause()` 与 `webView.pauseTimers()` 彻底冻结 JS 定时器、动画与后台渲染；回到前台自动恢复，杜绝后台空转耗电。
  - **移除多余 GPU 离屏渲染层**：移除 WebView 显式 `LAYER_TYPE_HARDWARE` 配置，利用系统默认硬件加速，消除整屏离屏缓冲双重绘制，显著降低 GPU 显存带宽与发热。
  - **URL 持久化内存去重 (I/O Deduplication)**：ViewModel 层拦截相同 URL 的重复写入，避免频繁 SPA 路由跳转引发无谓的 DataStore 磁盘 I/O 与 Compose 界面多余重组。
- **链接文字与网址独立提取，防范围选区扩散优化 (v1.0.10)**：
  - **表格单元格独立双按钮**：在表格单元格各链接旁独立注入「📝 复制文字」与「🔗 复制链接」快捷操作胶囊，单指轻触即可精确提取链接文字或真实网址，提供清晰震动反馈与 Toast 提示。
  - **原子化文本隔离保护 (Atomic Selection Containment)**：针对表格应用 `user-select: contain` 与 `table a { user-select: all !important }` 样式隔离，拖动选择光标时严格限制在链接内部，彻底杜绝手机上拖动时光标溢出扩散选择大段外部表格内容的顽疾。
  - **表格单元格双击极速提取**：双击任意表格单元格即可直接提取该单元格所有纯文本，完全无需手动拖拽选择手柄。
  - **全局划词浮动复制胶囊 (Floating Selection Bubble)**：页面任意位置划选文字时，自动在选区上方悬浮「📋 复制所选文字」微型气泡，点按即拷。
  - **原生操作弹窗全面升级**：
    - 点击链接或长按时弹出大号文字卡片，优先提供「复制链接文字」、「复制链接地址」、「复制文字与网址」及「外部浏览器打开」；
    - 表格链接提取栏为每条链接独立配备「复制文字」与「复制链接」按钮，并支持底部「一键复制全部文字」和「一键复制全部链接」。
  - **外部链接安全保护**：点击消息/表格中的外部链接（如 GitHub、文档等）不再冲掉 Antigravity 会话容器，而是弹出便捷操作选项或唤起手机外部浏览器，保障对话上下文安全无损。
  - **悬浮菜单一键复制当前网址**：半透明快捷悬浮球新增「复制当前网址」功能，方便随时提取并分享正在运行的远程会话链接。
- **多账号自动选择与冷启动零重置 (v1.0.8 深度强化)**：
  - **首帧账号零重置**：彻底消除冷启动时 DataStore 本地读取延迟与 Compose 重组引起的重载竞态，等待首选项就绪后在首次网络请求直接带上目标账号，**绝不再闪退/重载回默认账号 (u/0)**。
  - **路径 + 参数双重锁定**：将 Google 账号标识同时注入 `/u/{index}/` 路径与 `?authuser={index}` 参数，确保 Google GFE 网关无论读取哪个层级都能 100% 精确选中目标账号。
  - **全链路单向事件分发**：彻底移除 `AndroidView.update` 中的 URL 比较重载逻辑，任何界面重组或软键盘开合都不会触发误刷新。
  - **头像弹窗与新标签深度拦截**：深度拦截 Google 右上角头像菜单中的 `window.open` 与 `target="_blank"` 账号切换链接，强制在当前 WebView 内部原位无缝跳转，防止丢失上下文。
  - **全生命周期 Cookie 深度落盘**：集成 Activity `onPause`/`onStop` 及页面开始/结束监听，实时调用 `CookieManager.flush()` 确保 Google OAuth 凭证安全持久化。
  - **便捷多账号切换面板**：悬浮菜单直观展示当前账号角标（如 `u/1`），支持一键秒切至 `u/0` / `u/1` / `u/2` / `u/3`、唤起 Google 官方账号选择器或新增账号。
- **零黑边自适应**：自动注入 Viewport 与 CSS，将桌面版 Antigravity 界面满屏拉伸铺满移动设备屏幕，彻底消除 16:9 桌面版在长屏手机上的上下大黑边。
- **全能文件下载支持**：
  - **原生 HTTP/HTTPS 下载**：自动附加 Google OAuth 登录 Cookie 和 User-Agent，接管系统 `DownloadManager` 下载任务。
  - **SPA Blob / Data 内存流下载**：深度拦截前端动态生成的 `blob:` 和 `data:` 导出文件（如代码 Zip 包、diff 补丁、导出文档），桥接 Native 并保存至系统 Downloads 目录。
  - **状态栏完成通知**：下载成功后自动推送系统通知与 Toast，点击即可直接查看或打开文件。
- **新窗口与文件上传支持**：支持 `target="_blank"` 新标签拦截与 `<input type="file">` 唤起手机系统相册/文件管理器。
- **状态栏安全避让**：保留顶部系统状态栏与前置挖孔/刘海屏安全间距，左上角 `←` 返回按钮、工作区名称与会话标题完整露出，100% 轻松点击。
- **软键盘智能避让与滚屏**：配置 `adjustResize` 与 Compose `.imePadding()`，键盘弹出时自动平滑顶起，并自动将当前正在输入的文本框推至可视安全区。
- **Google OAuth 登录兼容**：内置持久化 Cookie 管理，模拟 Chrome Mobile UA 并剥离 WebView 阻断标识，无缝支持 Google 账号授权登录。
- **全手势与悬浮菜单**：支持系统边缘侧滑返回、硬件返回键后退，并配备半透明快捷悬浮球（刷新、后退、复制当前网址、切换账号、修改链接、切换模式），打字时智能自动隐藏。

### 2. ⚡ 原生 WebSocket 控制面板 (Native Dashboard)
- **实时会话流**：通过 WebSocket 与 Antigravity 后端服务双向通信，实时推送 Agent 状态与消息流。
- **支持文本划词复制**：原生集成 SelectionContainer，支持长按划词复制消息与链接。
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
| **v1.0.13** | **Release (正式版)** | 推荐使用，前台发热与耗电深度治理、60Hz锁频降温、消除DOM渲染死循环 | [下载 app-release.apk](https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.13/app-release.apk) |
| **v1.0.13** | **Debug (调试版)** | 包含调试日志输出 | [下载 app-debug.apk](https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.13/app-debug.apk) |
| **v1.0.12** | **Release (历史版)** | 应用内检测更新与版本选择器 | [下载 app-release.apk](https://github.com/lublue147-netizen/antigravity-remote/releases/download/v1.0.12/app-release.apk) |

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
│   ├── AccountUrlHelper.kt       # 多账号 URL 路由解析与双重参数注入工具
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
