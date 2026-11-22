# Niobium Lite for Android

This is **Niobium Lite**, the low-memory Android edition. It uses Android System WebView for page rendering. It is not the main Chromium-based Niobium browser.

The Android app name is **Niobium Lite**, and its launcher icon is generated directly from the project's own `assets/icon.svg` branding—not Cromite's icon or name.

## Build on GitHub (no PC required)

1. Push this project to the `main` branch, or open **Actions → Build Niobium Lite (Android WebView APK) → Run workflow**.
2. Wait for the workflow to finish successfully.
3. Open the completed run and download **Niobium-Lite-Android-APK** under Artifacts.
4. Extract the ZIP and install `app-debug.apk` on your Android device.

The workflow builds an unsigned-by-release debug APK for personal testing. A production release should use a securely configured signing key.
