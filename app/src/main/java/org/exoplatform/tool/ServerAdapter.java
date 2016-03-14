package org.exoplatform.tool;

/*
 * Copyright (C) 2003-${YEAR} eXo Platform SAS.
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.activity.WebViewActivity;
import org.exoplatform.model.Server;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by chautran on 28/11/2015. Adapter class for a list of servers.
 */
public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

  private Context           mContext;

  private ServerManagerImpl mServerManager;

  private ArrayList<Server> mServers;

  public ServerAdapter(Context context) {
    this.mContext = context;
    mServerManager = new ServerManagerImpl(context.getSharedPreferences(App.Preferences.FILE_NAME, 0));
    mServers = mServerManager.getServerList();
    if (mServers != null) {
      Collections.sort(mServers, Collections.reverseOrder());
    }
  }

  public ServerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    CardView itemLayoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.server_list_item,
                                                                                          parent,
                                                                                          false);
    ViewHolder holder = new ViewHolder(itemLayoutView, new ViewHolder.ServerClickListener() {
      @Override
      public void openWebView(View v, int position) {
        Intent intent = new Intent(ServerAdapter.this.mContext, WebViewActivity.class);
        intent.putExtra(WebViewActivity.INTENT_KEY_URL, ServerAdapter.this.mServers.get(position).getUrl().toString());
        ServerAdapter.this.mContext.startActivity(intent);
      }

      @Override
      public void deleteServer(View v, int position) {
        final String url = mServers.get(position).getUrl().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage("Delete " + url + "?");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Delete", new AlertDialog.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int which) {
            mServerManager.removeServer(url);
            mServers = mServerManager.getServerList();
            if (mServers != null) {
              Collections.sort(mServers, Collections.reverseOrder());
            }
            ServerAdapter.this.notifyDataSetChanged();
          }
        });
        builder.show();
      }
    });
    return holder;
  }

  public void onBindViewHolder(ViewHolder holder, final int position) {
    Server item = mServers.get(position);
    holder.server_name.setText(item.getShortUrl());
  }

  public int getItemCount() {
    if (mServers != null) {
      return mServers.size();
    } else {
      return 0;
    }
  }

  public void onActivityResume() {
    mServers = mServerManager.getServerList();
    if (mServers != null) {
      Collections.sort(mServers, Collections.reverseOrder());
    }
    notifyDataSetChanged();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public CardView            server_card;

    public TextView            server_name;

    public Button              delete_server_btn;

    public ServerClickListener listener;

    public ViewHolder(View itemLayoutView, ServerClickListener listener) {
      super(itemLayoutView);
      this.listener = listener;
      server_card = (CardView) itemLayoutView.findViewById(R.id.ListServer_CardView);
      server_name = (TextView) itemLayoutView.findViewById(R.id.ListServer_Item_Name);
      // delete_server_btn = (Button)
      // itemLayoutView.findViewById(R.id.delete_server);
      server_name.setOnClickListener(this);
      // delete_server_btn.setOnClickListener(this);
    }

    public void onClick(View v) {
      // if (v.getId() == delete_server_btn.getId()) {
      // listener.deleteServer(v, getAdapterPosition());
      // } else {
      listener.openWebView(v, getAdapterPosition());
      // }
    }

    public interface ServerClickListener {
      void openWebView(View v, int position);

      void deleteServer(View v, int position);
    }
  }
}
