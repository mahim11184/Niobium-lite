const tabsEl = document.getElementById("tabs");
const viewport = document.getElementById("viewport");
const address = document.getElementById("address");
const statusText = document.getElementById("statusText");
let tabs = [];
let activeId = null;
let nextId = 1;

function normalizeInput(value) {
  const text = value.trim();
  if (!text) return "https://www.google.com";
  if (/^(https?:\/\/|file:\/\/)/i.test(text)) return text;
  if (/^[\w.-]+\.[a-z]{2,}([/:?#]|$)/i.test(text)) return "https://" + text;
  return "https://www.google.com/search?q=" + encodeURIComponent(text);
}
function activeTab() { return tabs.find(tab => tab.id === activeId); }
function setStatus(message) { statusText.textContent = message; }

function renderTabs() {
  tabsEl.replaceChildren();
  for (const tab of tabs) {
    const button = document.createElement("button");
    button.className = "tab" + (tab.id === activeId ? " active" : "");
    button.type = "button";
    button.setAttribute("role", "tab");
    button.setAttribute("aria-selected", String(tab.id === activeId));
    const title = document.createElement("span");
    title.className = "tab-title";
    title.textContent = tab.title || "New tab";
    button.append(title);
    const close = document.createElement("span");
    close.className = "close-tab";
    close.textContent = "×";
    close.title = "Close tab";
    close.addEventListener("click", event => { event.stopPropagation(); closeTab(tab.id); });
    button.append(close);
    button.addEventListener("click", () => activateTab(tab.id));
    tabsEl.append(button);
  }
}
function makeWelcome() {
  const welcome = document.createElement("section");
  welcome.className = "welcome";
  welcome.innerHTML = `
    <div class="welcome-inner">
      <img class="hero-logo" src="../assets/icon.svg" alt="Niobium">
      <h1>Explore beyond.</h1>
      <p>A calmer window to the web, powered by Chromium.</p>
      <form class="search-box" id="welcomeSearch">
        <input id="welcomeInput" placeholder="Search the web or enter a URL" autocomplete="off">
        <button type="submit">Search</button>
      </form>
      <div class="shortcuts">
        <button class="shortcut" data-url="https://www.wikipedia.org">Wikipedia</button>
        <button class="shortcut" data-url="https://github.com">GitHub</button>
        <button class="shortcut" data-url="https://www.youtube.com">YouTube</button>
      </div>
    </div>`;
  welcome.querySelector("#welcomeSearch").addEventListener("submit", event => {
    event.preventDefault();
    navigate(welcome.querySelector("#welcomeInput").value);
  });
  welcome.querySelectorAll("[data-url]").forEach(button => button.addEventListener("click", () => navigate(button.dataset.url)));
  return welcome;
}
function createTab(url = "") {
  const id = nextId++;
  const view = document.createElement("webview");
  view.className = "browser-view";
  view.setAttribute("partition", "persist:niobium");
  view.setAttribute("allowpopups", "false");
  view.setAttribute("webpreferences", "contextIsolation=yes, javascript=yes");
  const tab = { id, title: "New tab", url: "", view, welcome: null };
  view.addEventListener("page-title-updated", (_event, title) => {
    tab.title = title || "New tab";
    renderTabs();
  });
  view.addEventListener("did-navigate", (_event, url) => {
    tab.url = url;
    if (activeId === id) { address.value = url; setStatus("Loaded"); }
  });
  view.addEventListener("did-navigate-in-page", (_event, url) => {
    tab.url = url;
    if (activeId === id) address.value = url;
  });
  view.addEventListener("did-start-loading", () => { if (activeId === id) setStatus("Loading…"); });
  view.addEventListener("did-stop-loading", () => { if (activeId === id) setStatus("Ready"); });
  view.addEventListener("did-fail-load", (_event, errorCode) => {
    if (errorCode !== -3 && activeId === id) setStatus("Page could not be loaded");
  });
  tab.welcome = makeWelcome();
  viewport.append(view, tab.welcome);
  tabs.push(tab);
  activateTab(id);
  if (url) load(tab, url);
}
function activateTab(id) {
  activeId = id;
  for (const tab of tabs) {
    const active = tab.id === id;
    tab.view.classList.toggle("active", active && !!tab.url);
    tab.welcome.hidden = active && !!tab.url;
    if (!active) tab.view.classList.remove("active");
  }
  const tab = activeTab();
  if (tab) address.value = tab.url || "";
  renderTabs();
  setStatus(tab?.url ? "Ready" : "New tab");
}
function load(tab, input) {
  const url = normalizeInput(input);
  tab.url = url;
  tab.title = "Loading…";
  tab.welcome.hidden = true;
  tab.view.classList.add("active");
  tab.view.src = url;
  if (activeId === tab.id) address.value = url;
  renderTabs();
}
function navigate(input) {
  const tab = activeTab();
  if (tab) load(tab, input);
}
function closeTab(id) {
  const index = tabs.findIndex(tab => tab.id === id);
  if (index < 0) return;
  const [tab] = tabs.splice(index, 1);
  tab.view.remove();
  tab.welcome.remove();
  if (!tabs.length) createTab();
  else if (activeId === id) activateTab(tabs[Math.max(0, index - 1)].id);
  renderTabs();
}

document.getElementById("newTab").addEventListener("click", () => createTab());
document.getElementById("addressForm").addEventListener("submit", event => {
  event.preventDefault();
  navigate(address.value);
});
document.getElementById("back").addEventListener("click", () => {
  const tab = activeTab(); if (tab?.view.canGoBack()) tab.view.goBack();
});
document.getElementById("forward").addEventListener("click", () => {
  const tab = activeTab(); if (tab?.view.canGoForward()) tab.view.goForward();
});
document.getElementById("reload").addEventListener("click", () => {
  const tab = activeTab(); if (tab?.url) tab.view.reload();
});
document.getElementById("home").addEventListener("click", () => {
  const tab = activeTab();
  if (!tab) return;
  tab.url = "";
  tab.view.stop();
  tab.view.classList.remove("active");
  tab.welcome.hidden = false;
  tab.title = "New tab";
  address.value = "";
  renderTabs();
  setStatus("Home");
});
document.getElementById("menu").addEventListener("click", () => {
  setStatus("Niobium Browser · Electron / Chromium engine · early preview");
});
createTab();
