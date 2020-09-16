package org.exoplatform.tool.cookies;

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

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.exoplatform.service.push.PushTokenSynchronizerLocator;

import java.net.URL;
import java.util.Map;

class UsernameCookieInterceptor implements CookiesInterceptor {

  private static final String USERNAME_COOKIE_KEY = "last_login_username";

  @Override
  public void intercept(Map<String, String> cookies, String url) {
    if (cookies.containsKey(USERNAME_COOKIE_KEY)) {
      PushTokenSynchronizerLocator.getInstance().setConnectedUserAndSync(cookies.get(USERNAME_COOKIE_KEY), calculateBaseUrl(url));
    }
  }

  @Nullable
  private String calculateBaseUrl(String urlStr) {
    try {
      URL url = new URL(urlStr);
      if (!TextUtils.isEmpty(url.getPath())) {
        urlStr = urlStr.replaceFirst(url.getPath(), "");
      }
      if (!TextUtils.isEmpty(url.getQuery())) {
        urlStr = urlStr.replaceFirst(url.getQuery(), "");
      }
      return urlStr;
    } catch (Exception e) {
      return null;
    }
  }
}
