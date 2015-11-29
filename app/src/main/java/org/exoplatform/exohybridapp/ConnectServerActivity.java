package org.exoplatform.exohybridapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.exoplatform.exohybridapp.model.Server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by chautran on 21/11/2015.
 */
public class ConnectServerActivity extends AppCompatActivity {

  TextView            defaultServerView;
  ServerManagerImpl   serverManager;
  Server              defaultServer;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.connect_server_layout);
    defaultServerView = (TextView)findViewById(R.id.default_server);
    serverManager = new ServerManagerImpl(getSharedPreferences(App.SHARED_PREFERENCES_NAME, 0));

    defaultServer = serverManager.getLastVisitedServer();
    if (defaultServer == null) {
      try {
        defaultServer = new Server(new URL(App.DEFAULT_SERVER));
      } catch (MalformedURLException e) {
        Log.d(this.getClass().getName(),e.getMessage());
      }
    }
    defaultServerView.setText(defaultServer.getShortUrl());

    defaultServerView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String url = defaultServer.getUrl();
        Intent intent = new Intent(ConnectServerActivity.this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, url);
        ConnectServerActivity.this.startActivity(intent);
      }
    });
  }

  public void openServerList(View v) {
    Intent intent = new Intent(this, ServerActivity.class);
    startActivity(intent);
  }
}
