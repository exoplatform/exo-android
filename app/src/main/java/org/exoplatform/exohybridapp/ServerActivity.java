package org.exoplatform.exohybridapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

/**
 * Created by chautran on 25/11/2015.
 */
public class ServerActivity extends AppCompatActivity {

  RecyclerView                      server_list_view;
  RecyclerView.LayoutManager        layoutManager;
  ServerAdapter                     adapter;
  EditText                          url_input_txt;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.server_layout);

    url_input_txt = (EditText) findViewById(R.id.url_input_txt);
    url_input_txt.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
          submitUrl();
          return true;
        }
        return false;
      }
    });

    server_list_view = (RecyclerView) findViewById(R.id.server_list_view);
    adapter = new ServerAdapter(this);
    layoutManager = new LinearLayoutManager(this);
    server_list_view.setLayoutManager(layoutManager);
    server_list_view.setAdapter(adapter);

    //
  }

  public void onClickBack(View v) {
    Intent intent = new Intent(this, ConnectServerActivity.class);
    startActivity(intent);
  }

  public void submitUrl() {
    String url = url_input_txt.getText().toString();
    if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
      url = "http://" + url;
    }
    Intent intent = new Intent(this, WebViewActivity.class);
    intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, url);
    startActivity(intent);
  }

  @Override
  public void onResume() {
    super.onResume();
    adapter.onActivityResume();
  }
}
