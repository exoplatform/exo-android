package org.exoplatform.exohybridapp;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by chautran on 22/12/2015.
 */
public class ServerListFragment extends Fragment {

  RecyclerView                  server_list_view;
  RecyclerView.LayoutManager    layoutManager;
  ServerAdapter                 adapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View fragmentLayout = inflater.inflate(R.layout.server_list_fragment, container, false);
    server_list_view = (RecyclerView) fragmentLayout.findViewById(R.id.server_list_view);
    adapter = new ServerAdapter(getActivity());
    layoutManager = new LinearLayoutManager(getActivity());
    server_list_view.setLayoutManager(layoutManager);
    server_list_view.setAdapter(adapter);
    return fragmentLayout;
  }

  @Override
  public void onResume() {
    super.onResume();
    adapter.onActivityResume();
  }
}
