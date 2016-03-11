/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
 */
package org.exoplatform.service.share;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.exoplatform.R;
import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.SocialComment;
import org.exoplatform.model.UploadInfo;
import org.exoplatform.service.share.Action.ActionListener;
import org.exoplatform.service.share.PostAction.PostActionListener;
import org.exoplatform.tool.DocumentUtils;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.PlatformUtils;
import org.exoplatform.tool.SocialRestService;
import org.exoplatform.tool.TitleExtractor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by The eXo Platform SAS.<br/>
 * IntentService that publishes a post with optional attachments. If multiple
 * files are attached, the first one is part of the activity, the other are
 * added in comments on the activity.
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 4, 2015
 */
public class ShareService extends IntentService {

  public static final String LOG_TAG     = ShareService.class.getName();

  public static final String POST_INFO   = "postInfo";

  private int                notifId     = 1;

  private SocialActivity     postInfo;

  // key is uri in device, value is url on server
  private List<UploadInfo>   uploadedMap = new ArrayList<UploadInfo>();

  private enum ShareResult {
    SUCCESS, ERROR_INCORRECT_CONTENT_URI, ERROR_INCORRECT_ACCOUNT, ERROR_CREATE_FOLDER, ERROR_UPLOAD_FAILED, ERROR_POST_FAILED, ERROR_COMMENT_FAILED
  }

  public ShareService() {
    super("eXo_Share_Service");
  }

  /*
   * We start here when the service is called
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    // Retrieve the content of the post from the intent
    postInfo = intent.getParcelableExtra(POST_INFO);

    if (postInfo == null) {
      Toast.makeText(getApplicationContext(), R.string.ShareErrorNullPostInfo, Toast.LENGTH_LONG).show();
      return;
    }

    // Notify the user that the share has started
    notifyBegin();

    if (postInfo.ownerAccount == null) {
      notifyResult(ShareResult.ERROR_INCORRECT_ACCOUNT);
      return;
    }

    if (postInfo.hasAttachment()) {

      UploadInfo initUploadInfo = initUpload();
      boolean uploadStarted = startUpload(initUploadInfo);
      if (uploadStarted) {
        boolean uploadedAll = doUpload(initUploadInfo);
        if (uploadedAll) {
          // already set templateParam when first doc upload completed
          doPost();
        }
      }
    } else {
      // We don't have an attachment, maybe a link
      // TODO move as a separate Action - MOB-1866
      String link = extractLinkFromText();
      if (link != null) {
        postInfo.type = SocialActivity.TYPE_LINK;
        postInfo.title = postInfo.title.replace(link, String.format(Locale.US, "<a href=\"%s\">%s</a>", link, link));
      }
      postInfo.templateParams = linkParams(link);
      doPost();
    }
    PlatformUtils.reset();
  }

  /**
   * Create the resources needed to create the upload destination folder and
   * upload the file
   */
  private UploadInfo initUpload() {
    postInfo.type = SocialActivity.TYPE_DOC;
    UploadInfo uploadInfo = new UploadInfo();
    uploadInfo.init(postInfo);

    return uploadInfo;

  }

  /**
   * Create the directory where the files are stored on the server, if it does
   * not already exist.
   */
  private boolean startUpload(UploadInfo uploadInfo) {
    return CreateFolderAction.execute(postInfo, uploadInfo, new ActionListener() {

      @Override
      public boolean onSuccess(String message) {
        return true;
      }

      @Override
      public boolean onError(String error) {
        notifyResult(ShareResult.ERROR_CREATE_FOLDER);
        return false;
      }
    });
  }

  /**
   * Upload the file
   */
  private boolean doUpload(UploadInfo initUploadInfo) {
    boolean uploadedAll = false;
    uploadedMap.clear();
    UploadInfo uploadInfo = initUploadInfo;
    final int numberOfFiles = postInfo.postAttachedFiles.size();
    for (int i = 0; i < numberOfFiles; i++) {
      // notify the start of the upload i / total
      notifyProgress(i + 1, numberOfFiles);
      // close the current open input stream
      if (uploadInfo != null && uploadInfo.fileToUpload != null)
        uploadInfo.fileToUpload.closeDocStream();
      // Retrieve details of the document to upload
      if (i != 0) {
        uploadInfo = new UploadInfo(uploadInfo);
      }

      String fileUri = "file://" + postInfo.postAttachedFiles.get(i);
      Uri uri = Uri.parse(fileUri);
      uploadInfo.fileToUpload = DocumentUtils.documentInfoFromUri(uri, getBaseContext());

      if (uploadInfo.fileToUpload == null) {
        notifyResult(ShareResult.ERROR_INCORRECT_CONTENT_URI);
        return false;
      } else {
        uploadInfo.fileToUpload.documentName = DocumentUtils.cleanupFilename(uploadInfo.fileToUpload.documentName);
      }
      uploadedAll = UploadAction.execute(postInfo, uploadInfo, new ActionListener() {

        @Override
        public boolean onSuccess(String message) {
          return true;
        }

        @Override
        public boolean onError(String error) {
          notifyResult(ShareResult.ERROR_UPLOAD_FAILED);
          return false;
        }
      });
      if (uploadInfo != null && uploadInfo.fileToUpload != null)
        uploadInfo.fileToUpload.closeDocStream();
      if (!uploadedAll) {
        Log.e(LOG_TAG, String.format("Failed to upload file %d/%d : %s (doUpload)", i + 1, numberOfFiles, fileUri));
        break;
      }
      if (uploadedAll) {
        Log.d(LOG_TAG, String.format("Uploaded file %d/%d OK %s (doUpload)", i + 1, numberOfFiles, fileUri));
        if (i == 0)
          postInfo.buildTemplateParams(uploadInfo);
        else {
          uploadedMap.add(uploadInfo);
        }
      }
      // Delete file after upload
      File f = new File(postInfo.postAttachedFiles.get(i));
      Log.d(LOG_TAG, "File " + f.getName() + " deleted: " + (f.delete() ? "YES" : "NO"));
    }
    return uploadedAll;

  }

  /**
   * Post the message
   */
  private boolean doPost() {
    SocialActivity createdAct = PostAction.execute(postInfo, new PostActionListener());
    boolean ret = createdAct != null;
    if (ret) {
      Log.d(LOG_TAG, "Post activity done");
      for (UploadInfo commentInfo : uploadedMap) {
        ret = doComment(createdAct, commentInfo);
        if (!ret)
          break;
        Log.d(LOG_TAG, "Comment activity done");
      }
      // Share finished successfully
      if (ret) {
        // Notify
        notifyResult(ShareResult.SUCCESS);
      } else
        notifyResult(ShareResult.ERROR_COMMENT_FAILED);
    } else
      notifyResult(ShareResult.ERROR_POST_FAILED);
    return ret;
  }

  /**
   * Post a comment on an activity
   * 
   * @param activity the activity to comment
   * @param commentInfo the info to put in the comment
   * @return
   */
  private boolean doComment(@NonNull SocialActivity activity, @NonNull UploadInfo commentInfo) {
    // TODO create a Comment Action to delegate the operation
    boolean ret = false;
    String mimeType = (commentInfo.fileToUpload == null ? null : commentInfo.fileToUpload.documentMimeType);
    String urlWithoutServer;
    try {
      URL url = new URL(commentInfo.getUploadedUrl());
      urlWithoutServer = url.getPath();
      if (urlWithoutServer != null && !urlWithoutServer.startsWith("/"))
        urlWithoutServer = "/" + urlWithoutServer;
    } catch (MalformedURLException e) {
      Log.w(LOG_TAG, e.getMessage());
      return false;
    }
    StringBuilder bld = new StringBuilder();
    // append link
    bld.append("<a href=\"").append(urlWithoutServer).append("\">").append(commentInfo.fileToUpload.documentName).append("</a>");
    // add image in the comment's body
    if (mimeType != null && mimeType.startsWith("image/")) {
      String thumbnailUrl = urlWithoutServer.replace("/jcr/", "/thumbnailImage/large/");
      bld.append("<br/><a href=\"").append(urlWithoutServer).append("\"><img src=\"").append(thumbnailUrl).append("\" /></a>");
    }

    Retrofit retrofit = new Retrofit.Builder()
              .baseUrl(PlatformUtils.getPlatformDomain())
              .client(ExoHttpClient.getInstance())
              .addConverterFactory(GsonConverterFactory.create())
              .build();
    SocialRestService service = retrofit.create(SocialRestService.class);
    SocialComment comment = new SocialComment(bld.toString());
      try {
          Response<SocialComment> response = service.createCommentOnActivity(activity.id, comment).execute();
          ret = response.isSuccess();
      } catch (IOException e) {
          Log.e(LOG_TAG, "Post comment failed", e);
      }
      return ret;
  }

  private Map<String, String> linkParams(String link) {
    // Create and return TemplateParams for a LINK_ACTIVITY
    // Return null if there is no link
    if (link == null)
      return null;
    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("comment", postInfo.title);
    templateParams.put("link", link);
    templateParams.put("description", "");
    templateParams.put("image", "");
    try {
      templateParams.put("title", TitleExtractor.getPageTitle(link));
    } catch (IOException e) {
      Log.e(LOG_TAG, "Cannot retrieve link title", e);
      templateParams.put("title", link);
    }
    return templateParams;
  }

  private String extractLinkFromText() {
    String text = postInfo.title;
    // Find an occurrence of http:// or https://
    // And return the corresponding URL if any
    int posHttp = text.indexOf("http://");
    int posHttps = text.indexOf("https://");
    int startOfLink = -1;
    if (posHttps > -1)
      startOfLink = posHttps;
    else if (posHttp > -1)
      startOfLink = posHttp;
    if (startOfLink > -1) {
      int endOfLink = text.indexOf(' ', startOfLink);
      if (endOfLink == -1)
        return text.substring(startOfLink);
      else
        return text.substring(startOfLink, endOfLink);
    } else {
      return null;
    }
  }

  /**
   * Send a local notification to inform that the share has started
   */
  private void notifyBegin() {
    notifId = (int) System.currentTimeMillis();
    String title = postInfo.hasAttachment() ? getString(R.string.ShareDocumentTitle) : getString(R.string.ShareMessageTitle);
    String text = postInfo.hasAttachment() ? getString(R.string.ShareDocumentText) : getString(R.string.ShareMessageText);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
    builder.setSmallIcon(R.drawable.icon_share_notif);
    builder.setContentTitle(title);
    builder.setContentText(text);
    builder.setAutoCancel(true);
    builder.setProgress(0, 0, true);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(notifId, builder.build());
  }

  /**
   * Send a local notification to inform of the progress. Only called if the
   * share contains 1 or more attachments.
   * 
   * @param current the index of the current file being uploaded
   * @param total the total number of files to upload
   */
  private void notifyProgress(int current, int total) {
    String text = String.format(Locale.US, "%s (%d/%d)", getString(R.string.ShareDocumentText), current, total);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
    builder.setSmallIcon(R.drawable.icon_share_notif);
    builder.setContentTitle(getString(R.string.ShareDocumentTitle));
    builder.setContentText(text);
    builder.setAutoCancel(true);
    builder.setProgress(0, 0, true);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(notifId, builder.build());
  }

  /**
   * Notify the end of the sharing. The message depends on the given result.
   * 
   * @param result one of {@link ShareResult} values
   */
  private void notifyResult(ShareResult result) {
    String text = "";
    switch (result) {
    case ERROR_CREATE_FOLDER:
      text = getString(R.string.ShareErrorUploadFolderFailed);
      break;
    case ERROR_INCORRECT_ACCOUNT:
      text = getString(R.string.ShareErrorIncorrectAccount);
      break;
    case ERROR_INCORRECT_CONTENT_URI:
      text = getString(R.string.ShareErrorCannotReadDoc);
      break;
    case ERROR_POST_FAILED:
      text = getString(R.string.ShareErrorPostFailed);
      break;
    case ERROR_COMMENT_FAILED:
      text = getString(R.string.ShareErrorCommentFailed);
      break;
    case ERROR_UPLOAD_FAILED:
      text = getString(R.string.ShareErrorUploadFailed);
      break;
    case SUCCESS:
      text = getString(R.string.ShareOperationSuccess);
      break;
    default:
      break;
    }
    String title = postInfo.hasAttachment() ? getString(R.string.ShareDocumentTitle) : getString(R.string.ShareMessageTitle);
    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
    builder.setSmallIcon(R.drawable.icon_share_notif);
    builder.setContentTitle(title);
    builder.setContentText(text);
    builder.setAutoCancel(true);
    builder.setProgress(0, 0, false);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.notify(notifId, builder.build());
  }

}
