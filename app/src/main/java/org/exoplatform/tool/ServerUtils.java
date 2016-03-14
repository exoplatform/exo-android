package org.exoplatform.tool;

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

/**
 * Created by Philippe on 2/24/16. Utilities methods to use with servers
 */
public class ServerUtils {

  public static final String LOG_TAG = "ServerUtils";

  public static Double convertVersionFromString(String version) {
    if (version == null)
      throw new IllegalArgumentException("Argument 'version' must not be null");

    String[] versionNumbers = version.split("\\.");
    StringBuilder majorMinorVersion = new StringBuilder();
    if (versionNumbers.length > 0) {
      majorMinorVersion.append(versionNumbers[0]);
    }
    if (versionNumbers.length > 1) {
      majorMinorVersion.append(".").append(versionNumbers[1]);
    }
    return Double.parseDouble(majorMinorVersion.toString());
  }
}
