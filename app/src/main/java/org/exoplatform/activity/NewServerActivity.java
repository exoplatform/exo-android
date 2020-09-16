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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerAdapter;

/**
 * Created by chautran on 25/11/2015. Activity to create a new server or select
 * an existing one.
 */
public class NewServerActivity extends AppCompatActivity implements ServerAdapter.ServerClickListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_server);

    Toolbar toolbar = (Toolbar) findViewById(R.id.New_Server_Toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null)
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public void onClickServer(Server server) {
    Intent intent = new Intent(this, WebViewActivity.class);
    intent.putExtra(WebViewActivity.INTENT_KEY_URL, server.getUrl().toString());
    startActivity(intent);
  }
}
