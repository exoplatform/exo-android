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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.exoplatform.R;

import static android.app.Notification.GROUP_ALERT_SUMMARY;

public class PushNotificationsService extends FirebaseMessagingService {

  private static final String TAG = PushNotificationsService.class.getSimpleName();

  private static final String GROUP_NOTIFICATIONS = "com.android.NOTIFICATIONS";

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);
    Log.d(TAG, "onMessageReceived: " + remoteMessage.getFrom());
    Log.d(TAG, "onMessageReceived: " + remoteMessage.getData());

    sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"));
  }

  private void sendNotification(String messageTitle, String messageBody) {
    String channelId = getString(R.string.default_notification_channel_id);
    int notificationId = 0;

    NotificationCompat.Builder notificationBuilder = null;

    NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // Since android Oreo notification channel is needed.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(channelId,
              "eXo Notifications Channel",
              NotificationManager.IMPORTANCE_DEFAULT);
      notificationManager.createNotificationChannel(channel);
    }

    // Since API 23 active notifications can be directly get from the NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
      if(activeNotifications != null && activeNotifications.length > 0) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for(int i = 0; i < activeNotifications.length; i++) {
          CharSequence[] textLines = activeNotifications[i].getNotification().extras.getCharSequenceArray(NotificationCompat.EXTRA_TEXT_LINES);
          if(textLines != null) {
            for (CharSequence textLine : textLines) {
              inboxStyle.addLine(textLine);
            }
          } else {
            inboxStyle.addLine(activeNotifications[i].getNotification().extras.getString(NotificationCompat.EXTRA_TEXT));
          }
        }
        inboxStyle.addLine(messageBody);

        notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.icon_share_notif_multiple)
                .setContentTitle(messageTitle)
                .setStyle(inboxStyle)
                .setAutoCancel(true)
                .setGroup(GROUP_NOTIFICATIONS);
      }
    } else {
      notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    if (notificationBuilder == null) {
      notificationBuilder = new NotificationCompat.Builder(this, channelId)
              .setSmallIcon(R.drawable.icon_share_notif)
              .setContentTitle(messageTitle)
              .setContentText(messageBody)
              .setAutoCancel(true)
              .setGroup(GROUP_NOTIFICATIONS);
    }

    notificationManager.notify(notificationId, notificationBuilder.build());
  }
}
