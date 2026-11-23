# Niobium Lite

**Niobium Lite is the only browser edition maintained in this repository.**

Niobium Lite is an Android browser powered by Android System WebView. This repository contains only the Lite Android app, its branding assets, and its GitHub Actions APK build.

## Build Niobium Lite on GitHub (no PC required)

1. Open the repository's **Actions** tab.
2. Select **Build Niobium Lite (Android WebView APK)**.
3. Tap **Run workflow**, or push a change to the `android/` app or `assets/icon.svg`.
4. Wait for the workflow to finish successfully.
5. Download the **Niobium-Lite-Android-APK** artifact.
6. Extract the ZIP and install `app-debug.apk` on your Android device.

The workflow uses GitHub-hosted runners. The debug APK is for personal testing; a production release should use a securely configured signing key.

## Project structure

- `android/` — Niobium Lite Android application
- `android/app/src/main/java/` — native browser UI and WebView
- `android/app/src/main/assets/` — home page, search interface, and styles
- `assets/icon.svg` — Niobium Lite branding and launcher icon source
- `.github/workflows/android-apk.yml` — Android APK build workflow

## Rendering engine

Niobium Lite uses Android System WebView. It is not a bundled Chromium engine. Website compatibility and updates to the rendering engine depend on the Android System WebView provider installed on the device.

## License

See [LICENSE](LICENSE).
