package app.niobium.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.net.Uri;
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
    private String lastFailedUrl = "https://www.google.com";
    private boolean screenOpen=false;
    private Runnable screenBackAction=null;

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
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void buildShell() {
        // Keep the app inside the usable screen area; Android handles status/navigation bar insets.
        getWindow().getDecorView().setSystemUiVisibility(0);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        root.setClipChildren(true); root.setClipToPadding(true);

        // TOP: responsive search/address bar. All widths are weighted or compact fixed dp slots.
        bottomBar=new LinearLayout(this); bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL); bottomBar.setPadding(dp(8),dp(6),dp(8),dp(6));
        bottomBar.setBackgroundColor(Color.WHITE); bottomBar.setClipChildren(true);
        leafButton=action(theme.icon,30,()->active().loadUrl("file:///android_asset/index.html"));
        bottomBar.addView(leafButton,new LinearLayout.LayoutParams(dp(44),dp(52)));
        address=new EditText(this); address.setSingleLine(true); address.setTextSize(17); address.setTextColor(0xff5d6870);
        address.setHintTextColor(0xff929ba1); address.setHint("Search or enter URL"); address.setPadding(dp(14),0,dp(8),0);
        address.setMinWidth(0); address.setBackground(shape(0xfff0f1f2,0,dp(32)));
        address.setSelectAllOnFocus(true); address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_GO){navigate(address.getText().toString());return true;}return false;});
        LinearLayout.LayoutParams addressParams=new LinearLayout.LayoutParams(0,dp(52),1f);
        addressParams.leftMargin=dp(4); addressParams.rightMargin=dp(4);
        bottomBar.addView(address,addressParams);
        TextView go=action("GO",17,()->navigate(address.getText().toString())); go.setTextColor(theme.accent); go.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bottomBar.addView(go,new LinearLayout.LayoutParams(dp(42),dp(52)));
        paletteButton=action("🎨",25,()->showThemes()); paletteButton.setTextColor(theme.accent);
        bottomBar.addView(paletteButton,new LinearLayout.LayoutParams(dp(44),dp(52)));
        View underline=new View(this); underline.setBackgroundColor(theme.accent);
        LinearLayout searchWrap=new LinearLayout(this); searchWrap.setOrientation(LinearLayout.VERTICAL);
        searchWrap.addView(bottomBar,new LinearLayout.LayoutParams(-1,dp(68)));
        searchWrap.addView(underline,new LinearLayout.LayoutParams(-1,dp(4)));
        root.addView(searchWrap,new LinearLayout.LayoutParams(-1,-2));

        pageHost=new FrameLayout(this); pageHost.setClipChildren(true);
        root.addView(pageHost,new LinearLayout.LayoutParams(-1,0,1f));

        // BOTTOM: four equally sized navigation targets; no fixed total width.
        topBar=new LinearLayout(this); topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER); topBar.setPadding(dp(6),dp(4),dp(6),dp(4));
        topBar.setBackgroundColor(Color.WHITE); topBar.setClipChildren(true);
        topBar.addView(action("←",32,()->goBack()),new LinearLayout.LayoutParams(0,dp(64),1f));
        topBar.addView(action("⌂",34,()->active().loadUrl("file:///android_asset/index.html")),new LinearLayout.LayoutParams(0,dp(64),1f));
        tabButton=action("▦",30,()->showTabs()); topBar.addView(tabButton,new LinearLayout.LayoutParams(0,dp(64),1f));
        topBar.addView(action("☰",29,()->showMenu()),new LinearLayout.LayoutParams(0,dp(64),1f));
        root.addView(topBar,new LinearLayout.LayoutParams(-1,dp(72)));

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
        WebView w=new WebView(this); CookieManager.getInstance().setAcceptThirdPartyCookies(w,getSharedPreferences("niobium_settings",MODE_PRIVATE).getBoolean("third_party_cookies",true)); w.setBackgroundColor(Color.WHITE);
        w.getSettings().setJavaScriptEnabled(getSharedPreferences("niobium_settings",MODE_PRIVATE).getBoolean("javascript_enabled",true)); w.getSettings().setDomStorageEnabled(true); w.getSettings().setDatabaseEnabled(true);
        w.getSettings().setSupportZoom(true); w.getSettings().setBuiltInZoomControls(true); w.getSettings().setDisplayZoomControls(false);
        w.addJavascriptInterface(new BrowserBridge(w),"Niobium");
        w.setWebChromeClient(new WebChromeClient(){@Override public void onReceivedTitle(WebView view,String title){int i=tabs.indexOf(view);if(i>=0)titles.set(i,title==null?"New tab":title);}});
        w.setDownloadListener((url,userAgent,contentDisposition,mimeType,contentLength)->startDownload(url,userAgent,contentDisposition,mimeType));
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return false;}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon){if(view==active()&&!url.startsWith("file:"))address.setText(url);}
            @Override public void onPageFinished(WebView view,String url){CookieManager.getInstance().flush();if(view==active()&&!url.startsWith("file:"))address.setText(url);}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,android.webkit.WebResourceError error){
                if(request.isForMainFrame()){
                    String failed=request.getUrl().toString();
                    if(failed.startsWith("http://")||failed.startsWith("https://")){
                        lastFailedUrl=failed;
                        if(view==active())address.setText("");
                        view.loadUrl("file:///android_asset/no-connection.html");
                    }
                }
            }
        }); return w;
    }
    private void startDownload(String url,String userAgent,String contentDisposition,String mimeType){
        if(url==null || !(url.startsWith("http://") || url.startsWith("https://"))){
            Toast.makeText(this,"This download link is not supported",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            String fileName=URLUtil.guessFileName(url,contentDisposition,mimeType);
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Downloading with Niobium Lite");
            request.setMimeType(mimeType);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName);
            String cookie=CookieManager.getInstance().getCookie(url);
            if(cookie!=null) request.addRequestHeader("Cookie",cookie);
            if(userAgent!=null) request.addRequestHeader("User-Agent",userAgent);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            if(manager==null) throw new IllegalStateException("Download service unavailable");
            manager.enqueue(request);
            Toast.makeText(this,"Download started: "+fileName+" — check Downloads",Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"Could not start download: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
    private void addTab(String url){WebView w=createWebView();tabs.add(w);titles.add("New tab");pageHost.addView(w,new FrameLayout.LayoutParams(-1,-1));switchTab(tabs.size()-1);w.loadUrl(url);}
    private void switchTab(int index){if(index<0||index>=tabs.size())return;current=index;for(int i=0;i<tabs.size();i++)tabs.get(i).setVisibility(i==current?View.VISIBLE:View.GONE);tabButton.setText("▦");String u=active().getUrl();address.setText(u==null||u.startsWith("file:")?"":u);}
    private void showTabs(){String[] items=new String[tabs.size()+1];for(int i=0;i<tabs.size();i++)items[i]=(i==current?"✓ ":"")+(titles.get(i).isEmpty()?"New tab":titles.get(i));items[tabs.size()]="＋ New tab";new AlertDialog.Builder(this).setTitle("Niobium tabs").setItems(items,(d,n)->{if(n==tabs.size())addTab("file:///android_asset/index.html");else switchTab(n);}).setNeutralButton("Close current tab",(d,n)->closeTab()).show();}
    private void closeTab(){if(tabs.size()<=1){active().loadUrl("file:///android_asset/index.html");return;}WebView w=active();pageHost.removeView(w);w.destroy();tabs.remove(current);titles.remove(current);switchTab(Math.max(0,current-1));}
    private void showThemes(){String[] names=new String[themes.length];for(int i=0;i<themes.length;i++)names[i]=themes[i].icon+"  "+themes[i].name+(themes[i]==theme?"  ✓":"");new AlertDialog.Builder(this).setTitle("Choose your Niobium theme").setItems(names,(d,n)->{theme=themes[n];applyTheme();}).show();}
    private TextView panelText(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private LinearLayout pageColumn(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setBackgroundColor(theme.background);return c;}
    private TextView pageAction(String value,int size,Runnable run){TextView t=panelText(value,size,theme.ink);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->run.run());return t;}
    private void openFullPage(String title,String subtitle,View content,Runnable back){
        LinearLayout page=pageColumn();
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(8),dp(8),dp(16),dp(8));header.setBackgroundColor(Color.WHITE);
        TextView backButton=pageAction("‹",38,back);header.addView(backButton,new LinearLayout.LayoutParams(dp(48),dp(52)));
        LinearLayout titlesBox=new LinearLayout(this);titlesBox.setOrientation(LinearLayout.VERTICAL);titlesBox.setPadding(dp(8),0,0,0);
        TextView heading=panelText(title,20,0xff17212b);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);titlesBox.addView(heading);
        TextView sub=panelText(subtitle,12,0xff77838d);LinearLayout.LayoutParams subp=new LinearLayout.LayoutParams(-1,-2);subp.topMargin=dp(2);titlesBox.addView(sub,subp);
        header.addView(titlesBox,new LinearLayout.LayoutParams(0,-2,1));
        page.addView(header,new LinearLayout.LayoutParams(-1,dp(68)));
        View line=new View(this);line.setBackgroundColor(theme.accent);page.addView(line,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setPadding(dp(16),dp(16),dp(16),dp(24));
        if(content!=null)scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);screenOpen=true;screenBackAction=back;
        page.setAlpha(0f);page.setTranslationX(dp(48));page.animate().alpha(1f).translationX(0).setDuration(260).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    private void closeFullPage(){
        View currentView=getWindow().getDecorView().findViewById(android.R.id.content);
        if(currentView!=null)currentView.animate().alpha(0f).translationX(dp(32)).setDuration(150).withEndAction(()->{
            screenOpen=false;screenBackAction=null;setContentView(root);root.setAlpha(0f);root.animate().alpha(1f).setDuration(180).start();
        }).start();else{screenOpen=false;setContentView(root);}
    }
    private LinearLayout pageRows(){LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);return rows;}
    private void addPageRow(LinearLayout rows,String icon,String title,String detail,Runnable action){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(14),dp(12),dp(14));row.setBackground(shape(Color.WHITE,0xffe8edf0,dp(16)));
        TextView glyph=panelText(icon,23,theme.accent);glyph.setGravity(Gravity.CENTER);row.addView(glyph,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout textBox=new LinearLayout(this);textBox.setOrientation(LinearLayout.VERTICAL);textBox.setPadding(dp(12),0,dp(8),0);
        textBox.addView(panelText(title,16,0xff1c2730));TextView small=panelText(detail,12,0xff78848d);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(4);textBox.addView(small,sp);
        row.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=panelText("›",28,0xff8b969e);row.addView(arrow);
        row.setOnClickListener(v->action.run());LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);rows.addView(row,rp);
    }
    private void addSettingSwitch(LinearLayout rows,String title,String detail,String key,boolean defaultValue,java.util.function.Consumer<Boolean> changed){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(13),dp(12),dp(13));row.setBackground(shape(Color.WHITE,0xffe8edf0,dp(16)));
        LinearLayout textBox=new LinearLayout(this);textBox.setOrientation(LinearLayout.VERTICAL);textBox.addView(panelText(title,16,0xff1c2730));
        TextView small=panelText(detail,12,0xff78848d);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(4);textBox.addView(small,sp);
        row.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle=new Switch(this);toggle.setChecked(getSharedPreferences("niobium_settings",MODE_PRIVATE).getBoolean(key,defaultValue));
        toggle.setOnCheckedChangeListener((button,checked)->{getSharedPreferences("niobium_settings",MODE_PRIVATE).edit().putBoolean(key,checked).apply();changed.accept(checked);});
        row.addView(toggle);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(10);rows.addView(row,rp);
    }
    private void showMenu(){
        LinearLayout rows=pageRows();
        addPageRow(rows,"⇩","Downloads","View progress and downloaded files",()->showDownloads());
        addPageRow(rows,"⚙","Settings","Privacy, permissions and browsing preferences",()->showSettings());
        addPageRow(rows,"ⓘ","About Niobium Lite","Version, browser engine and credits",()->showInfo());
        openFullPage("Niobium","Browser menu",rows,()->closeFullPage());
    }
    private void showDownloads(){
        LinearLayout rows=pageRows();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView info=panelText("Files downloaded by Niobium Lite",13,0xff77838d);top.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView refresh=panelText("↻  Refresh",14,theme.accent);refresh.setPadding(dp(10),dp(8),dp(10),dp(8));refresh.setOnClickListener(v->showDownloads());top.addView(refresh);rows.addView(top);
        DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);Cursor cursor=null;int count=0;
        try{if(manager!=null){cursor=manager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_PENDING|DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PAUSED|DownloadManager.STATUS_SUCCESSFUL|DownloadManager.STATUS_FAILED));
            while(cursor!=null&&cursor.moveToNext()){
                long id=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                String name=cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long got=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total=cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                String state=status==DownloadManager.STATUS_SUCCESSFUL?"Completed":status==DownloadManager.STATUS_FAILED?"Failed":status==DownloadManager.STATUS_PAUSED?"Paused":status==DownloadManager.STATUS_PENDING?"Waiting":"Downloading";
                String size=total>0?String.format(java.util.Locale.US,"%.1f / %.1f MB",got/1048576d,total/1048576d):"Size unknown";
                final long downloadId=id;final int downloadStatus=status;
                addPageRow(rows,"⇩",name==null?"Downloaded file":name,state+" • "+size,()->{
                    if(downloadStatus==DownloadManager.STATUS_SUCCESSFUL){Uri uri=manager.getUriForDownloadedFile(downloadId);if(uri!=null){Intent open=new Intent(Intent.ACTION_VIEW);open.setDataAndType(uri,getContentResolver().getType(uri));open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivity(open);}catch(Exception e){Toast.makeText(this,"No app can open this file",Toast.LENGTH_SHORT).show();}}}
                    else if(downloadStatus==DownloadManager.STATUS_FAILED)Toast.makeText(this,"Download failed",Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this,"Download is still in progress",Toast.LENGTH_SHORT).show();
                });count++;
            }
        }}catch(Exception e){Toast.makeText(this,"Could not read downloads",Toast.LENGTH_SHORT).show();}finally{if(cursor!=null)cursor.close();}
        if(count==0){TextView empty=panelText("Nothing here yet\n\nYour downloads will appear here with their progress and status.",15,0xff77838d);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(70),dp(20),dp(70));rows.addView(empty);}
        openFullPage("Downloads",count+" download(s)",rows,()->showMenu());
    }
    private void showSettings(){
        SharedPreferences prefs=getSharedPreferences("niobium_settings",MODE_PRIVATE);LinearLayout rows=pageRows();
        TextView privacy=panelText("GENERAL",12,theme.accent);privacy.setTypeface(Typeface.DEFAULT,Typeface.BOLD);privacy.setPadding(dp(4),dp(8),0,dp(10));rows.addView(privacy);
        addSettingSwitch(rows,"JavaScript","Allow interactive websites","javascript_enabled",true,enabled->{for(WebView w:tabs)w.getSettings().setJavaScriptEnabled(enabled);});
        addSettingSwitch(rows,"Third-party cookies","Allow embedded services to use cookies","third_party_cookies",true,enabled->{for(WebView w:tabs)CookieManager.getInstance().setAcceptThirdPartyCookies(w,enabled);});
        addSettingSwitch(rows,"Save form data","Allow websites to remember form entries","save_form_data",true,enabled->{});
        TextView data=panelText("BROWSING DATA",12,theme.accent);data.setTypeface(Typeface.DEFAULT,Typeface.BOLD);data.setPadding(dp(4),dp(20),0,dp(10));rows.addView(data);
        addPageRow(rows,"⌫","Clear browsing data","Clear cache, history, form data and cookies",()->clearBrowsingData());
        addPageRow(rows,"⇩","Downloads","Open download manager",()->showDownloads());
        TextView about=panelText("WEBSITE SETTINGS",12,theme.accent);about.setTypeface(Typeface.DEFAULT,Typeface.BOLD);about.setPadding(dp(4),dp(20),0,dp(10));rows.addView(about);
        addSettingSwitch(rows,"Pop-up windows","WebView blocks pop-ups by default","block_popups",true,enabled->{});
        openFullPage("Settings","Customize your browsing experience",rows,()->showMenu());
    }
    private void clearBrowsingData(){
        new AlertDialog.Builder(this).setTitle("Clear browsing data?")
            .setMessage("This clears page cache, browsing history, saved form data and website cookies.")
            .setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,n)->{
                for(WebView w:tabs){w.clearCache(true);w.clearHistory();w.clearFormData();}
                CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();
                Toast.makeText(this,"Browsing data cleared",Toast.LENGTH_SHORT).show();showSettings();
            }).show();
    }
    private void showInfo(){
        LinearLayout rows=pageRows();
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setGravity(Gravity.CENTER);brand.setPadding(dp(18),dp(30),dp(18),dp(30));brand.setBackground(shape(Color.WHITE,0xffe8edf0,dp(20)));
        TextView logo=panelText("Nb",38,theme.accent);logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);logo.setGravity(Gravity.CENTER);brand.addView(logo);
        TextView name=panelText("Niobium Lite",23,0xff17212b);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(8);brand.addView(name,np);
        TextView version=panelText("Version 1.0.0",13,0xff77838d);version.setGravity(Gravity.CENTER);LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.topMargin=dp(5);brand.addView(version,vp);
        rows.addView(brand);
        addPageRow(rows,"🌐","Browser engine","Android System WebView",()->{});
        addPageRow(rows,"🛡","Privacy","Browsing data is stored locally on this device. Websites may still collect data when you visit them.",()->{});
        addPageRow(rows,"⚡","Built for speed","A lightweight browser experience by Niobium",()->{});
        openFullPage("About","About this browser",rows,()->showMenu());
    }
    private void navigate(String input){String v=input==null?"":input.trim();if(v.isEmpty())return;if(!v.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*"))v=v.matches("^(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[^\\s]+\\.[^\\s]+)(/.*)?$")?"https://"+v:"https://www.google.com/search?q="+android.net.Uri.encode(v);active().loadUrl(v);((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);address.clearFocus();}
    private void goBack(){if(active().canGoBack())active().goBack();}
    private class BrowserBridge{private final WebView owner;BrowserBridge(WebView w){owner=w;}@android.webkit.JavascriptInterface public void open(String u){runOnUiThread(()->{if(u!=null&&(u.startsWith("https://")||u.startsWith("http://")||u.startsWith("file:///android_asset/")))owner.loadUrl(u);});}@android.webkit.JavascriptInterface public void back(){runOnUiThread(()->{if(owner.canGoBack())owner.goBack();});}@android.webkit.JavascriptInterface public void forward(){runOnUiThread(()->owner.goForward());}@android.webkit.JavascriptInterface public void reload(){runOnUiThread(owner::reload);}@android.webkit.JavascriptInterface public void home(){runOnUiThread(()->owner.loadUrl("file:///android_asset/index.html"));}@android.webkit.JavascriptInterface public void retry(){runOnUiThread(()->owner.loadUrl(lastFailedUrl));}}
    @Override public void onBackPressed(){if(screenOpen&&screenBackAction!=null){screenBackAction.run();return;}if(active().canGoBack())active().goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){for(WebView w:tabs)w.destroy();tabs.clear();super.onDestroy();}
}