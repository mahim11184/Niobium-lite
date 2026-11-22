const { app, BrowserWindow, session, shell } = require("electron");
const path = require("node:path");

function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 760,
    minHeight: 540,
    backgroundColor: "#07131c",
    title: "Niobium",
    icon: path.join(__dirname, "..", "assets", "icon.png"),
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      webviewTag: true
    }
  });

  win.loadFile(path.join(__dirname, "index.html"));

  // Keep external application protocols out of the browser window.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith("https://") || url.startsWith("http://")) {
      shell.openExternal(url);
    }
    return { action: "deny" };
  });

  win.webContents.on("will-attach-webview", (_event, webPreferences, params) => {
    // Do not allow guest pages to inherit privileged renderer settings.
    webPreferences.nodeIntegration = false;
    webPreferences.contextIsolation = true;
    webPreferences.sandbox = true;
    delete webPreferences.preload;
    if (!/^https?:\/\//i.test(params.src || "")) {
      params.src = "https://www.google.com";
    }
  });
}

app.whenReady().then(() => {
  // Default-deny permission requests. Add individual permissions only after
  // implementing a user-facing permission prompt.
  session.defaultSession.setPermissionRequestHandler((_webContents, _permission, callback) => {
    callback(false);
  });

  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
