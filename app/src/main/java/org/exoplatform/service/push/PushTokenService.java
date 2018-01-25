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

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class PushTokenService extends FirebaseInstanceIdService {

  private static final String TAG = PushTokenService.class.getSimpleName();

  @Override
  public void onTokenRefresh() {
    super.onTokenRefresh();
    String newToken = FirebaseInstanceId.getInstance().getToken();
    PushTokenStorage.getInstance().setPushToken(newToken, getApplicationContext());
    PushTokenSynchronizerLocator.getInstance().setTokenAndSync(newToken);
    Log.d(TAG, "Refreshed push token: " + newToken);
  }
}
