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
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.exoplatform.R;

/**
 * Fragment that represents an on-boarding page
 */
public class OnBoardingViewFragment extends Fragment {

  public static final int     WELCOME       = 0;

  public static final int     COLLABORATION = 1;

  public static final int     ENGAGEMENT    = 2;

  public static final int     NETWORKING    = 3;

  // the on-boarding message id
  private static final String ARG_PAGE_ID   = "PAGE_ID";

  private int                 mPageId;

  public OnBoardingViewFragment() {
    // Required empty public constructor
  }

  /**
   * @param pageId the message to display on the screen
   * @return A new instance of fragment OnBoardingViewFragment.
   */
  public static OnBoardingViewFragment newInstance(int pageId) {
    if (pageId < WELCOME || pageId > NETWORKING)
      throw new IllegalArgumentException("Parameter pageId must be within range [0,3]");

    OnBoardingViewFragment fragment = new OnBoardingViewFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_PAGE_ID, pageId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mPageId = getArguments().getInt(ARG_PAGE_ID);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.fragment_on_boarding_view, container, false);
    ((ImageView) layout.findViewById(R.id.OnBoardingFragment_View_Image)).setImageResource(getImageResourceId());
    ((TextView) layout.findViewById(R.id.OnBoardingFragment_View_Title)).setText(getTitle());
    ((TextView) layout.findViewById(R.id.OnBoardingFragment_View_Message)).setText(getMessage());
    return layout;
  }

  private int getImageResourceId() {
    switch (mPageId) {
    case WELCOME:
      return R.drawable.onboarding_00_welcome;
    case COLLABORATION:
      return R.drawable.onboarding_01_collaboration;
    case ENGAGEMENT:
      return R.drawable.onboarding_02_engagement;
    case NETWORKING:
      return R.drawable.onboarding_03_networking;
    default:
      throw new IllegalArgumentException("Invalid on-boarding page id");
    }
  }

  private String getTitle() {
    switch (mPageId) {
    case WELCOME:
      return getString(R.string.OnBoarding_Title_Welcome);
    case COLLABORATION:
      return getString(R.string.OnBoarding_Title_Collaboration);
    case ENGAGEMENT:
      return getString(R.string.OnBoarding_Title_Engagement);
    case NETWORKING:
      return getString(R.string.OnBoarding_Title_Networking);
    default:
      throw new IllegalArgumentException("Invalid on-boarding page id");
    }
  }

  private String getMessage() {
    switch (mPageId) {
    case WELCOME:
      return getString(R.string.OnBoarding_Message_Welcome);
    case COLLABORATION:
      return getString(R.string.OnBoarding_Message_Collaboration);
    case ENGAGEMENT:
      return getString(R.string.OnBoarding_Message_Engagement);
    case NETWORKING:
      return getString(R.string.OnBoarding_Message_Networking);
    default:
      throw new IllegalArgumentException("Invalid on-boarding page id");
    }
  }

}
