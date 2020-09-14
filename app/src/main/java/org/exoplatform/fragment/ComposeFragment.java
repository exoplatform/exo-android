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

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.tool.ServerManager;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.activity.ShareExtensionActivity;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.SocialSpace;
import org.exoplatform.service.share.LoginTask;
import org.exoplatform.service.share.PrepareAttachmentsTask;
import org.exoplatform.tool.ServerUtils;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 3, 2015
 */
public class ComposeFragment extends Fragment implements LoginTask.Listener, PrepareAttachmentsTask.Listener {

  private static ComposeFragment instance;

  public static final String     COMPOSE_FRAGMENT     = "compose_fragment";

  private EditText               mMessageField;

  private LinearLayout           mSpaceSelectorWrapper;

  private TextView               mAccountSelector, mSpaceSelector, mMoreAttachmentsLabel;

  private ImageView              mThumbnailImageView;

  private ScrollView             mScrollView;

  private boolean                isSigningIn          = false;

  private final TextWatcher      mMessageFieldWatcher = new TextWatcher() {
                                                        @Override
                                                        public void beforeTextChanged(CharSequence s,
                                                                                      int start,
                                                                                      int count,
                                                                                      int after) {
                                                        }

                                                        @Override
                                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                                          // update the message
                                                          // of the activity
                                                          // post as the user
                                                          // types it
                                                          getShareActivity().getActivityPost().title = (s == null) ? ""
                                                                                                                  : s.toString()
                                                                                                                     .trim();
                                                        }

                                                        @Override
                                                        public void afterTextChanged(Editable s) {
                                                        }
                                                      };

  public ComposeFragment() {
  }

  public static ComposeFragment getFragment() {
    if (instance == null) {
      instance = new ComposeFragment();
    }
    return instance;
  }

  public void setTouchListener() {
    // Open the soft keyboard and give focus to the edit text field when the
    // scroll view is tapped
    mScrollView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mMessageField.requestFocus();
          InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
          mgr.showSoftInput(mMessageField, InputMethodManager.SHOW_IMPLICIT);
        }
        return v.performClick();
      }
    });
  }

  private void hideSoftKeyboard() {
    InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    mgr.hideSoftInputFromWindow(mMessageField.getWindowToken(), 0);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.share_extension_compose_fragment, container, false);
    mMessageField = (EditText) layout.findViewById(R.id.share_post_message);
    mMessageField.addTextChangedListener(mMessageFieldWatcher);
    mAccountSelector = (TextView) layout.findViewById(R.id.share_account);
    mAccountSelector.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getShareActivity().onSelectAccount();
      }
    });
    mSpaceSelectorWrapper = (LinearLayout) layout.findViewById(R.id.share_space_wrapper);
    mSpaceSelector = (TextView) layout.findViewById(R.id.share_space);
    mSpaceSelector.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getShareActivity().onSelectSpace();
      }
    });
    mMoreAttachmentsLabel = (TextView) layout.findViewById(R.id.share_attachment_more);
    mThumbnailImageView = (ImageView) layout.findViewById(R.id.share_attachment_thumbnail);
    mScrollView = (ScrollView) layout.findViewById(R.id.share_scroll_wrapper);
    return layout;
  }

  @Override
  public void onResume() {
    setPostMessage();
    setAccountLabel();
    setSpaceLabel();
    setTouchListener();
    SocialActivity activity = getShareActivity().getActivityPost();
    String type;
    if (ServerUtils.isOldVersion()) {
      type = SocialActivity.OLD_DOC_ACTIVITY_TYPE;
    } else {
      type = SocialActivity.DOC_ACTIVITY_TYPE;
    }
    if (!type.equals(activity.type)) {
      // Text or link activity, no thumbnail
      mThumbnailImageView.setVisibility(View.GONE);
      mMoreAttachmentsLabel.setVisibility(View.GONE);
    } else {
      // Doc activity
      setThumbnailImage(getShareActivity().getThumbnail());
      if (activity.postAttachedFiles != null)
        setNumberOfAttachments(activity.postAttachedFiles.size());
    }
    super.onResume();
  }

  @Override
  public void onDetach() {
    hideSoftKeyboard();
    instance = null;
    super.onDetach();
  }

  /*
   * GETTERS & SETTERS
   */

  private void setPostMessage() {
    if (mMessageField != null)
      mMessageField.setText(getShareActivity().getActivityPost().title);
  }

  private void setAccountLabel() {
    if (mAccountSelector != null) {
      if (getShareActivity().isLoggedIn()) {
        // Set the selected intranet url
        Server selectedAccount = getShareActivity().getActivityPost().ownerAccount;
        if (selectedAccount != null)
          mAccountSelector.setText(String.format("%s (%s)", selectedAccount.getShortUrl(), selectedAccount.getLastLogin()));
      } else if (isSigningIn) {
        // Display a signing in label
        mAccountSelector.setText(R.string.ShareActivity_Compose_Title_SigningIn);
      } else {
        // Display a select intranet label
        mAccountSelector.setText(R.string.ShareActivity_Compose_Title_SignInToPost);
      }

      ServerManager serverManager = new ServerManagerImpl(App.Preferences.get(getContext()));
      boolean manyAccounts = serverManager.getServerList().size() > 1;
      if (manyAccounts || !getShareActivity().isLoggedIn()) {
        // Show a > icon if 2 or more accounts exist or the user is not logged
        // in
        mAccountSelector.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_chevron_right_grey, 0);
      } else {
        mAccountSelector.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
      }
    }
  }

  private void setSpaceLabel() {
    if (mSpaceSelector != null && mSpaceSelectorWrapper != null) {
      if (getShareActivity().isLoggedIn()) {
        mSpaceSelectorWrapper.setVisibility(View.VISIBLE);
        SocialSpace space = getShareActivity().getActivityPost().destinationSpace;
        if (space != null)
          mSpaceSelector.setText(space.displayName);
        else
          mSpaceSelector.setText(getString(R.string.ShareActivity_Compose_Title_AllConnections));
      } else {
        // hide the space selector area when user is logged out
        mSpaceSelectorWrapper.setVisibility(View.GONE);
      }
    }
  }

  private void setThumbnailImage(Bitmap bm) {
    if (bm != null && mThumbnailImageView != null) {
      mThumbnailImageView.setImageBitmap(bm);
    }
  }

  private void setNumberOfAttachments(int nb) {
    if (nb > 1 && mMoreAttachmentsLabel != null) {
      mMoreAttachmentsLabel.setText(String.format("+ %d", (nb - 1)));
      mMoreAttachmentsLabel.setVisibility(View.VISIBLE);
    }
  }

  public ShareExtensionActivity getShareActivity() {
    if (getActivity() instanceof ShareExtensionActivity) {
      return (ShareExtensionActivity) getActivity();
    } else {
      throw new UnsupportedOperationException("This fragment is only valid in the activity org.exoplatform.activity.ShareExtensionActivity");
    }
  }

  @Override
  public void onPrepareAttachmentsFinished(PrepareAttachmentsTask.AttachmentsResult result) {
    setThumbnailImage(result.thumbnail);
    setNumberOfAttachments(result.attachments.size());
  }

  @Override
  public void onLoginStarted(LoginTask loginTask) {
    isSigningIn = true;
    if (getView() != null) {
      mAccountSelector.setText(R.string.ShareActivity_Compose_Title_SigningIn);
      getView().findViewById(R.id.share_space_wrapper).setVisibility(View.GONE);
    }
  }

  @Override
  public void onLoginSuccess(PlatformInfo result) {
    isSigningIn = false;
    if (getView() != null) {
      mAccountSelector.setText(getShareActivity().getActivityPost().ownerAccount.getShortUrl());
      getView().findViewById(R.id.share_space_wrapper).setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onLoginFailed() {
    isSigningIn = false;
    if (getView() != null) {
      mAccountSelector.setText(R.string.ShareActivity_Compose_Title_SignInToPost);
      getView().findViewById(R.id.share_space_wrapper).setVisibility(View.GONE);
    }
  }

  @Override
  public void onPlatformVersionNotSupported() {
    onLoginFailed();
  }
}
