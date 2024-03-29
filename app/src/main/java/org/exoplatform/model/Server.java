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
import android.util.Log;

import androidx.annotation.NonNull;

import org.exoplatform.App;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;

/**
 * Created by chautran on 21/11/2015. A server / intranet configured in the app.
 */
public class Server implements Comparable<Server>, Parcelable {

  private URL                         url;         // "http(s)://host(:port)"

  private Long                        lastVisited;

  private String                      lastLogin;   // can be used to store
                                                    // username

  private String                      lastPassword; // clear text

  private SealedObject                password;    // encrypted

  public static final Creator<Server> CREATOR = new Creator<Server>() {
                                                @Override
                                                public Server createFromParcel(Parcel source) {
                                                  Server srv = null;
                                                  try {
                                                    srv = new Server(source);
                                                  } catch (MalformedURLException e) {
                                                    Log.e(this.getClass().getName(), e.getMessage(), e);
                                                  }
                                                  return srv;
                                                }

                                                @Override
                                                public Server[] newArray(int size) {
                                                  return new Server[size];
                                                }
                                              };

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    try {
      this.url = new URL(format(url));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(String.format("Given URL is incorrect (%s)", url));
    }
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

  public void setLastPassword(String lastPassword) {
    this.lastPassword = lastPassword;
    // this.password = encrypt(lastPassword);
  }

  public String getLastPassword() {
    // if (lastPassword == null) {
    // lastPassword = decrypt(password);
    // }
    return lastPassword;
  }

  private SealedObject encrypt(String clearText) {
    SealedObject encrypted = null;
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
      KeyGenerator keygen = KeyGenerator.getInstance("AES");
      c.init(Cipher.ENCRYPT_MODE, keygen.generateKey());
      encrypted = new SealedObject(clearText, c);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | IOException | IllegalBlockSizeException | InvalidKeyException e) {
      e.printStackTrace();
    }
    return encrypted;
  }

  private String decrypt(SealedObject encrypted) {
    String clearText = null;
    try {
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
      clearText = encrypted.getObject(c).toString();
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | ClassNotFoundException | IllegalBlockSizeException
        | BadPaddingException | IOException e) {
      e.printStackTrace();
    }
    return clearText;
  }

  public Server(URL url, Long lastVisited, String lastLogin) throws MalformedURLException {
    this.url = new URL(format(url));
    this.lastVisited = lastVisited;
    this.lastLogin = lastLogin;
  }

  public Server(URL url) throws MalformedURLException {
    this(url, -1L, "");
  }

  public Server(URL url, Long lastVisited) throws MalformedURLException {
    this(url, lastVisited, "");
  }

  public Server(Parcel in) throws MalformedURLException {
    readFromParcel(in);
  }

  public String getShortUrl() {
    return (url != null) ? url.getHost() : "";
  }

  public boolean isExoTribe() {
    return App.TRIBE_URL.contains(this.getShortUrl());
  }

  public static String format(URL url) {
    String protocol = url.getProtocol();
    String host = url.getHost();
    String explicitPort = url.getPort() == -1 ? "" : ":" + url.getPort();
    String url_ = protocol + "://" + host + explicitPort;
    return url_.toLowerCase();
  }

  @Override
  public int compareTo(@NonNull Server another) {
    return (getLastVisited().compareTo(another.getLastVisited()));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Server))
      return false;
    String thisShortUrl = getShortUrl();
    String otherShortUrl = ((Server) o).getShortUrl();
    return thisShortUrl.equalsIgnoreCase(otherShortUrl);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(url.toString());
    dest.writeLong(lastVisited);
    dest.writeString(lastLogin);
    // TODO store encrypted password
  }

  public void readFromParcel(Parcel in) throws MalformedURLException {
    url = new URL(in.readString());
    lastVisited = in.readLong();
    lastLogin = in.readString();
  }

}
