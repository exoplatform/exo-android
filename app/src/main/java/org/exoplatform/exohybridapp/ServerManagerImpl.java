package org.exoplatform.exohybridapp;

import android.content.SharedPreferences;
import com.google.gson.Gson;

import org.exoplatform.exohybridapp.model.Server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chautran on 22/11/2015.
 */
public class ServerManagerImpl implements ServerManager {

  public static final String  HISTORY_PKEY = "HISTORY";
  SharedPreferences           preferences;
  public static final Gson    gson = new Gson();

  public ServerManagerImpl(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  public ArrayList<Server> getServerList() {
    String servers_json = preferences.getString(HISTORY_PKEY, null);
    if (servers_json == null) {
      return null;
    } else {
      Server[] servers = gson.fromJson(servers_json, ServersJSON.class).getServers();
      if (servers != null && servers.length != 0) {
        return new ArrayList<>(Arrays.asList(servers));
      } else {
        return null;
      }
    }
  }

  public void addServer(Server server) {
    List<Server> servers = getServerList();
    if (servers == null || servers.size() == 0) {
      servers = new ArrayList<>();
      servers.add(server);
      save(servers);
    } else {
      int size = servers.size();
      for (int i = 0; i < size; i++) {
        if (servers.get(i).getUrl().toString().equals(server.getUrl().toString())) {
          if (server.getLastVisited() > servers.get(i).getLastVisited()) {
            servers.set(i, server);
            save(servers);
          }
          return;
        }
      }
      //if the url is not found in for loop.
      servers.add(server);
      save(servers);
    }
  }

  public void removeServer(URL url) {
    List<Server> servers = getServerList();
    if (servers != null && servers.size() > 0) {
      int size = servers.size();
      for (int i = 0; i < size; i++) {
        if (servers.get(i).getUrl().toString().equals(Server.format(url))) {
          servers.remove(i);
          save(servers);
          break;
        }
      }
    }
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
    editor.putString(HISTORY_PKEY, json);
    editor.commit();
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
