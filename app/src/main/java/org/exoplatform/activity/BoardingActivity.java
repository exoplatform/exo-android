package org.exoplatform.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerUtils;
import org.jsoup.Jsoup;

import java.util.TimerTask;

//import io.fabric.sdk.android.services.concurrency.AsyncTask;

public class BoardingActivity extends AppCompatActivity {

    private ViewPager mSlideViewPager;
    private TabLayout mDotLayout;
    private SliderAdapter sliderAdapter;
    private TextView slideTitle;
    private TextView currentPage;
    private TextView scanQRBtn;
    private ActionDialog dialog;
    private ActionDialog updateDialog;
    private CheckConnectivity checkConnectivity;

    LinearLayout scanQRFragmentBtn;
    TextView enterServerFragmentBtn;
    String currentVersionString;
    Integer currentVersion;
    Integer storeVersion;

    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        dialog = new ActionDialog(R.string.SettingsActivity_Title_DeleteConfirmation,R.string.SettingsActivity_Message_DeleteConfirmation, R.string.Word_Delete, BoardingActivity.this);
        updateDialog = new ActionDialog(R.string.OnBoarding_Title_Update,R.string.OnBoarding_Message_Update, R.string.Word_Update, BoardingActivity.this);
        checkConnectivity = new CheckConnectivity(BoardingActivity.this);
        updateDialog.deleteAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent viewIntent = new Intent("android.intent.action.VIEW",Uri.parse("https://play.google.com/store/apps/details?id=org.exoplatform"));
                    startActivity(viewIntent);
                }catch(Exception e) {
                    Log.d("Unable to Connect Try Again:", String.valueOf(e));
                }
            }
        });

        updateDialog.cancelAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDialog.dismiss();
            }
        });
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
        try {
            currentVersionString = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Unable to get current version:", String.valueOf(e));
        }
        //CheckForeXoUpdate checkForeXoUpdate = new CheckForeXoUpdate();
        //checkForeXoUpdate.execute();
        final String[] slide_page_numbers = {"1","2","3"};
        // The_slide_timer
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new The_slide_timer(),2000,8000);
        mDotLayout.setupWithViewPager(mSlideViewPager,true);

        // Set action buttons
        scanQRFragmentBtn.setOnClickListener(new View.OnClickListener() {
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                dialog = new ActionDialog(R.string.ServerManager_Error_TitleVersion,
                        R.string.ServerManager_Error_PlatformVersionNotSupported, R.string.Word_OK, BoardingActivity.this);
                dialog.cancelAction.setVisibility(View.GONE);
                LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams)dialog.deleteAction.getLayoutParams();
                ll.setMarginStart(0);
                dialog.deleteAction.setLayoutParams(ll);
                dialog.showDialog();
                dialog.deleteAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void onServerInvalid() {
                progressDialog.dismiss();
                dialog = new ActionDialog(R.string.ServerManager_Error_TitleIncorrect,
                        R.string.ServerManager_Error_IncorrectUrl, R.string.Word_OK, BoardingActivity.this);
                dialog.cancelAction.setVisibility(View.GONE);
                LinearLayout.LayoutParams ll = (LinearLayout.LayoutParams)dialog.deleteAction.getLayoutParams();
                ll.setMarginStart(0);
                dialog.deleteAction.setLayoutParams(ll);
                dialog.showDialog();
                dialog.deleteAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void onConnectionError() {
                progressDialog.dismiss();
                checkConnectivity.lostConnectionDialog.showDialog();
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

    public void statusBarColor(){
        getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color,this.getTheme()));
    }
}