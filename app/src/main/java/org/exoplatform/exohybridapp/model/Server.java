package org.exoplatform.exohybridapp.model;

import java.io.Serializable;
import java.net.URL;
import java.util.Comparator;

/**
 * Created by chautran on 21/11/2015.
 */
public class Server implements Serializable, Comparable<Server> {

  public static final String  UUID = "42L";

  private String              url;              //"http(s)://host(:port)"
  private Long                lastVisited;
  private String              lastLogin;        //can be used to store username

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Long getLastVisited() {
    return lastVisited;
  }

  public void setLastVisited(Long lastVisited) {
    this.lastVisited = lastVisited;
  }

  public String getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(String lastLogin) {
    this.lastLogin = lastLogin;
  }

  public Server(URL url, Long lastVisited, String lastLogin) {
    this.url = format(url);
    this.lastVisited = lastVisited;
    this.lastLogin = lastLogin;
  }

  public Server(URL url) {
    this(url, -1L, "");
  }

  public Server(URL url, Long lastVisited) {
    this(url, lastVisited, "");
  }

  public String getShortUrl() {
    int i = url.lastIndexOf("/");
    return url.substring(i + 1);
  }

  public static String format(URL url) {
    String protocol = url.getProtocol();
    String host = url.getHost();
    String explicitPort = url.getPort() == -1 ? "" : ":" + Integer.toString(url.getPort());
    String url_ = protocol + "://" + host + explicitPort;
    return url_.toLowerCase();
  }

  @Override
  public int compareTo(Server another) {
    return (getLastVisited().compareTo(another.getLastVisited()));
  }
}
