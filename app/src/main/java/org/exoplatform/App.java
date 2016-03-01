package org.exoplatform;

import android.app.Application;

/**
 * Created by chautn on 10/26/15.
 */
public class App extends Application {

    public static final Double MIN_PLATFORM_VERSION_SUPPORTED = 4.3;

    public static final String SHARED_PREFERENCES_NAME = "eXoPreferences";

    public static final String TRIBE_URL = "https://community.exoplatform.com";

    public static final String PREF_LAST_VISIT_TIME = "LAST_VISIT_TIME";

    public static long DELAY_1H_NANOS = 3600000000000L;

    public static final String PREF_SERVERS_STORAGE = "SERVERS_STORAGE";
}
