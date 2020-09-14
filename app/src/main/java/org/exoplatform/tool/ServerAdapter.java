package org.exoplatform.tool;

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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.Server;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by chautran on 28/11/2015. Adapter class for a list of servers.
 */
public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

  private ServerClickListener mListener;

  private ServerManagerImpl   mServerManager;

  private ArrayList<Server>   mServers;

  public ServerAdapter(@NonNull Context context, @NonNull ServerClickListener listener) {
    this.mListener = listener;
    mServerManager = new ServerManagerImpl(context.getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0));
    mServers = mServerManager.getServerList();
    Collections.sort(mServers, Collections.reverseOrder());
  }

  public ServerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    CardView itemLayoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.server_list_item,
                                                                                          parent,
                                                                                          false);
    return new ViewHolder(itemLayoutView);
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

  public class ViewHolder extends RecyclerView.ViewHolder {
    public CardView server_card;

    public TextView server_name;

    public ViewHolder(View itemLayoutView) {
      super(itemLayoutView);
      server_card = (CardView) itemLayoutView.findViewById(R.id.ListServer_CardView);
      server_name = (TextView) itemLayoutView.findViewById(R.id.ListServer_Item_Name);
      server_card.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          mListener.onClickServer(mServers.get(getAdapterPosition()));
        }
      });
    }
  }

  public interface ServerClickListener {
    void onClickServer(Server server);
  }
}
