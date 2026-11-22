package app.niobium.browser;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.JavascriptInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView web;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(7, 19, 27));
        w.setNavigationBarColor(Color.rgb(7, 19, 27));
        w.getDecorView().setSystemUiVisibility(0);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(7, 19, 27));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new BrowserBridge(), "Niobium");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        setContentView(web);
        if (state == null) web.loadUrl("file:///android_asset/index.html");
        else web.restoreState(state);
    }

    private class BrowserBridge {
        @JavascriptInterface public void open(String url) {
            runOnUiThread(() -> {
                if (url != null && (url.startsWith("https://") || url.startsWith("http://")))
                    web.loadUrl(url);
            });
        }
        @JavascriptInterface public void back() { runOnUiThread(() -> { if (web.canGoBack()) web.goBack(); }); }
        @JavascriptInterface public void forward() { runOnUiThread(() -> { if (web.canGoForward()) web.goForward(); }); }
        @JavascriptInterface public void reload() { runOnUiThread(() -> web.reload()); }
        @JavascriptInterface public void home() { runOnUiThread(() -> web.loadUrl("file:///android_asset/index.html")); }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        if (web != null) web.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
