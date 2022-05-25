package org.exoplatform.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManagerImpl;

import java.util.ArrayList;
import java.util.Collections;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    public static final String INTENT_KEY_URL = "SERVER_URL";

    public ArrayList<Server>   serverList;
    private final Activity activity;
    private final ServerManagerImpl mServerManager;
    private final ActionDialog dialog;
    private final CheckConnectivity checkConnectivity;

    int positionToDelete;
    public RecyclerAdapter(Activity activity) {
        this.activity = activity;
        mServerManager = new ServerManagerImpl(activity.getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0));
        this.serverList = mServerManager.getServerList();
        Collections.sort(serverList, Collections.reverseOrder());
        dialog = new ActionDialog(R.string.SettingsActivity_Title_DeleteConfirmation,
                R.string.SettingsActivity_Message_DeleteConfirmation, R.string.Word_Delete, activity);
        checkConnectivity = new CheckConnectivity(activity);
        dialog.deleteAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteServer(positionToDelete);
            }
        });

        dialog.cancelAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            //Inflating recycle view item layout
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_row_item, parent, false);
            return new ItemViewHolder(itemView);
        } else if (viewType == TYPE_FOOTER) {
            //Inflating footer view
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.footer_item_list, parent, false);
            return new FooterViewHolder(itemView);
        } else return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof ItemViewHolder) {
            final Server server = serverList.get(position);
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.serverDomainTextView.setText(server.getShortUrl());
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(activity);
            // Read previous value. If not found, use 0 as default value.
            final String urlKey = server.getUrl().getProtocol() + "://" + server.getShortUrl();
            System.out.println("urlKey Adapter ===> " + urlKey);
            int count = shared.getInt(urlKey, 0);
            System.out.println("count received Adapter ===> " + count);
            boolean isSessionTimedOut = shared.getBoolean("isSessionTimedOut",false);
            if (isSessionTimedOut && count != 0){
                itemViewHolder.notificationTextView.setText(String.valueOf(count));
            }else{
                itemViewHolder.notificationTextView.setVisibility(View.GONE);
            }
            Bitmap bm = retriveImageDecodedBase64(server) ;
            if (bm != null) {
                itemViewHolder.avatarImageViewServer.setImageBitmap(bm);
            }else{
                itemViewHolder.avatarImageViewServer.setBackgroundResource(R.drawable.exo_logo);
            }
            itemViewHolder.deleteServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    positionToDelete = position;
                    dialog.showDialog();
                }
            });

            itemViewHolder.server_card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkConnectivity.isConnectedToInternet()){
                        SharedPreferences.Editor editor = shared.edit();
                        editor.putInt(urlKey, 0);
                        editor.apply();
                        Intent intent = new Intent(activity, WebViewActivity.class);
                        intent.putExtra(WebViewActivity.INTENT_KEY_URL, server.getUrl().toString());
                        activity.startActivity(intent);
                    }
                }
            });
        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footerHolder = (FooterViewHolder) holder;
            footerHolder.addServerTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, BoardingActivity.class);
                    intent.putExtra("isFromInstance",true);
                    activity.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == serverList.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        if (serverList != null) {
            return serverList.size() + 1;
        } else {
            return 1;
        }
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView addServerTextView;
        public FooterViewHolder(View view) {
            super(view);
            addServerTextView = (TextView) view.findViewById(R.id.add_server_text_view);
        }
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView serverDomainTextView,notificationTextView;
        ImageView deleteServer,avatarImageViewServer;
        public CardView server_card;

        public ItemViewHolder(View itemView) {
            super(itemView);
            server_card = (CardView) itemView.findViewById(R.id.ServerCardViewT);
            serverDomainTextView = (TextView) itemView.findViewById(R.id.textViewDomain);
            notificationTextView = (TextView) itemView.findViewById(R.id.notificationTextView);
            avatarImageViewServer = (ImageView) itemView.findViewById(R.id.avatarServerImageView);
            deleteServer = (ImageView) itemView.findViewById(R.id.imageViewDelete);
        }
    }

    private void deleteServer(final int position) {
        final String serverUrl = serverList.get(position).getUrl().toString();
        mServerManager.removeServer(serverUrl);
        serverList.remove(position);
        notifyDataSetChanged();
        if (serverList.isEmpty()) {
            Intent intent = new Intent(activity, BoardingActivity.class);
            activity.startActivity(intent);
        }
        dialog.dismiss();
    }

    public void onActivityResume() {
        serverList = mServerManager.getServerList();
        if (serverList != null) {
            Collections.sort(serverList, Collections.reverseOrder());
        }
        notifyDataSetChanged();
    }

    // method for base64 to bitmap
    public Bitmap retriveImageDecodedBase64(Server server) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(RecyclerAdapter.this.activity);
        String photo = shared.getString(server.getShortUrl(), "photo");
        assert photo != null;
        if(!photo.equals("photo")){
            byte[] decodedByte = Base64.decode(photo, 0);
            return BitmapFactory
                    .decodeByteArray(decodedByte, 0, decodedByte.length);
        }
        return null;
    }
}
