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

import org.exoplatform.App;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Philippe on 3/10/16. Utility class and methods to store
 * information about the current connection to a Platform server.
 */
public class PlatformUtils {

  private static PlatformInfo platformInfo;

  private static String       platformDomain;

  public static void init(String domain, PlatformInfo info, String userName) {
    if (domain == null || info == null)
      throw new IllegalArgumentException("Domain and Info parameters must not be null.");

    platformInfo = info;
    if(platformInfo.defaultWorkSpaceName == null || platformInfo.defaultWorkSpaceName.isEmpty()) {
      platformInfo.defaultWorkSpaceName = "collaboration";
    }
    if(platformInfo.currentRepoName == null || platformInfo.currentRepoName.isEmpty()){
      platformInfo.currentRepoName = "repository";
    }
    if(platformInfo.userHomeNodePath == null || platformInfo.userHomeNodePath.isEmpty()) {
      platformInfo.userHomeNodePath = makeUserPersonalDocumentsPath(userName);
    }

    try {
      URL urlDomain = new URL(domain);
      platformDomain = Server.format(urlDomain);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Incorrect domain parameter: " + domain, e);
    }
  }

  public static void reset() {
    platformInfo = null;
    platformDomain = null;
  }

  public static String getUserHomeJcrFolderPath() {
    if (platformInfo == null || platformDomain == null)
      throw new NullPointerException("Incorrect Platform domain or info attributes. Use PlatformUtils.init().");


    StringBuilder b = new StringBuilder(platformDomain).append(App.Platform.DOCUMENT_JCR_PATH)
                                                       .append("/")
                                                       .append(platformInfo.currentRepoName)
                                                       .append("/")
                                                       .append(platformInfo.defaultWorkSpaceName)
                                                       .append(platformInfo.userHomeNodePath);

    return b.toString();

  }

  public static PlatformInfo getPlatformInfo() {
    return platformInfo;
  }

  public static String getPlatformDomain() {
    return platformDomain;
  }

  private static String makeUserPersonalDocumentsPath(String userName) {
    return "/Users/" + userName.substring(0,1) + "___/" +  userName.substring(0,2) + "___/" + userName;
  }

}
