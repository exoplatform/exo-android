package org.exoplatform.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerUtils;

public class AddDomainServerActivity extends AppCompatActivity {
    ImageView closeButton;
    Button  clearButton;
    EditText companyTextField,addDomainTextField;
    RelativeLayout parentLayout,addDomainButton;
    Boolean isAlreadyFocused = false;

    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_domain_server);
        parentLayout = findViewById(R.id.container_add_view);
        addDomainTextField = (EditText) findViewById(R.id.textEditAddDomain);
        companyTextField = (EditText) findViewById(R.id.company_placeholder);
        closeButton = (ImageView) findViewById(R.id.close_button_add_domain);
        clearButton = (Button) findViewById(R.id.clear_button_add_domain);
        addDomainButton = (RelativeLayout) findViewById(R.id.add_domain_button_layout);
        statusBarColor();
        addDomainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitUrl();
            }
        });
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                companyTextField.getText().clear();
                addDomainTextField.getText().clear();
                companyTextField.setVisibility(View.GONE);
                requestFocusTo(addDomainTextField);
            }
        });
        addDomainTextField = (EditText) findViewById(R.id.textEditAddDomain);
        companyTextField.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (!isAlreadyFocused) {
                    companyTextField.setText("");
                    companyTextField.setTextColor(getResources().getColor(R.color.cardview_dark_background));
                    requestFocusTo(companyTextField);
                    isAlreadyFocused = true;
                }
                return false;
            }
        });

        // Hide keyboard when tapped outside of fields
        parentLayout.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hideSoftKeyboard(AddDomainServerActivity.this);
                }
            }
        });
    }

    private void requestFocusTo(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    // when users taps "Go" or "Enter" on the keyboard
    private void submitUrl() {
        String composedUrl = "https://" + companyTextField.getText().toString() + addDomainTextField.getText().toString();
        final String url = composedUrl.trim();
        Log.d("url ======= >",url);
        final ProgressDialog progressDialog = ServerUtils.savingServerDialog(AddDomainServerActivity.this);
        ServerUtils.verifyUrl(url, new ServerUtils.ServerVerificationCallback() {
            @Override
            public void onVerificationStarted() {
                progressDialog.show();
            }

            @Override
            public void onServerValid(Server server) {
                progressDialog.dismiss();
                Intent intent = new Intent(AddDomainServerActivity.this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.INTENT_KEY_URL, url);
                startActivity(intent);
            }

            @Override
            public void onServerNotSupported() {
                progressDialog.dismiss();
                ServerUtils.dialogWithTitleAndMessage(AddDomainServerActivity.this,
                        R.string.ServerManager_Error_TitleVersion,
                        R.string.ServerManager_Error_PlatformVersionNotSupported).show();
            }

            @Override
            public void onServerInvalid() {
                progressDialog.dismiss();
                ServerUtils.dialogWithTitleAndMessage(AddDomainServerActivity.this,
                        R.string.ServerManager_Error_TitleIncorrect,
                        R.string.ServerManager_Error_IncorrectUrl).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void statusBarColor(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color,this.getTheme()));
        }else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color));
        }
    }

    public static void hideSoftKeyboard(Activity context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow( context.getCurrentFocus().getWindowToken(), 0);
    }
}