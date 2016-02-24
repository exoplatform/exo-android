package org.exoplatform.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.exoplatform.R;
import org.exoplatform.activity.WebViewActivity;

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
    Intent intent = new Intent(getActivity(), WebViewActivity.class);
    intent.putExtra(WebViewActivity.RECEIVED_INTENT_KEY, url);
    startActivity(intent);
  }
}
