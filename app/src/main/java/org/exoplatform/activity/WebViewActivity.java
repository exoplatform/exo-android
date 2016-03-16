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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import android.webkit.CookieManager;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by chautn on 10/14/15. Activity that loads Platform into a webview
 */
public class WebViewActivity extends AppCompatActivity {

  public static final String INTENT_KEY_URL = "URL";

  public WebView             webView;

  public ProgressBar         progressBar;

  ServerManagerImpl          serverManager;

  public boolean             loggedOut      = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    serverManager = new ServerManagerImpl(getSharedPreferences(App.Preferences.FILE_NAME, 0));
    String url = getIntent().getStringExtra(INTENT_KEY_URL);
    // save history
    try {
      Server server = new Server(new URL(url), new Date().getTime());
      serverManager.addServer(server);
    } catch (MalformedURLException e) {
      if (BuildConfig.DEBUG)
        Log.d(this.getClass().getName(), e.getMessage());
    }
    webView = (WebView) findViewById(R.id.webview);
    progressBar = (ProgressBar) findViewById(R.id.progressBar);

    webView.setWebViewClient(new MyWebViewClient());
    webView.getSettings().setJavaScriptEnabled(true);

    // Set progress bar.
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int progress) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setProgress(progress);
        if (progress == 100) {
          progressBar.setVisibility(ProgressBar.GONE);
        } else {
          progressBar.setVisibility(ProgressBar.VISIBLE);
        }
      }
    });

    webView.loadUrl(url);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Saving the last time an intranet was visited, for rule SIGN_IN_13
    SharedPreferences.Editor pref = getSharedPreferences(App.Preferences.FILE_NAME, 0).edit();
    pref.putLong(App.Preferences.LAST_VISIT_TIME, System.nanoTime());
    pref.apply();
  }

  /**
   * Go to the previous page in history when device Back button is pressed. If
   * there is no previous page, leave activity.
   */
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
      webView.goBack();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  private class MyWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url) {
      if (BuildConfig.DEBUG)
        Log.d(this.getClass().getName(), url); // print URL
      if (url.contains("portal:action=Logout")) {
        WebViewActivity.this.loggedOut = true;
      }
      webView.loadUrl(url);
      return false;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
      if (BuildConfig.DEBUG) {
        String cookies = CookieManager.getInstance().getCookie(url);
        if (cookies != null) {
          String[] cookie_array = cookies.split(";");
          for (String cookie : cookie_array) {
            Log.d(this.getClass().getName(), cookie.trim()); // print cookie
          }
        }
      }
      if (WebViewActivity.this.loggedOut) {
        WebViewActivity.this.loggedOut = false;
        WebViewActivity.this.finish();
      } else {
        super.onPageFinished(webView, url);
      }
    }
  }
}
