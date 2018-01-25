package org.exoplatform.fragment;

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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.cookies.CookiesConverter;
import org.exoplatform.tool.cookies.CookiesInterceptor;
import org.exoplatform.tool.cookies.CookiesInterceptorFactory;
import org.exoplatform.tool.cookies.WebViewCookieHandler;

import java.io.IOException;
import java.net.URLConnection;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * WebView that is configured to display content from a Platform 4.3+ intranet.
 * TODO extend WebViewFragment to reuse some methods
 */
public class PlatformWebViewFragment extends Fragment {

  // the URL to load in the web view
  private static final String        ARG_SERVER          = "SERVER";

  private static final int FILECHOOSER_RESULTCODE = 1;

  public static final String         TAG                 = PlatformWebViewFragment.class.getName();

  private PlatformNavigationCallback mListener;

  private WebView                    mWebView;

  private ProgressBar                mProgressBar;

  private Server                     mServer;

  private Button                     mDoneButton;

  private ValueCallback<Uri[]>       mUploadMessage;

  private boolean                    mDidShowOnboarding;

  private final Pattern              INTRANET_HOME_PAGE  = Pattern.compile("^(.*)(/portal/intranet)(/?)$");

  private final Pattern              LOGIN_REGISTER_PAGE = Pattern.compile("(/[a-z0-9]*/)([a-z0-9]*/)?(login|register)");

  private final String               LOGOUT_PATH         = "portal:action=Logout";

  private CookiesInterceptor         mCookiesInterceptor = new CookiesInterceptorFactory().create();

  private CookiesConverter           mCookiesConverter   = new CookiesConverter();

  public PlatformWebViewFragment() {
    // Required empty public constructor
  }

  /**
   * Create a new instance of PlatformWebViewFragment to load the given Platform
   * intranet server
   *
   * @param server the server to load initially in this fragment
   * @return A new instance of fragment PlatformWebViewFragment.
   */
  public static PlatformWebViewFragment newInstance(Server server) {
    PlatformWebViewFragment fragment = new PlatformWebViewFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_SERVER, server);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mServer = getArguments().getParcelable(ARG_SERVER);
      // save history
      new ServerManagerImpl(App.Preferences.get(getContext())).addServer(mServer);
    }
    mDidShowOnboarding = App.Preferences.get(getContext()).getBoolean(App.Preferences.DID_SHOW_ONBOARDING, false);
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_platform_web_view, container, false);
    // create web view
    mWebView = (WebView) layout.findViewById(R.id.PlatformWebViewFragment_WebView);
    mWebView.setWebViewClient(new PlatformWebViewClient());
    mWebView.getSettings().setJavaScriptEnabled(true);
    mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    mWebView.getSettings().setDomStorageEnabled(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }
    mWebView.getSettings().setUserAgentString("eXo/" + BuildConfig.VERSION_NAME + " (Android)");
    mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    mWebView.getSettings().setDisplayZoomControls(false);
    // set progress bar
    mProgressBar = (ProgressBar) layout.findViewById(R.id.PlatformWebViewFragment_ProgressBar);
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

      @Override
      public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        mUploadMessage = filePathCallback;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        PlatformWebViewFragment.this.startActivityForResult(
                Intent.createChooser(i, "File Browser"),
                FILECHOOSER_RESULTCODE);
        return true;
      }
    });
    mWebView.loadUrl(mServer.getUrl().toString());
    // done button for content without navigation, e.g. image
    mDoneButton = (Button) layout.findViewById(R.id.PlatformWebViewFragment_Done_Button);
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
    return layout;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == FILECHOOSER_RESULTCODE) {
      if (mUploadMessage == null) {
        return;
      }
      Uri result = data == null || resultCode != Activity.RESULT_OK ? null
              : data.getData();
      mUploadMessage.onReceiveValue(new Uri[] {result});
      mUploadMessage = null;
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof PlatformNavigationCallback) {
      mListener = (PlatformNavigationCallback) context;
    } else {
      throw new RuntimeException(context.toString() + " must implement PlatformNavigationCallback");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mWebView.stopLoading();
    mWebView.destroy();
    mListener = null;
  }

  /**
   * Go back in the webview's history, if possible
   * 
   * @return true if the webview did go back, false otherwise
   */
  public boolean goBack() {
    if (mWebView != null && mWebView.canGoBack()) {
      mWebView.goBack();
      return true;
    }
    return false;
  }

  private void refreshLayoutForContent(String contentType) {
    if (contentType != null && !contentType.contains("text/html")) {
      // Display content fullscreen, with done button visible
      if (getActivity() != null)
        getActivity().getWindow()
                     .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      mWebView.getSettings().setUseWideViewPort(true);
      mWebView.getSettings().setLoadWithOverviewMode(true);
      mWebView.getSettings().setBuiltInZoomControls(true);
      mDoneButton.setVisibility(View.VISIBLE);
    } else {
      // Display content normally, display status bar
      if (getActivity() != null)
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      mWebView.getSettings().setUseWideViewPort(false);
      mWebView.getSettings().setLoadWithOverviewMode(false);
      mWebView.getSettings().setBuiltInZoomControls(false);
      mDoneButton.setVisibility(View.GONE);
    }
  }

  private void getContentTypeAsync(String url) {
    OkHttpClient client = ExoHttpClient.getInstance().newBuilder().cookieJar(new WebViewCookieHandler()).build();
    Request req = new Request.Builder().url(url).head().build();
    client.newCall(req).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        // ignore network failure
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (response.isSuccessful()) {
          final String contentType = response.header("Content-Type");
          if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                refreshLayoutForContent(contentType);
              }
            });
          }
        }
        response.body().close();
      }
    });
  }

  private void checkUserLoggedInAsync() {
    OkHttpClient client = ExoHttpClient.getInstance().newBuilder().cookieJar(new WebViewCookieHandler()).build();
    String plfInfo = mServer.getUrl().getProtocol() + "://" + mServer.getShortUrl() + "/rest/private/platform/info";
    Request req = new Request.Builder().url(plfInfo).get().build();
    client.newCall(req).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        // ignore network failure
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (response.isSuccessful()) {
          App.Preferences.get(getContext()).edit().putBoolean(App.Preferences.DID_SHOW_ONBOARDING, true).apply();
          mDidShowOnboarding = true;
          mListener.onFirstTimeUserLoggedIn();
        }
        response.body().close();
      }
    });
  }

  private class PlatformWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url != null && url.contains(mServer.getShortUrl())) {
        if (url.contains(LOGOUT_PATH)) {
          mListener.onUserJustBeforeSignedOut();
        }
        // url is on the server's domain, keep loading normally
        return super.shouldOverrideUrlLoading(view, url);
      } else {
        // url is on an external domain, load in a different fragment
        mListener.onExternalContentRequested(url);
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
        getContentTypeAsync(url);
      // new GetContentTypeHeaderTask().execute(url);
      else
        refreshLayoutForContent(contentType);
      // Inform the activity whether we are on the login or register page
      String path = uri.getPath();
      if (mListener != null)
        // May be called after the listener has been set to null in onDetach
        mListener.onPageStarted(path != null && LOGIN_REGISTER_PAGE.matcher(path).matches());
      // Return to the previous activity if user has signed out
      String queryString = uri.getQuery();
      if (queryString != null && queryString.contains("portal:action=Logout")) {
        mListener.onUserSignedOut();
      }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
      super.onPageFinished(view, url);
      if (!mDidShowOnboarding && INTRANET_HOME_PAGE.matcher(url).matches()) {
        checkUserLoggedInAsync();
      }
      mCookiesInterceptor.intercept(mCookiesConverter.toMap(CookieManager.getInstance().getCookie(url)), url);
      if (BuildConfig.DEBUG)
        Log.d(TAG, "COOKIES: " + CookieManager.getInstance().getCookie(url));
    }
  }

  public interface PlatformNavigationCallback {
    void onPageStarted(boolean needsToolbar);

    void onUserSignedOut();

    void onUserJustBeforeSignedOut();

    void onExternalContentRequested(String url);

    void onFirstTimeUserLoggedIn();
  }
}
