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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManager;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.ServerUtils;

public class EditDeleteServerActivity extends AppCompatActivity {

  public static final String INTENT_KEY_URL = "SERVER_URL";

  private ServerManager      mServerManager;

  private String             mOriginalUrl;

  private EditText           mNewUrlField;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_delete_server);

    Toolbar toolbar = (Toolbar) findViewById(R.id.Settings_Edit_Toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null)
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    if (getIntent() != null && getIntent().hasExtra(INTENT_KEY_URL)) {
      // keep the original url in memory to edit/delete this server
      mOriginalUrl = getIntent().getStringExtra(INTENT_KEY_URL);
      mNewUrlField = (EditText) findViewById(R.id.Settings_Edit_UrlField);
      mNewUrlField.setText(mOriginalUrl);
      mServerManager = new ServerManagerImpl(getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0));
      Button deleteButton = (Button) findViewById(R.id.Settings_Delete_Button);
      deleteButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          deleteServer();
        }
      });
    } else {
      // stop there
      throw new IllegalArgumentException("Missing String Extra with key EditDeleteServerActivity.INTENT_KEY_URL");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.edit_server, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.EditServer_Menu_Save) {
      String url = mNewUrlField.getText().toString().trim();
      final ProgressDialog progressDialog = ServerUtils.savingServerDialog(this);
      ServerUtils.verifyUrl(url, new ServerUtils.ServerVerificationCallback() {
        @Override
        public void onVerificationStarted() {
          progressDialog.show();
        }

        @Override
        public void onServerValid(Server server) {
          saveServer(server);
          progressDialog.dismiss();
        }

        @Override
        public void onServerNotSupported() {
          progressDialog.dismiss();
          ServerUtils.dialogWithTitleAndMessage(EditDeleteServerActivity.this,
                                                R.string.ServerManager_Error_TitleVersion,
                                                R.string.ServerManager_Error_PlatformVersionNotSupported).show();
        }

        @Override
        public void onServerInvalid() {
          progressDialog.dismiss();
          ServerUtils.dialogWithTitleAndMessage(EditDeleteServerActivity.this,
                                                R.string.ServerManager_Error_TitleIncorrect,
                                                R.string.ServerManager_Error_IncorrectUrl).show();
        }

        @Override
        public void onConnectionError() {

        }
      });
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void saveServer(Server server) {
    Server originalServer = mServerManager.getServerByUrl(mOriginalUrl);
    Server editedServer = mServerManager.getServerByUrl(server.getUrl().toString());
    if (editedServer != null && !editedServer.equals(originalServer)) {
      // "new" url exists as another server
      // update the existing server with potential new protocol or port
      editedServer.setUrl(server.getUrl());
      // delete the current server
      mServerManager.removeServer(mOriginalUrl);
      // save the edited server
      mServerManager.addServer(editedServer);
    } else if (originalServer != null) {
      // replace the original url by the new one, but keep other information
      // about the server
      // (last username and last login time)
      originalServer.setUrl(server.getUrl());
      mServerManager.addServer(originalServer);
    } else {
      // odd, couldn't find the server we're editing... saving a new one
      mServerManager.addServer(server);
    }
    finish();
  }

  private void deleteServer() {

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.SettingsActivity_Title_DeleteConfirmation);
    builder.setMessage(R.string.SettingsActivity_Message_DeleteConfirmation);
    builder.setNegativeButton(R.string.Word_Cancel, null);
    builder.setPositiveButton(R.string.Word_OK, new AlertDialog.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        mServerManager.removeServer(mOriginalUrl);
        finish(); // go back to the settings activity
      }
    });
    builder.show();
  }
}
