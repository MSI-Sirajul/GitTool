# 🛠️ GitTool

[![GitHub Release](https://img.shields.io/github/v/release/MSI-Sirajul/GitTool?logo=github&style=flat-square&color=blue)](https://github.com/MSI-Sirajul/GitTool/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?logo=android&style=flat-square)](https://android-sdk.support/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg?logo=kotlin&style=flat-square)](https://kotlinlang.org/)

**GitTool** is a high-performance, feature-rich Android client designed for power users and developers to manage, browse, and sync their GitHub spaces directly from their mobile devices. Built entirely on top of Jetpack Compose and modern Material 3 guidelines, it offers a secure, offline-first client experience for viewing and manipulating remote git repositories, inspecting developer profiles, and keeping up with workflow updates on the go.

---

## 📱 Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/MSI-Sirajul/GitTool/main/assets/screen_dashboard.png" width="30%" alt="Repository Dashboard" />
  <img src="https://raw.githubusercontent.com/MSI-Sirajul/GitTool/main/assets/screen_filebrowser.png" width="30%" alt="Code Browser" />
  <img src="https://raw.githubusercontent.com/MSI-Sirajul/GitTool/main/assets/screen_profile.png" width="30%" alt="User Profile" />
</p>

---

## ✨ Features

- **🔐 Robust Secure Login**: Perform logins securely with full-fledged GitHub OAuth authorization flow using your custom Client ID and Client Secret, or login directly using Personal Access Tokens (PAT).
- **📂 Public & Private Repositories**: Navigate separate, beautifully styled tabs to view, sort, and filter your public and private repositories seamlessly, bolstered by a pull-to-refresh container.
- **⬆️ Folder Upload Engine**: Easily select and upload complete local directories, nested documents, and codebase structures directly to new repositories with background upload worker progress notifications.
- **🔄 Import and Fork**: Seamlessly fork existing remote repositories or import external git projects directly into your account in one click.
- **👁️ Interactive File Browser**: Navigate repositories using a fluid folder hierarchy viewer and an inline file reader complete with elegant code theme styling.
- **🔔 Live In-App Notifications**: Stay up to date with real-time GitHub notifications, threads, and unread feeds. Read and manage mark-all-read sweeps directly from the app.
- **🔍 Global Search Engine**: Conduct extensive global searches for any developer or repository across the entirety of GitHub with dynamic results.
- **💾 Offline Caching & Bookmarks**: Full offline reliability powered by Jetpack Room. Bookmarks, repositories, file metadata, and user stats are persistently cached for lightning-quick offline access.
- **👤 Profile Management**: View stats, biographies, organization memberships, repositories, followers, following counts, and update user bios directly.
- **🎨 Modern Material 3 Theming**: Responsive fluid Edge-to-Edge display support, custom dynamic colors, and smooth switching between Light, Dark, and System Theme presets.
- **🔒 NDK Security & splits**: Uses low-level Play Integrity helpers and C++ binary integrity checks to secure API calls and tokens. Supports multi-ABI APK splitting to ensure compact install sizes.

---

## 📥 Downloads

To run or try GitTool, get the pre-built configurations:

<p align="left">
  <a href="https://play.google.com/store/apps/details?id=com.msi.gittool">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="220" alt="Get it on Google Play" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://github.com/MSI-Sirajul/GitTool/releases">
    <img src="https://img.shields.io/badge/Download_APK-GitHub_Releases-gray?style=for-the-badge&logo=github" height="65" alt="Download APK from GitHub" />
  </a>
</p>

---

## ⚙️ How to Build

Follow these simple phases to build and deploy GitTool locally:

### 1. Prerequisites
- **JDK 17** or higher
- **Android Studio Ladybug** or newer
- **Android SDK** installed (Targeting API Level 34/36)

### 2. Configure Environment Variables
Copy `.env.example` to `.env` in the root of the project:

```bash
cp .env.example .env
```

Open `.env` and fill in your GitHub Application credentials (register your application under GitHub Developer settings with the redirect URI `gittool://callback`):

```env
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
```

### 3. Signing Setup (Optional)
To sign your application with production keys, create `app/keystore.properties` in your project with the following fields:

```properties
storeFile=msi.gittool.jks
storePassword=your_keystore_password
keyAlias=your_alias
keyPassword=your_key_password
```

Ensure `msi.gittool.jks` resides inside the `app/` directory (or specify its relative path). If `keystore.properties` is omitted, the build automatically falls back to your local debug keystore keys!

### 4. Build and Install Tasks
To build a signed Debug APK:
```bash
gradle assembleDebug
```
To build a fully-optimized, ProGuard-minified Release APK:
```bash
gradle assembleRelease
```

The compiled APKs will be located under:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 🛠️ Tech Stack

- **UI Framework**: Modern Jetpack Compose with declarative layouts
- **Local Database**: Jetpack Room for local persistence, bookmarks, and user caching
- **DI Container**: Simple constructor dependency injection for ease of unit testing
- **Network Stack**: Retrofit 2 + OkHttp 3 for robust HTTP request management
- **OAuth Protocol**: RFC 8252 standard Client Authorization implemented with `AppAuth-Android`
- **Crypto & Security**: `Jetpack Security-Crypto` (EncryptedSharedPreferences) and low-level NDK binary verification
- **Asynchrony**: Kotlin Coroutines & Flow structures with state hoisting
- **Build System**: Gradle Kotlin DSL structured around Centralized Catalog (`libs.versions.toml`)

---

## 📄 License

GitTool is released under the **MIT License**. Check the [LICENSE](LICENSE) file for more information.
