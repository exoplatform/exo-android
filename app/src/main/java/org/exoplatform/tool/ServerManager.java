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

import android.support.annotation.NonNull;

import org.exoplatform.model.Server;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chautran on 21/11/2015. Interface with the servers storage.
 */
public interface ServerManager {

  /**
   * @return the collection of servers.
   */
  @NonNull ArrayList<Server> getServerList();

  /**
   * adds a server to the store.
   */
  void addServer(Server newServer);

  /**
   * removes a server.
   */
  void removeServer(URL url);

  /**
   * removes a server.
   */
  void removeServer(String url);

  /**
   * Checks whether the eXo tribe server (community.exoplatform.com) exists
   * 
   * @return true if the eXo tribe server exists
   */
  boolean tribeServerExists();

  /**
   * @return the number of servers
   */
  int getServerCount();

  /**
   * @return the last visited server, null if the server list is empty.
   */
  Server getLastVisitedServer();

  /**
   * Saves the list of servers in the shared preferences storage
   * 
   * @param servers the list of servers to save
   */
  void save(List<Server> servers);

  /**
   * Finds the server with the given url
   * 
   * @param url the server's URL as a String
   * @return the Server with this url or null if no such server exists
   */
  Server getServerByUrl(String url);
}
