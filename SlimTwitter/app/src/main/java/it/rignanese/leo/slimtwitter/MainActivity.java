/*
SlimSocial for Twitter is an Open Source app realized by Leonardo Rignanese
 GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
*/

package it.rignanese.leo.slimtwitter;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import im.delight.android.webview.AdvancedWebView;


public class MainActivity extends AppCompatActivity implements AdvancedWebView.Listener {
    private AdvancedWebView webViewTwitter;//the main webView where is shown twitter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //setup the floating button
        Button fab = (Button) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webViewTwitter.scrollTo(0, 0);//scroll up
            }
        });

        // setup the webView
        webViewTwitter = (AdvancedWebView) findViewById(R.id.webView);
        webViewTwitter.addPermittedHostname("mobile.twitter.com");
        webViewTwitter.addPermittedHostname("twitter.com");

        webViewTwitter.loadUrl(getString(R.string.urlTwitterMobile));//load homepage
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        webViewTwitter.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        webViewTwitter.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("Info", "onDestroy()");
        webViewTwitter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        webViewTwitter.onActivityResult(requestCode, resultCode, intent);
    }

    //*********************** WebView methods ****************************

    @Override
    public void onPageStarted(String url, Bitmap favicon) {}

    @Override
    public void onPageFinished(String url) { }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        String summary = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head><body><h1 " +
                "style='text-align:center; padding-top:15%;'>" + getString(R.string.titleNoConnection) + "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;'>" + getString(R.string.descriptionNoConnection) + "</h3>  <h5 style='text-align:center; padding-top:80%; opacity: 0.3;'>" + getString(R.string.awards) + "</h5></body></html>";
        webViewTwitter.loadData(summary, "text/html; charset=utf-8", "utf-8");//load a custom html page
    }

    @Override
    public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

    }

    @Override
    public void onExternalPageRequest(String url) {

    }


    //*********************** KEY ****************************
    // handling the back button
    @Override
    public void onBackPressed() {
        if (webViewTwitter.canGoBack()) {
            webViewTwitter.goBack();
        } else {
            finish();// exit
        }
    }
}



