package org.exoplatform.exohybridapp;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Created by chautn on 10/26/15.
 */
public class App extends Application {

  public static final String SHARED_PREFERENCES_NAME = "eXoPreferences";

  @Override
  public void onCreate() {
    super.onCreate();
    SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString("BASE_URL", "http://plfent-4.3.x-pkgpriv-responsive-design-snapshot.acceptance6.exoplatform.org");
    editor.commit();
  }
}
