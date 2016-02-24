package org.exoplatform.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.exoplatform.R;

/**
 * Created by chautran on 25/11/2015.
 */
public class ServerActivity extends AppCompatActivity {

  Toolbar                           toolbar;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.server);

    toolbar = (Toolbar) findViewById(R.id.server_toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().getThemedContext().setTheme(R.style.ActionBarTheme);
  }

  @Override
  public void onResume() {
    super.onResume();
  }
}
