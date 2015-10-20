package org.exoplatform.exohybridapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by chautn on 10/14/15.
 */
public class MainActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
    getWindow().requestFeature(Window.FEATURE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		WebView webView = (WebView) findViewById(R.id.webview);

		webView.setWebViewClient(new WebViewClient());

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);

    final Activity myActivity = this;
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int progress) {
        myActivity.setProgress(progress * 1000);
      }
    });

		//webView.loadUrl("file:///android_asset/html/test.html");
    webView.loadUrl("http://plfent-4.3.x-pkgpriv-responsive-design-snapshot.acceptance6.exoplatform.org/");
	}
}
