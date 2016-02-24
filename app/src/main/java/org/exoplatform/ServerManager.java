package org.exoplatform;

import org.exoplatform.model.Server;

import java.net.URL;
import java.util.ArrayList;

/**
 * Created by chautran on 21/11/2015.
 */
public interface ServerManager {

  /**
   * @return the collection of servers.
   */
  public ArrayList<Server> getServerList();

  /**
   * adds a server to the store.
   */
  public void addServer(Server server);

  /**
   * removes a server.
   */
  public void removeServer(URL url);

  /**
   * removes a server.
   */
  public void removeServer(String url);

  /**
   * @return the last visited server, null if the server list is empty.
   */
  public Server getLastVisitedServer();

  public void verifyServer(Server srv, VerifyServerCallback callback);

  public static interface VerifyServerCallback {
      public void result(boolean correct);
  }
}
