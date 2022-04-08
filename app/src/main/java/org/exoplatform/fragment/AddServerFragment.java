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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.activity.WebViewActivity;
import org.exoplatform.tool.ServerUtils;

/**
 * Created by chautran on 22/12/2015. Fragment that displays a text field to add
 * a new server.
 */
public class AddServerFragment extends Fragment {

  private EditText mIntranetUrlField;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View fragmentLayout = inflater.inflate(R.layout.add_server_fragment, container, false);
    mIntranetUrlField = (EditText) fragmentLayout.findViewById(R.id.AddServer_Field_Url);
    mIntranetUrlField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL) {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return true; // do nothing but consume the key down event
          }
          if (event.getAction() == KeyEvent.ACTION_UP) {
            submitUrl();
            return true;
          }
        }
        if (actionId == EditorInfo.IME_ACTION_GO) {
          submitUrl();
          return true;
        }
        return false; // otherwise let the system handle the event
      }
    });
    return fragmentLayout;
  }

  // when users taps "Go" or "Enter" on the keyboard
  private void submitUrl() {
    String url = mIntranetUrlField.getText().toString().trim();
    final ProgressDialog progressDialog = ServerUtils.savingServerDialog(getActivity());
    ServerUtils.verifyUrl(url, new ServerUtils.ServerVerificationCallback() {
      @Override
      public void onVerificationStarted() {
        progressDialog.show();
      }

      @Override
      public void onServerValid(Server server) {
        progressDialog.dismiss();
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.putExtra(WebViewActivity.INTENT_KEY_URL, server.getUrl().toString());
        startActivity(intent);
      }

      @Override
      public void onServerNotSupported() {
        progressDialog.dismiss();
        ServerUtils.dialogWithTitleAndMessage(getActivity(),
                                              R.string.ServerManager_Error_TitleVersion,
                                              R.string.ServerManager_Error_PlatformVersionNotSupported).show();
      }

      @Override
      public void onServerInvalid() {
        progressDialog.dismiss();
        ServerUtils.dialogWithTitleAndMessage(getActivity(),
                                              R.string.ServerManager_Error_TitleIncorrect,
                                              R.string.ServerManager_Error_IncorrectUrl).show();
      }

      @Override
      public void onConnectionError() {

      }
    });
  }
}
