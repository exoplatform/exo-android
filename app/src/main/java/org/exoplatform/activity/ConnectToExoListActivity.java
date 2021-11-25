package org.exoplatform.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManagerImpl;

import java.util.ArrayList;
import java.util.Collections;

public class ConnectToExoListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    public static RecyclerAdapter recyclerAdapter;
    public static ServerManagerImpl mServerManager;
    private ArrayList<Server>   serverList;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_exo_list);
        mServerManager = new ServerManagerImpl((ConnectToExoListActivity.this).getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0));
        serverList = mServerManager.getServerList();
        Collections.sort(serverList, Collections.reverseOrder());
        statusBarColor();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerAdapter = new RecyclerAdapter(this);
        recyclerView.setAdapter(recyclerAdapter);
        CustomLinearLayoutManager clm = new CustomLinearLayoutManager(ConnectToExoListActivity.this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(clm);
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, BoardingActivity.class);
        startActivity(intent);
    }

    private void reloadAllData(){
        // update data in our adapter
        recyclerAdapter.serverList.clear();
        recyclerAdapter.serverList.addAll(serverList);
        // fire the event
        recyclerAdapter.notifyDataSetChanged();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void statusBarColor(){
        getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color,this.getTheme()));
    }

    public static class CustomLinearLayoutManager extends LinearLayoutManager {
        public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }
        // it will always pass false to RecyclerView when calling "canScrollVertically()" method.
        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        recyclerAdapter.onActivityResume();
    }
}

