package org.exoplatform.activity;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.ViewfinderView;

import org.exoplatform.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    TextView closeButton;
    TextView detectedURL;
    ImageView qr_code_imageView;
    ViewfinderView viewFinder;
    private Handler handler;
    private static String desiredRange = "/portal/login?username=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        handler = new Handler();
        //Initialize barcode scanner view
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        closeButton = findViewById(R.id.close_camera_button);
        detectedURL = findViewById(R.id.detected_scan_url);
        qr_code_imageView = findViewById(R.id.imageView_qr_code);
        viewFinder = findViewById(R.id.zxing_viewfinder_view);
        //start capture
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();
        barcodeScannerView.decodeContinuous(callback);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScannerView.resume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScannerView.pause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText().contains(desiredRange)){
                barcodeScannerView.pause();
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP);
                final String detected_url = result.getText()  + "&source=qrcode";
                String urlDomain = null;
                try {
                    System.out.println("detected_url ===========> " + detected_url);
                    urlDomain = new URL(detected_url).getHost();
                    detectedURL.setText(urlDomain);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                qr_code_imageView.setBackgroundResource(R.drawable.qr_code_scanner);
                // Scaling
                ScaleAnimation fade_in = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                fade_in.setDuration(1000);
                fade_in.setFillAfter(true);
                qr_code_imageView.startAnimation(fade_in);
                handler.postDelayed(new Runnable() {
                    public void run() {
                        Intent mIntent = new Intent();
                        mIntent.putExtra("keyQRCode", detected_url);
                        setResult(RESULT_OK, mIntent);
                        finish();
                    }
                }, 3000);
            }else{
                detectedURL.setText("Not valid URL");
            }
        }


        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            if (resultPoints.size() != 0) {

            }
        }
    };



}