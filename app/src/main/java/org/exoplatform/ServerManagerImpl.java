package org.exoplatform;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by chautran on 22/11/2015.
 */
public class ServerManagerImpl implements ServerManager {

  SharedPreferences           preferences;
  public static final Gson    gson = new Gson();

  public ServerManagerImpl(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  public ArrayList<Server> getServerList() {
    String servers_json = preferences.getString(App.PREF_SERVERS_STORAGE, null);
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
    editor.putString(App.PREF_SERVERS_STORAGE, json);
    editor.commit();
  }

    @Override
    public void verifyServer(Server srv, final VerifyServerCallback callback) {
        new AsyncTask<Server, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Server... params) {
                Boolean result = false;
                if (params.length > 0) {
                    Double plfVersion = ServerUtils.getPlatformVersionSync(params[0]);
                    result = (plfVersion >= App.MIN_PLATFORM_VERSION_SUPPORTED);
                }
                return result;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                callback.result(result);
                super.onPostExecute(result);
            }
        }.execute(srv);
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
