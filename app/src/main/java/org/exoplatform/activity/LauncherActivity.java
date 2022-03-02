package org.exoplatform.activity;

import static org.exoplatform.activity.WebViewActivity.INTENT_KEY_URL;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import org.exoplatform.R;
import org.exoplatform.App;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManagerImpl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LauncherActivity extends AppCompatActivity {

    private CheckConnectivity checkConnectivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkConnectivity = new CheckConnectivity(LauncherActivity.this);
        if (savedInstanceState == null) {
            try {
                bypassIfRecentlyVisited();
            } catch (IOException e) {
                Log.e("error", String.valueOf(e));
            }
        }
    }

    private void bypassIfRecentlyVisited() throws IOException {
        SharedPreferences prefs = App.Preferences.get(this);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(LauncherActivity.this);
        Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
        SharedPreferences.Editor editor = shared.edit();
        Uri uri = getIntent().getData();

        try {
            setWakeUpActivityRoot(new LauncherActivity.ResultHandler<Boolean>() {
                @Override
                public void onSuccess(Boolean isSessionsAlive) {
                    if (isSessionsAlive) {
                        String url = getIntent().getStringExtra(INTENT_KEY_URL);
                        if(url != null && !url.equals("")) {
                            openWebViewWithURL(url);
                        } else {
                            if (uri != null) {
                                Log.d("deepLink url ======>",uri.toString());
                                openWebViewWithURL(uri.toString());
                            }else {
                                Intent intent = new Intent(LauncherActivity.this, ConnectToExoListActivity.class);
                                startActivity(intent);
                                editor.putBoolean("isSessionTimedOut",true);
                                editor.apply();
                            }
                        }
                    }else{
                        Intent intent = new Intent(LauncherActivity.this, ConnectToExoListActivity.class);
                        startActivity(intent);
                        editor.putBoolean("isSessionTimedOut",true);
                        editor.apply();
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Log.e("error", String.valueOf(e));
                    rootOnboardingScreen();
                }
            });
        } catch (IOException e) {
            Log.e("error", String.valueOf(e));
            rootOnboardingScreen();
        }
    }

    private void openWebViewWithURL(String url) {
        if (url == null)
            throw new IllegalArgumentException("URL must not be null");
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(INTENT_KEY_URL, url);
        this.startActivity(intent);
    }

    private void rootOnboardingScreen(){
       Intent intent = new Intent(this, BoardingActivity.class);
       this.startActivity(intent);
    }

    private void setWakeUpActivityRoot(final LauncherActivity.ResultHandler<Boolean> handler) throws IOException {
        if (checkConnectivity.isConnectedToInternet()) {
            SharedPreferences prefs = App.Preferences.get(this);
            Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
            String username = prefs.getString("connectedUsername", "username");
            String cookies = prefs.getString("connectedCookies", "cookies");
            if (serverToConnect != null) {
                final String url = App.getCheckSessionURL(serverToConnect.getUrl().getProtocol(), serverToConnect.getShortUrl(), username);
                Log.d("url =========> ", url);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Cookie", cookies)
                                    .url(url)
                                    .build();
                            Response httpResponse = client.newCall(request).execute();
                            if (httpResponse.code() == 200) {
                                handler.onSuccess(true);
                            } else {
                                handler.onSuccess(false);
                            }
                        } catch (Exception e) {
                            Log.e("error ========== ", String.valueOf(e));
                            handler.onFailure(e);
                        }
                    }
                });
                thread.start();
            }else{
                rootOnboardingScreen();
            }
        }
    }

    public interface ResultHandler<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }
}