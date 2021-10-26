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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureManager;

import org.exoplatform.App;
import org.exoplatform.BuildConfig;
import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerManagerImpl;
import org.exoplatform.tool.ServerUtils;

import java.util.TimerTask;

public class BoardingActivity extends AppCompatActivity {

    private ViewPager mSlideViewPager;
    private TabLayout mDotLayout;
    private SliderAdapter sliderAdapter;
    private TextView slideTitle;
    private TextView currentPage;
    private TextView scanQRBtn;
    int PERMISSION_CAMERA = 0;

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
            bypassIfRecentlyVisited();
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

                // permission denied, boo! Disable the
                // functionality that depends on this permission.
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

    private void bypassIfRecentlyVisited() {
        SharedPreferences prefs = App.Preferences.get(this);
        Server serverToConnect = new ServerManagerImpl(prefs).getLastVisitedServer();
        long lastVisit = prefs.getLong(App.Preferences.LAST_VISIT_TIME, 0L);
        // Rule SIGN_IN_13: if the app was left less than 1h ago
        if (serverToConnect != null && (System.nanoTime() - App.DELAY_1H_NANOS) < lastVisit) {
            String url = getIntent().getStringExtra(INTENT_KEY_URL);
            if(url != null && !url.equals("")) {
                openWebViewWithURL(url);
            } else {
                isFromInstances = getIntent().getBooleanExtra("isFromInstance",false);
                if (!isFromInstances) {
                    openWebViewWithURL(serverToConnect.getUrl().toString());
                }
            }
        }

        if (BuildConfig.DEBUG) {
            long minSinceLastVisit = (System.nanoTime() - lastVisit) / (60000000000L);
            Log.d(this.getClass().getName(), "*** Minutes since last visit : " + minSinceLastVisit);
        }
    }

    private void openWebViewWithURL(String url) {
        if (url == null)
            throw new IllegalArgumentException("URL must not be null");

        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(INTENT_KEY_URL, url);
        this.startActivity(intent);
    }
}