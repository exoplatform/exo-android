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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.exoplatform.R;
import org.exoplatform.activity.ConnectServerActivity;
import org.exoplatform.activity.WebViewActivity;

public class PushNotificationsService extends FirebaseMessagingService {

  private static final String TAG = PushNotificationsService.class.getSimpleName();

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);
    Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
    Log.d(TAG, "onMessageReceived: " + remoteMessage.getData());

    sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"), remoteMessage.getData().get("url"));
  }

  private void sendNotification(String messageTitle, String messageBody, String messageTargetUrl) {
    String channelId = getString(R.string.default_notification_channel_id);
    int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

    NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // Since android Oreo notification channel is needed.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(channelId,
              "eXo",
              NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
    }

    CharSequence contentText;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      contentText = Html.fromHtml(messageBody, Html.FROM_HTML_MODE_LEGACY);
    } else {
      contentText = Html.fromHtml(messageBody);
    }

    Intent notificationIntent;
    if(messageTargetUrl != null && !messageTargetUrl.equals("")) {
      notificationIntent = new Intent(this, WebViewActivity.class);
      notificationIntent.putExtra(WebViewActivity.INTENT_KEY_URL, messageTargetUrl);
    } else {
      notificationIntent = new Intent(this, ConnectServerActivity.class);
    }

    PendingIntent pendingIntent =
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(notificationIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
              .setSmallIcon(R.drawable.icon_share_notif)
              .setContentTitle(messageTitle)
              .setContentText(contentText)
              .setContentIntent(pendingIntent)
              .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
              .setAutoCancel(true);

    notificationManager.notify(notificationId, notificationBuilder.build());
  }
}
