# Niobium Browser

Niobium is an open-source desktop browser starter built with **Electron**, which embeds the Chromium browser engine.

## Features
- Chromium-powered web rendering through Electron
- Tab strip with new-tab and close-tab controls
- Address bar with URL navigation and web search
- Back, forward, reload, and home controls
- Dark, cyan-accented Niobium interface
- Custom Niobium app icon (SVG source)

## Requirements
- Node.js 20.19+ or 22.12+
- npm
- Windows, macOS, or Linux

## Run locally
```bash
npm install
npm start
```

## Build installers
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

## Security notes
This is an early browser project, not yet a hardened daily-use browser. Electron's renderer is isolated from Node.js, and web content runs in sandboxed webviews. Before distributing publicly, add a reviewed permission policy, download handling, content filtering, update signing, and a thorough security audit. Do not use it for sensitive browsing until those protections are implemented.

## License
MIT. See [LICENSE](LICENSE).
