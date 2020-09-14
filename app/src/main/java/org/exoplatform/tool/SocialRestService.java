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

import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.SocialComment;
import org.exoplatform.model.SocialSpace;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Philippe on 3/8/16. Interface to access to Platform's Social REST
 * services
 */
public interface SocialRestService {

  /**
   * Loads 20 spaces starting from offset, of the authenticated user (by session cookie).<br/>
   * Use the method SocialSpace.getIdentityId() on a resulting space, to post an activity to that space
   * stream.
   */
  @GET("/rest/v1/social/spaces?returnSize=true")
  Call<SpaceListResult> loadSpaces(@Query("offset") String offset, @Query("limit") String limit);

   //@Query("sort") String sort

  /**
   * Creates an activity.<br/>
   * The resulting activity has its attribute 'id' set. Use it to post a comment
   * to this activity.
   *
   * @param activity The activity to create.
   * @param identityId If empty, creates the activity on the authenticated
   *          user's stream. If set, creates the activity on the stream of the
   *          identified space.
   */
  @POST("/rest/private/api/social/v1-alpha3/portal/activity.json")
  Call<SocialActivity> createActivity(@Query("identity_id") String identityId, @Body SocialActivity activity);

  /**
   * Posts a comment to an activity.
   * 
   * @param activityId The activity ID that will be commented.
   * @param comment The comment to post.
   */
  @POST("/rest/v1/social/activities/{id}/comments")
  Call<SocialComment> createCommentOnActivity(@Path("id") String activityId, @Body SocialComment comment);

  /*
   * CANNOT USE THESE METHODS BECAUSE THEY DON'T HANDLE ATTACHMENTS CF
   * https://jira.exoplatform.org/browse/SOC-5264
   */

  @POST("/rest/v1/social/users/{id}/activities")
  Call<SocialActivity> createPublicActivity(@Path("id") String userId, @Body SocialActivity activity);

  @POST("/rest/v1/social/spaces/{id}/activities")
  Call<SocialActivity> createSpaceActivity(@Path("id") String spaceId, @Body SocialActivity activity);

  /**
   * A class that maps to the response JSON of a list of spaces
   */
  class SpaceListResult {

    public List<SocialSpace> spaces;

    public int               offset;

    public int               limit;

    public int               size;

  }

}
