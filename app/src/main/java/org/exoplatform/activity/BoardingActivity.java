package org.exoplatform.activity;

import static org.exoplatform.activity.WebViewActivity.INTENT_KEY_URL;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.ServerUtils;

import java.io.IOException;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BoardingActivity extends AppCompatActivity {

    private ViewPager mSlideViewPager;
    private TabLayout mDotLayout;
    private SliderAdapter sliderAdapter;
    private TextView slideTitle;
    private TextView currentPage;
    private TextView scanQRBtn;

    LinearLayout scanQRFragmentBtn;
    TextView enterServerFragmentBtn;
    Boolean isFromInstances;

    private static final int REQUEST_CODE = 101;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        if (savedInstanceState == null) {
            try {
                bypassIfRecentlyVisited();
            } catch (IOException e) {
                Log.e("error", String.valueOf(e));
            }
        }
        statusBarColor();
        mSlideViewPager = (ViewPager) findViewById(R.id.slide_view_pager);
        mDotLayout = (TabLayout) findViewById(R.id.onboarding_dots);
        slideTitle = (TextView) findViewById(R.id.onboarding_title_text_view);
        currentPage = (TextView) findViewById(R.id.current_page_textview);
        scanQRFragmentBtn = (LinearLayout) findViewById(R.id.scan_code_button);
        scanQRBtn = (TextView) findViewById(R.id.ScanButtonTitle);
        enterServerFragmentBtn = (TextView) findViewById(R.id.enter_server_url);
        sliderAdapter = new SliderAdapter(this);
        mSlideViewPager.setAdapter(sliderAdapter);
        scanQRBtn.setText(R.string.Onboarding_Button_Scan);
        enterServerFragmentBtn.setText(R.string.Onboarding_Button_addURL);
        final String[] slide_headings = {
                this.getResources().getString(R.string.Onboarding_Title_Slide1),
                this.getResources().getString(R.string.Onboarding_Title_slide2),
                this.getResources().getString(R.string.Onboarding_Title_slide3)
        };

        final String[] slide_page_numbers = {"1","2","3"};
        // The_slide_timer
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new The_slide_timer(),2000,8000);
        mDotLayout.setupWithViewPager(mSlideViewPager,true);
        // Set action buttons
        scanQRFragmentBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                Activity context = BoardingActivity.this;
                context.requestPermissions(new String[]{Manifest.permission.CAMERA}, 1011);
            }
        });

        enterServerFragmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addDomainActivity = new Intent(BoardingActivity.this, AddDomainServerActivity.class);
                startActivity(addDomainActivity);
            }
        });

        mSlideViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                slideTitle.setText(slide_headings[position]);
                currentPage.setText(slide_page_numbers[position]);
            }
            @Override
            public void onPageSelected(int position) { }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    public void onBackPressed() {

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 1011) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(BoardingActivity.this, ScannerActivity.class);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                intent.putExtra("SCAN_CAMERA_ID", 0);
                startActivityForResult(intent,REQUEST_CODE);
            } else {
                Toast.makeText(BoardingActivity.this, "Permission denied to read your Camera", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE && data !=null) {
                String detectedURL = data.getStringExtra("keyQRCode");
                System.out.println("detectedURL ===XXXXXXXX>>>>" + detectedURL.trim());
                submitUrl(detectedURL.trim());
            }
        }
    }

    private void submitUrl(final String url) {
        final ProgressDialog progressDialog = ServerUtils.savingServerDialog(this);
        ServerUtils.verifyUrl(url, new ServerUtils.ServerVerificationCallback() {
            @Override
            public void onVerificationStarted() {
                progressDialog.show();
            }

            @Override
            public void onServerValid(Server server) {
                progressDialog.dismiss();
                Intent intent = new Intent(BoardingActivity.this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.INTENT_KEY_URL, url); // server.getUrl().toString()
                startActivity(intent);
            }

            @Override
            public void onServerNotSupported() {
                progressDialog.dismiss();
                ServerUtils.dialogWithTitleAndMessage(BoardingActivity.this,
                        R.string.ServerManager_Error_TitleVersion,
                        R.string.ServerManager_Error_PlatformVersionNotSupported).show();
            }

            @Override
            public void onServerInvalid() {
                progressDialog.dismiss();
                ServerUtils.dialogWithTitleAndMessage(BoardingActivity.this,
                        R.string.ServerManager_Error_TitleIncorrect,
                        R.string.ServerManager_Error_IncorrectUrl).show();
            }
        });
    }

    public class The_slide_timer extends TimerTask {
        @Override
        public void run() {
            BoardingActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSlideViewPager.getCurrentItem() < SliderAdapter.slide_images.length - 1) {
                        mSlideViewPager.setCurrentItem(mSlideViewPager.getCurrentItem()+1);
                    }else{
                        mSlideViewPager.setCurrentItem(0);
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void statusBarColor(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color,this.getTheme()));
        }else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color));
        }
    }

    private void bypassIfRecentlyVisited() throws IOException {
        SharedPreferences prefs = App.Preferences.get(this);
        Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
        try {
            setWakeUpActivityRoot(new ResultHandler<Boolean>() {
                @Override
                public void onSuccess(Boolean isSessionsAlive) {
                     if (isSessionsAlive) {
                         String url = getIntent().getStringExtra(INTENT_KEY_URL);
                         if(url != null && !url.equals("")) {
                             openWebViewWithURL(url);
                         } else {
                             isFromInstances = getIntent().getBooleanExtra("isFromInstance",false);
                             if (!isFromInstances) {
                                 openWebViewWithURL(serverToConnect.getUrl().toString());
                             }
                         }
                     }else{
                         isFromInstances = getIntent().getBooleanExtra("isFromInstance",false);
                         if (!isFromInstances) {
                             Intent intent = new Intent(BoardingActivity.this, ConnectToExoListActivity.class);
                             startActivity(intent);
                         }
                     }
                }
                @Override
                public void onFailure(Exception e) {

                }
            });
        } catch (IOException e) {
            Log.e("error", String.valueOf(e));
        }
    }

    private void openWebViewWithURL(String url) {
        if (url == null)
            throw new IllegalArgumentException("URL must not be null");
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(INTENT_KEY_URL, url);
        this.startActivity(intent);
    }

    private void setWakeUpActivityRoot(final ResultHandler<Boolean> handler) throws IOException {
        SharedPreferences prefs = App.Preferences.get(this);
        Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
        String username = prefs.getString("connectedUsername","username");
        String cookies = prefs.getString("connectedCookies","cookies");
        if (serverToConnect != null) {
            final String url = serverToConnect.getUrl().getProtocol() + "://" + serverToConnect.getShortUrl() + "/portal/rest/state/status/" + username;
            System.out.println("url =========> " + url);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Cookie", cookies)
                                .url(url)
                                .build();
                        Response httpResponse = client.newCall(request).execute();
                        if (httpResponse.code() == 200){
                            handler.onSuccess(true);
                        }else{
                            handler.onSuccess(false);
                        }
                    } catch (Exception e) {
                        Log.e("error", String.valueOf(e));
                        handler.onFailure(e);
                    }
                }
            });
            thread.start();
        }
    }

    public interface ResultHandler<T> {
        void onSuccess(T data);
        void onFailure(Exception e);
    }
}