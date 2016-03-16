package org.exoplatform.fragment;

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

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.exoplatform.R;
import org.exoplatform.tool.ServerAdapter;

/**
 * Created by chautran on 22/12/2015. Fragment that displays a list of servers
 */
public class ServerListFragment extends Fragment {

  private ServerAdapter mServerAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View fragmentLayout = inflater.inflate(R.layout.server_list_fragment, container, false);
    RecyclerView serverListView = (RecyclerView) fragmentLayout.findViewById(R.id.ListServer_RecyclerView);
    if (getActivity() instanceof ServerAdapter.ServerClickListener)
      mServerAdapter = new ServerAdapter(getActivity(), (ServerAdapter.ServerClickListener) getActivity());
    else
      throw new IllegalArgumentException("This fragment's parent activity must implement ServerAdapter.ServerClickListener");
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
    serverListView.setLayoutManager(layoutManager);
    serverListView.setAdapter(mServerAdapter);
    return fragmentLayout;
  }

  @Override
  public void onResume() {
    super.onResume();
    mServerAdapter.onActivityResume();
  }
}
