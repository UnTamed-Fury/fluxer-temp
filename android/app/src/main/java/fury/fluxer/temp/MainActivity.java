package fury.fluxer.temp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends BridgeActivity {
    private TextView urlDisplay;
    private ValueCallback<Uri[]> fileUploadCallback;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private boolean isUIInjected = false;
    private String lastKnownUrl = "";
    private final String THEME_COLOR = "#4641D9";
    private final String DARK_BG = "#121212";
    
    // Settings State
    private boolean isDesktopMode = false;
    private List<String> historyList = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("fluxer_settings", MODE_PRIVATE);
        loadHistory();
        cleanupHistory(); // Auto-vanish on start
    }

    @Override
    public void onStart() {
        super.onStart();
        final WebView webView = (WebView) this.bridge.getWebView();
        if (webView == null) return;
        webView.setBackgroundColor(Color.BLACK);
        
        // Apply custom User Agent if set
        String customUA = prefs.getString("user_agent", "");
        if (!customUA.isEmpty()) {
            webView.getSettings().setUserAgentString(customUA);
        }

        setupBridge(webView);
        new Handler(Looper.getMainLooper()).postDelayed(this::injectPremiumShell, 800);
    }

    private void injectPremiumShell() {
        if (isUIInjected) return;
        final WebView webView = (WebView) this.bridge.getWebView();
        if (webView == null || webView.getParent() == null) return;

        float d = getResources().getDisplayMetrics().density;
        int barHeight = (int) (35 * d);
        int btnSize = (int) (35 * d);

        RelativeLayout topBar = new RelativeLayout(this);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(-1, barHeight));
        topBar.setBackgroundColor(Color.parseColor(THEME_COLOR));

        // URL Display (Centered)
        urlDisplay = new TextView(this);
        RelativeLayout.LayoutParams lpUrl = new RelativeLayout.LayoutParams(-1, -2);
        lpUrl.addRule(RelativeLayout.CENTER_IN_PARENT);
        lpUrl.setMarginStart(btnSize);
        lpUrl.setMarginEnd(btnSize);
        urlDisplay.setLayoutParams(lpUrl);
        urlDisplay.setTextColor(Color.WHITE);
        urlDisplay.setTextSize(11);
        urlDisplay.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        urlDisplay.setSingleLine(true);
        urlDisplay.setGravity(Gravity.CENTER);
        urlDisplay.setMarqueeRepeatLimit(-1);
        urlDisplay.setHorizontallyScrolling(true);
        urlDisplay.setText("web.fluxer.app");
        topBar.addView(urlDisplay);

        // Buttons
        topBar.addView(createIconBtn(R.drawable.ic_refresh, d, btnSize, true, v -> webView.reload()));
        topBar.addView(createIconBtn(R.drawable.ic_more_horiz, d, btnSize, false, v -> showMainMenu(webView)));

        // Layout Injection
        ViewGroup root = findViewById(android.R.id.content);
        if (root.getChildCount() > 0) {
            View capacitorView = root.getChildAt(0);
            root.removeView(capacitorView);
            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setBackgroundColor(Color.BLACK);
            wrapper.setFitsSystemWindows(true); 
            wrapper.addView(topBar);
            capacitorView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
            wrapper.addView(capacitorView);
            root.addView(wrapper);
            isUIInjected = true;
        }
        startUrlPolling(webView);
    }

    private ImageView createIconBtn(int resId, float d, int size, boolean left, View.OnClickListener listener) {
        ImageView btn = new ImageView(this);
        btn.setImageResource(resId);
        btn.setPadding((int)(8*d), (int)(8*d), (int)(8*d), (int)(8*d));
        btn.setOnClickListener(listener);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(size, size);
        lp.addRule(left ? RelativeLayout.ALIGN_PARENT_START : RelativeLayout.ALIGN_PARENT_END);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void showMainMenu(WebView webView) {
        final Dialog dialog = createPremiumDialog(Gravity.TOP | Gravity.END);
        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = createDialogContainer(d);

        root.addView(createMenuRow("History", R.drawable.ic_history, d, v -> { dialog.dismiss(); showHistoryPanel(webView); }));
        
        // Desktop Mode Toggle
        LinearLayout desktopRow = createMenuRow("Desktop Mode", R.drawable.ic_desktop, d, v -> {
            isDesktopMode = !isDesktopMode;
            applyDesktopMode(webView, isDesktopMode);
            dialog.dismiss();
        });
        CheckBox cb = new CheckBox(this);
        cb.setChecked(isDesktopMode);
        cb.setClickable(false);
        desktopRow.addView(cb);
        root.addView(desktopRow);

        root.addView(createMenuRow("Status", R.drawable.ic_numbers, d, v -> { webView.loadUrl("https://fluxerstatus.com/"); dialog.dismiss(); }));
        root.addView(createMenuRow("Blog", R.drawable.ic_news, d, v -> { webView.loadUrl("https://blog.fluxer.app/"); dialog.dismiss(); }));
        root.addView(createMenuRow("Settings", R.drawable.ic_settings, d, v -> { dialog.dismiss(); showSettingsPanel(webView); }));

        dialog.setContentView(root);
        dialog.show();
    }

    private void showSettingsPanel(WebView webView) {
        final Dialog dialog = createPremiumDialog(Gravity.BOTTOM);
        float d = getResources().getDisplayMetrics().density;
        
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = createDialogContainer(d);
        root.setPadding((int)(25*d), (int)(25*d), (int)(25*d), (int)(25*d));

        addSectionHeader(root, "History Settings", d);
        root.addView(createSettingInput("Limit (Max entries)", "30", InputType.TYPE_CLASS_NUMBER, "history_limit", d));
        root.addView(createSettingInput("Vanish after (Days)", "7", InputType.TYPE_CLASS_NUMBER, "history_days", d));
        root.addView(createToggleRow("Vanish Mode", "history_vanish_enabled", d));

        addSectionHeader(root, "Browser Identity", d);
        root.addView(createSettingInput("Custom User Agent", "set up your custom user agent", InputType.TYPE_CLASS_TEXT, "user_agent", d));

        addSectionHeader(root, "Permissions Center", d);
        root.addView(createToggleRow("Notifications", "perm_notif", d));
        root.addView(createToggleRow("Camera Access", "perm_camera", d));
        root.addView(createToggleRow("Microphone Access", "perm_mic", d));
        root.addView(createToggleRow("Storage Access", "perm_storage", d));

        addSectionHeader(root, "App Ecosystem", d);
        root.addView(createMenuRow("Check for Updates", R.drawable.ic_refresh, d, v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/UnTamed-Fury/fluxer-temp/releases"));
            startActivity(intent);
        }));
        root.addView(createMenuRow("About Us", R.drawable.ic_info, d, v -> {
            Toast.makeText(this, "About Us page coming in next update!", Toast.LENGTH_SHORT).show();
        }));

        scroll.addView(root);
        dialog.setContentView(scroll);
        dialog.show();
    }

    private void showHistoryPanel(WebView webView) {
        final Dialog dialog = createPremiumDialog(Gravity.BOTTOM);
        float d = getResources().getDisplayMetrics().density;
        LinearLayout root = createDialogContainer(d);
        
        TextView title = new TextView(this);
        title.setText("Browser History");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, (int)(15*d));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        
        for (String url : historyList) {
            TextView item = new TextView(this);
            item.setText(url.replace("https://", ""));
            item.setTextColor(Color.LTGRAY);
            item.setPadding(0, (int)(12*d), 0, (int)(12*d));
            item.setOnClickListener(v -> { webView.loadUrl(url); dialog.dismiss(); });
            list.addView(item);
        }
        
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, (int)(300 * d)));
        dialog.setContentView(root);
        dialog.show();
    }

    // --- Helper Methods ---

    private Dialog createPremiumDialog(int gravity) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.getWindow().setGravity(gravity);
        d.getWindow().setLayout(-1, -2);
        return d;
    }

    private LinearLayout createDialogContainer(float d) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(DARK_BG));
        gd.setCornerRadius(20 * d);
        root.setBackground(gd);
        root.setPadding((int)(20*d), (int)(20*d), (int)(20*d), (int)(20*d));
        return root;
    }

    private void addSectionHeader(LinearLayout root, String title, float d) {
        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase());
        tv.setTextColor(Color.parseColor(THEME_COLOR));
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, (int)(15*d), 0, (int)(5*d));
        root.addView(tv);
    }

    private View createSettingInput(String label, String hint, int inputType, String prefKey, float d) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, (int)(5*d), 0, (int)(5*d));

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.LTGRAY);
        lbl.setTextSize(13);
        container.addView(lbl);

        EditText input = new EditText(this);
        input.setHint(hint);
        input.setText(prefs.getString(prefKey, ""));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setInputType(inputType);
        input.setTextSize(14);
        input.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { prefs.edit().putString(prefKey, s.toString()).apply(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        container.addView(input);
        return container;
    }

    private View createToggleRow(String label, String prefKey, float d) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int)(10*d), 0, (int)(10*d));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        row.addView(tv);

        Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(prefKey, true));
        sw.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(prefKey, checked).apply());
        row.addView(sw);
        return row;
    }

    private LinearLayout createMenuRow(String text, int iconRes, float d, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int)(10*d), 0, (int)(10*d));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setLayoutParams(new LinearLayout.LayoutParams((int)(20*d), (int)(20*d)));
        row.addView(icon);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setPadding((int)(10*d), 0, (int)(10*d), 0);
        row.addView(tv);
        return row;
    }

    private void applyDesktopMode(WebView webView, boolean enabled) {
        String desktopUA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        webView.getSettings().setUserAgentString(enabled ? desktopUA : null);
        webView.getSettings().setUseWideViewPort(enabled);
        webView.getSettings().setLoadWithOverviewMode(enabled);
        webView.reload();
    }

    private void loadHistory() {
        String raw = prefs.getString("history", "");
        if (!raw.isEmpty()) historyList.addAll(Arrays.asList(raw.split(",")));
    }

    private void saveHistory() {
        int limit = Integer.parseInt(prefs.getString("history_limit", "30").replaceAll("[^0-9]", ""));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(historyList.size(), limit); i++) {
            sb.append(historyList.get(i)).append(",");
        }
        prefs.edit().putString("history", sb.toString()).apply();
    }

    private void cleanupHistory() {
        if (prefs.getBoolean("history_vanish_enabled", false)) {
            // Placeholder for date-based cleanup (requires timestamp storage)
            // For now, it just respects the limit
            saveHistory();
        }
    }

    private void setupBridge(WebView webView) {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http")) { view.loadUrl(url); return true; }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Check preferences before granting
                String[] resources = request.getResources();
                boolean allow = true;
                for (String r : resources) {
                    if (r.contains("VIDEO_CAPTURE") && !prefs.getBoolean("perm_camera", true)) allow = false;
                    if (r.contains("AUDIO_CAPTURE") && !prefs.getBoolean("perm_mic", true)) allow = false;
                }
                if (allow) MainActivity.this.runOnUiThread(() -> request.grant(resources));
                else MainActivity.this.runOnUiThread(() -> request.deny());
            }
        });
    }

    private void startUrlPolling(WebView webView) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                String url = webView.getUrl();
                if (url != null && !url.equals(lastKnownUrl)) {
                    lastKnownUrl = url;
                    String clean = url.replace("https://", "").replace("http://", "").replace("www.", "");
                    if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
                    urlDisplay.setText(clean);
                    urlDisplay.setEllipsize(clean.length() > 35 ? TextUtils.TruncateAt.MARQUEE : null);
                    urlDisplay.setSelected(clean.length() > 35);
                    if (!historyList.contains(url)) {
                        historyList.add(0, url);
                        saveHistory();
                    }
                }
                new Handler(Looper.getMainLooper()).postDelayed(this, 1500);
            }
        }, 1000);
    }
}
