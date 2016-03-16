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

import android.util.Log;

import org.exoplatform.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Created by paristote on 3/7/16. Information about a document (file).
 */
public class DocumentInfo {
  public String      documentName;

  public long        documentSizeKb;

  public InputStream documentData;

  public String      documentMimeType;

  public int         orientationAngle = 0; // ROTATION_0

  @Override
  public String toString() {
    return String.format(Locale.US, "File %s [%s - %s KB]", documentName, documentMimeType, documentSizeKb);
  }

  public void closeDocStream() {
    if (documentData != null)
      try {
        documentData.close();
      } catch (IOException e) {
        if (BuildConfig.DEBUG)
          Log.d("", e.getMessage(), e);
      }
  }
}
