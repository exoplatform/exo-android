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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import static android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE;
import static android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.BroadcastIntentHelper;
import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import timber.log.Timber;
/**
 * A simple web view with a progress bar, in a fragment
 */
public class WebViewFragment extends Fragment {

  private static final String     ARG_URL = "URL";

  public static final String      TAG     = WebViewFragment.class.getName();

  private String                  mUrl;

  private WebViewFragmentCallback mListener;

  protected WebView               mWebView;

  protected ProgressBar           mProgressBar;

  private Integer                  countJs = 0;

  public WebViewFragment() {
    // Required empty public constructor
  }
  private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      onBroadcastReceived(intent);
    }
  };
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
    mWebView = layout.findViewById(R.id.WebViewFragment_WebView);
    mWebView.setWebViewClient(new WebViewClient());
    mWebView.getSettings().setJavaScriptEnabled(true);
    mWebView.getSettings().setDomStorageEnabled(true);
    mWebView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    mWebView.getSettings().setBuiltInZoomControls(true);
    mWebView.getSettings().setDisplayZoomControls(false);
    // set custom user agent by filtering the default one
    String default_userAgent = mWebView.getSettings().getUserAgentString();
    int startIndex = default_userAgent.indexOf("Mozilla/");
    int endIndex = default_userAgent.indexOf("wv");
    String toBeReplaced = default_userAgent.substring(startIndex, endIndex);
    String userAgent = "eXo/" + BuildConfig.VERSION_NAME + default_userAgent.replace(toBeReplaced, "") + " (Android)";
    mWebView.getSettings().setUserAgentString(userAgent);
    mProgressBar = (ProgressBar) layout.findViewById(R.id.WebViewFragment_ProgressBar);
    String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE,
                             Manifest.permission.WRITE_EXTERNAL_STORAGE,
                             Manifest.permission.INTERNET,
                             Manifest.permission.RECORD_AUDIO,
                             Manifest.permission.CAMERA };
    ActivityCompat.requestPermissions(this.getActivity(),permissions,1010);
    if (mUrl.contains("/jitsi/meet/")) {
      // Configure the the default setting for the Jitsi call.
      initializeJitsiCall(mUrl);
    }
    mWebView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
      }

      public void onCloseWindow(WebView window) {
        mListener.onCloseWebViewFragment();
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
      @Override
      public void onPermissionRequest(PermissionRequest request) {
        request.grant(request.getResources());
      }

      @Override
      public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String url = consoleMessage.sourceId();
        if (url.contains("/jitsiweb/") && url.contains("?jwt=")){
          mListener.onCloseWebViewFragment();
          countJs += 1;
          if (countJs == 1) {
            // launch the call.
            openJitsiCall(url);
            return true;
          }
          return false;
        }
        return false;
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
      throw new RuntimeException(context + " must implement WebViewFragmentCallback");
    }
  }

  @Override
  public void onDetach() {
    mWebView.stopLoading();
    mWebView.destroy();
    mListener = null;
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver);
    super.onDetach();
  }

  private void openJitsiCall(String url) {
    // Build options object for joining the conference. The SDK will merge the default
    // one we set earlier and this one when joining.
    JitsiMeetConferenceOptions options
            = new JitsiMeetConferenceOptions.Builder()
            .setRoom(url)
            // Settings for audio and video
            //.setAudioMuted(true)
            //.setVideoMuted(true)
            .build();
    // Launch the new activity with the given options. The launch() method takes care
    // of creating the required Intent and passing the options.
    JitsiMeetActivity.launch(getContext(), options);
  }

  private void registerForBroadcastMessages() {
    IntentFilter intentFilter = new IntentFilter();

        /* This registers for every possible event sent from JitsiMeetSDK
           If only some of the events are needed, the for loop can be replaced
           with individual statements:
           ex:  intentFilter.addAction(BroadcastEvent.Type.AUDIO_MUTED_CHANGED.getAction());
                intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction());
                ... other events
         */
    for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
      intentFilter.addAction(type.getAction());
    }
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, intentFilter);
  }

  // Example for handling different JitsiMeetSDK events
  private void onBroadcastReceived(Intent intent) {
    if (intent != null) {
      BroadcastEvent event = new BroadcastEvent(intent);

      switch (event.getType()) {
        case CONFERENCE_JOINED:
          Timber.i("Conference Joined with url%s", event.getData().get("url"));
          break;
        case PARTICIPANT_JOINED:
          Timber.i("Participant joined%s", event.getData().get("name"));
          break;
      }
    }
  }

  // Example for sending actions to JitsiMeetSDK
  private void hangUp() {
    Intent hangupBroadcastIntent = BroadcastIntentHelper.buildHangUpIntent();
    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(hangupBroadcastIntent);
  }

  private void initializeJitsiCall(String url) {
    // Initialize default options for Jitsi Meet conferences.
    URL serverURL;
    URL urlJitsi;
    try {
      // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
      urlJitsi = new URL(url);

      urlJitsi = new URL(urlJitsi.getProtocol() +"://"+ urlJitsi.getHost() + "/jitsi");
      serverURL = urlJitsi;
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new RuntimeException("Invalid server URL!");
    }
    JitsiMeetConferenceOptions defaultOptions
            = new JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            // When using JaaS, set the obtained JWT here
            //.setToken("MyJWT")
            // Different features flags can be set
            // .setFeatureFlag("toolbox.enabled", false)
            // .setFeatureFlag("filmstrip.enabled", false)
            .setFeatureFlag("welcomepage.enabled", false)
            .build();
    JitsiMeet.setDefaultConferenceOptions(defaultOptions);
    registerForBroadcastMessages();
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
    void onCloseWebViewFragment();
  }
}
