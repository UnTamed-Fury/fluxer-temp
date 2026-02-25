# Fluxer Architecture

This project is a hybrid application that wraps the Fluxer web experience in a native Android shell.

## 🧱 Components

1.  **Capacitor Core**: The bridge that allows JavaScript to communicate with native Android APIs (Camera, Filesystem, etc.).
2.  **WebView**: A high-performance Chrome-based engine that renders `https://web.fluxer.app`.
3.  **UI Overlay**: A thin HTML/CSS/JS layer that provides the custom top bar and history management.

## 🔌 Native Integrations

-   **Deep Linking**: Configured in `AndroidManifest.xml` to capture all Fluxer-related URLs.
-   **Hardware Acceleration**: Enabled via manifest attributes for smooth media playback.
-   **Download Manager**: Native Android DownloadManager handling for all supported file extensions.

## 🛠️ Build Logic

To support building on **Termux**, the project uses:
-   **AGP 8.1.0**: A stable version of the Android Gradle Plugin.
-   **AAPT2 Override**: A custom property in `gradle.properties` that points to the native Termux `aapt2` binary.
