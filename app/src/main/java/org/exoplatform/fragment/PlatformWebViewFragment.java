package org.exoplatform.fragment;

/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Context.DOWNLOAD_SERVICE;
import static org.exoplatform.activity.WebViewActivity.INTENT_KEY_URL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.service.push.PushTokenSynchronizerLocator;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.JavaScriptInterface;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.cookies.CookiesConverter;
import org.exoplatform.tool.cookies.CookiesInterceptor;
import org.exoplatform.tool.cookies.CookiesInterceptorFactory;
import org.exoplatform.tool.cookies.WebViewCookieHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2;

  public static final String         TAG                 = PlatformWebViewFragment.class.getName();

  private PlatformNavigationCallback mListener;

  private WebView                    mWebView;
  private WebView                    newWebView;
  private ProgressBar                mProgressBar;

  private Server                     mServer;

  private String userName;

  private Button                     mDoneButton;

  private ValueCallback<Uri[]>       mUploadMessage;

  private final Pattern              LOGIN_REGISTER_PAGE = Pattern.compile("(/[a-z0-9]*/)([a-z0-9]*/)?(login|register)");

  private final String               LOGOUT_PATH         = "portal:action=Logout";

  private final CookiesInterceptor         mCookiesInterceptor = new CookiesInterceptorFactory().create();

  private final CookiesConverter           mCookiesConverter   = new CookiesConverter();

  private String downloadFileUrl;

  private String downloadUserAgent;

  private String downloadFileContentDisposition;

  private String downloadFileMimetype;

  private String default_user_agent;

  private String username;

  private boolean isWhileLoginProcess = false;

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
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_platform_web_view, container, false);
    // create web view
    mWebView = (WebView) layout.findViewById(R.id.PlatformWebViewFragment_WebView);
    mWebView.setWebViewClient(new PlatformWebViewClient());
    setWebViewSettings(mWebView);
    // set custom user agent by filtering the default one
    String default_userAgent = mWebView.getSettings().getUserAgentString();
    default_user_agent = default_userAgent;
    int startIndex = default_userAgent.indexOf("Mozilla/");
    int endIndex = default_userAgent.indexOf("wv");
    String toBeReplaced = default_userAgent.substring(startIndex, endIndex);
    String userAgent = "eXo/" + BuildConfig.VERSION_NAME + default_userAgent.replace(toBeReplaced, "") + " (Android)";
    mWebView.getSettings().setUserAgentString(userAgent);
    // set progress bar
    mProgressBar = (ProgressBar) layout.findViewById(R.id.PlatformWebViewFragment_ProgressBar);
    mWebView.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        newWebView = new WebView(getContext());
        newWebView.setLayoutParams(new ViewGroup.LayoutParams(mWebView.getLayoutParams()));
        setWebViewSettings(newWebView);
        int startIndex = default_user_agent.indexOf("AppleWebKit/");
        int endIndex = default_user_agent.indexOf("Mobile");
        String toBeReplaced = default_user_agent.substring(startIndex, endIndex);
        String ua = "eXo/" + BuildConfig.VERSION_NAME + " " + default_userAgent.replace(toBeReplaced, "Gecko/101.0 Firefox/101.0 ");
        newWebView.getSettings().setUserAgentString(ua);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        view.addView(newWebView);
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();
        if (resultMsg.obj != null){
          newWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
              String url = consoleMessage.sourceId();
              switchToJitsiAppWith(url);
              if (url.contains("Call started")) {
                newWebView.getWebChromeClient().onCloseWindow(newWebView);
              }
              return true;
            }
            @Override
            public void onCloseWindow(WebView window) {
              super.onCloseWindow(window);
              view.removeView(newWebView);
              newWebView.destroy();
              newWebView = null;
            }
            @Override
            public void onPermissionRequest(PermissionRequest request) {
              request.grant(request.getResources());
            }

          });

          newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
              String url = request.getUrl().toString();
              String contentType = getMimeType(url);
              if (contentType != null && !contentType.contains("text/html")) {
                  refreshLayoutForContent(contentType);
                  if (url.contains("/download/") || (url.contains("/portal/rest/jcr") && !contentType.startsWith("image/"))){
                    refreshLayoutForContent("text/html");
                    downloadFile(url,ua,contentType);
                    if(newWebView.getWebChromeClient() != null) {
                      newWebView.getWebChromeClient().onCloseWindow(view);
                    }
                  }
              }
              if (!url.contains(mServer.getShortUrl()) || url.startsWith(mServer.getUrl() + "/portal")) {
                if (!url.contains(mServer.getShortUrl())) {
                  view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                  if (newWebView != null && newWebView.getWebChromeClient() != null) {
                    newWebView.getWebChromeClient().onCloseWindow(view);
                  }
                  return true;
                } else if(!url.startsWith(mServer.getUrl() + "/portal/rest/jcr") || contentType == null || !contentType.startsWith("image/")){
                  mWebView.loadUrl(url);
                  if (newWebView != null && newWebView.getWebChromeClient() != null) {
                    newWebView.getWebChromeClient().onCloseWindow(view);
                  }
                }
              }
              if (url.contains("/jitsi/meet")) {
                // Set permission to Camera and micro to be enabled for the Jitsi call.
                String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.INTERNET,Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA };
                for (String permission : permissions) {
                  if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(), permissions, 1010);
                  }
                 }
              }
              return false;
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
              super.onReceivedError(view, request, error);
              Log.d("onReceivedError ========< ",error.toString());
              if (String.valueOf(error.getDescription()).contains("ERR_UNKNOWN_URL_SCHEME")) {
                newWebView.getWebChromeClient().onCloseWindow(view);
              }
            }
          });
          //setDownloadListenerFor(newWebView,getContext());
          // Set the created window to be closed automatically when we have Jitsi call or google sign in (related to JS window.close()).
        }
        return true;
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
      public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        mUploadMessage = filePathCallback;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        PlatformWebViewFragment.this.startActivityForResult(Intent.createChooser(i, "File Browser"),FILECHOOSER_RESULTCODE);
        return true;
      }

      @Override
      public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
        result.confirm();
        return true;
      }
    });
    setDownloadListenerFor(mWebView,getContext());
    String url = Objects.requireNonNull(getActivity()).getIntent().getStringExtra(INTENT_KEY_URL);
    if(url != null && !url.equals("")) {
      mWebView.loadUrl(url);
    } else {
      mWebView.loadUrl(mServer.getUrl().toString());
    }
    // done button for content without navigation, e.g. image
    mDoneButton = (Button) layout.findViewById(R.id.PlatformWebViewFragment_Done_Button);
    if (mDoneButton != null) {
      mDoneButton.setVisibility(View.GONE);
      mDoneButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mDoneButton.setVisibility(View.GONE);
          if (newWebView != null && mWebView != null){
             refreshLayoutForContent("text/html");
             newWebView.getWebChromeClient().onCloseWindow(newWebView);
          }else{
            if (mWebView != null && mWebView.canGoBack())
              mWebView.goBack();
          }
        }
      });
    }
    return layout;
  }

  public void switchToJitsiAppWith(String url){
    if (url.contains("/jitsiweb/") && url.contains("?jwt=") && !url.contains("intent://")){
      String fUrl = url.replaceFirst("^(http[s]?://)","");
      Uri uri = Uri.parse("org.jitsi.meet://" + fUrl);
      Intent jitsiURL = new Intent(Intent.ACTION_VIEW, uri);
      jitsiURL.setPackage("org.jitsi.meet");
      try {
        startActivity(jitsiURL);
        newWebView.getWebChromeClient().onCloseWindow(newWebView);
      } catch (ActivityNotFoundException e) {
        Log.e("Jitsi App not found:", e.toString());
      }
    }
  }

  public static String getMimeType(String url) {
    String type = null;
    try {
      // HTML special characters are encoded twice, we need to decode them twice to get the correct name
      url = URLDecoder.decode(url, "UTF-8");
      url = URLDecoder.decode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        //Do nothing
    }
    String extension = url.substring(url.lastIndexOf(".") + 1);
    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

    return type;
  }

  public void setDownloadListenerFor(WebView webView, Context context) {
    webView.setDownloadListener(new DownloadListener() {
      public void onDownloadStart(String url, String userAgent,String contentDisposition, String mimetype,long contentLength) {
        if (!hasPermission(context)) {
          // save info of file to download before waiting for permission, so it
          // can be downloaded from the callback method (onRequestPermissionsResult)
          downloadFileUrl = url;
          downloadUserAgent = userAgent;
          downloadFileContentDisposition = contentDisposition;
          downloadFileMimetype = mimetype;
          requestPermissions(new String[]{ WRITE_EXTERNAL_STORAGE },
                  WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
        } else {
          // Prevent blob urls form being handled
          if (!url.startsWith("blob")) {
            downloadFile(url, userAgent, contentDisposition);
          }else{
            Toast.makeText(getContext(), "DOWNLOAD STARTED....", Toast.LENGTH_SHORT).show();
            downloadFileMimetype = mimetype;
            webView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url,downloadFileMimetype));
          }
        }
      }
    });
  }

  public static boolean hasPermission(Context context) {
    if (context != null) {
      return ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    return true;
  }

  private void downloadFile(String url, String userAgent, String contentDisposition) {
    String filename = URLUtil.guessFileName(url, contentDisposition, null);
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
    String cookie = CookieManager.getInstance().getCookie(url);
    request.addRequestHeader("Cookie", cookie);
    request.addRequestHeader("User-Agent", userAgent);
    request.allowScanningByMediaScanner();
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    DownloadManager downloadmanager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
    downloadmanager.enqueue(request);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST: {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Prevent blob urls form being handled
          if (!downloadFileUrl.startsWith("blob")) {
            downloadFile(downloadFileUrl, downloadUserAgent, downloadFileContentDisposition);
          }else{
            mWebView.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(downloadFileUrl,downloadFileMimetype));
          }
        }
        return;
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == FILECHOOSER_RESULTCODE) {
      if (mUploadMessage == null) {
        return;
      }
      Uri result = data == null || resultCode != Activity.RESULT_OK ? null : data.getData();
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
      throw new RuntimeException(context + " must implement PlatformNavigationCallback");
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
    } else if(newWebView != null) {
      newWebView.getWebChromeClient().onCloseWindow(newWebView);
      mDoneButton.setVisibility(View.GONE);
      return true;
    }
    return false;
  }

  private void refreshLayoutForContent(String contentType) {
    if(newWebView != null) {
      if (contentType != null && !contentType.contains("text/html")) {
        newWebView.getSettings().setUseWideViewPort(true);
        newWebView.getSettings().setLoadWithOverviewMode(true);
        newWebView.getSettings().setBuiltInZoomControls(true);
        mDoneButton.setVisibility(View.VISIBLE);
      } else {
        newWebView.getSettings().setUseWideViewPort(false);
        newWebView.getSettings().setLoadWithOverviewMode(false);
        newWebView.getSettings().setBuiltInZoomControls(true);
        mDoneButton.setVisibility(View.GONE);
      }
    }
  }

  public void setWebViewSettings(WebView webView) {
    webView.clearCache(true);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    webView.getSettings().setDisplayZoomControls(false);
    webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
    webView.getSettings().setSupportMultipleWindows(true);
    webView.getSettings().setAllowFileAccess(true);
    webView.getSettings().setAllowContentAccess(true);
    webView.getSettings().setAllowFileAccessFromFileURLs(true);
    webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
    webView.getSettings().setDefaultTextEncodingName("utf-8");
    webView.addJavascriptInterface(new JavaScriptInterface(getContext()), "Android");
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

  private class PlatformWebViewClient extends WebViewClient {

    private final List<String> resourceIds = new ArrayList<>();

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      if(request.getUrl().getPath() != null && request.getUrl().getPath().equals("/portal/download")) {
        String resourceId = request.getUrl().getQueryParameter("resourceId");
        if(!resourceIds.contains(resourceId)) {
          resourceIds.add(resourceId);
          try {
            URL url = new URL(request.getUrl().toString() + "&remove=false");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            return new WebResourceResponse(connection.getContentType(), connection.getHeaderField("encoding"), connection.getResponseCode(), String.valueOf(connection.getResponseCode()), flattenHeaders(connection.getHeaderFields()), connection.getInputStream());
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else {
          resourceIds.remove(resourceId);
        }
      }
      return super.shouldInterceptRequest(view, request);
    }

    @NonNull
    private Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
      Map<String, String> flattenHeaders = new HashMap<>();
      for(String headerName : headers.keySet()) {
        String headerConcatenatedValue = "";
        List<String> headerValues = headers.get(headerName);
        if(headerValues != null && !headerValues.isEmpty()) {
          if (headerValues.size() > 1) {
            for (String headerValue : headerValues) {
              headerConcatenatedValue += headerValue + ";";
            }
          } else {
            headerConcatenatedValue = headerValues.get(0);
          }
        }
        flattenHeaders.put(headerName, headerConcatenatedValue);
      }
      return flattenHeaders;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
      String url = request.getUrl().toString();
      Log.d("shouldOverride", url);
      // For external and short links, broadcast logout event if done
      if (url.contains(LOGOUT_PATH)) {
        mListener.onUserJustBeforeSignedOut();
        mListener.onUserSignedOut();
      }

      String FACEBOOK_LOGIN_PATH = "www.facebook.com/dialog/oauth";
      String GOOGLE_LOGIN_PATH = "accounts.google.com/o/oauth2";
      String LINKEDIN_LOGIN_PATH = "www.linkedin.com/uas/oauth2";
      if (url.contains(GOOGLE_LOGIN_PATH) || url.contains(FACEBOOK_LOGIN_PATH) || url.contains(LINKEDIN_LOGIN_PATH)) {
         isWhileLoginProcess = true ;
      }

      if (url.contains(mServer.getShortUrl()) && url.contains("/portal/login?username=")) {
        isWhileLoginProcess = false ;
      }

      if (((url.contains(mServer.getShortUrl()) && !super.shouldOverrideUrlLoading(view, request))) || isWhileLoginProcess) {
        // url is on the server's domain, keep loading normally
        if (url.contains("/jitsi/meet")) {
          mListener.onExternalContentRequested(url);
          return true;
        }
        return false;
      } else{
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
      if(url.startsWith("http")) {
        getContentTypeAsync(url);
      }
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
      Log.d("onPageFinished",url);
      mCookiesInterceptor.intercept(mCookiesConverter.toMap(CookieManager.getInstance().getCookie(url)), url,PlatformWebViewFragment.this.getContext());
      SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getContext());
      SharedPreferences.Editor editor = shared.edit();
      mWebView.evaluateJavascript("eXo.env.portal.userName;", userName -> {
        if(userName != null && !userName.isEmpty()) {
          userName = userName.replace("\"", "");
          Log.d(TAG, "Username is " + userName);
          editor.putString("connectedUsername", userName);
          PushTokenSynchronizerLocator.getInstance().setConnectedUserAndSync(userName, calculateBaseUrl(url));
        }
      } );
      if (url.contains("/portal/dw")) {
        editor.putString("urlLogin", url);
        editor.apply();
        getAvatarServerLogo();
      }
      if (BuildConfig.DEBUG)
        Log.d(TAG, "COOKIES: " + CookieManager.getInstance().getCookie(url));
    }
  }

  @Nullable
  private String calculateBaseUrl(String urlStr) {
    try {
      URL url = new URL(urlStr);
      if (!TextUtils.isEmpty(url.getPath())) {
        urlStr = urlStr.replaceFirst(url.getPath(), "");
      }
      if (!TextUtils.isEmpty(url.getQuery())) {
        urlStr = urlStr.replaceFirst(url.getQuery(), "");
      }
      return urlStr;
    } catch (Exception e) {
      return null;
    }
  }
  
  // Clear Webview cache and data when logging out.
  public void clearWebViewData() {
    mWebView.clearCache(true);
    mWebView.clearFormData();
    mWebView.clearHistory();
    mWebView.clearSslPreferences();
    PlatformWebViewFragment.this.getContext().deleteDatabase("webviewCache.db");
    PlatformWebViewFragment.this.getContext().deleteDatabase("webview.db");
    CookieManager.getInstance().removeAllCookies(null);
    CookieManager.getInstance().flush();
    WebStorage.getInstance().deleteAllData();
  }

  private void getAvatarServerLogo() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlatformWebViewFragment.this.getContext());
    String photo = preferences.getString(mServer.getShortUrl(), "photo");
    if (photo == "photo" || photo == null){
      OkHttpClient client = ExoHttpClient.getInstance().newBuilder().cookieJar(new WebViewCookieHandler()).build();
      String plfInfo = mServer.getUrl().getProtocol() + "://" + mServer.getShortUrl() + "/portal/rest/v1/platform/branding/logo";
      System.out.println("ImageURL ======>" + plfInfo);
      Request req = new Request.Builder().url(plfInfo).get().build();
      client.newCall(req).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
          // ignore network failure
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
          if (response.isSuccessful()) {
            final Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
            new Handler(Looper.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                if (bitmap != null) {
                  SharedPreferences.Editor editor = preferences.edit();
                  editor.putString(mServer.getShortUrl(), encodeTobase64(bitmap));
                  editor.commit();
                }
              }
            });
          }
          response.body().close();
        }
      });
    }
  }

  // method for bitmap to base64
  public static String encodeTobase64(Bitmap image) {
    Bitmap immage = image;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    immage.compress(Bitmap.CompressFormat.PNG, 100, baos);
    byte[] b = baos.toByteArray();
    String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);
    Log.d("Image Log:", imageEncoded);
    return imageEncoded;
  }

  public interface PlatformNavigationCallback {
    void onPageStarted(boolean needsToolbar);

    void onUserSignedOut();

    void onUserJustBeforeSignedOut();

    void onExternalContentRequested(String url);
  }

}

