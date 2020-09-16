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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.exoplatform.App;

public class PushTokenStorage {

  private static PushTokenStorage instance;

  private static final String TOKEN_PREF_KEY = "push_token_preferences_key";

  public static PushTokenStorage getInstance() {
    if (instance == null) {
      instance = new PushTokenStorage();
    }
    return instance;
  }

  private PushTokenStorage() {
  }

  public void setPushToken(@NonNull String token, Context appContext) {
    saveToken(appContext, token);
  }

  public void clearPushToken(@NonNull Context appContext) {
    saveToken(appContext, null);
  }

  @Nullable
  public String getPushToken(@NonNull Context appContext) {
    return App.Preferences.get(appContext).getString(TOKEN_PREF_KEY, null);
  }

  private void saveToken(@NonNull Context appContext, @Nullable String token) {
    App.Preferences.get(appContext)
            .edit()
            .putString(TOKEN_PREF_KEY, token)
            .apply();
  }
}
