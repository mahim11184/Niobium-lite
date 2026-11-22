# Niobium Browser

Niobium is an open-source browser project with separate desktop and Android editions.

## Editions

| Edition | Platform | Rendering engine | Status |
|---|---|---|---|
| Niobium Desktop | Windows, macOS, Linux | Chromium via Electron | Desktop starter |
| Niobium Lite | Android | Android System WebView | Lightweight Android app |
| Niobium Full | Android | Chromium with Cromite patches | Source-level Niobium branding and arm64 build workflow configured |

Niobium Full is a separate Chromium browser build, not a WebView wrapper and not a renamed Cromite APK. Niobium Lite remains a separate, smaller WebView app.

## Desktop edition

The desktop starter is built with Electron, which embeds Chromium.

### Features
- Chromium-powered web rendering through Electron
- Tab strip with new-tab and close-tab controls
- Address bar with URL navigation and web search
- Back, forward, reload, and home controls
- Dark, cyan-accented Niobium interface
- Custom Niobium app icon (SVG source)

### Requirements
- Node.js 20.19+ or 22.12+
- npm
- Windows, macOS, or Linux

### Run locally
```bash
npm install
npm start
```

### Build installers
```bash
npm run dist
```

Build outputs are written to `dist/`. The package configuration includes Windows NSIS and portable builds, Linux AppImage and deb, and macOS DMG.

## Android Lite

Niobium Lite uses Android System WebView. See [android/README.md](android/README.md) for building it through GitHub Actions. Its application ID is `app.niobium.browser`.

## Android Full (ungoogled Chromium)

Niobium Full now uses the Android build system from [ungoogled-chromium-android](https://github.com/ungoogled-software/ungoogled-chromium-android), rather than the previous Cromite/self-hosted-runner setup.

### Niobium identity
- App label: **Niobium**
- Android application ID: `app.niobium.browser.full` (separate from Lite)
- Launcher/adaptive artwork: generated from the Niobium SVG assets
- Niobium Lite remains a separate Android System WebView app

The source overlay is maintained in [scripts/rebrand-ungoogled.sh](scripts/rebrand-ungoogled.sh). It changes the Android app-name resources, sets the Full package ID, and generates launcher artwork. It intentionally fails if the expected app-name resources cannot be found.

### Build from an Android phone
1. Open the repository's **Actions** tab.
2. Select **Build Niobium Full Android (ungoogled Chromium)**.
3. Tap **Run workflow**.
4. Select `arm64` for most modern Android phones, then start the workflow.
5. When all build stages finish, download the `Niobium-Full-Android-arm64` artifact.

The workflow uses GitHub-hosted `ubuntu-latest` runners. It follows the upstream project's staged build-tree artifact process so the Chromium compilation can continue across multiple hosted jobs. No self-hosted computer is required.

**Build status:** this workflow and branding overlay have been committed, but a successful Niobium APK build has not yet been verified. The first run is the integration test. The produced APK must be checked for its actual package name, displayed app name, icon, and signing status before distribution. A persistent release keystore is not configured; do not treat the first artifact as a production-signed release.

The upstream ungoogled-chromium-android project includes Chromium, ungoogled-chromium, and Android-specific patches. Preserve its license and all third-party notices when redistributing the browser.

## Project structure
- `src/main.js` — Electron main process and secure window setup
- `src/preload.js` — minimal isolated bridge
- `src/index.html` — browser interface
- `src/style.css` — Niobium theme
- `src/renderer.js` — tabs, navigation, and address handling
- `assets/icon.svg` — main Niobium app icon
- `assets/niobium-foreground.svg` / `assets/niobium-background.svg` — adaptive launcher icon layers
- `scripts/rebrand-ungoogled.sh` — source-level Full Android rebrand overlay
- `.github/workflows/niobium-full-android.yml` — staged hosted Full Chromium Android build
- `.github/actions/niobium-build-stage/action.yml` — Niobium branding and hosted build stage
- `android/` — Niobium Lite Android WebView project

## Security notes

The desktop project is an early browser starter, not yet a hardened daily-use browser. Before distributing publicly, add a reviewed permission policy, download handling, content filtering, update signing, and a thorough security audit. Do not use it for sensitive browsing until those protections are implemented.

The Full Android build is a Chromium derivative. Keep all applicable upstream license, copyright, and attribution notices when distributing it. Review its privacy, security, and update behavior before treating it as a daily-use release.

## License

MIT for the original Niobium desktop starter. Third-party components and the Cromite-derived Android edition remain subject to their respective licenses; Cromite is GPL-3.0. See the upstream license and preserve all applicable notices.
