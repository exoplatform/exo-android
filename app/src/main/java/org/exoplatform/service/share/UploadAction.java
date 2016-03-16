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

import android.net.Uri;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.exoplatform.App;
import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.UploadInfo;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.PlatformUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by The eXo Platform SAS<br/>
 * An Action for uploading a file to Platform. Uses the upload service of ECMS.
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 17, 2015
 */
public class UploadAction extends Action {

  private UploadInfo uploadInfo;

  @Override
  protected void check() {
    if (uploadInfo == null)
      throw new IllegalArgumentException("Cannot pass null as the UploadInfo argument");
    super.check();
  }

  /**
   * create and execute upload action, wait for result
   * 
   * @param post the activity for which we upload the document(s)
   * @param upload the upload information
   * @param listener the listener to call back after the upload
   * @return true if the upload was successful
   */
  public static boolean execute(SocialActivity post, UploadInfo upload, ActionListener listener) {
    UploadAction action = new UploadAction();
    action.postInfo = post;
    action.uploadInfo = upload;
    action.listener = listener;
    return action.execute();
  }

  @Override
  protected boolean doExecute() {
    // TODO use okhttp
    String id = uploadInfo.uploadId;
    String boundary = "----------------------------" + id;
    String CRLF = "\r\n";
    int status = -1;
    OutputStream output = null;
    PrintWriter writer = null;
    StringBuilder cookieString = new StringBuilder();
    try {
      if (postInfo == null || postInfo.ownerAccount == null)
        throw new IOException("Input parameter 'info' is null");
      // Open a connection to the upload web service
      StringBuilder stringUrl = new StringBuilder(postInfo.ownerAccount.getUrl().toString()).append("/portal")
                                                                                            .append(App.Platform.DOCUMENT_UPLOAD_PATH_REST)
                                                                                            .append("?uploadId=")
                                                                                            .append(id);
      URL uploadUrl = new URL(stringUrl.toString());
      HttpURLConnection uploadReq = (HttpURLConnection) uploadUrl.openConnection();
      uploadReq.setDoOutput(true);
      uploadReq.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      for (okhttp3.Cookie c : ExoHttpClient.cookiesForUrl(stringUrl.toString())) {
        cookieString.append(c.name()).append("=").append(c.value()).append(";");
      }
      uploadReq.addRequestProperty("Cookie", cookieString.toString());
      // Write the form data
      output = uploadReq.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
      writer.append("--").append(boundary).append(CRLF);
      writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            .append(uploadInfo.fileToUpload.documentName)
            .append("\"")
            .append(CRLF);
      writer.append("Content-Type: ").append(uploadInfo.fileToUpload.documentMimeType).append(CRLF);
      writer.append(CRLF).flush();
      byte[] buf = new byte[1024];
      while (uploadInfo.fileToUpload.documentData.read(buf) != -1) {
        output.write(buf);
      }
      output.flush();
      writer.append(CRLF).flush();
      writer.append("--").append(boundary).append("--").append(CRLF).flush();
      // Execute the connection and retrieve the status code
      status = uploadReq.getResponseCode();
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error while uploading " + uploadInfo.fileToUpload, e);
    } finally {
      if (uploadInfo != null && uploadInfo.fileToUpload != null && uploadInfo.fileToUpload.documentData != null)
        try {
          uploadInfo.fileToUpload.documentData.close();
        } catch (IOException e1) {
          Log.e(LOG_TAG, "Error while closing the upload stream", e1);
        }
      if (output != null)
        try {
          output.close();
        } catch (IOException e) {
          Log.e(LOG_TAG, "Error while closing the connection", e);
        }
      if (writer != null)
        writer.close();
    }
    if (status < HttpURLConnection.HTTP_OK || status >= HttpURLConnection.HTTP_MULT_CHOICE) {
      // Exit if the upload went wrong
      return listener.onError(String.format("Could not upload the file %s", uploadInfo.fileToUpload.documentName));
    }
    status = -1;
    try

    {
      if (postInfo == null || postInfo.ownerAccount == null)
        throw new IOException("Input parameter 'info' is null");
      // Prepare the request to save the file in JCR
      String stringUrl = new StringBuilder(postInfo.ownerAccount.getUrl().toString()).append("/portal")
                                                                                     .append(App.Platform.DOCUMENT_CONTROL_PATH_REST)
                                                                                     .toString();
      Uri moveUri = Uri.parse(stringUrl);
      moveUri = moveUri.buildUpon()
                       .appendQueryParameter("uploadId", id)
                       .appendQueryParameter("action", "save")
                       .appendQueryParameter("workspaceName", PlatformUtils.getPlatformInfo().defaultWorkSpaceName)
                       .appendQueryParameter("driveName", uploadInfo.drive)
                       .appendQueryParameter("currentFolder", uploadInfo.folder)
                       .appendQueryParameter("fileName", uploadInfo.fileToUpload.documentName)
                       .build();
      HttpGet moveReq = new HttpGet(moveUri.toString());
      moveReq.addHeader("Cookie", cookieString.toString());
      // Execute the request and retrieve the status code
      HttpResponse move = new DefaultHttpClient().execute(moveReq);
      status = move.getStatusLine().getStatusCode();
    } catch (Exception e) {
      // XXX can not remove because Uri.parse can throw runtime exception.
      Log.e(LOG_TAG, "Error while saving " + uploadInfo.fileToUpload + " in JCR", e);
    }

    boolean ret;
    if (status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE) {
      ret = listener.onSuccess(String.format("File %s uploaded successfully", uploadInfo.fileToUpload.documentName));
    } else

    {
      ret = listener.onError(String.format("Could not save the file %s", uploadInfo.fileToUpload.documentName));
    }
    return ret;
  }

}
