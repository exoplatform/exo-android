package org.exoplatform.exohybridapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by chautn on 10/14/15.
 */
public class MainActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		WebView webView = (WebView) findViewById(R.id.webview);

		WebChromeClient client = new WebChromeClient();
		webView.setWebChromeClient(client);

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);

		//webView.loadUrl("file:///android_asset/html/test.html");
    webView.loadUrl("http://plfent-4.3.x-pkgpriv-responsive-design-snapshot.acceptance6.exoplatform.org/");
	}
}
