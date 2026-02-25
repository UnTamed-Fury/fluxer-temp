# Fluxer Android Wrapper

> **Note:** This repo + app only exists till there is a stable version fluxer native app is unavaiable.

[![License: BSL 1.1](https://img.shields.io/badge/License-BSL%201.1-blue.svg)](LICENSE)
[![Built with: Ionic/Capacitor](https://img.shields.io/badge/Built%20with-Ionic%2FCapacitor-orange.svg)](https://ionicframework.com/)

**Fluxer** is a high-performance, native Android wrapper for [web.fluxer.app](https://web.fluxer.app). Built using Ionic and Capacitor, it provides a seamless mobile experience with native hardware acceleration, file handling, and deep OS integration.

---

## 🚀 Features

- **Native WebView Wrapper**: Directly points to `web.fluxer.app` for maximum performance.
- **Enhanced UI**: 
  - **Thin Topbar**: Displays current URL with marquee effect.
  - **Native History**: View navigation history in a floating window.
  - **Quick Actions**: Refresh and Desktop Mode toggle.
- **Deep Integration**: 
  - Captures all `*.fluxer.app` links.
  - Handles PDF, ZIP, and media downloads natively.
- **Hardware Acceleration**: Smooth 60FPS rendering.

---

## 🛠️ Tech Stack

- **Framework**: [Ionic](https://ionicframework.com/)
- **Native Bridge**: [Capacitor 6](https://capacitorjs.com/)
- **Target OS**: Android (API 29+)
- **Build System**: Gradle 8.1 (Termux-optimized)

---

## 📦 Build Instructions (Termux)

```bash
# 1. Install prerequisites
pkg install nodejs openjdk-17 aapt2 -y

# 2. Sync native platform
npx cap sync android

# 3. Build APK
export ANDROID_HOME=$PREFIX/opt/Android/sdk
export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk
cd android && ./gradlew assembleDebug -Dandroid.aapt2.executable=$PREFIX/bin/aapt2
```
( How to install andoid sdk on [Termux](https://termux.dev/) and how to set it can be found at - [this reddit post from r/termux](https://www.reddit.com/r/termux/s/D4eqJbrAva))
---

## 🤝 Contributing & Documentation

- See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
- Technical documentation is available in the [docs/](docs/) directory.
- For official Fluxer service issues, refer to the [Fluxer Help Center](https://docs.fluxer.app/).

---

## ⚖️ License

This project is licensed under the **Business Source License 1.1 (BSL 1.1)**. 

- **Personal/Non-commercial:** Free for life.
- **Commercial:** Requires a **50% revenue share** paid to **UnTamed-Fury**.
- **Open Source Transition:** This project will automatically become **Apache 2.0** on **January 1, 2030**.

See the [LICENSE](LICENSE) file for details.

---
**Owner:** [UnTamed-Fury](https://github.com/UnTamed-Fury)
