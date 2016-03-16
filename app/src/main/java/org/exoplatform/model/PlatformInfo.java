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

/**
 * Created by paristote on 3/7/16. Information about a Platform server.
 */
public class PlatformInfo {

  public String platformEdition;

  public String buildNumber;

  public String productCode;

  public String platformVersion;

  public String unlockKey;

  public String nbUsers;

  public String dateOfKeyGeneration;

  public String isMobileCompliant;

  public String platformBuildNumber;

  public String platformRevision;

  public String userHomeNodePath;    // "/Users/r___/ro___/roo___/root"

  public String runningProfile;

  public String currentRepoName;     // "repository"

  public String defaultWorkSpaceName; // "collaboration"

  public String duration;
}
