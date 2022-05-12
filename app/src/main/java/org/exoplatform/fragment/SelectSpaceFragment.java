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

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.exoplatform.R;
import org.exoplatform.activity.ShareExtensionActivity;
import org.exoplatform.model.SocialSpace;
import org.exoplatform.tool.SocialRestService;
import org.exoplatform.tool.SpaceListAdapter;
import org.exoplatform.tool.SpaceListLoader;

import java.util.List;

/**
 * Created by paristote on 3/8/16. Fragment that displays a list of the user's
 * spaces.
 */
public class SelectSpaceFragment extends Fragment implements LoaderManager.LoaderCallbacks<SocialRestService.SpaceListResult>,
    AdapterView.OnItemClickListener {

  public static final String         SPACES_FRAGMENT = "spaces_fragment";

  private final int INITIAL_LOAD = 1;

  private final int MORE_LOAD = 2;

  private SpaceListAdapter           mSpaceListAdapter;

  private List<SocialSpace>          mSpaceList;

  private ListView                   mSpaceListView;

  private TextView                   mMessageView;

  private ProgressBar                mProgressView;

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
    setupFooterProgressBar(); // must be called after mSpaceListView is created
    return layout;
  }

  @Override
  public void onResume() {
    super.onResume();
    mSpaceListAdapter = new SpaceListAdapter(getActivity(), getShareActivity().getActivityPost().ownerAccount.getUrl().toString());
    mSpaceListView.setAdapter(mSpaceListAdapter);
    // Will start loading spaces starting at offset = 0
    getLoaderManager().initLoader(INITIAL_LOAD, null, this).forceLoad();
  }

  @Override
  public Loader<SocialRestService.SpaceListResult> onCreateLoader(int id, Bundle args) {
    int offset = (args == null) ? 0 : args.getInt("LOAD_OFFSET", 0);
    return new SpaceListLoader(getActivity(), offset, 20, getShareActivity().getActivityPost().ownerAccount);
  }

  @Override
  public void onLoadFinished(Loader<SocialRestService.SpaceListResult> loader, SocialRestService.SpaceListResult data) {
    mProgressView.setVisibility(View.INVISIBLE);
    if (data == null || data.spaces == null || data.spaces.isEmpty()) {
      // Empty state
      mMessageView.setText(R.string.ShareActivity_Spaces_Title_NoSpace);
    } else {
      // Set on scroll listener on the list view, to load more spaces when reaching the bottom
      int totalSpaceCount = data.size;
      int currentOffset = data.offset;
      int loadSpaceCount = data.limit; // always 20
      setLoadMoreSpacesListener(currentOffset, loadSpaceCount, totalSpaceCount);
      // Save and display the loaded spaces
      List<SocialSpace> loadedSpaces = data.spaces;
      switch (loader.getId()) {
        case MORE_LOAD:
          // Add the loaded spaces to the existing list of spaces
          mSpaceList.addAll(loadedSpaces);
          break;
        case INITIAL_LOAD:
        default:
          // Display the loaded spaces
          mSpaceList = loadedSpaces;
          break;
      }
      mSpaceListAdapter.setSpaceList(mSpaceList);
      mSpaceListAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onLoaderReset(Loader<SocialRestService.SpaceListResult> loader) {
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    SocialSpace selectedSpace = mSpaceList.get(position);
    getShareActivity().onSpaceSelected(selectedSpace);
  }

  private void setLoadMoreSpacesListener(int currentOffset, int loadedSpaceCount, int totalSpaceCount) {
    if (mSpaceListView != null) {
      final int newOffset = currentOffset + loadedSpaceCount;
      if (newOffset < totalSpaceCount) {
        // Means we have more spaces to load when we reach the bottom of the listview
        mProgressView.setVisibility(View.VISIBLE);
//        mSpaceListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mSpaceListView.setOnScrollListener(new AbsListView.OnScrollListener() {
          @Override
          public void onScrollStateChanged(AbsListView view, int scrollState) {
          }

          @Override
          public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
              if (!getLoaderManager().hasRunningLoaders()) {
                Bundle args = new Bundle();
                args.putInt("LOAD_OFFSET", newOffset);
                getLoaderManager().restartLoader(MORE_LOAD, args, SelectSpaceFragment.this).forceLoad();
              }
            }
          }
        });
      } else {
        // We already loaded all the possible spaces
        mSpaceListView.setOnScrollListener(null);
//        mSpaceListView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        mProgressView.setVisibility(View.GONE);
      }
    }
  }

  public ShareExtensionActivity getShareActivity() {
    if (getActivity() instanceof ShareExtensionActivity) {
      return (ShareExtensionActivity) getActivity();
    } else {
      throw new UnsupportedOperationException("This fragment is only valid in the activity org.exoplatform.activity.ShareExtensionActivity");
    }
  }

  private void setupFooterProgressBar() {
    mProgressView = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleSmall);
    // To get an horizontal bar, use android.R.attr.progressBarStyleHorizontal as the progress bar style
    mProgressView.setIndeterminate(true);
    int eXoYellow = getResources().getColor(R.color.eXoLightGray);
    mProgressView.getIndeterminateDrawable().setColorFilter(eXoYellow, PorterDuff.Mode.SRC_IN);
    mProgressView.setLayoutParams(new ListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, AbsListView.LayoutParams.WRAP_CONTENT));
    mProgressView.setPadding(0, 10, 0, 10); // 0px left and right, 10px top and bottom
    mProgressView.setVisibility(View.INVISIBLE);
    LinearLayout layout = new LinearLayout(getContext());
    layout.setGravity(Gravity.CENTER);
    layout.addView(mProgressView);
    if (mSpaceListView != null) {
      mSpaceListView.addFooterView(layout);
      mSpaceListView.setFooterDividersEnabled(false);
    }
  }
}
