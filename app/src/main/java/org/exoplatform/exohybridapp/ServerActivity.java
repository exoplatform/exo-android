package org.exoplatform.exohybridapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by chautran on 25/11/2015.
 */
public class ServerActivity extends AppCompatActivity {

  RecyclerView                      server_list_view;
  RecyclerView.LayoutManager        layoutManager;
  ServerAdapter                     adapter;
  EditText                          url_input_txt;
  Toolbar                           toolbar;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.server);

    toolbar = (Toolbar) findViewById(R.id.server_toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().getThemedContext().setTheme(R.style.ActionBarTheme);

    url_input_txt = (EditText) findViewById(R.id.url_input_txt);
    /*url_input_txt.setOnKeyListener(new View.OnKeyListener() {
      @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
          submitUrl();
          return true;
        }
        return false;
      }
    });*/
    url_input_txt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL) {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return true;
          }
          if (event.getAction() == KeyEvent.ACTION_UP) {
            submitUrl();
            return true;
          }
        }
        if (actionId == EditorInfo.IME_ACTION_DONE) {
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
