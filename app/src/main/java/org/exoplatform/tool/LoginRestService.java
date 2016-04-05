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

import org.exoplatform.model.PlatformInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;

/**
 * Created by Philippe on 3/7/16. Interface to connect to the Platform's REST
 * service used to authenticate a user.
 */
public interface LoginRestService {

  /**
   * Login service
   * 
   * @param authorization the BASIC authorization header string, i.e.
   *          "BASIC username:password" base-64 encoded
   * @return a Call object with a PlatformInfo response
   */
  @GET("/rest/private/platform/info")
  Call<PlatformInfo> login(@Header("Authorization") String authorization);

  /**
   * Login service.<br/>
   * Authentication is made with the BasicAuthenticator from ExoHttpClient.
   * 
   * @see ExoHttpClient
   * @return a Call object with a PlatformInfo response
   */
  @GET("/rest/private/platform/info")
  Call<PlatformInfo> login();

}
