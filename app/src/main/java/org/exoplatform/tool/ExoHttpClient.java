package org.exoplatform.tool;

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

import org.exoplatform.BuildConfig;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cookie;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * A Http Client based on OkHttpClient with the following customizations:
 * <ul>
 * <li>The User-Agent is set to "eXo/{app-version}"</li>
 * <li>The connection times out after 10 seconds</li>
 * <li>Accepts all cookies and stores them in the java.net.CookieManager</li>
 * </ul>
 */
public class ExoHttpClient {

  private static OkHttpClient client;

  /**
   * @return the Http Client with the following customizations:
   *         <ul>
   *         <li>The User-Agent is set to "eXo/{app-version}"</li>
   *         <li>The connection times out after 10 seconds</li>
   *         <li>Accepts all cookies and stores them in the
   *         java.net.CookieManager</li>
   *         </ul>
   */
  public static OkHttpClient getInstance() {
    if (client == null) {
      CookieManager cookieManager = new CookieManager();
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
      client = new OkHttpClient.Builder().addInterceptor(new ExoUserAgentInterceptor())
                                         .connectTimeout(10, TimeUnit.SECONDS)
                                         .cookieJar(new JavaNetCookieJar(cookieManager))
                                         .build();
    }
    return client;
  }

  public static List<Cookie> cookiesForUrl(String url) {
    HttpUrl okhttpUrl = HttpUrl.parse(url);
    if (okhttpUrl == null)
      throw new IllegalArgumentException("Incorrect URL : " + url);

    return getInstance().cookieJar().loadForRequest(okhttpUrl);
  }

  // public static OkHttpClient newAuthenticatedClient(String username, String
  // password) {
  // OkHttpClient getInstance = getInstance().newBuilder()
  // .authenticator(new BasicAuthenticator(username, password))
  // .build();
  // return getInstance;
  // }

  private static class ExoUserAgentInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
      String ver = BuildConfig.VERSION_NAME;
      Request req = chain.request().newBuilder().header("User-Agent", "eXo/" + ver + " (Android)") // replaces
                                                                                                   // any
                                                                                                   // existing
                                                                                                   // User-Agent
                                                                                                   // header
                         .build();

      return chain.proceed(req);
    }
  }

  private static class BasicAuthenticator implements Authenticator {

    private String username, password;

    public BasicAuthenticator(String username, String password) {
      this.username = username;
      this.password = password;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
      String credential = Credentials.basic(username, password);
      return response.request().newBuilder().header("Authorization", credential).build();
    }
  }

}
