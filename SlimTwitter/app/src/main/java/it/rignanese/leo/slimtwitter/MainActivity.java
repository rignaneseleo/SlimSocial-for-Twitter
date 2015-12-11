/*
SlimTwitter is an Open Source app realized by Leonardo Rignanese
 GNU GENERAL PUBLIC LICENSE  Version 2, June 1991
*/

package it.rignanese.leo.slimtwitter;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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



public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefreshLayout;//the layout that allows the swipe refresh

    private WebView webViewTwitter;//the main webView where is shown twitter
    //to upload files
    public static final int INPUT_FILE_REQUEST_CODE = 1;
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    private Menu optionsMenu;//contains the main menu

    private SharedPreferences savedPreferences;//contains all the values of saved preferences

    boolean noConnectionError = false;//flag: is true if there is a connection error and it should be reload not the error page but the last useful
    boolean swipeRefresh = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setup the sharedPreferences
        savedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        setContentView(R.layout.activity_main);

        //setup the floating button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webViewTwitter.scrollTo(0, 0);//scroll up
            }
        });

        // setup the refresh layout
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeResources(R.color.officialAzureTwitter, R.color.darkAzureSlimTwitterTheme);// set the colors
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPage();//reload the page
                swipeRefresh = true;
            }
        });

        // setup the webView
        webViewTwitter = (WebView) findViewById(R.id.webView);
        setUpWebViewDefaults(webViewTwitter);//set the settings

        goHome();//load homepage

        //WebViewClient that is the client callback.
        webViewTwitter.setWebViewClient(new WebViewClient() {//advanced set up

            // when there isn't a connetion
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String summary = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /></head><body><h1 " +
                        "style='text-align:center; padding-top:15%;'>" + getString(R.string.titleNoConnection) + "</h1> <h3 style='text-align:center; padding-top:1%; font-style: italic;'>" + getString(R.string.descriptionNoConnection) + "</h3>  <h5 style='text-align:center; padding-top:80%; opacity: 0.3;'>" + getString(R.string.awards) + "</h5></body></html>";
                webViewTwitter.loadData(summary, "text/html; charset=utf-8", "utf-8");//load a custom html page

                noConnectionError = true;
                swipeRefreshLayout.setRefreshing(false); //when the page is loaded, stop the refreshing
            }

            // when I click in a external link
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null || url.contains("twitter.com")) {
                    //url is ok
                    return false;
                } else {
                    //if the link doesn't contain 'twitter.com', open it using the browser
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
            }

            //START management of loading
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefreshLayout.setRefreshing(false); //when the page is loaded, stop the refreshing
                super.onPageFinished(view, url);
            }
            //END management of loading

        });

        //WebChromeClient for handling all chrome functions.
        webViewTwitter.setWebChromeClient(new WebChromeClient() {
            //to upload files
            //thanks to gauntface
            //https://github.com/GoogleChrome/chromium-webview-samples
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getBaseContext().getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });
    }

    //*********************** UPLOAD FILES ****************************
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //used to upload files
        //thanks to gauntface
        //https://github.com/GoogleChrome/chromium-webview-samples
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                if (mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
        return;
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }


    //*********************** KEY ****************************
    // management the back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webViewTwitter.canGoBack()) {
                        webViewTwitter.goBack();
                    } else {
                        // exit
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    //*********************** MENU ****************************
    //add my menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    //management the tap on the menu's items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh: {//refresh the page
                refreshPage();
                break;
            }
            case R.id.openInBrowser: {//open the actual page into using the browser
                webViewTwitter.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webViewTwitter.getUrl())));
                break;
            }
            case R.id.share: {//share this app
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.downloadInstruction));
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share)));

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.thanks),
                        Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.settings: {//open settings
                startActivity(new Intent(this, ShowSettingsActivity.class));
                return true;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    //*********************** WEBVIEW FACILITIES ****************************
    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();

        // Enable Javascript
        settings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            settings.setDisplayZoomControls(false);
        }

        // Enable remote debugging via chrome://inspect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private void goHome() {
        webViewTwitter.loadUrl(getString(R.string.urlTwitterMobile));//load .twitter.com
    }

    private void refreshPage() {
        if (noConnectionError) {
            webViewTwitter.goBack();
            noConnectionError = false;
        } else webViewTwitter.reload();
    }
}


