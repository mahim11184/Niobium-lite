package app.niobium.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(20, 43, 57);
    private final int teal = Color.rgb(0, 151, 166);
    private LinearLayout root;
    private EditText address;
    private TextView tabButton;
    private FrameLayout pageHost;
    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final ArrayList<String> titles = new ArrayList<>();
    private int current = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 251, 252));
        getWindow().setNavigationBarColor(Color.rgb(245, 251, 252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(new WebView(this), true);
        buildShell();
        addTab("file:///android_asset/index.html");
    }

    private GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(1, stroke);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER); return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextColor(ink); b.setTextSize(17);
        b.setAllCaps(false); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(8, 0, 8, 0);
        b.setBackground(shape(Color.rgb(235, 247, 249), Color.rgb(210, 231, 235), 22));
        b.setOnClickListener(v -> action.run()); return b;
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout brandRow = new LinearLayout(this); brandRow.setGravity(Gravity.CENTER_VERTICAL); brandRow.setPadding(14, 8, 14, 5);
        TextView brand = text("⬡  niobium lite", 18, ink, true); brand.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.addView(brand, new LinearLayout.LayoutParams(0, 42, 1));
        tabButton = text("▢ 1", 14, teal, true); tabButton.setPadding(12, 0, 12, 0);
        tabButton.setOnClickListener(v -> showTabs()); brandRow.addView(tabButton);
        root.addView(brandRow);

        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL); nav.setPadding(9, 5, 9, 9);
        nav.setBackgroundColor(Color.WHITE);
        nav.addView(button("‹", () -> goBack()), new LinearLayout.LayoutParams(40, 42));
        nav.addView(button("›", () -> goForward()), new LinearLayout.LayoutParams(40, 42));
        nav.addView(button("↻", () -> active().reload()), new LinearLayout.LayoutParams(40, 42));
        address = new EditText(this); address.setSingleLine(true); address.setTextSize(14); address.setTextColor(ink);
        address.setHintTextColor(Color.rgb(117, 143, 151)); address.setHint("Search or enter website");
        address.setPadding(13, 0, 13, 0); address.setSelectAllOnFocus(true);
        address.setBackground(shape(Color.rgb(242, 249, 250), Color.rgb(199, 224, 229), 25));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v, id, event) -> { if (id == EditorInfo.IME_ACTION_GO) { navigate(address.getText().toString()); return true; } return false; });
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, 42, 1); ep.setMargins(7,0,7,0); nav.addView(address, ep);
        nav.addView(button("Go", () -> navigate(address.getText().toString())), new LinearLayout.LayoutParams(54,42));
        root.addView(nav);
        View line = new View(this); line.setBackgroundColor(Color.rgb(226, 239, 241)); root.addView(line, new LinearLayout.LayoutParams(-1,1));
        pageHost = new FrameLayout(this); root.addView(pageHost, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private WebView active() { return tabs.get(current); }

    private WebView createWebView() {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.WHITE);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);
        w.getSettings().setDatabaseEnabled(true);
        w.getSettings().setSupportZoom(true);
        w.getSettings().setBuiltInZoomControls(true);
        w.getSettings().setDisplayZoomControls(false);
        w.getSettings().setMediaPlaybackRequiresUserGesture(true);
        w.addJavascriptInterface(new BrowserBridge(w), "Niobium");
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView view, String title) {
                int i = tabs.indexOf(view); if (i >= 0) titles.set(i, title == null ? "New tab" : title);
            }
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return false; }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                if (view == active() && !url.startsWith("file:")) address.setText(url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (view == active() && !url.startsWith("file:")) address.setText(url);
            }
        });
        return w;
    }

    private void addTab(String url) {
        WebView w = createWebView(); tabs.add(w); titles.add("New tab");
        pageHost.addView(w, new FrameLayout.LayoutParams(-1,-1));
        switchTab(tabs.size()-1); w.loadUrl(url);
    }

    private void switchTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        current = index;
        for (int i=0;i<tabs.size();i++) tabs.get(i).setVisibility(i == current ? View.VISIBLE : View.GONE);
        tabButton.setText("▢ " + tabs.size());
        String u = active().getUrl(); address.setText(u == null || u.startsWith("file:") ? "" : u);
    }

    private void showTabs() {
        String[] items = new String[tabs.size()+1];
        for (int i=0;i<tabs.size();i++) items[i] = (i == current ? "✓ " : "") + (titles.get(i).isEmpty() ? "New tab" : titles.get(i));
        items[tabs.size()] = "+  New tab";
        new AlertDialog.Builder(this).setTitle("Niobium tabs").setItems(items, (d, which) -> {
            if (which == tabs.size()) addTab("file:///android_asset/index.html"); else switchTab(which);
        }).setNeutralButton("Close current tab", (d, which) -> closeTab()).show();
    }

    private void closeTab() {
        if (tabs.size() <= 1) { active().loadUrl("file:///android_asset/index.html"); return; }
        WebView old = active(); pageHost.removeView(old); old.destroy(); tabs.remove(current); titles.remove(current);
        switchTab(Math.max(0, current-1));
    }

    private void navigate(String input) {
        String value = input == null ? "" : input.trim(); if (value.isEmpty()) return;
        if (!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")) {
            value = value.matches("^(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[^\\s]+\\.[^\\s]+)(/.*)?$") ? "https://" + value : "https://www.google.com/search?q=" + android.net.Uri.encode(value);
        }
        active().loadUrl(value);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);
        address.clearFocus();
    }

    private void goBack() { if (active().canGoBack()) active().goBack(); }
    private void goForward() { if (active().canGoForward()) active().goForward(); }

    private class BrowserBridge {
        private final WebView owner;
        BrowserBridge(WebView w) { owner = w; }
        @android.webkit.JavascriptInterface public void open(String url) {
            runOnUiThread(() -> { if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) owner.loadUrl(url); });
        }
        @android.webkit.JavascriptInterface public void back() { runOnUiThread(() -> { if (owner.canGoBack()) owner.goBack(); }); }
        @android.webkit.JavascriptInterface public void forward() { runOnUiThread(() -> { if (owner.canGoForward()) owner.goForward(); }); }
        @android.webkit.JavascriptInterface public void reload() { runOnUiThread(owner::reload); }
        @android.webkit.JavascriptInterface public void home() { runOnUiThread(() -> owner.loadUrl("file:///android_asset/index.html")); }
    }

    @Override public void onBackPressed() { if (active().canGoBack()) active().goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { for (WebView w : tabs) w.destroy(); tabs.clear(); super.onDestroy(); }
}
