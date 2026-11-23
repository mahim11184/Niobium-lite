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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static class Theme {
        String name, icon; int accent, soft, background, ink;
        Theme(String n,String i,int a,int s,int b,int t){name=n;icon=i;accent=a;soft=s;background=b;ink=t;}
    }
    private final Theme[] themes = {
        new Theme("Leaf","🍃",0xff20b66a,0xffe9f8ef,0xfff5fff8,0xff183b2a),
        new Theme("Fire","🔥",0xfff06b35,0xffffeee5,0xfffffaf7,0xff482719),
        new Theme("Water","💧",0xff238ed4,0xffe8f5ff,0xfff6fbff,0xff17364b),
        new Theme("Moon","🌙",0xff7774c9,0xffefefff,0xfffafaff,0xff292943),
        new Theme("Sun","☀️",0xffd99b20,0xfffff5d9,0xfffffdf5,0xff493b19)
    };
    private Theme theme;
    private LinearLayout root, topBar, bottomBar;
    private EditText address;
    private TextView tabButton, leafButton, paletteButton;
    private FrameLayout pageHost;
    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final ArrayList<String> titles = new ArrayList<>();
    private int current = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        theme = themes[0];
        CookieManager.getInstance().setAcceptCookie(true);
        buildShell();
        addTab("file:///android_asset/index.html");
    }
    private GradientDrawable shape(int color,int stroke,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if(stroke!=0)d.setStroke(1,stroke); return d;
    }
    private TextView action(String value,int size,Runnable run) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(theme.ink);
        t.setGravity(Gravity.CENTER); t.setOnClickListener(v->run.run()); return t;
    }
    private void buildShell() {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);

        // TOP: wide address/search bar with theme controls (matches the supplied top reference).
        bottomBar=new LinearLayout(this); bottomBar.setGravity(Gravity.CENTER_VERTICAL); bottomBar.setPadding(18,12,18,12); bottomBar.setBackgroundColor(Color.WHITE);
        leafButton=action(theme.icon,34,()->active().loadUrl("file:///android_asset/index.html"));
        bottomBar.addView(leafButton,new LinearLayout.LayoutParams(64,64));
        address=new EditText(this); address.setSingleLine(true); address.setTextSize(19); address.setTextColor(0xff5d6870);
        address.setHintTextColor(0xff929ba1); address.setHint("Search Niobium or URL"); address.setPadding(22,0,12,0);
        address.setBackground(shape(0xfff0f1f2,0,48)); address.setSelectAllOnFocus(true); address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_GO){navigate(address.getText().toString());return true;}return false;});
        bottomBar.addView(address,new LinearLayout.LayoutParams(0,64,1));
        TextView go=action("GO",21,()->navigate(address.getText().toString())); go.setTextColor(theme.accent); go.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bottomBar.addView(go,new LinearLayout.LayoutParams(62,64));
        paletteButton=action("🎨",31,()->showThemes()); paletteButton.setTextColor(theme.accent);
        bottomBar.addView(paletteButton,new LinearLayout.LayoutParams(64,64));
        View underline=new View(this); underline.setBackgroundColor(theme.accent);
        LinearLayout searchWrap=new LinearLayout(this); searchWrap.setOrientation(LinearLayout.VERTICAL);
        searchWrap.addView(bottomBar,new LinearLayout.LayoutParams(-1,88));
        searchWrap.addView(underline,new LinearLayout.LayoutParams(-1,5));
        root.addView(searchWrap,new LinearLayout.LayoutParams(-1,-2));

        pageHost=new FrameLayout(this); root.addView(pageHost,new LinearLayout.LayoutParams(-1,0,1));

        // BOTTOM: thick navigation strip (back, home, tabs, info).
        topBar=new LinearLayout(this); topBar.setGravity(Gravity.CENTER); topBar.setPadding(16,8,16,8); topBar.setBackgroundColor(Color.WHITE);
        topBar.addView(action("←",38,()->goBack()),new LinearLayout.LayoutParams(0,72,1));
        topBar.addView(action("⌂",39,()->active().loadUrl("file:///android_asset/index.html")),new LinearLayout.LayoutParams(0,72,1));
        tabButton=action("▦",35,()->showTabs()); topBar.addView(tabButton,new LinearLayout.LayoutParams(0,72,1));
        topBar.addView(action("● i",27,()->showInfo()),new LinearLayout.LayoutParams(0,72,1));
        root.addView(topBar,new LinearLayout.LayoutParams(-1,88));

        setContentView(root); applyTheme();
    }
    private void applyTheme(){
        root.setBackgroundColor(theme.background); leafButton.setText(theme.icon); paletteButton.setText("🎨"); paletteButton.setTextColor(theme.accent);
        address.setBackground(shape(theme.soft,0,48)); address.setHintTextColor(0xff929ba1);
        for(int i=0;i<topBar.getChildCount();i++) ((TextView)topBar.getChildAt(i)).setTextColor(theme.ink);
        topBar.setBackgroundColor(Color.WHITE); bottomBar.setBackgroundColor(Color.WHITE);
        View underline=((LinearLayout)root.getChildAt(0)).getChildAt(1); underline.setBackgroundColor(theme.accent);
    }
    private WebView active(){return tabs.get(current);}
    private WebView createWebView(){
        WebView w=new WebView(this); CookieManager.getInstance().setAcceptThirdPartyCookies(w,true); w.setBackgroundColor(Color.WHITE);
        w.getSettings().setJavaScriptEnabled(true); w.getSettings().setDomStorageEnabled(true); w.getSettings().setDatabaseEnabled(true);
        w.getSettings().setSupportZoom(true); w.getSettings().setBuiltInZoomControls(true); w.getSettings().setDisplayZoomControls(false);
        w.addJavascriptInterface(new BrowserBridge(w),"Niobium");
        w.setWebChromeClient(new WebChromeClient(){@Override public void onReceivedTitle(WebView view,String title){int i=tabs.indexOf(view);if(i>=0)titles.set(i,title==null?"New tab":title);}});
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return false;}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){if(view==active()&&!url.startsWith("file:"))address.setText(url);}
            @Override public void onPageFinished(WebView view,String url){CookieManager.getInstance().flush();if(view==active()&&!url.startsWith("file:"))address.setText(url);}
        }); return w;
    }
    private void addTab(String url){WebView w=createWebView();tabs.add(w);titles.add("New tab");pageHost.addView(w,new FrameLayout.LayoutParams(-1,-1));switchTab(tabs.size()-1);w.loadUrl(url);}
    private void switchTab(int index){if(index<0||index>=tabs.size())return;current=index;for(int i=0;i<tabs.size();i++)tabs.get(i).setVisibility(i==current?View.VISIBLE:View.GONE);tabButton.setText("▦");String u=active().getUrl();address.setText(u==null||u.startsWith("file:")?"":u);}
    private void showTabs(){String[] items=new String[tabs.size()+1];for(int i=0;i<tabs.size();i++)items[i]=(i==current?"✓ ":"")+(titles.get(i).isEmpty()?"New tab":titles.get(i));items[tabs.size()]="＋ New tab";new AlertDialog.Builder(this).setTitle("Niobium tabs").setItems(items,(d,n)->{if(n==tabs.size())addTab("file:///android_asset/index.html");else switchTab(n);}).setNeutralButton("Close current tab",(d,n)->closeTab()).show();}
    private void closeTab(){if(tabs.size()<=1){active().loadUrl("file:///android_asset/index.html");return;}WebView w=active();pageHost.removeView(w);w.destroy();tabs.remove(current);titles.remove(current);switchTab(Math.max(0,current-1));}
    private void showThemes(){String[] names=new String[themes.length];for(int i=0;i<themes.length;i++)names[i]=themes[i].icon+"  "+themes[i].name+(themes[i]==theme?"  ✓":"");new AlertDialog.Builder(this).setTitle("Choose your Niobium theme").setItems(names,(d,n)->{theme=themes[n];applyTheme();}).show();}
    private void showInfo(){new AlertDialog.Builder(this).setTitle("Niobium Lite").setMessage("A lightweight browser powered by Android System WebView. Choose a theme with the palette button.").setPositiveButton("OK",null).show();}
    private void navigate(String input){String v=input==null?"":input.trim();if(v.isEmpty())return;if(!v.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*"))v=v.matches("^(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[^\\s]+\\.[^\\s]+)(/.*)?$")?"https://"+v:"https://www.google.com/search?q="+android.net.Uri.encode(v);active().loadUrl(v);((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);address.clearFocus();}
    private void goBack(){if(active().canGoBack())active().goBack();}
    private class BrowserBridge{private final WebView owner;BrowserBridge(WebView w){owner=w;}@android.webkit.JavascriptInterface public void open(String u){runOnUiThread(()->{if(u!=null&&(u.startsWith("https://")||u.startsWith("http://")))owner.loadUrl(u);});}@android.webkit.JavascriptInterface public void back(){runOnUiThread(()->{if(owner.canGoBack())owner.goBack();});}@android.webkit.JavascriptInterface public void forward(){runOnUiThread(()->owner.goForward());}@android.webkit.JavascriptInterface public void reload(){runOnUiThread(owner::reload);}@android.webkit.JavascriptInterface public void home(){runOnUiThread(()->owner.loadUrl("file:///android_asset/index.html"));}}
    @Override public void onBackPressed(){if(active().canGoBack())active().goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){for(WebView w:tabs)w.destroy();tabs.clear();super.onDestroy();}
}