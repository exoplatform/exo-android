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

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.model.PlatformInfo;
import org.exoplatform.model.Server;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Philippe on 2/24/16. Utilities methods to use with servers
 */
public class ServerUtils {

  public static final String LOG_TAG = "ServerUtils";

  public interface ServerVerificationCallback {
    void onVerificationStarted();

    void onServerValid(Server server);

    void onServerNotSupported();

    void onServerInvalid();
  }

  public static void verifyUrl(@NonNull String urlStr, @NonNull
  final ServerVerificationCallback callback) {
    String url = urlStr.trim();
    if (!(url.indexOf("http://") == 0) && !(url.indexOf("https://") == 0)) {
      url = "http://" + url;
    }

    callback.onVerificationStarted();
    try {
      if (Patterns.WEB_URL.matcher(url).matches()) {
        final Server server = new Server(new URL(url));
        Retrofit retrofit = new Retrofit.Builder().baseUrl(server.getUrl().toString())
                                                  .addConverterFactory(GsonConverterFactory.create())
                                                  .client(ExoHttpClient.getInstance())
                                                  .build();
        PlatformRestService platformService = retrofit.create(PlatformRestService.class);
        platformService.getInfo().enqueue(new Callback<PlatformInfo>() {
          @Override
          public void onResponse(Call<PlatformInfo> call, Response<PlatformInfo> response) {
            if (response.isSuccessful()) {
              try {
                // This will be the URL that was finally called after any HTTP redirection(s)
                URL finalUrl = new URL(response.raw().request().url().toString());
                // Update the server URL to the final value
                server.setUrl(finalUrl);
              } catch (MalformedURLException ignored) {}
              Double plfVersion = ServerUtils.convertVersionFromString(response.body().platformVersion);
              if (plfVersion >= App.Platform.MIN_SUPPORTED_VERSION) {
                callback.onServerValid(server);
              } else {
                callback.onServerNotSupported();
              }
            } else {
              callback.onServerInvalid();
            }
          }

          @Override
          public void onFailure(Call<PlatformInfo> call, Throwable t) {
            Log.e(LOG_TAG, "Unable to retrieve platform information", t);
            callback.onServerInvalid();
          }
        });
      } else {
        callback.onServerInvalid();
      }
    } catch (MalformedURLException e) {
      callback.onServerInvalid();
    }

  }

  public static ProgressDialog savingServerDialog(@NonNull Context context) {
    ProgressDialog progressDialog = new ProgressDialog(context);
    progressDialog.setMessage(context.getString(R.string.ServerManager_Message_SavingServer));
    progressDialog.setCancelable(false);
    return progressDialog;
  }

  public static AlertDialog dialogWithTitleAndMessage(Context context, int titleId, int messageId) {
    return new AlertDialog.Builder(context).setTitle(titleId)
                                           .setMessage(messageId)
                                           .setNeutralButton(R.string.Word_OK, null)
                                           .create();
  }

  public static Double convertVersionFromString(String version) {
    if (version == null)
      throw new IllegalArgumentException("Argument 'version' must not be null");

    String[] versionNumbers = version.split("\\.");
    StringBuilder majorMinorVersion = new StringBuilder();
    if (versionNumbers.length > 0) {
      majorMinorVersion.append(versionNumbers[0]);
    }
    if (versionNumbers.length > 1) {
      majorMinorVersion.append(".").append(versionNumbers[1]);
    }
    return Double.parseDouble(majorMinorVersion.toString());
  }

  public static boolean legacyServersExist(Context context) {
    if (context == null)
      throw new IllegalArgumentException("Argument 'context' must not be null");

    boolean exists = false;
    try {
      // Use context.getFilesDir() with File.exists() instead ?
      FileInputStream fis = context.openFileInput(App.Preferences.EXO_2X_SERVERS_STORAGE);
      exists = true;
      fis.close();
    } catch (IOException ignored) {
    }
    return exists;
  }

  public static List<Server> loadLegacyServerList(Context context) {
    FileInputStream fis = null;
    List<Server> serverList = new ArrayList<>();
    try {
      fis = context.openFileInput(App.Preferences.EXO_2X_SERVERS_STORAGE);
      DocumentBuilderFactory doc_build_fact = DocumentBuilderFactory.newInstance();
      DocumentBuilder doc_builder = doc_build_fact.newDocumentBuilder();
      Document obj_doc = doc_builder.parse(fis);

      if (null != obj_doc) {
        org.w3c.dom.Element feed = obj_doc.getDocumentElement();
        NodeList obj_nod_list = feed.getElementsByTagName("server");

        for (int i = 0; i < obj_nod_list.getLength(); i++) {
          Node itemNode = obj_nod_list.item(i);
          if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
            Element itemElement = (Element) itemNode;
            String serverUrl = itemElement.getAttribute("serverUrl");
            String username = itemElement.getAttribute("username");
            long lastLoginDate = -1L;
            try {
              String logTime = itemElement.getAttribute("lastLogin");
              if (logTime != null)
                lastLoginDate = Long.parseLong(logTime);
            } catch (NumberFormatException ignored) {
            }
            Server server = new Server(new URL(serverUrl), lastLoginDate, username);
            serverList.add(server);
          }
        }
      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      if (BuildConfig.DEBUG)
        Log.d(LOG_TAG, e.getMessage(), e);
    } finally {
      if (fis != null)
        try {
          fis.close();
        } catch (IOException e) {
          if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, e.getMessage(), e);
        }
    }
    return serverList;
  }
}
