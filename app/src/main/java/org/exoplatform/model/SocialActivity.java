package org.exoplatform.model;

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

import android.os.Parcel;
import android.os.Parcelable;

import org.exoplatform.App;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 16, 2015
 */
public class SocialActivity implements Parcelable {

  public final static String TYPE_DEFAULT       = "DEFAULT_ACTIVITY";

  public final static String TYPE_DEFAULT_SPACE = "exosocial:spaces";

  public final static String TYPE_DOC           = "DOC_ACTIVITY";

  public final static String TYPE_LINK          = "LINK_ACTIVITY";

  /*
   * Attributes mapped to the JSON representation of an activity
   */

  public String              id;

  public String              title;

  public Map<String, String> templateParams;

  public String              type               = TYPE_DEFAULT;

  /*
   * Other attributes
   */

  public Server              ownerAccount;

  public List<String>        postAttachedFiles;

  public SocialSpace         destinationSpace;

  public SocialActivity() {
  }

  private SocialActivity(Parcel in) {
    readFromParcel(in);
  }

  public static final Creator<SocialActivity> CREATOR = new Creator<SocialActivity>() {
                                                        public SocialActivity createFromParcel(Parcel in) {
                                                          return new SocialActivity(in);
                                                        }

                                                        public SocialActivity[] newArray(int size) {
                                                          return new SocialActivity[size];
                                                        }
                                                      };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeParcelable(ownerAccount, flags);
    out.writeString(id);
    out.writeString(title);
    out.writeStringList(postAttachedFiles);
    out.writeParcelable(destinationSpace, flags);
    out.writeMap(templateParams);
    out.writeString(type);
  }

  public void readFromParcel(Parcel in) {
    ownerAccount = in.readParcelable(SocialActivity.class.getClassLoader());
    id = in.readString();
    title = in.readString();
    postAttachedFiles = new ArrayList<>();
    in.readStringList(postAttachedFiles);
    destinationSpace = in.readParcelable(SocialActivity.class.getClassLoader());
    in.readMap(templateParams, SocialActivity.class.getClassLoader());
    type = in.readString();
  }

  /*
   * 
   */

  /**
   * @return true if the post's destination space is null or empty, i.e. the
   *         post is public
   */
  public boolean isPublic() {
    return (destinationSpace == null || "".equals(destinationSpace.toString()));
  }

  /**
   * @return true if the post's attachments list is not null and not empty
   */
  public boolean hasAttachment() {
    return postAttachedFiles != null && !postAttachedFiles.isEmpty();
  }

  /**
   * Set the template param with the given name and value.<br/>
   * Create a new Map if necessary.
   * 
   * @param name the name of the parameter
   * @param value the value of the parameter
   */
  public void addTemplateParam(String name, String value) {
    if (templateParams == null)
      templateParams = new HashMap<>(1);
    templateParams.put(name, value);
  }

  /**
   * Get the template param with the given name. <br/>
   * Return null if the Map is null or if no param is found under this name.
   * 
   * @param name the name of the parameter
   * @return the value associated with the given parameter name
   */
  public String getTemplateParam(String name) {
    if (templateParams == null)
      return null;
    return templateParams.get(name);
  }

  /**
   * Create TemplateParams for a DOC_ACTIVITY
   * 
   * @param uploadInfo Info about the uploaded DOC
   */
  public void buildTemplateParams(UploadInfo uploadInfo) {
    String docUrl = uploadInfo.getUploadedUrl();
    templateParams = new HashMap<>();
    templateParams.put("WORKSPACE", uploadInfo.workspace);
    templateParams.put("REPOSITORY", uploadInfo.repository);
    String docLink = docUrl.substring(ownerAccount.getUrl().toString().length());
    templateParams.put("DOCLINK", docLink);
    StringBuilder beginPath = new StringBuilder(App.Platform.DOCUMENT_JCR_PATH).append("/")
                                                                               .append(uploadInfo.repository)
                                                                               .append("/")
                                                                               .append(uploadInfo.workspace);
    String docPath = docLink.substring(beginPath.length());
    templateParams.put("DOCPATH", docPath);
    templateParams.put("DOCNAME", uploadInfo.fileToUpload.documentName);
    String mimeType = uploadInfo.fileToUpload.documentMimeType;
    if (mimeType != null && !mimeType.trim().isEmpty()) {
      templateParams.put("mimeType", uploadInfo.fileToUpload.documentMimeType);
    }
  }

}
