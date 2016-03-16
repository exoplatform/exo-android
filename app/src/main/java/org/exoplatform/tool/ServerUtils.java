package org.exoplatform.tool;

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

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Philippe on 2/24/16. Utilities methods to use with servers
 */
public class ServerUtils {

  public static final String LOG_TAG = "ServerUtils";

  public interface ServerVerificationCallback {
    void onServerValid(Server server);

    void onServerNotSupported();

    void onServerInvalid();
  }

  public static void verifyUrl(@NonNull String urlStr, @NonNull Context context, @NonNull
  final ServerVerificationCallback callback) {
    String url = urlStr.trim();
    if (!(url.indexOf("http://") == 0) && !(url.indexOf("https://") == 0)) {
      url = "http://" + url;
    }

    final ProgressDialog progressDialog = new ProgressDialog(context);
    progressDialog.setMessage(context.getString(R.string.ServerManager_Message_SavingServer));
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
                callback.onServerValid(server);
              } else {
                callback.onServerNotSupported();
              }
            } else {
              callback.onServerInvalid();
            }
          }

          @Override
          public void onFailure(Call<PlatformInfo> call, Throwable t) {
            progressDialog.dismiss();
            callback.onServerInvalid();
          }
        });
      } else {
        dialogWithTitleAndMessage(context, R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
      }
    } catch (MalformedURLException e) {
      dialogWithTitleAndMessage(context, R.string.ServerManager_Error_TitleIncorrect, R.string.ServerManager_Error_IncorrectUrl).show();
      Log.d(LOG_TAG, e.getMessage(), e);
    }

  }

  public static AlertDialog dialogWithTitleAndMessage(Context context, int titleId, int messageId) {
    return new AlertDialog.Builder(context).setTitle(titleId)
                                           .setMessage(messageId)
                                           .setNeutralButton(R.string.Word_OK, null)
                                           .create();
  }

  public static Double convertVersionFromString(String version) {
    if (version == null)
      throw new IllegalArgumentException("Argument 'version' must not be null");

    String[] versionNumbers = version.split("\\.");
    StringBuilder majorMinorVersion = new StringBuilder();
    if (versionNumbers.length > 0) {
      majorMinorVersion.append(versionNumbers[0]);
    }
    if (versionNumbers.length > 1) {
      majorMinorVersion.append(".").append(versionNumbers[1]);
    }
    return Double.parseDouble(majorMinorVersion.toString());
  }
}
