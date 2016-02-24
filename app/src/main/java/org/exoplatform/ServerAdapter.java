package org.exoplatform;

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

import org.exoplatform.activity.WebViewActivity;
import org.exoplatform.model.Server;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by chautran on 28/11/2015.
 */
public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

  private Context             context;
  private ServerManagerImpl   serverManager;
  private ArrayList<Server>   servers;

  public ServerAdapter(Context context) {
    this.context = context;
    serverManager = new ServerManagerImpl(context.getSharedPreferences(App.SHARED_PREFERENCES_NAME, 0));
    servers = serverManager.getServerList();
    if (servers != null) {
      Collections.sort(servers, Collections.reverseOrder());
    }
  }

  public ServerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    CardView itemLayoutView = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.server_card, parent, false);
    ViewHolder holder = new ViewHolder(itemLayoutView, new ViewHolder.ServerClickListener() {
      @Override
      public void openWebView(View v, int position) {
        Intent intent = new Intent(ServerAdapter.this.context, WebViewActivity.class);
        intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, ServerAdapter.this.servers.get(position).getUrl().toString());
        ServerAdapter.this.context.startActivity(intent);
      }

      @Override
      public void deleteServer(View v, int position) {
        final String url = servers.get(position).getUrl().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage("Delete " + url + "?");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Delete", new AlertDialog.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int which) {
            serverManager.removeServer(url);
            servers = serverManager.getServerList();
            if (servers != null) {
              Collections.sort(servers, Collections.reverseOrder());
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
    Server item = servers.get(position);
    holder.server_name.setText(item.getShortUrl());
  }

  public int getItemCount() {
    if (servers != null) {
      return servers.size();
    } else {
      return 0;
    }
  }

  public void onActivityResume() {
    servers = serverManager.getServerList();
    if (servers != null) {
      Collections.sort(servers, Collections.reverseOrder());
    }
    notifyDataSetChanged();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public CardView             server_card;
    public TextView             server_name;
    public Button               delete_server_btn;
    public ServerClickListener  listener;

    public ViewHolder(View itemLayoutView, ServerClickListener listener) {
      super(itemLayoutView);
      this.listener = listener;
      server_card = (CardView) itemLayoutView.findViewById(R.id.server_card);
      server_name = (TextView) itemLayoutView.findViewById(R.id.server_name_view);
      delete_server_btn = (Button) itemLayoutView.findViewById(R.id.delete_server);
      server_name.setOnClickListener(this);
      delete_server_btn.setOnClickListener(this);
    }

    public void onClick(View v) {
      if (v.getId() == delete_server_btn.getId()) {
        listener.deleteServer(v, getAdapterPosition());
      } else {
        listener.openWebView(v, getAdapterPosition());
      }
    }

    public static interface ServerClickListener {
      void openWebView(View v, int position);
      void deleteServer(View v, int position);
    }
  }
}
