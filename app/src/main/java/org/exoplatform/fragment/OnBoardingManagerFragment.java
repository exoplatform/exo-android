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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.exoplatform.R;
import org.exoplatform.ui.CirclePageIndicator;

/**
 * Fragment that displays the on-boarding screens
 */
public class OnBoardingManagerFragment extends Fragment {

  public static final String         TAG = OnBoardingManagerFragment.class.getName();

  private OnBoardingFragmentCallback mListener;

  public OnBoardingManagerFragment() {
    // Required empty public constructor
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnBoardingFragmentCallback)
      mListener = (OnBoardingFragmentCallback) context;
    else
      throw new RuntimeException(context.toString() + " must implement OnBoardingFragmentCallback");
  }

  @Override
  public void onDetach() {
    mListener = null;
    super.onDetach();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.fragment_on_boarding, container, false);
    ViewPager pager = (ViewPager) layout.findViewById(R.id.OnBoardingFragment_ViewPager);
    pager.setAdapter(buildAdapter());
    Button gotItButton = (Button) layout.findViewById(R.id.OnBoardingFragment_Button_GotIt);
    gotItButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mListener.onCloseOnBoardingFragment();
      }
    });
    CirclePageIndicator pageIndicator = (CirclePageIndicator) layout.findViewById(R.id.OnBoardingFragment_PageIndicator);
    pageIndicator.setViewPager(pager, 0);
    return layout;
  }

  private PagerAdapter buildAdapter() {
    return new OnBoardingPagerAdapter(getChildFragmentManager());
  }

  private class OnBoardingPagerAdapter extends FragmentPagerAdapter {

    public OnBoardingPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // Position within [0-3] represents the pageId in OnBoardingViewFragment
      return OnBoardingViewFragment.newInstance(position);
    }

    @Override
    public int getCount() {
      // We have 4 pages, whose IDs range from 0 to 3
      return 4;
    }
  }

  public interface OnBoardingFragmentCallback {
    void onCloseOnBoardingFragment();
  }

}
