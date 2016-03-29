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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.exoplatform.R;
import org.exoplatform.activity.ShareExtensionActivity;
import org.exoplatform.model.SocialSpace;
import org.exoplatform.tool.ExoHttpClient;
import org.exoplatform.tool.SocialRestService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by paristote on 3/8/16. Fragment that displays a list of the user's
 * spaces.
 */
public class SelectSpaceFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<SocialSpace>>,
    AdapterView.OnItemClickListener {

  public static final String         SPACES_FRAGMENT = "spaces_fragment";

  private SpaceListAdapter           mSpaceListAdapter;

  private List<SocialSpace>          mSpaceList;

  private ListView                   mSpaceListView;

  private TextView                   mMessageView;

  private static SelectSpaceFragment instance;

  public SelectSpaceFragment() {
  }

  public static SelectSpaceFragment getFragment() {
    if (instance == null) {
      instance = new SelectSpaceFragment();
    }
    return instance;
  }

  @Override
  public void onDetach() {
    instance = null;
    super.onDetach();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.select_space_fragment, container, false);
    TextView allConnections = (TextView) layout.findViewById(R.id.list_spaces_all_connections);
    allConnections.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        getShareActivity().onSpaceSelected(null);
      }
    });
    mSpaceListView = (ListView) layout.findViewById(R.id.list_spaces);
    mSpaceListView.setOnItemClickListener(this);
    mMessageView = (TextView) layout.findViewById(R.id.list_spaces_message_view);
    mSpaceListView.setEmptyView(mMessageView);
    return layout;
  }

  @Override
  public void onResume() {
    super.onResume();
    mSpaceListAdapter = new SpaceListAdapter(getActivity(), getShareActivity().getActivityPost().ownerAccount.getUrl().toString());
    mSpaceListView.setAdapter(mSpaceListAdapter);
    getLoaderManager().initLoader(0, null, this).forceLoad();
  }

  @Override
  public Loader<List<SocialSpace>> onCreateLoader(int id, Bundle args) {
    return new AsyncTaskLoader<List<SocialSpace>>(getActivity()) {
      @Override
      public List<SocialSpace> loadInBackground() {
        List<SocialSpace> spaces = new ArrayList<>();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(getShareActivity().getActivityPost().ownerAccount.getUrl().toString())
                                                  .client(ExoHttpClient.getInstance())
                                                  .addConverterFactory(GsonConverterFactory.create())
                                                  .build();
        SocialRestService service = retrofit.create(SocialRestService.class);
        try {
          Response<SocialRestService.RestSpaceList> response = service.loadSpaces().execute();
          if (response != null && response.body() != null)
            spaces.addAll(response.body().spaces);
        } catch (IOException e) {
          Log.e(SelectSpaceFragment.this.getClass().getName(), e.getMessage(), e);
        }
        return spaces;
      }
    };
  }

  @Override
  public void onLoadFinished(Loader<List<SocialSpace>> loader, List<SocialSpace> data) {
    if (data != null) {
      mSpaceList = data;
      mSpaceListAdapter.setSpaceList(data);
      mSpaceListAdapter.notifyDataSetChanged();
      if (data.isEmpty())
        mMessageView.setText(R.string.ShareActivity_Spaces_Title_NoSpace);
    }
  }

  @Override
  public void onLoaderReset(Loader<List<SocialSpace>> loader) {
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    SocialSpace selectedSpace = mSpaceList.get(position);
    if (selectedSpace != null) {
      getShareActivity().onSpaceSelected(selectedSpace);
    }
  }

  public ShareExtensionActivity getShareActivity() {
    if (getActivity() instanceof ShareExtensionActivity) {
      return (ShareExtensionActivity) getActivity();
    } else {
      throw new UnsupportedOperationException("This fragment is only valid in the activity org.exoplatform.activity.ShareExtensionActivity");
    }
  }

  static class SpaceListAdapter extends BaseAdapter {

    private Context           mContext;

    private List<SocialSpace> mSpaceList;

    private String            baseUrl;   // Needed to make the full avatar url

    public SpaceListAdapter(Context ctx, String url) {
      mContext = ctx;
      baseUrl = url;
    }

    public void setSpaceList(List<SocialSpace> list) {
      mSpaceList = list;
    }

    @Override
    public int getCount() {
      return mSpaceList == null ? 0 : mSpaceList.size();
    }

    @Override
    public Object getItem(int pos) {
      return mSpaceList == null ? null : mSpaceList.get(pos);
    }

    @Override
    public long getItemId(int pos) {
      return pos;
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
      ViewHolder holder;
      if (convertView == null) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.select_space_fragment_space_item, parent, false);
        holder = new ViewHolder();
        holder.spaceName = (TextView) convertView.findViewById(R.id.item_space_name);
        holder.spaceAvatar = (ImageView) convertView.findViewById(R.id.item_space_avatar);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      SocialSpace space = (SocialSpace) getItem(index);
      holder.spaceName.setText(space.displayName);
      URL url = null;
      String avatarUrl = space.avatarUrl;
      try {
        url = new URL(avatarUrl);
      } catch (MalformedURLException ignored) {
      }
      if (url == null) {
        avatarUrl = baseUrl + space.avatarUrl;
      }
      Picasso.with(mContext)
             .load(avatarUrl)
             .placeholder(R.drawable.icon_space_default)
             .error(R.drawable.icon_space_default)
             .resizeDimen(R.dimen.ShareActivity_Spaces_Icon_Size, R.dimen.ShareActivity_Spaces_Icon_Size)
             .into(holder.spaceAvatar);
      holder.spaceAvatar.setContentDescription(space.displayName);
      return convertView;
    }
  }

  static class ViewHolder {
    ImageView spaceAvatar;

    TextView  spaceName;
  }
}
