package org.exoplatform.tool;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;

import org.exoplatform.R;
import org.exoplatform.model.SocialSpace;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Adapter for the list of spaces in the SelectSpaceFragment.
 * @author paristote on 4/26/16.
 */
public class SpaceListAdapter extends BaseAdapter {

    static class ViewHolder {
        ImageView spaceAvatar;
        TextView spaceName;
    }

    private Context mContext;

    private List<SocialSpace> mSpaceList;

    private String baseUrl;   // Needed to make the full avatar url

    public SpaceListAdapter(@NonNull Context ctx, @NonNull String url) {
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
