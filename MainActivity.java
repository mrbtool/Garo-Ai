package com.garo.translate;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Make sure you have a webView in this layout

        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // 1. Link the HTML to this Java class
        webView.addJavascriptInterface(new NativeBridge(this), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        
        // Load your HTML file
        webView.loadUrl("file:///android_asset/index.html"); 
    }

    // 2. Native Account Picker Logic (From your code)
    public void startNativeGoogleLogin() {
        try {
            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                intent = (Intent) AccountManager.class.getMethod("newChooseAccountIntent", 
                        android.accounts.Account.class, java.util.List.class, String[].class, String.class, String.class, String[].class, Bundle.class)
                        .invoke(null, null, null, new String[]{"com.google"}, null, null, null, null);
            } else {
                intent = (Intent) AccountManager.class.getMethod("newChooseAccountIntent", 
                        android.accounts.Account.class, java.util.ArrayList.class, String[].class, boolean.class, String.class, String.class, String[].class, Bundle.class)
                        .invoke(null, null, null, new String[]{"com.google"}, false, null, null, null, null);
            }
            startActivityForResult(intent, 1001);
        } catch (Exception e) {
            // Fallback
            sendEmailToWeb("Guest User");
        }
    }

    // 3. Catch the Result from the Google Account Picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            String accountEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountEmail != null) {
                sendEmailToWeb(accountEmail);
            } else {
                Toast.makeText(this, "Login Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 4. Send the chosen email BACK to the HTML
    private void sendEmailToWeb(final String email) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Calls the window.onNativeLoginSuccess() function inside your HTML
                webView.evaluateJavascript("window.onNativeLoginSuccess('" + email + "');", null);
            }
        });
    }

    // ===================================================
    // THIS IS THE BRIDGE THAT THE HTML IS LOOKING FOR
    // ===================================================
    public class NativeBridge {
        Context mContext;
        NativeBridge(Context c) { mContext = c; }

        @JavascriptInterface
        public String getNativeUserEmail() {
            return null; 
        }

        @JavascriptInterface
        public void logoutNative() {
            // Add native logout logic here if needed
        }

        // The HTML button looks for THIS exact method!
        @JavascriptInterface
        public void startGoogleLogin() {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MainActivity) mContext).startNativeGoogleLogin();
                }
            });
        }
    }
}
