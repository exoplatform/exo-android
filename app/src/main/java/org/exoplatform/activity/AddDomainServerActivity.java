package org.exoplatform.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.exoplatform.R;
import org.exoplatform.model.Server;
import org.exoplatform.tool.ServerUtils;

public class AddDomainServerActivity extends AppCompatActivity {
    ImageView closeButton;
    Button  clearButton;
    EditText companyTextField;
    RelativeLayout parentLayout,addDomainButton;
    LinearLayout topViewAddUrlLayout;
    Boolean isAlreadyFocused = false;
    TextView headerTitle;
    TextView addURLTextView;
    private ActionDialog dialog;
    private CheckConnectivity checkConnectivity;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_domain_server);
        checkConnectivity = new CheckConnectivity(AddDomainServerActivity.this);
        parentLayout = findViewById(R.id.container_add_view);
        topViewAddUrlLayout = (LinearLayout) findViewById(R.id.topViewAddUrl);
        companyTextField = (EditText) findViewById(R.id.company_placeholder);
        closeButton = (ImageView) findViewById(R.id.close_button_add_domain);
        clearButton = (Button) findViewById(R.id.clear_button_add_domain);
        addDomainButton = (RelativeLayout) findViewById(R.id.add_domain_button_layout);
        headerTitle = (TextView) findViewById(R.id.textViewAddServer);
        addURLTextView = (TextView) findViewById(R.id.addURLTextView);
        headerTitle.setText(R.string.AddDomain_Title_Header);
        addURLTextView.setText(R.string.AddDomain_Title_addURL);
        companyTextField.setHint(R.string.AddDomain_Title_Placeholder);
        statusBarColor();
        handleActionGoKeyKeyboard();
        addDomainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitUrl();
            }
        });
        closeButton.setOnClickListener(v -> onBackPressed());
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                companyTextField.getText().clear();
                requestFocusTo(companyTextField);
            }
        });
        requestFocusTo(companyTextField);
        // Hide keyboard when tapped outside of fields
        topViewAddUrlLayout.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                hideSoftKeyboard(AddDomainServerActivity.this);
            }
        });
    }

    private void requestFocusTo(EditText editText) {
        editText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    // when users taps "Go" or "Enter" on the keyboard
    private void submitUrl() {
        if (checkConnectivity.isConnectedToInternet()) {
            String urlPrefix = companyTextField.getText().toString();
/*            if (!urlPrefix.startsWith("https://")) {
                urlPrefix = "https://" + urlPrefix;
            }*/
            final String url = urlPrefix.trim();
            Log.d("url ======= >", url);
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
                    dialog = new ActionDialog(R.string.ServerManager_Error_TitleVersion,
                            R.string.ServerManager_Error_PlatformVersionNotSupported, R.string.Word_OK, AddDomainServerActivity.this);
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
                            R.string.ServerManager_Error_IncorrectUrl, R.string.Word_OK, AddDomainServerActivity.this);
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
    }

    public void statusBarColor(){
        getWindow().setStatusBarColor(getResources().getColor(R.color.status_bar_color,this.getTheme()));
    }

    private void handleActionGoKeyKeyboard(){
        companyTextField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    submitUrl();
                    return true;
                }
                return false; // otherwise let the system handle the event
            }
        });
    }

    public static void hideSoftKeyboard(Activity context) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow( context.getCurrentFocus().getWindowToken(), 0);
    }
}