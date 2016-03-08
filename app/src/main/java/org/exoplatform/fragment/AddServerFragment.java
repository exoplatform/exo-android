package org.exoplatform.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.ServerManager;
import org.exoplatform.ServerManagerImpl;
import org.exoplatform.activity.WebViewActivity;
import org.exoplatform.model.Server;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by chautran on 22/12/2015.
 */
public class AddServerFragment extends Fragment {

  EditText url_input_txt;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View fragmentLayout = inflater.inflate(R.layout.add_server_fragment, container, false);
    url_input_txt = (EditText) fragmentLayout.findViewById(R.id.url_input_txt);

    url_input_txt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_NULL) {
          if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return true; //do nothing but consume the key down event
          }
          if (event.getAction() == KeyEvent.ACTION_UP) {
            submitUrl();
            return true;
          }
        }
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          submitUrl();
          return true;
        }
        return false; //otherwise let the system handle the event
      }
    });
    return fragmentLayout;
  }

  //when IME_ACTION_DONE happens on the url input.
  public void submitUrl() {
    String url = url_input_txt.getText().toString();
    if (!(url.indexOf("http://") == 0) && !(url.indexOf("https://") == 0)) {
      url = "http://" + url;
    }

    try {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.ServerManager_Message_SavingServer));
        progressDialog.setCancelable(false);
        progressDialog.show();
        final URL u = new URL(url);
          Server server = new Server(u);
          new ServerManagerImpl(getActivity().getSharedPreferences(App.Preferences.FILE_NAME, 0))
                  .verifyServer(server, new ServerManager.VerifyServerCallback() {
                      @Override
                      public void result(boolean correct) {
                          if (correct) {
                              Intent intent = new Intent(getActivity(), WebViewActivity.class);
                              intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, u.toExternalForm());
                              startActivity(intent);
                          } else {
                              Toast.makeText(getActivity(), R.string.ServerManager_Error_IncorrectUrl, Toast.LENGTH_LONG).show();
                          }
                          progressDialog.dismiss();
                      }
                  });
      } catch (MalformedURLException e) {

      }
  }
}
