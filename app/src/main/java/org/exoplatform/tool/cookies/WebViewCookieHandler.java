package org.exoplatform.tool.cookies;

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

import android.webkit.CookieManager;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by paristote on 3/29/16.<br/>
 * Inspired from <a href=
 * "https://www.snip2code.com/Snippet/556017/OkHttp-and-Webview-cookie-sharing"
 * >WebviewCookieHandler</a>
 */
public class WebViewCookieHandler implements CookieJar {

  private final CookieManager webviewCookieManager = CookieManager.getInstance();
  private final CookiesConverter cookiesConverter = new CookiesConverter();

  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    if (cookies != null && url != null) {
      for (Cookie cookie : cookies) {
        webviewCookieManager.setCookie(cookie.domain(), cookie.value());
      }
    }
  }

  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    List<Cookie> cookieList = new ArrayList<>();
    if (url != null) {
      String cookiesStr = webviewCookieManager.getCookie(url.url().toString());
      // last_login_username=root;
      // JSESSIONIDSSO=AF9296C7B383CE3D4032340FB36F85C3;
      // JSESSIONID=A068BB1CCBFE5B24BCF5A0E622C193EB
      cookieList.addAll(cookiesConverter.toList(url.host(), cookiesStr));
    }
    return cookieList;
  }
}
