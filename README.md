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

## Android Full (Chromium + Cromite)

Niobium Full is built from the Chromium source tree prepared by the upstream [Cromite project](https://github.com/uazo/cromite), retaining Cromite's browser patches while applying Niobium's own app identity and artwork.

### Niobium branding applied by the build
- App name and Android launcher label: **Niobium**
- Android application ID: `app.niobium.browser.full` (separate from Lite, so both can be installed together)
- Launcher and adaptive icon: generated from the existing `assets/icon.svg`
- Themed monochrome launcher mark: Niobium N
- Android widget titles, product metadata, and visible browser branding: Niobium
- Help/about destinations and the built-in update channel: this Niobium GitHub repository, not Cromite's release channel

The source overlay is maintained in [scripts/rebrand-cromite.sh](scripts/rebrand-cromite.sh). It deliberately checks for the expected upstream branding strings before changing them, so an upstream layout change fails visibly instead of silently producing a partially branded APK.

### Build workflow

Use [Build Niobium Full Android](.github/workflows/niobium-full-android.yml) from the repository's Actions tab. It resolves the latest Cromite release, pulls the matching prepared `uazo/cromite-build` image, applies the Niobium branding, and builds the arm64 `ChromePublic.apk` target. The artifact is named `Niobium-Full-Android-arm64-...-debug`.

**Runner requirement:** this workflow is intentionally assigned to a self-hosted Linux x64 GitHub Actions runner with the label `cromite-builder`, Docker access, and substantial persistent disk/memory. A normal GitHub-hosted runner is not used: Cromite's documented Chromium build relies on a large prepared source/build image, persistent build outputs, and long-running custom runners. The workflow will remain queued until a matching self-hosted runner is registered. See [Cromite's build instructions](https://github.com/uazo/cromite/blob/master/docs/HOW_TO_BUILD.md).

The current workflow produces an **arm64 debug/test APK**, not a production release. No private release keystore is stored in this repository. A production distribution needs a persistent signing key protected in GitHub Actions secrets and a release workflow; never commit the keystore or its password.

The separate `Fetch Cromite Upstream Android APKs (Reference Only)` workflow only downloads upstream APKs for reference. Those files remain Cromite, are not Niobium builds, and are not used by the Niobium Full build workflow.

## Project structure
- `src/main.js` — Electron main process and secure window setup
- `src/preload.js` — minimal isolated bridge
- `src/index.html` — browser interface
- `src/style.css` — Niobium theme
- `src/renderer.js` — tabs, navigation, and address handling
- `assets/icon.svg` — main Niobium app icon
- `assets/niobium-foreground.svg` / `assets/niobium-background.svg` — adaptive launcher icon layers
- `scripts/rebrand-cromite.sh` — source-level Full Android rebrand
- `.github/workflows/niobium-full-android.yml` — Full Chromium Android build
- `android/` — Niobium Lite Android WebView project

## Security notes

The desktop project is an early browser starter, not yet a hardened daily-use browser. Before distributing publicly, add a reviewed permission policy, download handling, content filtering, update signing, and a thorough security audit. Do not use it for sensitive browsing until those protections are implemented.

The Full Android build is a Chromium derivative. Keep all applicable upstream license, copyright, and attribution notices when distributing it. Review its privacy, security, and update behavior before treating it as a daily-use release.

## License

MIT for the original Niobium desktop starter. Third-party components and the Cromite-derived Android edition remain subject to their respective licenses; Cromite is GPL-3.0. See the upstream license and preserve all applicable notices.
