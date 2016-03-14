package org.exoplatform.model;

/*
 * Copyright (C) 2003-${YEAR} eXo Platform SAS.
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

import org.exoplatform.App;
import org.exoplatform.tool.PlatformUtils;

/**
 * Created by paristote on 3/7/16. Describe a document upload to Platform.
 */
public class UploadInfo {

  public String       uploadId;

  public DocumentInfo fileToUpload;

  public String       domain;

  public String       repository;

  public String       workspace;

  public String       drive;

  public String       folder;

  public String       jcrUrl;

  public UploadInfo() {
  }

  public UploadInfo(UploadInfo another) {
    this.uploadId = Long.toHexString(System.currentTimeMillis());
    this.repository = another.repository;
    this.workspace = another.workspace;
    this.drive = another.drive;
    this.folder = another.folder;
    this.jcrUrl = another.jcrUrl;
    this.domain = another.domain;
  }

  public void init(SocialActivity postInfo) {

    uploadId = Long.toHexString(System.currentTimeMillis());
    PlatformInfo platformInfo = PlatformUtils.getPlatformInfo();
    repository = platformInfo.currentRepoName;
    workspace = platformInfo.defaultWorkSpaceName;

    if (postInfo.isPublic()) {
      // File will be uploaded in the Public folder of the user's drive
      // e.g. /Users/u___/us___/use___/user/Public/Mobile
      drive = App.Platform.DOCUMENT_PERSONAL_DRIVE_NAME;
      folder = "Public/Mobile";
      StringBuilder jcrUrlBuilder = new StringBuilder();
      jcrUrl = PlatformUtils.getUserHomeJcrFolderPath();
    } else {
      // File will be uploaded in the Documents folder of the space's drive
      // e.g. /Groups/spaces/the_space/Documents/Mobile
      drive = ".spaces." + postInfo.destinationSpace.getOriginalName();
      folder = "Mobile";
      StringBuilder url = new StringBuilder(postInfo.ownerAccount.getUrl().toString()).append(App.Platform.DOCUMENT_JCR_PATH)
                                                                                      .append("/")
                                                                                      .append(repository)
                                                                                      .append("/")
                                                                                      .append(workspace)
                                                                                      .append("/Groups/spaces/")
                                                                                      .append(postInfo.destinationSpace.getOriginalName())
                                                                                      .append("/Documents");
      jcrUrl = url.toString();
    }
  }

  public String getUploadedUrl() {
    return new StringBuffer(jcrUrl).append("/").append(folder).append("/").append(fileToUpload.documentName).toString();
  }
}
