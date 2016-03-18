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

import org.exoplatform.model.SocialActivity;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 17, 2015
 */
public abstract class Action {

  protected final String   LOG_TAG = this.getClass().getName();

  protected SocialActivity postInfo;

  protected ActionListener listener;

  protected void check() {
    if (postInfo == null)
      throw new IllegalArgumentException("Cannot pass null as the SocialPostInfo argument");
    if (listener == null)
      throw new IllegalArgumentException("Cannot pass null as the ActionListener argument");
  }

  protected abstract boolean doExecute();

  protected boolean execute() {
    check();
    return doExecute();
  }

  public interface ActionListener {

    boolean onSuccess(String message);

    boolean onError(String error);

  }

}
