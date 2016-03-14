package org.exoplatform.service.share;

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

import android.os.AsyncTask;
import android.util.Log;

import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.LoginRestService;
import org.exoplatform.tool.PlatformUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Credentials;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by paristote on 3/11/16. An async task used to authenticate a user on
 * a given server.
 */
public class LoginTask extends AsyncTask<Server, Void, PlatformInfo> {

  public interface Listener {
    void onLoginStarted(LoginTask thisTask);

    void onLoginSuccess(PlatformInfo result);

    void onLoginFailed();
  }

  private List<Listener> mListeners;

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
  protected PlatformInfo doInBackground(Server... params) {
    if (params.length > 0) {
      Server server = params[0];
      Retrofit retrofit = new Retrofit.Builder().baseUrl(server.getUrl().toString())
                                                .addConverterFactory(GsonConverterFactory.create())
                                                .client(ExoHttpClient.getInstance())
                                                .build();
      LoginRestService loginService = retrofit.create(LoginRestService.class);
      try {
        Response<PlatformInfo> response = loginService.login(Credentials.basic(server.getLastLogin(), server.getLastPassword()))
                                                      .execute();
        if (response.isSuccess()) {
          PlatformUtils.init(server.getUrl().toString(), response.body());
          return response.body();
        } else {
          PlatformUtils.reset();
        }
      } catch (IOException e) {
        Log.e("LoginTask", e.getMessage());
      }
    }
    return null;
  }

  @Override
  protected void onPostExecute(PlatformInfo result) {
    super.onPostExecute(result);
    for (Listener l : mListeners) {
      if (result != null)
        l.onLoginSuccess(result);
      else
        l.onLoginFailed();
    }
  }
}
