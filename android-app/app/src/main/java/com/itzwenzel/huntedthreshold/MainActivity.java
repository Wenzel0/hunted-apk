package com.itzwenzel.huntedthreshold;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.webkit.WebViewAssetLoader;

import java.io.IOException;

/**
 * Hunted Threshold viewer: opens the game in landscape + immersive fullscreen.
 *  - If the game is bundled (app/src/main/assets/game/index.html) it runs fully offline from inside the app.
 *  - Otherwise (or after "Change address") it opens the address you type, for example http://192.168.1.5:3000/ (node server.js).
 *  - Back button = small menu (Resume / Reload / Change address / Exit).
 */
public class MainActivity extends Activity {

    private static final String PREFS = "hunted";
    private static final String KEY_URL = "url";
    private static final String BUNDLED_URL = "https://appassets.androidplatform.net/assets/game/index.html";

    private FrameLayout root;
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private WebViewAssetLoader assetLoader;
    private boolean hasBundle;
    private String currentHost = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 28) {                                    // use the notch area too
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        hasBundle = bundleExists();

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        root.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        configure(web.getSettings());
        web.setWebViewClient(new GameClient());
        web.setWebChromeClient(new GameChrome());
        hideSystemUi();

        if (savedInstanceState != null && web.restoreState(savedInstanceState) != null) {
            return;
        }
        start();
    }

    private void configure(WebSettings s) {
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);                          // accounts, achievements and settings live in localStorage
        s.setMediaPlaybackRequiresUserGesture(false);          // the game's sounds start without an extra tap
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    private boolean bundleExists() {
        try {
            getAssets().open("game/index.html").close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /* ---------- what to open ---------- */
    private void start() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = p.getString(KEY_URL, "");
        if (saved != null && saved.length() > 0) {
            load(saved);
        } else if (hasBundle) {
            load(BUNDLED_URL);
        } else {
            askAddress(true);
        }
    }

    private void load(String url) {
        Uri u = Uri.parse(url);
        currentHost = u.getHost() == null ? "" : u.getHost();
        web.loadUrl(url);
    }

    private void askAddress(final boolean firstRun) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("http://192.168.1.5:3000/");
        input.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_URL, ""));
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Game address")
                .setMessage(firstRun
                        ? "This app has no bundled game. Type the address where Hunted Threshold is running (for example your node server.js)."
                        : "Type the address of the game. Leave it empty to use the game bundled inside this app (if there is one).")
                .setView(input)
                .setCancelable(!firstRun)
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        String v = input.getText().toString().trim();
                        if (v.length() > 0 && !v.contains("://")) v = "http://" + v;
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_URL, v).apply();
                        hideSystemUi();
                        if (v.length() == 0 && !hasBundle) { askAddress(true); return; }
                        start();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override public void onDismiss(DialogInterface d) { hideSystemUi(); }
                });
        b.show();
    }

    private void showMenu() {
        final String[] items = new String[]{"Resume", "Reload", "Change address / server", "Exit"};
        new AlertDialog.Builder(this)
                .setTitle("Hunted Threshold")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 1) web.reload();
                        else if (which == 2) askAddress(false);
                        else if (which == 3) finish();
                        hideSystemUi();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override public void onDismiss(DialogInterface d) { hideSystemUi(); }
                })
                .show();
    }

    /* ---------- web view clients ---------- */
    private class GameClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return assetLoader.shouldInterceptRequest(request.getUrl());   // null for anything that is not a bundled file
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri u = request.getUrl();
            if ("app".equals(u.getScheme())) {                              // buttons on the error page
                if ("address".equals(u.getHost())) askAddress(false); else start();
                return true;
            }
            String host = u.getHost() == null ? "" : u.getHost();
            boolean isHttp = "http".equals(u.getScheme()) || "https".equals(u.getScheme());
            if (isHttp && !host.equals(currentHost) && !host.equals("appassets.androidplatform.net")) {   // other sites open in the browser
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                String html = "<html><body style='background:#000;color:#cfc8b8;font-family:monospace;text-align:center;padding-top:12vh'>"
                        + "<h2 style='letter-spacing:.2em'>CAN'T REACH THE GAME</h2>"
                        + "<p>Check your connection or the server address.</p>"
                        + "<p><a style='color:#ff6a6a' href='app://retry'>Try again</a> &nbsp;&nbsp; <a style='color:#e8c27a' href='app://address'>Change address</a></p></body></html>";
                view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            start();                                                         // the web process was killed: just start again
            return true;
        }
    }

    private class GameChrome extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {      // fullscreen video / the game's own fullscreen request
            if (customView != null) { callback.onCustomViewHidden(); return; }
            customView = view;
            customCallback = callback;
            web.setVisibility(View.GONE);
            root.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            hideSystemUi();
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;
            root.removeView(customView);
            customView = null;
            if (customCallback != null) customCallback.onCustomViewHidden();
            customCallback = null;
            web.setVisibility(View.VISIBLE);
            hideSystemUi();
        }
    }

    /* ---------- fullscreen + lifecycle ---------- */
    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (customView != null) { new GameChrome().onHideCustomView(); return; }
        showMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();                                                       // stops the game's sound when you leave the app
        web.pauseTimers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.resumeTimers();
        hideSystemUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        root.removeAllViews();
        web.destroy();
        super.onDestroy();
    }
}
