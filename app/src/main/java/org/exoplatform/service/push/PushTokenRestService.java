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


import androidx.annotation.NonNull;

import org.exoplatform.model.TokenInfo;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface PushTokenRestService {

  /**
   * Push Token service.<br/>
   *
   * @param tokenInfo assigns current user to fetched token and register the pair on server
   * @return a Call object with a details about HTTP response
   */
  @POST("/rest/private/v1/messaging/device")
  Call<ResponseBody> registerToken(@Body TokenInfo tokenInfo);

  /**
   * Push Token service.<br/>
   * The method destroys token on server
   *
   * @return a Call object with a details about HTTP response
   */
  @DELETE("/rest/private/v1/messaging/device/{token}")
  Call<ResponseBody> deleteToken(@Path("token") @NonNull String token);
}
