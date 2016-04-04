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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.tool.ServerManager;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.fragment.AccountsFragment;
import org.exoplatform.fragment.ComposeFragment;
import org.exoplatform.fragment.SelectSpaceFragment;
import org.exoplatform.fragment.SignInFragment;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.SocialSpace;
import org.exoplatform.service.share.LoginTask;
import org.exoplatform.service.share.PrepareAttachmentsTask;
import org.exoplatform.service.share.ShareService;
import org.exoplatform.tool.DocumentUtils;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.PlatformUtils;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since 3 Jun 2015
 */
public class ShareExtensionActivity extends AppCompatActivity implements LoginTask.Listener, PrepareAttachmentsTask.Listener {

  /**
   * Direction of the animation to switch from one fragment to another
   */
  public enum Anim {
    NO_ANIM, FROM_LEFT, FROM_RIGHT
  }

  /**
   * What type of button should we display in the toolbar
   */
  public enum ToolbarButtonType {
    HIDDEN, SHARE, SIGNIN
  }

  public static final String LOG_TAG = ShareExtensionActivity.class.getName();

  private boolean            mUserLoggedIn;

  // toolbar button for Share and Sign In actions
  private Button             mToolbarButton;

  // this activity
  private SocialActivity     mActivityPost;

  // the list of URIs present in the intent
  private List<Uri>          mAttachmentUris;

  // the thumbnail bitmap
  private Bitmap             mThumbnail;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    setContentView(R.layout.activity_share_extension);
    Toolbar toolbar = (Toolbar) findViewById(R.id.Share_Toolbar);
    setSupportActionBar(toolbar);
    mToolbarButton = (Button) findViewById(R.id.Share_Main_Button);
    mUserLoggedIn = false;
    mActivityPost = new SocialActivity();

    if (isIntentCorrect()) {
      Intent intent = getIntent();
      String type = intent.getType();
      if ("text/plain".equals(type)) {
        // User is sharing some text
        mActivityPost.title = intent.getStringExtra(Intent.EXTRA_TEXT);
        mActivityPost.type = SocialActivity.TYPE_DEFAULT;
      } else {
        // User is sharing some document(s)
        mActivityPost.title = "";
        mActivityPost.type = SocialActivity.TYPE_DOC;
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
          mAttachmentUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } else {
          Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
          if (contentUri != null) {
            mAttachmentUris = new ArrayList<>();
            mAttachmentUris.add(contentUri);
          }
        }
        prepareAttachmentsAsync();
      }

      if (!prepareAccounts()) {
        Toast.makeText(this, R.string.ShareActivity_Error_NoAccountConfigured, Toast.LENGTH_LONG).show();
        finish();
        return;
        // We could open NewServerActivity to create a new intranet, and return
        // here after
      }

      Server srv = mActivityPost.ownerAccount;
      if (srv.getLastLogin() != null && srv.getLastPassword() != null) {
        login(srv);
      }

      // Create and display the composer
      ComposeFragment composer = ComposeFragment.getFragment();
      openFragment(composer, ComposeFragment.COMPOSE_FRAGMENT, Anim.NO_ANIM);
    } else {
      // We're not supposed to reach this activity by anything else than an
      // ACTION_SEND intent
      finish();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    finish();
    // Don't keep the share extension in standby when we switch to another app,
    // or hit the home button
  }

  private boolean isIntentCorrect() {
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    return ((Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) && type != null);
  }

  private boolean prepareAccounts() {
    // Load the list of accounts
    ServerManager serverManager = new ServerManagerImpl(getSharedPreferences(App.Preferences.FILE_NAME, 0));
    // Init the activity with the selected account
    mActivityPost.ownerAccount = serverManager.getLastVisitedServer();
    if (mActivityPost.ownerAccount == null) {
      List<Server> serverList = serverManager.getServerList();
      if (serverList == null || serverList.size() == 0) {
        return false;
      } else {
        // use the first server in the list
        mActivityPost.ownerAccount = serverList.get(0);
      }
    }
    return true;
  }

  /**
   * Util method to switch from one fragment to another, with an animation
   *
   * @param toOpen The Fragment to open in this transaction
   * @param key the string key of the fragment
   * @param anim the type of animation
   */
  public void openFragment(Fragment toOpen, String key, Anim anim) {
    FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
    // configure the transition between the two fragments
    switch (anim) {
    case FROM_LEFT:
      tr.setCustomAnimations(R.anim.fragment_enter_ltr, R.anim.fragment_exit_ltr);
      break;
    case FROM_RIGHT:
      tr.setCustomAnimations(R.anim.fragment_enter_rtl, R.anim.fragment_exit_rtl);
      break;
    default:
    case NO_ANIM:
      break;
    }
    // set the toolbar button + title
    if (ComposeFragment.COMPOSE_FRAGMENT.equals(key)) {
      setToolbarButtonType(ShareExtensionActivity.ToolbarButtonType.SHARE);
      setTitle(R.string.Word_eXo);
    } else if (SignInFragment.SIGN_IN_FRAGMENT.equals(key)) {
      setToolbarButtonType(ToolbarButtonType.SIGNIN);
      setTitle(R.string.ShareActivity_SignIn_Title_EnterCredentials);
    } else if (AccountsFragment.ACCOUNTS_FRAGMENT.equals(key)) {
      setToolbarButtonType(ToolbarButtonType.HIDDEN);
      setTitle(R.string.ShareActivity_Intranets_Title_Intranets);
    } else if (SelectSpaceFragment.SPACES_FRAGMENT.equals(key)) {
      setToolbarButtonType(ToolbarButtonType.HIDDEN);
      setTitle(R.string.ShareActivity_Compose_Title_ShareWith);
    }
    // go!
    tr.replace(R.id.share_extension_fragment, toOpen, key);
    tr.commit();
  }

  @Override
  public void onBackPressed() {
    // Intercept the back button taps to display previous state with animation
    // If we're on the composer, call super to finish the activity
    ComposeFragment composer = ComposeFragment.getFragment();
    if (composer.isAdded()) {
      if (mActivityPost != null)
        DocumentUtils.deleteLocalFiles(mActivityPost.postAttachedFiles);
      PlatformUtils.reset();
      super.onBackPressed();
    } else if (AccountsFragment.getFragment().isAdded()) {
      // close the accounts fragment and reopen the composer fragment
      openFragment(composer, ComposeFragment.COMPOSE_FRAGMENT, Anim.FROM_LEFT);
    } else if (SignInFragment.getFragment().isAdded()) {
      // close the sign in fragment and reopen the accounts fragment
      openFragment(AccountsFragment.getFragment(), AccountsFragment.ACCOUNTS_FRAGMENT, Anim.FROM_LEFT);
    } else if (SelectSpaceFragment.getFragment().isAdded()) {
      // close the select space fragment and reopen the composer fragment
      openFragment(composer, ComposeFragment.COMPOSE_FRAGMENT, Anim.FROM_LEFT);
    }
  }

  /*
   * GETTERS & SETTERS
   */

  public boolean isLoggedIn() {
    return mUserLoggedIn;
  }

  public SocialActivity getActivityPost() {
    return mActivityPost;
  }

  public Bitmap getThumbnail() {
    return mThumbnail;
  }

  /**
   * @param type how to display the toolbar button
   */
  public void setToolbarButtonType(ToolbarButtonType type) {
    switch (type) {
    case HIDDEN:
      mToolbarButton.setVisibility(View.INVISIBLE);
      break;
    case SIGNIN:
      mToolbarButton.setText(R.string.ShareActivity_Compose_Title_SignIn);
      mToolbarButton.setVisibility(View.VISIBLE);
      break;
    default: // SHARE
      mToolbarButton.setText(R.string.ShareActivity_Main_Title_Post);
      mToolbarButton.setVisibility(View.VISIBLE);
      break;
    }
    mToolbarButton.setTag(type);
  }

  /**
   * Switch the main button to the given enabled state. If the main button is
   * the post button, we also check that the account is mUserLoggedIn.
   *
   * @param enabled whether to enable or disable the toolbar button
   */
  public void setToolbarButtonEnabled(boolean enabled) {
    mToolbarButton.setEnabled(enabled);
  }

  /*
   * CLICK LISTENERS
   */

  public void onMainButtonClicked(View view) {
    ToolbarButtonType type = (ToolbarButtonType) view.getTag();
    switch (type) {
    case SHARE: // Tap on the SHARE button

      if (mActivityPost.ownerAccount == null || !mUserLoggedIn) {
        // no account selected or account offline
        Toast.makeText(this, R.string.ShareActivity_Error_CannotPostBecauseOffline, Toast.LENGTH_LONG).show();
        return;
      }

      if (!mActivityPost.hasAttachment() && mActivityPost.title.trim().isEmpty()) {
        // no document or message to share
        Toast.makeText(this, R.string.ShareActivity_Error_CannotPostBecauseEmpty, Toast.LENGTH_LONG).show();
        return;
      }

      if (BuildConfig.DEBUG)
        Log.d(LOG_TAG, "Starting share service...");
      Intent share = new Intent(getBaseContext(), ShareService.class);
      share.putExtra(ShareService.POST_INFO, mActivityPost);
      startService(share);
      Toast.makeText(getBaseContext(), R.string.ShareActivity_Message_OperationStarted, Toast.LENGTH_LONG).show();

      // Post is in progress, our work is done here
      finish();
      break;

    case SIGNIN: // Tap on the SIGN IN button
      String username = SignInFragment.getFragment().getUsername();
      mActivityPost.ownerAccount.setLastLogin(username);
      String password = SignInFragment.getFragment().getPassword();
      mActivityPost.ownerAccount.setLastPassword(password);
      openFragment(ComposeFragment.getFragment(), ComposeFragment.COMPOSE_FRAGMENT, Anim.FROM_LEFT);
      login(mActivityPost.ownerAccount);
      break;

    default: // HIDDEN, do nothing
      break;
    }
  }

  public void onSelectAccount() {
    // Called when the select account field is tapped
    openFragment(AccountsFragment.getFragment(), AccountsFragment.ACCOUNTS_FRAGMENT, Anim.FROM_RIGHT);
  }

  public void onAccountSelected(Server account) {
    // Called when an account with password was selected.
    if (!account.equals(mActivityPost.ownerAccount)) {
      mUserLoggedIn = false;
      mActivityPost.ownerAccount = account;
      login(account);
    }
  }

  public void onSelectSpace() {
    // Called when the select space field is tapped
    if (mUserLoggedIn) {
      openFragment(SelectSpaceFragment.getFragment(), SelectSpaceFragment.SPACES_FRAGMENT, Anim.FROM_RIGHT);
    } else {
      Toast.makeText(this, R.string.ShareActivity_Error_CannotSelectSpaceBecauseOffline, Toast.LENGTH_LONG).show();
    }
  }

  public void onSpaceSelected(SocialSpace space) {
    // When we come back from the space selector activity
    mActivityPost.destinationSpace = space;
    openFragment(ComposeFragment.getFragment(), ComposeFragment.COMPOSE_FRAGMENT, Anim.FROM_LEFT);
  }

  /*
   * LOGIN
   */

  public void login(Server server) {
    if (server == null)
      throw new IllegalArgumentException("Server must not be null");

    LoginTask task = new LoginTask();
    task.addListener(this);
    task.addListener(ComposeFragment.getFragment());
    task.execute(server);
  }

  @Override
  public void onLoginStarted(LoginTask loginTask) {
    mUserLoggedIn = false;
    setToolbarButtonEnabled(false);
  }

  private void handleLoginResult(boolean result) {
    mUserLoggedIn = result;
    Server server = mActivityPost.ownerAccount;
    if (mUserLoggedIn) {
      server.setLastVisited(System.currentTimeMillis());
    } else {
      server.setLastPassword("");
    }
    new ServerManagerImpl(getSharedPreferences(App.Preferences.FILE_NAME, 0)).addServer(server);
    setToolbarButtonEnabled(mUserLoggedIn);
  }

  @Override
  public void onLoginSuccess(PlatformInfo result) {
    handleLoginResult(true);
    // -------
    if (BuildConfig.DEBUG) {
      Server server = mActivityPost.ownerAccount;
      HttpUrl url = HttpUrl.parse(server.getUrl().toString());
      for (Cookie c : ExoHttpClient.getInstance().cookieJar().loadForRequest(url)) {
        Log.d(LOG_TAG, "COOKIE : " + c.toString());
      }
    }
  }

  @Override
  public void onLoginFailed() {
    Toast.makeText(getApplicationContext(), R.string.ShareActivity_Error_SignInFailed, Toast.LENGTH_LONG).show();
    handleLoginResult(false);
  }

  /*
   * PREPARE ATTACHMENTS
   */

  private void prepareAttachmentsAsync() {
    if (!DocumentUtils.didRequestPermission(this, App.Permissions.REQUEST_PICK_IMAGE_FROM_GALLERY)) {
      PrepareAttachmentsTask task = new PrepareAttachmentsTask(this);
      task.addListener(this);
      task.addListener(ComposeFragment.getFragment());
      task.execute(mAttachmentUris);
    }
  }

  @Override
  public void onPrepareAttachmentsFinished(PrepareAttachmentsTask.AttachmentsResult result) {
    if (result.error != null) {
      Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
    }
    mThumbnail = result.thumbnail;
    mActivityPost.postAttachedFiles = result.attachments;
  }

  @SuppressLint("Override")
  @Override
  public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] results) {
    if (reqCode == App.Permissions.REQUEST_PICK_IMAGE_FROM_GALLERY) {
      if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
        // permission granted
        prepareAttachmentsAsync();
      } else {
        // permission denied
        AlertDialog.Builder db = new AlertDialog.Builder(this);
        OnClickListener dialogInterface = new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEUTRAL) {
              Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",
                                                                                                     getPackageName(),
                                                                                                     null));
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }
            finish();
          }
        };
        db.setMessage(R.string.ShareActivity_Error_StoragePermissionDenied)
          .setNegativeButton(R.string.ShareActivity_PermissionDialog_Title_Leave, dialogInterface)
          .setNeutralButton(R.string.ShareActivity_PermissionDialog_Title_AppSettings, dialogInterface);
        AlertDialog dialog = db.create();
        dialog.show();
      }
    }
  }
}
