package org.exoplatform.service.share;

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

import android.os.AsyncTask;
import android.util.Log;

import org.exoplatform.App;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.LoginRestService;
import org.exoplatform.tool.PlatformUtils;
import org.exoplatform.tool.ServerUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by paristote on 3/11/16. An async task used to authenticate a user on
 * a given server.
 */
public class LoginTask extends AsyncTask<Server, Void, LoginTask.LoginResult> {

  public interface Listener {
    void onLoginStarted(LoginTask thisTask);

    void onLoginSuccess(PlatformInfo result);

    void onPlatformVersionNotSupported();

    void onLoginFailed();
  }

  class LoginResult {
    Server       mServer;

    PlatformInfo mPlatformInfo;
  }

  private final List<Listener> mListeners;

  public LoginTask() {
    mListeners = new ArrayList<>();
  }

  public void addListener(Listener listener) {
    mListeners.add(listener);
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
    for (Listener l : mListeners) {
      l.onLoginStarted(this);
    }
  }

  @Override
  protected LoginResult doInBackground(Server... params) {
    if (params.length > 0) {
      Server server = params[0];
      Retrofit retrofit = new Retrofit.Builder().baseUrl(server.getUrl().toString())
                                                .addConverterFactory(GsonConverterFactory.create())
                                                .client(ExoHttpClient.newAuthenticatedClient(server.getLastLogin(),
                                                                                             server.getLastPassword()))
                                                .build();
      LoginRestService loginService = retrofit.create(LoginRestService.class);
      try {
        Response<PlatformInfo> response = loginService.login().execute();
        if (response.isSuccessful()) {
          LoginResult result = new LoginResult();
          result.mPlatformInfo = response.body();
          result.mServer = server;
          return result;
        }
      } catch (IOException e) {
        Log.e("LoginTask", e.getMessage());
      }
    }
    return null;
  }

  @Override
  protected void onPostExecute(LoginResult result) {
    super.onPostExecute(result);
    if (result == null) {
      // Login failed
      for (Listener l : mListeners) {
        l.onLoginFailed();
      }
    } else {
      PlatformUtils.reset();
      Double plfVersion = ServerUtils.convertVersionFromString(result.mPlatformInfo.platformVersion);
      if (plfVersion >= App.Platform.MIN_SUPPORTED_VERSION) {
        // Login successful and Platform version supported
        PlatformUtils.init(result.mServer.getUrl().toString(), result.mPlatformInfo, result.mServer.getLastLogin());
        for (Listener l : mListeners) {
          l.onLoginSuccess(result.mPlatformInfo);
        }
      } else {
        for (Listener l : mListeners) {
          l.onPlatformVersionNotSupported();
        }
      }
    }
  }
}
