/*
SlimSocial for Twitter is an Open Source app realized by Leonardo Rignanese
 GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
*/

package it.rignanese.leo.slimtwitter;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import im.delight.android.webview.AdvancedWebView;


public class MainActivity extends Activity implements AdvancedWebView.Listener {
    //the main webView where is shown twitter
    private AdvancedWebView webViewTwitter;

    //object to show full screen videos
    private WebChromeClient myWebChromeClient;
    private FrameLayout mTargetView;
    private FrameLayout mContentView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //setup the floating button
        com.github.clans.fab.FloatingActionButton fab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webViewTwitter.scrollTo(0, 0);//scroll up
            }
        });


        // setup the webView
        webViewTwitter = (AdvancedWebView) findViewById(R.id.webView);

        webViewTwitter.setListener(this, this);
        webViewTwitter.addPermittedHostname("mobile.twitter.com");
        webViewTwitter.addPermittedHostname("twitter.com");

        //full screen video
        myWebChromeClient = new WebChromeClient(){
            //this custom WebChromeClient allow to show video on full screen
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                mCustomViewCallback = callback;
                mTargetView.addView(view);
                mCustomView = view;
                mContentView.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null)
                    return;

                mCustomView.setVisibility(View.GONE);
                mTargetView.removeView(mCustomView);
                mCustomView = null;
                mTargetView.setVisibility(View.GONE);
                mCustomViewCallback.onCustomViewHidden();
                mContentView.setVisibility(View.VISIBLE);
            }
        };
        webViewTwitter.setWebChromeClient(myWebChromeClient);
        mContentView = (FrameLayout) findViewById(R.id.main_content);
        mTargetView = (FrameLayout) findViewById(R.id.target_view);


        String urlSharer = ExternalLinkListener();//get the external shared link (if it exists)
        if (urlSharer != null) {//if is a share request
            webViewTwitter.loadUrl(urlSharer);//load the sharer url
        } else {
            webViewTwitter.loadUrl(getString(R.string.urlTwitterMobile));//load homepage
        }
    }

    // app is already running and gets a new intent (used to share link without open another activity
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        webViewTwitter.loadUrl(ExternalLinkListener());
    }

    private String ExternalLinkListener() {
        // grab an url if opened by clicking a link
        String webViewUrl = getIntent().getDataString();
        /** get a subject and text and check if this is a link trying to be shared */
        String sharedSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String sharedUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        // if we have a valid URL that was shared by us, open the sharer
        if (sharedUrl != null) {
            if (!sharedUrl.equals("")) {
                Log.e("Info", "sharedUrl != null");
                // check if the URL being shared is a proper web URL
                if (!sharedUrl.startsWith("http://") || !sharedUrl.startsWith("https://")) {
                    // if it's not, let's see if it includes an URL in it (prefixed with a message)
                    int startUrlIndex = sharedUrl.indexOf("http:");
                    if (startUrlIndex > 0) {
                        // seems like it's prefixed with a message, let's trim the start and get the URL only
                        sharedUrl = sharedUrl.substring(startUrlIndex);
                    }
                }
                // final step, set the proper Sharer...
                webViewUrl = String.format("https://twitter.com/intent/tweet?text=%s&url=%s", sharedSubject, sharedUrl);
                // ... and parse it just in case
                webViewUrl = Uri.parse(webViewUrl).toString();
            }
        }
        return webViewUrl;
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
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        String summary = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head><body><h1 " +
                "style='text-align:center; padding-top:15%;'>" + getString(R.string.titleNoConnection) + "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;'>" + getString(R.string.descriptionNoConnection) + "</h3>  <h5 style='text-align:center; padding-top:80%; opacity: 0.3;'>" + getString(R.string.awards) + "</h5></body></html>";
        webViewTwitter.loadData(summary, "text/html; charset=utf-8", "utf-8");//load a custom html page
    }

    @Override
    public void onDownloadRequested(String url, String userAgent, String
            contentDisposition, String mimetype, long contentLength) {

    }

    @Override
    public void onExternalPageRequest(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }


    //*********************** KEY ****************************
    // handling the back button
    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            myWebChromeClient.onHideCustomView();//hide video player
        } else {
            if (webViewTwitter.canGoBack()) {
                webViewTwitter.goBack();
            } else {
                finish();// close app
            }
        }
    }
}




