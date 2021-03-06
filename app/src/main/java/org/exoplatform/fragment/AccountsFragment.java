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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.fragment.app.ListFragment;

import com.squareup.picasso.Picasso;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.tool.ServerManager;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.activity.ShareExtensionActivity;
import org.exoplatform.model.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Philippe Aristote paristote@exoplatform.com
 * @since Jun 9, 2015
 */
public class AccountsFragment extends ListFragment implements SimpleAdapter.ViewBinder {

  public static final String      ACCOUNTS_FRAGMENT = "accounts_fragment";

  private static AccountsFragment instance;

  List<Server>                    mServerList;

  public AccountsFragment() {
  }

  public static AccountsFragment getFragment() {
    if (instance == null) {
      instance = new AccountsFragment();
    }
    return instance;
  }

  @Override
  public boolean setViewValue(View view, Object data, String textRepresentation) {
    final Server acc = (Server) data;

    TextView server = (TextView) view.findViewById(R.id.share_account_item_server_url);
    server.setText(acc.getShortUrl());

    TextView username = (TextView) view.findViewById(R.id.share_account_item_username);
    if (acc.getLastLogin() == null || acc.getLastLogin().isEmpty())
      username.setVisibility(View.GONE);
    else
      username.setText(acc.getLastLogin());

    ImageView icon = (ImageView) view.findViewById(R.id.share_account_item_icon);
    if (acc.equals(getShareActivity().getActivityPost().ownerAccount)) {
      Picasso.with(getActivity()).load(R.drawable.icon_check_circle_grey).into(icon);
    } else {
      Picasso.with(getActivity()).load(R.drawable.empty_drawable).into(icon);
    }

    if (view.getId() == R.id.share_account_item_layout) {
      // Set the click listeners on the whole cell
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (acc.getLastLogin() != null && !"".equals(acc.getLastLogin()) && acc.getLastPassword() != null
              && !"".equals(acc.getLastPassword())) {
            onSelectAccount(acc);
          } else {
            onSignInAccount(acc);
          }
        }
      });
      view.setOnLongClickListener(new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          onSignInAccount(acc);
          return true;
        }
      });
    }
    return true;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.share_extension_accounts_fragment, container, false);
  }

  @Override
  public void onResume() {
    ServerManager serverManager = new ServerManagerImpl(getContext().getSharedPreferences(App.Preferences.PREFS_FILE_NAME, 0));
    mServerList = serverManager.getServerList();
    Collections.sort(mServerList, Collections.reverseOrder());
    ArrayList<Map<String, Server>> data = new ArrayList<>(mServerList.size());
    for (Server account : mServerList) {
      Map<String, Server> map = new HashMap<>();
      map.put("ACCOUNT_DATA", account);
      data.add(map);
    }
    String[] from = { "ACCOUNT_DATA" };
    int[] to = new int[] { R.id.share_account_item_layout };
    SimpleAdapter adapter = new SimpleAdapter(getContext(), data, R.layout.share_extension_account_item, from, to);
    adapter.setViewBinder(this);
    setListAdapter(adapter);
    super.onResume();
  }

  @Override
  public void onDetach() {
    instance = null;
    super.onDetach();
  }

  /*
   * GETTERS & SETTERS
   */

  public ShareExtensionActivity getShareActivity() {
    if (getActivity() instanceof ShareExtensionActivity) {
      return (ShareExtensionActivity) getActivity();
    } else {
      throw new UnsupportedOperationException("This fragment is only valid in the activity org.exoplatform.activity.ShareExtensionActivity");
    }
  }

  private void onSelectAccount(Server server) {
    getShareActivity().onAccountSelected(server);
    getShareActivity().openFragment(ComposeFragment.getFragment(),
                                    ComposeFragment.COMPOSE_FRAGMENT,
                                    ShareExtensionActivity.Anim.FROM_LEFT);
  }

  private void onSignInAccount(Server server) {
    // It's important to set the ownerAccount property before calling
    // verifySelectedIntranet()
    getShareActivity().getActivityPost().ownerAccount = server;
    getShareActivity().verifySelectedIntranet();
    SignInFragment signIn = SignInFragment.getFragment();
    getShareActivity().openFragment(signIn, SignInFragment.SIGN_IN_FRAGMENT, ShareExtensionActivity.Anim.FROM_RIGHT);
  }
}
