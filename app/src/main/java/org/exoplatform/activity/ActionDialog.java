package org.exoplatform.activity;

import androidx.annotation.StringRes;

import org.exoplatform.R;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.TextView;

public class ActionDialog {

        private TextView tvTitle;
        private TextView tvSubtitle;
        public TextView deleteAction;
        public TextView cancelAction;

        private final Activity activity;
        private Dialog dialog;


        public ActionDialog(@StringRes int title, @StringRes int subtitle, @StringRes int action, Activity activity) {
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
            dialog.setContentView(R.layout.custom_dialog_logo);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        private void findViews(){
            tvTitle = dialog.findViewById(R.id.tv_title);
            tvSubtitle = dialog.findViewById(R.id.tv_subtitle);
            deleteAction = dialog.findViewById(R.id.delete_action);
            cancelAction = dialog.findViewById(R.id.cancel_action);
            cancelAction.setText(R.string.Word_Cancel);
            deleteAction.setTextColor(Color.WHITE);
        }

        private void setData(int title, int subtitle, int action) {
            tvTitle.setText(title);
            tvSubtitle.setText(subtitle);
            deleteAction.setText(action);
        }

}

