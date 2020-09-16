package org.exoplatform.activity;

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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerAdapter;

/**
 * Activity that allows to edit / delete servers and view application
 * information
 */
public class SettingsActivity extends AppCompatActivity implements ServerAdapter.ServerClickListener {

  private ServerAdapter mServerAdapter;

  private TextView      mIntranetsHeader, mIntranetsEmptyView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);
    Toolbar toolbar = (Toolbar) findViewById(R.id.Settings_Toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null)
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    mIntranetsHeader = (TextView) findViewById(R.id.Settings_Header_Intranets);
    mIntranetsEmptyView = (TextView) findViewById(R.id.Settings_ServersRecyclerView_EmptyView);
    mIntranetsEmptyView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onClickEmptyView();
      }
    });

    TextView appVersion = (TextView) findViewById(R.id.Settings_ApplicationVersionTextView);
    appVersion.setText(BuildConfig.VERSION_NAME);

    RecyclerView serverListView = (RecyclerView) findViewById(R.id.Settings_ServersRecyclerView);
    mServerAdapter = new ServerAdapter(this, this);
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    serverListView.setLayoutManager(layoutManager);
    serverListView.setAdapter(mServerAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mServerAdapter.onActivityResume();
    setIntranetListView();
  }

  private void setIntranetListView() {
    if (mServerAdapter.getItemCount() == 0) {
      mIntranetsHeader.setText(R.string.SettingsActivity_Header_NoIntranet);
      mIntranetsEmptyView.setVisibility(View.VISIBLE);
    } else {
      mIntranetsHeader.setText(R.string.SettingsActivity_Header_Intranets);
      mIntranetsEmptyView.setVisibility(View.GONE);
    }
  }

  private void onClickEmptyView() {
    Intent newServer = new Intent(this, NewServerActivity.class);
    startActivity(newServer);
  }

  @Override
  public void onClickServer(Server server) {
    Intent editServer = new Intent(this, EditDeleteServerActivity.class);
    editServer.putExtra(EditDeleteServerActivity.INTENT_KEY_URL, server.getUrl().toString());
    startActivity(editServer);
  }
}
