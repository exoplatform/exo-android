package org.exoplatform.tool;

import android.util.Log;

import org.exoplatform.BuildConfig;
import org.exoplatform.model.Server;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Created by paristote on 2/24/16.
 */
public class ServerUtils {

    public static final String LOG_TAG = "ServerUtils";

    public static Double getPlatformVersionSync(Server server) {
        if (server == null) throw new IllegalArgumentException("Argument 'server' must not be null");

        Double version = -1.0;
        HttpURLConnection cnx = null;
        InputStream in = null;
        try {
            URL serverUrl = server.getUrl();
            URL plfInfo = new URL(String.format(Locale.US, "%s/rest/platform/info", serverUrl.toExternalForm()));
            cnx = (HttpURLConnection)plfInfo.openConnection();
            int responseCode = cnx.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                // handle one redirect
                String newLoc = cnx.getHeaderField("Location");
                if (newLoc != null) {
                    plfInfo = new URL(newLoc);
                    cnx = (HttpURLConnection)plfInfo.openConnection();
                    responseCode = cnx.getResponseCode();
                }
            }
            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                // if response code is in the 200-299 range (OK)
                in = cnx.getInputStream();
                JSONObject json = new JSONObject(readStream(in));
                String plfVer = json.getString("platformVersion");
                version = convertVersionFromString(plfVer);
            }
        } catch (JSONException | IOException | NumberFormatException e) {
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
            if (cnx != null) {
                cnx.disconnect();
            }
        }
        return version;
    }

    public static String readStream(InputStream in) throws IOException {
        if (in == null) throw new IllegalArgumentException("Argument 'in' must not be null");

        BufferedInputStream bis = new BufferedInputStream(in);
        byte[] buf = new byte[1024];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int length;
        while ((length = bis.read(buf)) != -1) {
            bytes.write(buf, 0, length);
        }
        bis.close();
        bytes.close();
        return bytes.toString("UTF-8");
    }

    public static Double convertVersionFromString(String version) {
        if (version == null) throw new IllegalArgumentException("Argument 'version' must not be null");

        String[] versionNumbers = version.split("\\.");
        StringBuffer majorMinorVersion = new StringBuffer();
        if (versionNumbers.length > 0) {
            majorMinorVersion.append(versionNumbers[0]);
        }
        if (versionNumbers.length > 1) {
            majorMinorVersion.append(".").append(versionNumbers[1]);
        }
        Double doubleVersion = Double.parseDouble(majorMinorVersion.toString());
        return doubleVersion;
    }

}
