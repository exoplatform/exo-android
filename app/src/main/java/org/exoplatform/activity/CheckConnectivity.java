package org.exoplatform.activity;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.exoplatform.R;

public class CheckConnectivity {
    Context mContext;
    public LostConnectionDialog lostConnectionDialog;

    public CheckConnectivity(Activity activity){
        this.mContext = activity.getApplicationContext();
        lostConnectionDialog = new LostConnectionDialog(R.string.OnBoarding_Title_LostCnnection,
                R.string.OnBoarding_Message_LostCnnection, R.string.Word_OK, activity);
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager connMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isConnected = (networkInfo != null && networkInfo.isConnected());
        if (!isConnected) {
            lostConnectionDialog.showDialog();
        }
        return isConnected;
    }
}
