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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.exoplatform.BuildConfig;
import org.exoplatform.R;

/**
 * A simple web view with a progress bar, in a fragment
 */
public class WebViewFragment extends Fragment {

  private static final String     ARG_URL = "URL";

  private final String            LOG_TAG = WebViewFragment.class.getName();

  private String                  mUrl;

  private WebViewFragmentCallback mListener;

  protected WebView               mWebView;

  protected ProgressBar           mProgressBar;

  protected ImageButton           mCloseButton;

  protected TextView              mPageTitle;

  public WebViewFragment() {
    // Required empty public constructor
  }

  /**
   * url the URL to load in this webview
   * 
   * @return A new instance of fragment WebViewFragment.
   */
  public static WebViewFragment newInstance(String url) {
    WebViewFragment fragment = new WebViewFragment();
    Bundle args = new Bundle();
    args.putString(ARG_URL, url);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mUrl = getArguments().getString(ARG_URL);
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_web_view, container, false);
    mPageTitle = (TextView) layout.findViewById(R.id.WebViewFragment_PageTitle);
    mCloseButton = (ImageButton) layout.findViewById(R.id.WebViewFragment_CloseButton);
    mCloseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mListener != null)
          mListener.onCloseFragment();
      }
    });
    mWebView = (WebView) layout.findViewById(R.id.WebViewFragment_WebView);
    // TODO customize web view client
    mWebView.setWebViewClient(new WebViewClient());
    mWebView.getSettings().setJavaScriptEnabled(true);
    mWebView.getSettings().setUserAgentString("eXo/" + BuildConfig.VERSION_NAME + " (Android)");
    mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    mWebView.getSettings().setBuiltInZoomControls(true);
    mWebView.getSettings().setDisplayZoomControls(true);
    mProgressBar = (ProgressBar) layout.findViewById(R.id.WebViewFragment_ProgressBar);
    mWebView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        mPageTitle.setText(title);
      }

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
    mWebView.loadUrl(mUrl);
    return layout;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof WebViewFragmentCallback) {
      mListener = (WebViewFragmentCallback) context;
    } else {
      throw new RuntimeException(context.toString() + " must implement WebViewFragmentCallback");
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

  public interface WebViewFragmentCallback {
    void onCloseFragment();
  }
}
