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

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Apr 22, 2015
 */
public class SocialSpace implements Parcelable {

  public String id;

  public String avatarUrl;

  public String displayName;

  public String groupId;

  public String identity;

  public SocialSpace() {
  }

  @Override
  public String toString() {
    return displayName;
  }

  /**
   * If the space has been renamed, it's still possible to retrieve the original
   * name from the groupId.<br/>
   * It works by extracting the last part of the groupId, e.g.
   *
   * <pre>
   * groupId = /spaces/old_name
   * original name = old_name
   * </pre>
   *
   * @return the original name, or the display name if extraction fails.
   */
  public String getOriginalName() {
    String origName = displayName;
    if (groupId != null) {
      int lastPart = groupId.lastIndexOf('/');
      if (lastPart >= 0 && lastPart < groupId.length()) {
        origName = groupId.substring(lastPart + 1);
      }
    }
    return origName;
  }

  /**
   * Extracts the identity id of the space, from the identity url.<br/>
   * e.g
   *
   * <pre>
   *     identity = http://serverrest/v1/social/identities/637bfea0c063c86a1bcc19a378af61da
   *     id = 637bfea0c063c86a1bcc19a378af61da
   * </pre>
   * 
   * @return the identity id of this space
   */
  public String getIdentityId() {
    String id = null;
    if (identity != null) {
      int lastPart = identity.lastIndexOf('/');
      if (lastPart >= 0 && lastPart < identity.length()) {
        id = identity.substring(lastPart + 1);
      }
    }
    return id;
  }

  /*
   * PARCEL
   */

  private SocialSpace(Parcel in) {
    readFromParcel(in);
  }

  public static final Creator<SocialSpace> CREATOR = new Creator<SocialSpace>() {
                                                     public SocialSpace createFromParcel(Parcel in) {
                                                       return new SocialSpace(in);
                                                     }

                                                     public SocialSpace[] newArray(int size) {
                                                       return new SocialSpace[size];
                                                     }
                                                   };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(id);
    dest.writeString(displayName);
    dest.writeString(groupId);
    dest.writeString(avatarUrl);
    dest.writeString(identity);
  }

  public void readFromParcel(Parcel in) {
    id = in.readString();
    displayName = in.readString();
    groupId = in.readString();
    avatarUrl = in.readString();
    identity = in.readString();
  }
}
