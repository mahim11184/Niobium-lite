# Niobium Browser

Niobium is an open-source browser project with separate desktop and Android editions.

## Editions

| Edition | Platform | Rendering engine | Status |
|---|---|---|---|
| Niobium Desktop | Windows, macOS, Linux | Chromium via Electron | Desktop starter; not the Android full edition |
| Niobium Lite | Android | Android System WebView | Lightweight Android app |
| Niobium (Android, full) | Android | Chromium via the Cromite upstream project | Not yet rebranded or built as a Niobium APK |

**Important:** The Android full edition is not currently a Niobium-branded APK. The workflow in this repository only downloads official, unmodified Cromite APKs as upstream reference builds. Renaming those APK files does not change their installed app name, icon, package identity, or signing certificate. Do not install the reference APK expecting it to appear as Niobium.

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

## Project structure
- `src/main.js` — Electron main process and secure window setup
- `src/preload.js` — minimal isolated bridge
- `src/index.html` — browser interface
- `src/style.css` — Niobium theme
- `src/renderer.js` — tabs, navigation, and address handling
- `assets/icon.svg` — application icon source
- `android/` — Niobium Lite Android WebView project

## Android Lite
See [android/README.md](android/README.md) for building Niobium Lite through GitHub Actions.

## Full Android Chromium edition: current blocker

The full Android edition must be built from Cromite/Chromium source with Niobium's own application label, launcher assets, and package identity. A genuine build also needs a persistent release-signing key so future updates can install over previous Niobium builds. The current reference-download workflow does none of these things.

Cromite's official source and build instructions:
- https://github.com/uazo/cromite
- https://github.com/uazo/cromite/blob/master/docs/HOW_TO_BUILD.md

Cromite's build documentation describes a full Chromium source build using its pinned Chromium revision, patches, GN arguments, and a prepared Docker build image. This is a substantial build and cannot honestly be replaced by simply renaming downloaded APKs. Keep all required upstream license and attribution notices when creating a derivative build.

## Security notes

The desktop project is an early browser starter, not yet a hardened daily-use browser. Electron's renderer is isolated from Node.js, and web content runs in sandboxed webviews. Before distributing publicly, add a reviewed permission policy, download handling, content filtering, update signing, and a thorough security audit. Do not use it for sensitive browsing until those protections are implemented.

## License

MIT for the original Niobium desktop starter. Third-party components and any future Cromite-derived Android build remain subject to their respective licenses; Cromite is GPL-3.0. See the upstream license and preserve applicable notices.
