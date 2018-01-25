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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;

public class CookiesConverter {

  public Map<String, String> toMap(@Nullable String cookies) {
    Map<String, String> result = new HashMap<>();
    if (!TextUtils.isEmpty(cookies)) {
      String[] cookieArray = cookies.split(";");
      for (String cookie : cookieArray) {
        String[] parts = cookie.split("=");
        if (parts.length >= 2)
          result.put(parts[0].trim(), parts[1].trim());
      }
    }
    return result;
  }

  public List<Cookie> toList(@NonNull String url, @Nullable String cookies) {
    final Map<String, String> cookiesMap = toMap(cookies);
    final List<Cookie> result = new ArrayList<>();
    for (String key : cookiesMap.keySet()) {
      result.add(new Cookie.Builder()
              .domain(url)
              .name(key)
              .value(cookiesMap.get(key))
              .build()
      );
    }
    return result;
  }

}
