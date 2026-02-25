package fury.temp.fluxer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity v0.2.16 - The Native Shell Edition
 * 
 * Implements a 100% native Java topbar (Part A) that wraps the 
 * Capacitor WebView (Part B). No more iframes.
 */
public class MainActivity extends BridgeActivity {
    private TextView urlDisplay;
    private List<String> historyList = new ArrayList<>();
    private final String THEME_BLUE = "#4641D9";
    private final String CARD_BG = "#121212";
    
    private ValueCallback<Uri[]> fileUploadCallback;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private String lastKnownUrl = "";
    private boolean isUIInjected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(DesktopModePlugin.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Hide the web background during transition
        if (this.bridge != null && this.bridge.getWebView() != null) {
            this.bridge.getWebView().setBackgroundColor(Color.BLACK);
        }
        // Inject the native layout overlay
        new Handler(Looper.getMainLooper()).postDelayed(this::injectNativeShell, 800);
    }

    /**
     * Part A: The Native FrameLayout/LinearLayout Wrapper
     */
    private void injectNativeShell() {
        if (isUIInjected) return;
        final WebView webView = (WebView) this.bridge.getWebView();
        if (webView == null || webView.getParent() == null) return;

        float d = getResources().getDisplayMetrics().density;

        // 1. Create Native Top Bar (40dp height)
        RelativeLayout topBar = new RelativeLayout(this);
        int barHeight = (int) (40 * d);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(-1, barHeight));
        topBar.setBackgroundColor(Color.parseColor(THEME_BLUE));
        topBar.setElevation(10 * d);

        // --- Left: Refresh ---
        ImageView btnRefresh = new ImageView(this);
        btnRefresh.setImageResource(R.drawable.ic_refresh);
        btnRefresh.setPadding((int)(10*d), (int)(10*d), (int)(10*d), (int)(10*d));
        RelativeLayout.LayoutParams lpRefresh = new RelativeLayout.LayoutParams((int)(40*d), (int)(40*d));
        lpRefresh.addRule(RelativeLayout.ALIGN_PARENT_START);
        lpRefresh.addRule(RelativeLayout.CENTER_VERTICAL);
        lpRefresh.setMarginStart((int)(5*d));
        btnRefresh.setLayoutParams(lpRefresh);
        btnRefresh.setOnClickListener(v -> webView.reload());
        topBar.addView(btnRefresh);

        // --- Right: Options ---
        ImageView btnMore = new ImageView(this);
        btnMore.setImageResource(R.drawable.ic_more_horiz);
        btnMore.setPadding((int)(10*d), (int)(10*d), (int)(10*d), (int)(10*d));
        RelativeLayout.LayoutParams lpMore = new RelativeLayout.LayoutParams((int)(40*d), (int)(40*d));
        lpMore.addRule(RelativeLayout.ALIGN_PARENT_END);
        lpMore.addRule(RelativeLayout.CENTER_VERTICAL);
        lpMore.setMarginEnd((int)(5*d));
        btnMore.setLayoutParams(lpMore);
        btnMore.setOnClickListener(v -> showNativeOptions(webView));
        topBar.addView(btnMore);

        // --- Center: URL Display (Perfectly Centered) ---
        urlDisplay = new TextView(this);
        RelativeLayout.LayoutParams lpUrl = new RelativeLayout.LayoutParams(-2, -2);
        lpUrl.addRule(RelativeLayout.CENTER_IN_PARENT);
        urlDisplay.setLayoutParams(lpUrl);
        urlDisplay.setTextColor(Color.WHITE);
        urlDisplay.setTextSize(11);
        urlDisplay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        urlDisplay.setSingleLine(true);
        urlDisplay.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        urlDisplay.setMarqueeRepeatLimit(-1);
        urlDisplay.setSelected(true);
        urlDisplay.setText("web.fluxer.app");
        topBar.addView(urlDisplay);

        // 2. Perform Layout Injection
        ViewGroup contentRoot = findViewById(android.R.id.content);
        if (contentRoot.getChildCount() > 0) {
            View capacitorView = contentRoot.getChildAt(0);
            contentRoot.removeView(capacitorView);

            LinearLayout rootWrapper = new LinearLayout(this);
            rootWrapper.setOrientation(LinearLayout.VERTICAL);
            rootWrapper.setBackgroundColor(Color.BLACK);
            rootWrapper.setFitsSystemWindows(true); // NOTCH PROTECTION

            // Part A
            rootWrapper.addView(topBar);
            
            // Part B: The actual web page (Capacitor WebView)
            LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
            capacitorView.setLayoutParams(webParams);
            rootWrapper.addView(capacitorView);

            contentRoot.addView(rootWrapper);
            isUIInjected = true;
        }

        startUrlPolling(webView);
        setupBridgeFixes(webView);
    }

    private void startUrlPolling(WebView webView) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentUrl = webView.getUrl();
                if (currentUrl != null && !currentUrl.equals(lastKnownUrl)) {
                    lastKnownUrl = currentUrl;
                    String clean = currentUrl.replace("https://", "").replace("http://", "").replace("www.", "");
                    if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
                    if (urlDisplay != null) urlDisplay.setText(clean);
                    if (!historyList.contains(currentUrl)) historyList.add(0, currentUrl);
                }
                new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void showNativeOptions(final WebView webView) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setLayout(-1, -2);

        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(CARD_BG));
        float r = 30 * d;
        gd.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(gd);
        root.setPadding((int)(30*d), (int)(30*d), (int)(30*d), (int)(30*d));

        TextView title = new TextView(this);
        title.setText("Fluxer Menu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, (int)(20*d));
        root.addView(title);

        root.addView(createRow("🕒 History", v -> { showHistorySheet(); dialog.dismiss(); }));
        root.addView(createRow("📡 Service Status", v -> { webView.loadUrl("https://fluxerstatus.com/"); dialog.dismiss(); }));
        root.addView(createRow("📝 Project Blog", v -> { webView.loadUrl("https://blog.fluxer.app/"); dialog.dismiss(); }));
        root.addView(createRow("ℹ️ About App", v -> { showAboutDialog(); dialog.dismiss(); }));

        dialog.setContentView(root);
        dialog.show();
    }

    private View createRow(String text, View.OnClickListener listener) {
        float d = getResources().getDisplayMetrics().density;
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.LTGRAY);
        tv.setTextSize(15);
        tv.setPadding(0, (int)(15*d), 0, (int)(15*d));
        tv.setOnClickListener(listener);
        return tv;
    }

    private void showHistorySheet() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(new TextView(this)); // Placeholder
        d.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
            .setTitle("Fluxer Android")
            .setMessage("Version 0.2.16\nBuild 25\n\nNative Shell Edition")
            .setPositiveButton("OK", null)
            .show();
    }

    private void setupBridgeFixes(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                MainActivity.this.runOnUiThread(() -> request.grant(request.getResources()));
            }
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) fileUploadCallback.onReceiveValue(null);
                fileUploadCallback = filePathCallback;
                try { startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_RESULT_CODE); } 
                catch (Exception e) { fileUploadCallback = null; return false; }
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE && fileUploadCallback != null) {
            fileUploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileUploadCallback = null;
        }
    }

    @CapacitorPlugin(name = "DesktopMode")
    public static class DesktopModePlugin extends Plugin {
        @PluginMethod
        public void toggle(PluginCall call) {
            getActivity().runOnUiThread(() -> {
                WebView webView = (WebView) getBridge().getWebView();
                webView.reload();
                call.resolve();
            });
        }
    }
}
