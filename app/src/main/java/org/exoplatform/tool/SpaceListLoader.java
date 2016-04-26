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
import android.support.v4.BuildConfig;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.exoplatform.model.Server;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Loads the list of spaces for the SelectSpaceFragment
 *
 * @author paristote on 4/26/16.
 */
public class SpaceListLoader extends AsyncTaskLoader<SocialRestService.SpaceListResult> {

    private int mOffset;

    private Server mServer;

    public SpaceListLoader(Context context, int offset, Server server) {
        super(context);
        this.mOffset = offset;
        this.mServer = server;
    }

    @Override
    public SocialRestService.SpaceListResult loadInBackground() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(mServer.getUrl().toString())
                .client(ExoHttpClient.getInstance())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SocialRestService service = retrofit.create(SocialRestService.class);
        try {
            Response<SocialRestService.SpaceListResult> response = service.loadSpaces(String.valueOf(mOffset)).execute();
            if (response != null && response.body() != null)
                return response.body();
        } catch (IOException e) {
            Log.e(getContext().getClass().getName(), e.getMessage(), e);
        }
        return null;
    }
}
