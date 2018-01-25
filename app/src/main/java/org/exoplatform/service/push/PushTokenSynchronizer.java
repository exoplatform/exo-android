package org.exoplatform.service.push;

/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.exoplatform.model.TokenInfo;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PushTokenSynchronizer {

  private static final String TAG = PushTokenSynchronizer.class.getSimpleName();

  private Callback<ResponseBody> registerCallback = new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
      if (response.isSuccessful()) {
        Log.i(TAG, "Push token registered successfully");
        isSynchronized = true;
      } else {
        Log.e(TAG, "Register token unsuccessfully response");
      }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
      Log.e(TAG, "Register token onFailure: ", t);
    }
  };

  private Callback<ResponseBody> destroyCallback = new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
      if (response.isSuccessful()) {
        Log.i(TAG, "Push token destroyed successfully");
        username = null;
        url = null;
        isSynchronized = false;
      } else {
        Log.e(TAG, "Destroy token unsuccessfully response");
      }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
      Log.e(TAG, "Destroy token onFailure: ", t);
    }
  };

  private PushTokenRestServiceFactory restServiceFactory;

  private PushTokenRestService restService;

  private String username;

  private String url;

  private String token;

  private boolean isSynchronized = false;

  public PushTokenSynchronizer(PushTokenRestServiceFactory restServiceFactory) {
    this.restServiceFactory = restServiceFactory;
  }

  public void setConnectedUserAndSync(@Nullable String username, @Nullable String url) {
    final boolean isValuesNotEmpty = !TextUtils.isEmpty(username) && !TextUtils.isEmpty(url);
    final boolean isValuesChanged = !TextUtils.equals(this.username, username) && !TextUtils.equals(this.url, url);
    if (isValuesNotEmpty && (isValuesChanged || !isSynchronized)) {
      this.username = username;
      this.url = url;
      tryToSynchronizeToken();
    }
  }

  public void setTokenAndSync(@Nullable String token) {
    final boolean isValueNotEmpty = !TextUtils.isEmpty(token);
    final boolean isValueChanged = !TextUtils.equals(this.token, token);
    if (isValueNotEmpty && (isValueChanged || !isSynchronized)) {
      this.token = token;
      tryToSynchronizeToken();
    }
  }

  public void tryToDestroyToken() {
    if (!isStateValid() || !isSynchronized) {
      return;
    }

    initRestServiceIfNeeded();

    restService
            .deleteToken(token)
            .enqueue(destroyCallback);
  }

  private void tryToSynchronizeToken() {
    if (!isStateValid()) {
      return;
    }

    initRestServiceIfNeeded();

    restService
            .registerToken(new TokenInfo(token, username))
            .enqueue(registerCallback);
  }

  private void initRestServiceIfNeeded() {
    if (restService == null) {
      restService = restServiceFactory.create(url);
    }
  }

  private boolean isStateValid() {
    return !TextUtils.isEmpty(username) && !TextUtils.isEmpty(url) && !TextUtils.isEmpty(token);
  }
}
