package org.exoplatform.exohybridapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import android.webkit.CookieManager;

/**
 * Created by chautn on 10/14/15.
 */
public class MainActivity extends AppCompatActivity {

  public WebView webView;
  public ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
    getWindow().requestFeature(Window.FEATURE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
    getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		webView = (WebView) findViewById(R.id.webview);
    progressBar = (ProgressBar) findViewById(R.id.progressBar);

		webView.setWebViewClient(new MyWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);

    //Set progress bar.
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int progress) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setProgress(progress * 1000);
        if (progress == 100) {
          progressBar.setVisibility(ProgressBar.GONE);
        }
      }
    });

    SharedPreferences preferences = getSharedPreferences(App.SHARED_PREFERENCES_NAME, 0);
    String url = preferences.getString("BASE_URL", "http://localhost:8080");
    webView.loadUrl(url);
	}

  /**
   * Go to the previous page in history when device Back button is pressed.
   * If there is no previous page, leave activity.
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
      Log.d(this.getClass().getName(), url); //print URL
      webView.loadUrl(url);
      return true;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
      super.onPageFinished(webView, url);
      String cookies = CookieManager.getInstance().getCookie(url);
      if (cookies != null) {
        String[] cookie_array = cookies.split(";");
        if (cookie_array != null) {
          for (String cookie : cookie_array) {
            Log.d(this.getClass().getName(), cookie.trim()); //print cookie
          }
        }
      }
    }
  }
}
