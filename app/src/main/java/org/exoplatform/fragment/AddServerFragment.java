package org.exoplatform.fragment;

/*
 * Copyright (C) 2003-${YEAR} eXo Platform SAS.
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.PlatformRestService;
import org.exoplatform.activity.WebViewActivity;
import org.exoplatform.tool.ServerUtils;

import java.net.MalformedURLException;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

  // when IME_ACTION_DONE happens on the url input.
  private void submitUrl() {
    String url = mIntranetUrlField.getText().toString().trim();
    if (!(url.indexOf("http://") == 0) && !(url.indexOf("https://") == 0)) {
      url = "http://" + url;
    }

    final ProgressDialog progressDialog = new ProgressDialog(getActivity());
    progressDialog.setMessage(getString(R.string.ServerManager_Message_SavingServer));
    progressDialog.setCancelable(false);

    try {
      if (Patterns.WEB_URL.matcher(url).matches()) {
        final Server server = new Server(new URL(url));
        Retrofit retrofit = new Retrofit.Builder().baseUrl(server.getUrl().toString())
                                                  .addConverterFactory(GsonConverterFactory.create())
                                                  .client(ExoHttpClient.getInstance())
                                                  .build();
        PlatformRestService platformService = retrofit.create(PlatformRestService.class);
        progressDialog.show();
        platformService.getInfo().enqueue(new Callback<PlatformInfo>() {
          @Override
          public void onResponse(Call<PlatformInfo> call, Response<PlatformInfo> response) {
            progressDialog.dismiss();
            if (response.isSuccess()) {
              Double plfVersion = ServerUtils.convertVersionFromString(response.body().platformVersion);
              if (plfVersion >= App.Platform.MIN_SUPPORTED_VERSION) {
                Intent intent = new Intent(getActivity(), WebViewActivity.class);
                intent.putExtra(WebViewActivity.INTENT_KEY_URL, server.getUrl().toString());
                startActivity(intent);
              } else {
                dialogWithTitleAndMessage(R.string.ServerManager_Error_TitleVersion,
                                          R.string.ServerManager_Error_PlatformVersionNotSupported).show();
              }
            } else {
              dialogWithTitleAndMessage(R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
            }
          }

          @Override
          public void onFailure(Call<PlatformInfo> call, Throwable t) {
            progressDialog.dismiss();
            dialogWithTitleAndMessage(R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
          }
        });
      } else {
        dialogWithTitleAndMessage(R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
      }
    } catch (MalformedURLException e) {
      dialogWithTitleAndMessage(R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
      Log.d("AddServerFragment", e.getMessage(), e);
    }

  }

  private AlertDialog dialogWithTitleAndMessage(int titleId, int messageId) {
    return new AlertDialog.Builder(getActivity()).setTitle(titleId)
                                                 .setMessage(messageId)
                                                 .setNeutralButton(R.string.Word_OK, null)
                                                 .create();
  }
}
