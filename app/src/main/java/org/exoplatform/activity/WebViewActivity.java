package org.exoplatform.activity;

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

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.fragment.PlatformWebViewFragment;
import org.exoplatform.fragment.WebViewFragment;
import org.exoplatform.model.Server;
import org.exoplatform.service.push.PushTokenStorage;
import org.exoplatform.service.push.PushTokenSynchronizerLocator;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.ServerUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Activity that loads Platform into a web view
 *
 * @author chautn on 10/14/15
 * @author paristote
 */
public class  WebViewActivity extends AppCompatActivity implements PlatformWebViewFragment.PlatformNavigationCallback,
        WebViewFragment.WebViewFragmentCallback {

  public static final String      INTENT_KEY_URL = "URL";

  public static final String      LOG_TAG        = WebViewActivity.class.getName();

  private PlatformWebViewFragment mPlatformFragment;

  private WebViewFragment         mWebViewFragment;
  private ActionDialog dialog;
  private CheckConnectivity checkConnectivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // initialize Android with Push notifs
    if (Build.VERSION.SDK_INT >= 33) {
      if (ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(WebViewActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
      } else {
        Fabric.with(this, new Crashlytics());
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
          @Override
          public void onSuccess(InstanceIdResult instanceIdResult) {
            String newToken = instanceIdResult.getToken();
            PushTokenStorage.getInstance().setPushToken(newToken, getApplicationContext());
            PushTokenSynchronizerLocator.getInstance().setTokenAndSync(newToken);
          }
        });
      }
    }


    setContentView(R.layout.activity_webview);
    checkConnectivity = new CheckConnectivity(WebViewActivity.this);
    // Toolbar hidden by default, visible on certain pages cf
    // PlatformWebViewFragment->onPageStarted
    Toolbar mToolbar = (Toolbar) findViewById(R.id.WebClient_Toolbar);
    setSupportActionBar(mToolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    // Url of the intranet to load
    String url = getIntent().getStringExtra(INTENT_KEY_URL);
    try {
      Server server = new Server(new URL(url), new Date().getTime());
      verifyIntranet(server);
      mPlatformFragment = PlatformWebViewFragment.newInstance(server);
      getSupportFragmentManager().beginTransaction()
              .add(R.id.WebClient_WebViewFragment, mPlatformFragment, PlatformWebViewFragment.TAG)
              .commit();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Cannot load the Platform intranet at URL " + url, e);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Saving the last time an intranet was visited, for rule SIGN_IN_13
    SharedPreferences.Editor pref = getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0).edit();
    pref.putLong(App.Preferences.LAST_VISIT_TIME, System.nanoTime());
    pref.apply();
  }

  @Override
  public void onBackPressed() {
    // Leave the activity if there is no previous web page to go back to,
    // on either the Platform fragment or the WebView fragment
    boolean eventHandled = false;
    if (mPlatformFragment != null && mPlatformFragment.isVisible())
      eventHandled = mPlatformFragment.goBack();
    else if (mWebViewFragment != null && mWebViewFragment.isVisible())
      eventHandled = mWebViewFragment.goBack();
    if (!eventHandled)
      return;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      onBackToServerList();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onBackToServerList(){
    Intent intent = new Intent(WebViewActivity.this, ConnectToExoListActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    finish();
  }

  private void showHideToolbar(boolean show) {
    if (getSupportActionBar() != null) {
      if (show)
        getSupportActionBar().show();
      else
        getSupportActionBar().hide();
    }
  }

  @Override
  public void onPageStarted(boolean needsToolbar) {
    showHideToolbar(needsToolbar);
  }

  @Override
  public void onUserSignedOut() {
    mPlatformFragment.clearWebViewData();
  }

  @Override
  public void onUserJustBeforeSignedOut() {
    PushTokenSynchronizerLocator.getInstance().tryToDestroyToken();
    Intent intent = new Intent(WebViewActivity.this, ConnectToExoListActivity.class);
    startActivity(intent);
    finish();
  }

  @Override
  public void onExternalContentRequested(String url) {
    // create and open a new fragment
    mWebViewFragment = WebViewFragment.newInstance(url);
    getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter_bottom_up, 0, 0, R.anim.fragment_exit_top_down)
            .add(R.id.WebClient_WebViewFragment, mWebViewFragment, WebViewFragment.TAG)
            .addToBackStack(WebViewFragment.TAG)
            .hide(mPlatformFragment)
            .commit();
  }

  @Override
  public void onCloseWebViewFragment() {
    if (mWebViewFragment != null && mPlatformFragment != null) {
      // remove the fragment from the activity
      getSupportFragmentManager().popBackStack();
      getSupportFragmentManager().beginTransaction().remove(mWebViewFragment).show(mPlatformFragment).commit();
      // a new instance will be created if we load an external url again
      mWebViewFragment = null;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * Check that the intranet is based on a Platform 4.3+ server.<br/>
   * If not, displays an alert and go back to the previous activity.
   *
   * @param server the Intranet to check
   */
  private void verifyIntranet(final Server server) throws MalformedURLException {
    if (server == null)
      throw new MalformedURLException("Cannot verify a null server");

    ServerUtils.verifyUrl(server.getUrl().toString(), new ServerUtils.ServerVerificationCallback() {
      @Override
      public void onVerificationStarted() {
        //
      }

      @Override
      public void onServerValid(Server server) {
        //
      }

      @Override
      public void onServerNotSupported() {
        showDialog(R.string.ServerManager_Error_TitleVersion, R.string.ServerManager_Alert_PlatformVersionNotSupported);
        // Move then non-supported intranet at the bottom of the history
        server.setLastVisited(-1L);
        new ServerManagerImpl(App.Preferences.get(WebViewActivity.this)).addServer(server);
      }

      @Override
      public void onServerInvalid() {
        showDialog(R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl);
        // Move the incorrect intranet at the bottom of the history
        server.setLastVisited(-1L);
        new ServerManagerImpl(App.Preferences.get(WebViewActivity.this)).addServer(server);
      }

      @Override
      public void onConnectionError() {
        checkConnectivity.lostConnectionDialog.showDialog();
      }

      private void showDialog(int title, int message) {
        dialog = new ActionDialog(title,
                message, R.string.Word_Back, WebViewActivity.this);
        dialog.cancelAction.setVisibility(View.GONE);
        LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams)dialog.deleteAction.getLayoutParams();
        ll.setMarginStart(0);
        dialog.deleteAction.setLayoutParams(ll);
        dialog.showDialog();
        dialog.deleteAction.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            WebViewActivity.this.finish();
            dialog.dismiss();
          }
        });
      }
    });
  }
}
