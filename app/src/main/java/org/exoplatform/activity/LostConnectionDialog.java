package org.exoplatform.activity;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.StringRes;

import org.exoplatform.R;

public class LostConnectionDialog {

    private TextView lostConnectionTitle;
    private TextView lostConnectionMessage;
    public TextView okAction;
    private Activity activity;
    private Dialog dialog;


    public LostConnectionDialog(@StringRes int title, @StringRes int subtitle, @StringRes int action, Activity activity) {
        this.activity = activity;

        setDialog();
        findViews();
        setData(title, subtitle, action);
    }




    public void showDialog(){
        dialog.show();
    }

    public void dismiss(){
        dialog.dismiss();
    }

    private void setDialog() {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.lost_internet_connection_popup);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void findViews(){
        lostConnectionTitle = dialog.findViewById(R.id.lost_connection_title);
        lostConnectionMessage = dialog.findViewById(R.id.lost_connection_message);
        okAction = dialog.findViewById(R.id.ok_action);
        okAction.setText(R.string.Word_OK);
    }

    private void setData(int title, int subtitle, int action) {
        lostConnectionTitle.setText(title);
        lostConnectionMessage.setText(subtitle);
        okAction.setText(action);
        okAction.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               dialog.dismiss();
            }
        });
    }

}
