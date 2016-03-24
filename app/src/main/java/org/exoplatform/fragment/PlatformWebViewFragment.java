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
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
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

import java.io.IOException;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * WebView that is configured to display content from a Platform 4.3+ intranet.
 * TODO extend WebViewFragment to reuse some methods
 */
public class PlatformWebViewFragment extends Fragment {

  // the URL to load in the web view
  private static final String        ARG_SERVER        = "SERVER";

  private final String               LOG_TAG           = PlatformWebViewFragment.class.getName();

  private PlatformNavigationCallback mListener;

  private WebView                    mWebView;

  private ProgressBar                mProgressBar;

  private Server                     mServer;

  private Button                     mDoneButton;

  private final Pattern              PAGE_NAME_PATTERN = Pattern.compile("(/[a-z0-9]*/)([a-z0-9]*/)?(login|register)");

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
      new ServerManagerImpl(getActivity().getSharedPreferences(App.Preferences.FILE_NAME, 0)).addServer(mServer);
    }
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
    mWebView.stopLoading();
    mWebView.destroy();
    mListener = null;
    super.onDetach();
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

  public void refreshLayout() {
    if (mWebView != null)
      mWebView.getSettings().setLayoutAlgorithm(mWebView.getSettings().getLayoutAlgorithm());
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

  private class PlatformWebViewClient extends WebViewClient {

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url != null && url.contains(mServer.getShortUrl())) {
        // url is on the server's domain, keep loading normally
        return super.shouldOverrideUrlLoading(view, url);
      } else {
        // url is on an external domain, load in a different fragment
        mListener.onLoadExternalContent(url);
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
      // Inform the activity whether we are on the login or register page
      String path = uri.getPath();
      mListener.isOnPageWithoutNavigation(path != null && PAGE_NAME_PATTERN.matcher(path).matches());
      // Return to the previous activity if user has signed out
      String queryString = uri.getQuery();
      if (queryString != null && queryString.contains("portal:action=Logout")) {
        mListener.onUserSignedOut();
      }
    }
  }

  public interface PlatformNavigationCallback {
    void isOnPageWithoutNavigation(boolean value);

    void onUserSignedOut();

    void onLoadExternalContent(String url);
  }
}
