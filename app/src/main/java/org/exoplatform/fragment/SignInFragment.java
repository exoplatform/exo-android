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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;

import org.exoplatform.R;
import org.exoplatform.activity.ShareExtensionActivity;
import org.exoplatform.model.Server;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 10, 2015
 */
public class SignInFragment extends Fragment {

  private static SignInFragment instance;

  public static final String    SIGN_IN_FRAGMENT        = "sign_in_fragment";

  private EditText              mUsernameField;

  private EditText              mPasswordField;

  private final TextWatcher           usernamePasswordWatcher = new TextWatcher() {
                                                          @Override
                                                          public void onTextChanged(CharSequence s,
                                                                                    int start,
                                                                                    int before,
                                                                                    int count) {
                                                            enableDisableMainButton();
                                                          }

                                                          @Override
                                                          public void beforeTextChanged(CharSequence s,
                                                                                        int start,
                                                                                        int count,
                                                                                        int after) {
                                                          }

                                                          @Override
                                                          public void afterTextChanged(Editable s) {
                                                          }
                                                        };

  public SignInFragment() {
  }

  public static SignInFragment getFragment() {
    if (instance == null) {
      instance = new SignInFragment();
    }
    return instance;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.share_extension_sign_in_fragment, container, false);
    mUsernameField = (EditText) layout.findViewById(R.id.share_signin_username);
    mUsernameField.addTextChangedListener(usernamePasswordWatcher);
    mPasswordField = (EditText) layout.findViewById(R.id.share_signin_password);
    mPasswordField.addTextChangedListener(usernamePasswordWatcher);
    return layout;
  }

  @Override
  public void onResume() {
    Server acc = getShareActivity().getActivityPost().ownerAccount;
    mUsernameField.setText(acc.getLastLogin());
    mPasswordField.setText(acc.getLastPassword());
    enableDisableMainButton();
    super.onResume();
  }

  @Override
  public void onDetach() {
    instance = null;
    super.onDetach();
  }

  /*
   * GETTERS & SETTERS
   */

  private void enableDisableMainButton() {
    if (isAdded()) {
      boolean usernameEmpty = "".equals(mUsernameField.getText().toString().trim());
      boolean passwordEmpty = "".equals(mPasswordField.getText().toString().trim());
      getShareActivity().setToolbarButtonEnabled(!(usernameEmpty && passwordEmpty));
    }
  }

  public String getUsername() {
    return mUsernameField.getText().toString().trim();
  }

  public String getPassword() {
    return mPasswordField.getText().toString().trim();
  }

  public ShareExtensionActivity getShareActivity() {
    if (getActivity() instanceof ShareExtensionActivity) {
      return (ShareExtensionActivity) getActivity();
    } else {
      throw new UnsupportedOperationException(String.format("This fragment is only valid in the activity %s",
                                                            ShareExtensionActivity.class.getName()));
    }
  }
}
