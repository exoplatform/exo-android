package org.exoplatform.tool;

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
 * Created by paristote on 3/8/16.
 */
public interface SocialRestService {

     /**
      * Loads all spaces of the authenticated user (by session cookie).<br/>
      * Use the method SocialSpace.getIdentityId() to post an activity to a space stream.
     */
    @GET("/rest/v1/social/spaces")
    Call<RestSpaceList> loadSpaces();


    /**
     * Creates an activity.<br/>
     * The resulting activity has its attribute 'id' set. Use it to post a comment to this activity.
     *
     * @param activity The activity to create.
     * @param identityId If empty, creates the activity on the authenticated user's stream.
     *                   If set, creates the activity on the stream of the identified space.
    */
    @POST("/rest/private/api/social/v1-alpha3/portal/activity.json")
    Call<SocialActivity> createActivity(@Query("identity_id") String identityId, @Body SocialActivity activity);


    /**
     * Posts a comment to an activity.
     * @param activityId The activity ID that will be commented.
     * @param comment The comment to post.
     */
    @POST("/rest/v1/social/activities/{id}/comments")
    Call<SocialComment> createCommentOnActivity(@Path("id") String activityId, @Body SocialComment comment);

    /*
        CANNOT USE THESE METHODS BECAUSE THEY DON'T HANDLE ATTACHMENTS
        CF https://jira.exoplatform.org/browse/SOC-5264
     */

    @POST("/rest/v1/social/users/{id}/activities")
    Call<SocialActivity> createPublicActivity(@Path("id") String userId, @Body SocialActivity activity);

    @POST("/rest/v1/social/spaces/{id}/activities")
    Call<SocialActivity> createSpaceActivity(@Path("id") String spaceId, @Body SocialActivity activity);


    /**
     * A class that maps to the response JSON of a list of spaces
     */
    class RestSpaceList {

        public List<SocialSpace> spaces;

        public int offset;

        public int limit;

    }

}
