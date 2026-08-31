package com.adhna.fooddelivery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    // رابط تطبيقك المرفوع على الإنترنت
    private static final String APP_URL = "https://food-delivery-jgy7.onrender.com/";
    private static final String APP_HOST = "food-delivery-jgy7.onrender.com";
    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;

    private String pendingGeoOrigin;
    private GeolocationPermissions.Callback pendingGeoCallback;

    // بنتذكر إذا التطبيق كان بالخلفية حتى نعرف إذا لازم نعمل reload لما يرجع يفتح
    private boolean wasInBackground = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        swipeRefresh.setColorSchemeColors(0xFFFF6B35, 0xFF00B894);
        swipeRefresh.setOnRefreshListener(() -> {
            // سحب للتحديث = يجيب آخر نسخة من الموقع من جديد (يتجاوز أي كاش)
            webView.clearCache(true);
            webView.reload();
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // حتى تشتغل أصوات التنبيه بدون لمسة
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // ما منسمح بأي كاش للصفحة نفسها - حتى كل فتح/تحديث يجيب آخر نسخة من Render فوراً
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // أي رابط من نفس موقع التطبيق يبقى بالداخل
                if (request.getUrl().getHost() != null && request.getUrl().getHost().contains(APP_HOST)) {
                    return false;
                }
                // أي رابط خارجي (مثل خرائط جوجل) يفتح بتطبيق/متصفح خارجي
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(MainActivity.this,
                            "تعذر الاتصال بالخادم، تحقق من الإنترنت وأعد المحاولة",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST);
                }
            }

            // يلتقط أي window.open() (مثل رابط خرائط جوجل) ويفتحه بتطبيق خارجي
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView tempWebView = new WebView(MainActivity.this);
                tempWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                        } catch (Exception ignored) {
                        }
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(tempWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.clearCache(true);
        webView.loadUrl(APP_URL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // لو المستخدم رجع للتطبيق بعد ما كان بالخلفية، منعمل تحديث تلقائي
        // حتى نضمن إنه شايف آخر نسخة منشورة على Render
        if (wasInBackground && webView != null) {
            webView.clearCache(true);
            webView.reload();
        }
        wasInBackground = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasInBackground = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && pendingGeoCallback != null) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
