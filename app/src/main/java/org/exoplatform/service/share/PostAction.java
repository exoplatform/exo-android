package org.exoplatform.service.share;

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

import android.util.Log;

import org.exoplatform.model.SocialActivity;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.PlatformUtils;
import org.exoplatform.tool.ServerUtils;
import org.exoplatform.tool.SocialRestService;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by The eXo Platform SAS<br/>
 * An Action for posting an activity on Platform. Supports DEFAULT_ACTIVITY,
 * LINK_ACTIVITY and DOC_ACTIVITY types.
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 17, 2015
 */
public class PostAction extends Action {
  /**
   * create and execute post action, wait for return result
   * 
   * @param post the activity to post
   * @param listener the listener to call after the action
   * @return just created activity or null if execution failed.
   */
  public static SocialActivity execute(SocialActivity post, PostActionListener listener) {

    PostAction action = new PostAction();
    action.postInfo = post;
    action.listener = listener;
    action.execute();
    return listener.mCreatedActivity;

  }

  @Override
  protected boolean doExecute() {
    String type = SocialActivity.DOC_ACTIVITY_TYPE;
    if (ServerUtils.isOldVersion())
      type = SocialActivity.OLD_DOC_ACTIVITY_TYPE;
    if (type.equals(postInfo.type)) {
      postInfo.addTemplateParam("MESSAGE", postInfo.title);
      if (postInfo.title != null && postInfo.title.trim().isEmpty()) {
        postInfo.title = postInfo.postAttachedFiles.get(0);
      }
    } else if (!SocialActivity.TYPE_LINK.equals(postInfo.type)) {
      setPostTextActivity();
    }

    boolean postResult = postActivity();
    if (postResult) {
      return listener.onSuccess("Message posted successfully");
    } else {
      return listener.onError("Could not post the message");
    }
  }

  private void setPostTextActivity() {
    if (postInfo.isPublic())
      postInfo.type = SocialActivity.TYPE_DEFAULT;
    else
      postInfo.type = SocialActivity.TYPE_DEFAULT_SPACE;
  }

  private boolean postActivity() {
    // Perform the actual Post using the Social Activity service
    Retrofit retrofit = new Retrofit.Builder().baseUrl(PlatformUtils.getPlatformDomain())
                                              .client(ExoHttpClient.getInstance())
                                              .addConverterFactory(GsonConverterFactory.create())
                                              .build();
    SocialRestService service = retrofit.create(SocialRestService.class);
    try {
      if (postInfo.isPublic()) {
        Response<SocialActivity> response = service.createActivity("", postInfo).execute();
        if (listener instanceof PostActionListener && response.isSuccessful()) {
          ((PostActionListener) listener).mCreatedActivity = response.body();
        }
        return response.isSuccessful(); // return the result of the createActivity
                                        // method call
      } else {
        String spaceId = postInfo.destinationSpace.getIdentityId();
        if (spaceId != null) {
          Response<SocialActivity> response = service.createActivity(spaceId, postInfo).execute();
          if (listener instanceof PostActionListener && response.isSuccessful()) {
            ((PostActionListener) listener).mCreatedActivity = response.body();
          }
          return response.isSuccessful(); // return the result of the
                                       // createActivity method call
        } else {
          Log.e(LOG_TAG, "Post message failed: could not get space ID for space " + postInfo.destinationSpace);
        }
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Post message failed", e);
    }
    return false;
  }

  public static class PostActionListener implements ActionListener {

    private SocialActivity mCreatedActivity;

    @Override
    public boolean onSuccess(String message) {
      return true;
    }

    @Override
    public boolean onError(String error) {
      return false;
    }
  }
}
