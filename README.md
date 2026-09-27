# 🚀 Antigravity Remote

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Min%20SDK-26-brightgreen?style=for-the-badge" />
</p>

A native Android client for remotely controlling **Google Antigravity** AI agent sessions. Monitor, chat with, and manage your AI coding agents from your Android device.

## ✨ Features

- 📡 **Real-time Connection** — Connect to your Antigravity server via WebSocket for instant updates
- 📋 **Session Dashboard** — View all active agent sessions with live status indicators
- 💬 **Chat Interface** — Send messages to agents with streaming response support
- ✅ **Change Management** — Approve or reject agent-proposed changes directly from your phone
- 🔔 **Push Notifications** — Get notified when an agent needs your input
- 🎨 **Material 3 UI** — Beautiful, modern interface with dark/light theme support
- 🔐 **Secure** — Token-based authentication and TLS support

## 📱 Screenshots

| Dashboard | Chat | Settings |
|:---------:|:----:|:--------:|
| Session list with live status | Real-time agent chat | Connection & preferences |

## 🏗️ Architecture

```
com.antigravity.remote/
├── data/
│   ├── local/          # DataStore preferences
│   ├── model/          # Data classes
│   ├── remote/         # WebSocket service
│   └── repository/     # Repository pattern
├── di/                 # Hilt dependency injection
└── ui/
    ├── navigation/     # Compose Navigation
    ├── screen/         # UI screens
    ├── theme/          # Material 3 theme
    └── viewmodel/      # ViewModels
```

- **MVVM** with Repository pattern
- **Hilt** for dependency injection
- **Jetpack Compose** with Material 3
- **OkHttp** WebSocket for real-time communication
- **DataStore** for local preferences

## 🚀 Getting Started

### Prerequisites

- A running Antigravity server with Remote Control enabled
  - In Antigravity 2.0: Settings → App → Enable Remote Control
  - Or use a community backend like [antigravity-remote-backend](https://github.com/figaro-develop/antigravity-remote-backend)

### Download

1. Go to the [Releases](../../releases) page
2. Download the latest APK
3. Install on your Android device (enable "Install from unknown sources" if needed)

### Setup

1. Open the app
2. Go to **Settings**
3. Enter your server URL (e.g., `192.168.1.100:3000`)
4. (Optional) Enter your authentication token
5. Tap **Save**, then go to **Dashboard** and tap **Connect**

## 🔧 Building from Source

```bash
# Clone the repository
git clone https://github.com/lublue147-netizen/contra-go.git
cd contra-go

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/`.

## 📦 CI/CD

This project uses **GitHub Actions** for automated builds:

- **Every push to `main`**: Builds debug + release APKs and uploads as artifacts
- **Tagged releases (`v*`)**: Automatically creates a GitHub Release with signed APKs

### Setting up Release Signing

To enable signed releases, add these secrets to your GitHub repository:

| Secret | Description |
|--------|-------------|
| `SIGNING_KEY` | Base64-encoded keystore file |
| `KEY_ALIAS` | Key alias in the keystore |
| `KEY_STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |

```bash
# Generate a keystore
keytool -genkey -v -keystore release.keystore -alias antigravity -keyalg RSA -keysize 2048 -validity 10000

# Encode it to base64
base64 -i release.keystore -o keystore_base64.txt
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Google Antigravity](https://antigravity.google) — The AI development platform
- [OmniAntigravityRemoteChat](https://github.com/diegosouzapw/OmniAntigravityRemoteChat) — Inspiration for the project
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Modern Android UI toolkit
