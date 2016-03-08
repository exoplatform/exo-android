package org.exoplatform.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.ServerManagerImpl;
import org.exoplatform.model.Server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

/**
 * Created by chautran on 21/11/2015.
 */
public class ConnectServerActivity extends AppCompatActivity {

    private final String BUNDLE_BACKGROUND_IMAGE = "BACKGROUND_IMAGE";

    TextView mConnectButton;
    TextView mOtherButton;
    TextView mDiscoverTribeLink;
    ServerManagerImpl serverManager;
    int backgroundImageId;
    Point screenSize = new Point();

    /*
        LIFECYCLE
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // won't be called if the device was rotated
// TODO uncomment before release           bypassIfRecentlyVisited();
            // randomly select a background image
            backgroundImageId = getRandomBackgroundImageId();
        } else {
            // reuse the same background as before
            backgroundImageId = savedInstanceState.getInt(BUNDLE_BACKGROUND_IMAGE, getRandomBackgroundImageId());
        }
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(screenSize);
        setContentView(R.layout.main);
        LinearLayout layout = (LinearLayout) findViewById(R.id.Main_Layout);
        if (screenSize.x > screenSize.y) {
            // In landscape mode, we want to scale and crop the background image
            // We do this off the UI thread to not drop any frame
            setLandscapeBackgroundImage(screenSize, backgroundImageId, layout);
        } else {
            layout.setBackgroundDrawable(getResources().getDrawable(backgroundImageId));
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.Main_Toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        mConnectButton = (TextView) findViewById(R.id.MainScreen_Button_Connect);
        mOtherButton = (TextView) findViewById(R.id.MainScreen_Button_Other);
        mDiscoverTribeLink = (TextView) findViewById(R.id.MainScreen_Button_Discover);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(App.Preferences.FILE_NAME, 0);
        serverManager = new ServerManagerImpl(prefs);

        int serverCount = serverManager.getServerCount();
        if (serverCount == 0) {
            // Rule SIGN_IN_02
            mConnectButton.setText(R.string.ConnectActivity_Title_DiscoverExoTribe);
            mConnectButton.setOnClickListener(onClickDiscoverTribe());
            mOtherButton.setText(R.string.ConnectActivity_Title_AddIntranet);
        } else {
            // Rule SIGN_IN_05
            Server serverToConnect = serverManager.getLastVisitedServer();
            if (serverToConnect.isExoTribe()) {
                // Connect to eXo Tribe
                mConnectButton.setText(R.string.ConnectActivity_Title_ConnnectToExoTribe);
            } else {
                // Connect to <br/> www.intranet-url.com
                int width = screenSize.x;
                int urlLength = serverToConnect.getShortUrl().length();
                String labelFormat = String.format(connectLabelFormat(width, urlLength),
                        getString(R.string.ConnectActivity_Title_ConnectTo),
                        serverToConnect.getShortUrl());
                mConnectButton.setText(Html.fromHtml(labelFormat));
            }

            mConnectButton.setOnClickListener(onClickConnectServer(serverToConnect));
            if (serverCount == 1) {
                mOtherButton.setText(R.string.ConnectActivity_Title_AddNewIntranet);
            } else {
                mOtherButton.setText(R.string.ConnectActivity_Title_Others);
            }
            if (serverManager.tribeServerExists()) {
                mDiscoverTribeLink.setVisibility(View.INVISIBLE);
            } else {
                // Rule SIGN_IN_08
                mDiscoverTribeLink.setVisibility(View.VISIBLE);
                mDiscoverTribeLink.setOnClickListener(onClickDiscoverTribe());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_BACKGROUND_IMAGE, backgroundImageId);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
        BUTTON ACTIONS
     */

    public void openServerList(View v) {
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }

    private View.OnClickListener onClickDiscoverTribe() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebViewWithURL(App.TRIBE_URL);
                try {
                    Server serverToConnect = new Server(new URL(App.TRIBE_URL));
                    serverManager.addServer(serverToConnect);
                } catch (MalformedURLException e) {
                    Log.d(this.getClass().getName(), e.getMessage());
                }
            }
        };
    }

    private View.OnClickListener onClickConnectServer(final Server server) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (server != null) {
                    String url = server.getUrl().toString();
                    openWebViewWithURL(url);
                }
            }
        };
    }

    private void openWebViewWithURL(String url) {
        if (url == null)
            throw new IllegalArgumentException("URL must not be null");

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, url);
        this.startActivity(intent);
    }

    /*
        OTHER
     */

    private void bypassIfRecentlyVisited() {
        SharedPreferences prefs = getSharedPreferences(App.Preferences.FILE_NAME, 0);
        Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
        long lastVisit = prefs.getLong(App.Preferences.LAST_VISIT_TIME, 0L);
        if (serverToConnect != null && (System.nanoTime() - App.DELAY_1H_NANOS) < lastVisit) {
            // Rule SIGN_IN_13: if the app was left less than 1h ago
            openWebViewWithURL(serverToConnect.getUrl().toString());
        }
        if (BuildConfig.DEBUG) {
            long minSinceLastVisit = (System.nanoTime() - lastVisit)/(60000000000L);
            Log.d(this.getClass().getName(), "*** Minutes since last visit : " + minSinceLastVisit);
        }
    }

    private String connectLabelFormat(int screenWidth, int urlLength) {
        float ratio = screenWidth / urlLength;

//        if (ratio >= 30)
//            return "%s<br/>%s";

        if (ratio >= 25)
            return "%s<br/><small>%s</small>";

        return "%s<br/><small><small>%s</small></small>";
    }


    /*
        BACKGROUND IMAGE UTILS
     */

    private int getRandomBackgroundImageId() {
        final int id = new Random().nextInt(4) + 1; // generates a random int between 1 and 4
        switch (id) {
            case 1:
                return R.drawable.main_screen_bg1;
            case 2:
                return R.drawable.main_screen_bg2;
            case 3:
                return R.drawable.main_screen_bg3;
            default:
                return R.drawable.main_screen_bg4;
        }
    }

    private void setLandscapeBackgroundImage(final Point screen, final int backgroundResId, final LinearLayout layout) {
        new AsyncTask<Void, Void, BitmapDrawable>() {
            @Override
            protected BitmapDrawable doInBackground(Void... params) {
                BitmapDrawable finalBg = null;
                try {
                    Bitmap bg = Picasso.with(ConnectServerActivity.this).load(backgroundResId).resize(screen.x, screen.y).centerCrop().get();
                    finalBg = new BitmapDrawable(getResources(), bg);
                    finalBg.setGravity(Gravity.TOP|Gravity.START);
                } catch (IOException e) {
                    if (BuildConfig.DEBUG)
                        Log.d(ConnectServerActivity.class.getName(), e.getMessage());
                }
                return finalBg;
            }
            @Override
            protected void onPostExecute(BitmapDrawable bm) {
                super.onPostExecute(bm);
                if (bm != null)
                    layout.setBackgroundDrawable(bm);
            }
        }.execute();
    }
}
