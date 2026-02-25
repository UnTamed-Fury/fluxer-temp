package fury.fluxer.temp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
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
import java.util.List;

public class MainActivity extends BridgeActivity {
    private TextView urlDisplay;
    private ValueCallback<Uri[]> fileUploadCallback;
    private final static int FILE_CHOOSER_RESULT_CODE = 1;
    private boolean isUIInjected = false;
    private String lastKnownUrl = "";
    private final String THEME_COLOR = "#4641D9";
    private final String DARK_BG = "#000000";
    
    private boolean isDesktopMode = false;
    private List<String> historyList = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("fluxer_v3_settings", MODE_PRIVATE);
        loadHistory();
    }

    @Override
    public void onStart() {
        super.onStart();
        final WebView webView = (WebView) this.bridge.getWebView();
        if (webView == null) return;
        webView.setBackgroundColor(Color.BLACK);
        
        String customUA = prefs.getString("user_agent", "");
        if (!customUA.isEmpty()) webView.getSettings().setUserAgentString(customUA);

        setupBridge(webView);
        new Handler(Looper.getMainLooper()).postDelayed(this::injectSolidStateUI, 800);
    }

    private BitmapDrawable getSolidIcon(String type, float d) {
        int size = (int)(24 * d);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        Path path = new Path();
        if (type.equals("refresh")) {
            canvas.drawCircle(size/2, size/2, size/3, paint);
            paint.setColor(Color.parseColor(THEME_COLOR));
            canvas.drawCircle(size/2, size/2, size/4, paint);
        } else if (type.equals("menu")) {
            canvas.drawCircle(size/4, size/2, 3*d, paint);
            canvas.drawCircle(size/2, size/2, 3*d, paint);
            canvas.drawCircle(3*size/4, size/2, 3*d, paint);
        } else if (type.equals("back")) {
            path.moveTo(size*0.7f, size*0.2f);
            path.lineTo(size*0.3f, size*0.5f);
            path.lineTo(size*0.7f, size*0.8f);
            canvas.drawPath(path, paint);
        }
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void injectSolidStateUI() {
        if (isUIInjected) return;
        final WebView webView = (WebView) this.bridge.getWebView();
        if (webView == null || webView.getParent() == null) return;

        float d = getResources().getDisplayMetrics().density;
        int barHeight = (int) (35 * d);
        int btnSize = (int) (35 * d);

        RelativeLayout topBar = new RelativeLayout(this);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(-1, barHeight));
        topBar.setBackgroundColor(Color.parseColor(THEME_COLOR));

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

        ImageView btnRefresh = new ImageView(this);
        btnRefresh.setImageDrawable(getSolidIcon("refresh", d));
        btnRefresh.setPadding((int)(8*d), (int)(8*d), (int)(8*d), (int)(8*d));
        btnRefresh.setOnClickListener(v -> webView.reload());
        RelativeLayout.LayoutParams lpR = new RelativeLayout.LayoutParams(btnSize, btnSize);
        lpR.addRule(RelativeLayout.ALIGN_PARENT_START);
        btnRefresh.setLayoutParams(lpR);
        topBar.addView(btnRefresh);

        ImageView btnMenu = new ImageView(this);
        btnMenu.setImageDrawable(getSolidIcon("menu", d));
        btnMenu.setPadding((int)(8*d), (int)(8*d), (int)(8*d), (int)(8*d));
        btnMenu.setOnClickListener(v -> showHamburgerMenu(webView));
        RelativeLayout.LayoutParams lpM = new RelativeLayout.LayoutParams(btnSize, btnSize);
        lpM.addRule(RelativeLayout.ALIGN_PARENT_END);
        btnMenu.setLayoutParams(lpM);
        topBar.addView(btnMenu);

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

    private void showHamburgerMenu(WebView webView) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.getWindow().setGravity(Gravity.TOP | Gravity.END);
        
        float den = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1A1A1A"));
        root.setPadding((int)(20*den), (int)(20*den), (int)(20*den), (int)(20*den));

        root.addView(createMenuRow("🕒 History", den, v -> { d.dismiss(); showHistoryPanel(webView); }));
        root.addView(createMenuRow("🖥️ Desktop Mode", den, v -> {
            isDesktopMode = !isDesktopMode;
            applyDesktopMode(webView, isDesktopMode);
            d.dismiss();
        }));
        root.addView(createMenuRow("📡 Status", den, v -> { webView.loadUrl("https://fluxerstatus.com/"); d.dismiss(); }));
        root.addView(createMenuRow("📝 Blog", den, v -> { webView.loadUrl("https://blog.fluxer.app/"); d.dismiss(); }));
        root.addView(createMenuRow("⚙️ Settings", den, v -> { d.dismiss(); showSettingsPage(webView); }));

        d.setContentView(root);
        d.show();
    }

    private void showSettingsPage(WebView webView) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        float d = getResources().getDisplayMetrics().density;
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding((int)(20*d), (int)(20*d), (int)(20*d), (int)(20*d));

        RelativeLayout header = new RelativeLayout(this);
        ImageView back = new ImageView(this);
        back.setImageDrawable(getSolidIcon("back", d));
        back.setOnClickListener(v -> dialog.dismiss());
        header.addView(back, new RelativeLayout.LayoutParams((int)(35*d), (int)(35*d)));
        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        RelativeLayout.LayoutParams lpT = new RelativeLayout.LayoutParams(-2, -2);
        lpT.addRule(RelativeLayout.CENTER_IN_PARENT);
        title.setLayoutParams(lpT);
        header.addView(title);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        addSection(content, "History Engine", d);
        content.addView(createInput("Limit", "30", "history_limit", d));
        content.addView(createInput("Vanish after (Days)", "7", "history_days", d));
        content.addView(createToggle("Vanish Mode Enabled", "history_vanish", d));

        addSection(content, "Browser Identity", d);
        content.addView(createInput("Custom User Agent", "set up your custom user agent", "user_agent", d));

        addSection(content, "Permissions Dashboard", d);
        content.addView(createToggle("Notifications", "perm_notif", d));
        content.addView(createToggle("Camera Access", "perm_camera", d));
        content.addView(createToggle("Microphone Access", "perm_mic", d));
        content.addView(createToggle("Storage Access", "perm_storage", d));

        addSection(content, "System", d);
        content.addView(createMenuRow("Check for Updates", d, v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/UnTamed-Fury/fluxer-temp/releases")));
        }));
        content.addView(createMenuRow("About Us", d, v -> {
            Toast.makeText(this, "About Us page coming soon!", Toast.LENGTH_SHORT).show();
        }));

        scroll.addView(content);
        root.addView(scroll);
        dialog.setContentView(root);
        dialog.show();
    }

    private void showHistoryPanel(WebView webView) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.getWindow().setGravity(Gravity.TOP | Gravity.END);
        
        float den = getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1A1A1A"));
        root.setPadding((int)(15*den), (int)(15*den), (int)(15*den), (int)(15*den));
        root.setLayoutParams(new ViewGroup.LayoutParams((int)(250*den), (int)(400*den)));

        TextView title = new TextView(this);
        title.setText("History");
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (String url : historyList) {
            list.addView(createMenuRow(url.replace("https://", ""), den, v -> { webView.loadUrl(url); d.dismiss(); }));
        }
        scroll.addView(list);
        root.addView(scroll);

        d.setContentView(root);
        d.show();
    }

    private void addSection(LinearLayout root, String title, float d) {
        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase());
        tv.setTextColor(Color.parseColor(THEME_COLOR));
        tv.setTextSize(10);
        tv.setPadding(0, (int)(25*d), 0, (int)(10*d));
        root.addView(tv);
    }

    private View createInput(String label, String hint, String key, float d) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.GRAY);
        l.addView(lbl);
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(prefs.getString(key, ""));
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.DKGRAY);
        et.addTextChangedListener(new android.text.TextWatcher() {
            public void afterTextChanged(android.text.Editable s) { prefs.edit().putString(key, s.toString()).apply(); }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        l.addView(et);
        return l;
    }

    private View createToggle(String label, String key, float d) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Color.WHITE);
        s.setChecked(prefs.getBoolean(key, true));
        s.setOnCheckedChangeListener((v, b) -> prefs.edit().putBoolean(key, b).apply());
        s.setPadding(0, (int)(10*d), 0, (int)(10*d));
        return s;
    }

    private View createMenuRow(String text, float d, View.OnClickListener l) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15);
        tv.setPadding(0, (int)(15*d), 0, (int)(15*d));
        tv.setOnClickListener(l);
        return tv;
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(historyList.size(), 30); i++) sb.append(historyList.get(i)).append(",");
        prefs.edit().putString("history", sb.toString()).apply();
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
                MainActivity.this.runOnUiThread(() -> request.grant(request.getResources()));
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
