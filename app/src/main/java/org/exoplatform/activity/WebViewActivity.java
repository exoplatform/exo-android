package org.exoplatform.activity;

/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import android.webkit.CookieManager;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.model.Server;

import java.io.IOException;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by chautn on 10/14/15. Activity that loads Platform into a web view
 */
public class WebViewActivity extends AppCompatActivity {

  public static final String  INTENT_KEY_URL = "URL";

  private static final String LOG_TAG        = WebViewActivity.class.getName();

  private WebView             mWebView;

  private ProgressBar         mProgressBar;

  private Server              mServer;

  private Button              mDoneButton;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    // toolbar, hidden by default, visible on certain pages cf onPageStarted
    Toolbar mToolbar = (Toolbar) findViewById(R.id.WebViewScreen_Toolbar);
    setSupportActionBar(mToolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // url to load
    String url = getIntent().getStringExtra(INTENT_KEY_URL);
    // save history
    try {
      mServer = new Server(new URL(url), new Date().getTime());
      new ServerManagerImpl(getSharedPreferences(App.Preferences.FILE_NAME, 0)).addServer(mServer);
    } catch (MalformedURLException e) {
      if (BuildConfig.DEBUG)
        Log.d(LOG_TAG, e.getMessage());
    }
    // create web view
    mWebView = (WebView) findViewById(R.id.WebViewScreen_WebView);
    mWebView.setWebViewClient(new MyWebViewClient());
    mWebView.getSettings().setJavaScriptEnabled(true);
    mWebView.getSettings().setUserAgentString("eXo/" + BuildConfig.VERSION_NAME + " (Android)");
    mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    mWebView.getSettings().setDisplayZoomControls(false);
    // set progress bar
    mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    mWebView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int progress) {
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mProgressBar.setProgress(progress);
        if (progress == 100) {
          mProgressBar.setVisibility(ProgressBar.GONE);
        } else {
          mProgressBar.setVisibility(ProgressBar.VISIBLE);
        }
      }
    });
    mWebView.loadUrl(url);
    // done button for content without navigation, e.g. image
    mDoneButton = (Button) findViewById(R.id.WebViewScreen_Done_Button);
    if (mDoneButton != null) {
      mDoneButton.setVisibility(View.GONE);
      mDoneButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (mWebView != null && mWebView.canGoBack())
            mWebView.goBack();
        }
      });
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Saving the last time an intranet was visited, for rule SIGN_IN_13
    SharedPreferences.Editor pref = getSharedPreferences(App.Preferences.FILE_NAME, 0).edit();
    pref.putLong(App.Preferences.LAST_VISIT_TIME, System.nanoTime());
    pref.apply();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // refresh the layout of the webview, but don't reload it, when orientation
    // changes
    if (mWebView != null)
      mWebView.getSettings().setLayoutAlgorithm(mWebView.getSettings().getLayoutAlgorithm());
  }

  /**
   * Go to the previous page in history when device Back button is pressed. If
   * there is no previous page, leave activity.
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
      mWebView.goBack();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void animateShowHideToolbar(boolean show) {
    if (getSupportActionBar() != null) {
      if (show)
        getSupportActionBar().show();
      else
        getSupportActionBar().hide();
    }
  }

  private List<HttpCookie> getCookiesForUrl(String url) {
    List<HttpCookie> cookieList = new ArrayList<>();
    String cookies = CookieManager.getInstance().getCookie(url);
    if (cookies != null) {
      String[] cookieArray = cookies.split(";");
      for (String cookieStr : cookieArray) {
        String[] cookieParts = cookieStr.split("=");
        if (cookieParts.length >= 2)
          cookieList.add(new HttpCookie(cookieParts[0], cookieParts[1]));
      }
    }
    return cookieList;
  }

  private OkHttpClient httpClientWithWebViewCookies(@NonNull String url) throws URISyntaxException {
    URI uri = new URI(url);
    List<HttpCookie> cookieList = getCookiesForUrl(url);
    java.net.CookieManager cookieManager = new java.net.CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    for (HttpCookie cookie : cookieList) {
      cookieManager.getCookieStore().add(uri, cookie);
    }
    return ExoHttpClient.getInstance().newBuilder().cookieJar(new JavaNetCookieJar(cookieManager)).build();
  }

  private void refreshLayoutForContent(String contentType) {
    if (contentType != null && !contentType.contains("text/html")) {
      // Display content fullscreen, with done button visible
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      mWebView.getSettings().setUseWideViewPort(true);
      mWebView.getSettings().setLoadWithOverviewMode(true);
      mWebView.getSettings().setBuiltInZoomControls(true);
      mDoneButton.setVisibility(View.VISIBLE);
    } else {
      // Display content normally, display status bar
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      mWebView.getSettings().setUseWideViewPort(false);
      mWebView.getSettings().setLoadWithOverviewMode(false);
      mWebView.getSettings().setBuiltInZoomControls(false);
      mDoneButton.setVisibility(View.GONE);
    }
  }

  private class MyWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      Uri uri = Uri.parse(url);
      if (mServer.getShortUrl().equalsIgnoreCase(uri.getHost())) {
        return super.shouldOverrideUrlLoading(view, url);
      } else {
        // TODO pop up new webview to load the external content
        return true;
      }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      Uri uri = Uri.parse(url);
      // Show / hide the Done button
      String contentType = URLConnection.guessContentTypeFromName(uri.getPath());
      if (contentType == null)
        new GetContentTypeHeaderTask().execute(url);
      else
        refreshLayoutForContent(contentType);
      // Show / hide the toolbar
      Pattern loginOrRegister = Pattern.compile("(/[a-z0-9]*/)([a-z0-9]*/)?(login|register)");
      String path = uri.getPath();
      animateShowHideToolbar(path != null && loginOrRegister.matcher(path).matches());
      // Return to the previous activity if user has signed out
      String queryString = uri.getQuery();
      if (queryString != null && queryString.contains("portal:action=Logout")) {
        WebViewActivity.this.finish();
      }
    }
  }

  // TODO extract as separate class
  // use listener interface for callback(s)
  // pass OkHttpClient, URL and listener in constructor
  //
  private class GetContentTypeHeaderTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... urls) {
      if (urls == null || urls.length == 0)
        return null;

      String url = urls[0];
      OkHttpClient client;
      try {
        client = httpClientWithWebViewCookies(url);
      } catch (URISyntaxException e) {
        if (BuildConfig.DEBUG)
          Log.d(LOG_TAG, e.getMessage(), e);
        return null;
      }
      Request req = new Request.Builder().url(url).head().build();
      Response resp;
      try {
        resp = client.newCall(req).execute();
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
      String contentType = null;
      if (resp.isSuccessful()) {
        contentType = resp.header("Content-Type");
      }
      return contentType;
    }

    @Override
    protected void onPostExecute(String contentType) {
      super.onPostExecute(contentType);
      refreshLayoutForContent(contentType);
    }
  }
}
