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

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;

import org.exoplatform.App;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chautran on 22/11/2015. Utility class to store and retrieve
 * servers
 */
public class ServerManagerImpl implements ServerManager {

  SharedPreferences  preferences;

  private final Gson gson = new Gson();

  public ServerManagerImpl(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  public ArrayList<Server> getServerList() {
    String servers_json = preferences.getString(App.Preferences.SERVERS_STORAGE, null);
    ArrayList<Server> list = new ArrayList<>();
    if (servers_json != null) {
      Server[] servers = gson.fromJson(servers_json, ServersJSON.class).getServers();
      if (servers != null && servers.length != 0) {
        list.addAll(Arrays.asList(servers));
      }
    }
    return list;
  }

  public int getServerCount() {
    ArrayList servers = getServerList();
    if (servers != null)
      return servers.size();
    else
      return 0;
  }

  public boolean tribeServerExists() {
    ArrayList<Server> servers = getServerList();
    if (servers == null)
      return false;

    try {
      Server tribeSrv = new Server(new URL(App.TRIBE_URL));
      return servers.contains(tribeSrv);
    } catch (MalformedURLException e) {
      Log.d(this.getClass().getName(), e.getMessage());
    }
    return false;
  }

  public void addServer(Server newServer) {
    List<Server> servers = getServerList();
    if (servers == null || servers.size() == 0) {
      servers = new ArrayList<>();
      servers.add(newServer);
      save(servers);
    } else {
      int size = servers.size();
      for (int i = 0; i < size; i++) {
        // loop through the servers until we find the one we're creating
        // in order to edit it rather than duplicate it
        if (servers.get(i).equals(newServer)) {
          servers.set(i, newServer);
          save(servers);
          return;
        }
      }
      // if the url is not found in for loop.
      servers.add(newServer);
      save(servers);
    }
  }

  public void removeServer(URL url) {
    removeServer(url.toString());
  }

  public void removeServer(String url) {
    List<Server> servers = getServerList();
    if (servers != null && servers.size() > 0) {
      int size = servers.size();
      for (int i = 0; i < size; i++) {
        if (servers.get(i).getUrl().toString().equals(url)) {
          servers.remove(i);
          save(servers);
          break;
        }
      }
    }
  }

  public Server getLastVisitedServer() {
    List<Server> servers = getServerList();
    if (servers != null && servers.size() > 0) {
      int last = 0;
      int size = servers.size();
      for (int i = 1; i < size; i++) {
        if (servers.get(i).getLastVisited() > servers.get(last).getLastVisited()) {
          last = i;
        }
      }
      return servers.get(last);
    } else {
      return null;
    }
  }

  public void save(List<Server> servers) {
    Server[] arr = new Server[servers.size()];
    servers.toArray(arr);
    ServersJSON serversJSON = new ServersJSON();
    serversJSON.setServers(arr);
    String json = gson.toJson(serversJSON);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(App.Preferences.SERVERS_STORAGE, json);
    editor.apply();
  }

  @Override
  public Server getServerByUrl(@NonNull String url) {
    List<Server> servers = getServerList();
    if (servers == null)
      return null;
    int size = servers.size();
    Server oServer;
    try {
      oServer = new Server(new URL(url));
    } catch (MalformedURLException e) {
      // bad url given
      return null;
    }
    for (int i = 0; i < size; i++) {
      // loop through the servers until we find the one with the given url
      if (servers.get(i).equals(oServer)) {
        return servers.get(i);
      }
    }
    return null;
  }

  public class ServersJSON {

    public Server[] servers;

    public Server[] getServers() {
      return servers;
    }

    public void setServers(Server[] servers) {
      this.servers = servers;
    }
  }
}
