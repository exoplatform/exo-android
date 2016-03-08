package org.exoplatform;

import android.app.Application;
import android.content.pm.PackageManager;

import org.exoplatform.model.PlatformInfo;

/**
 * Created by chautn on 10/26/15.
 */
public class App extends Application {

    public static final String TRIBE_URL = "https://community.exoplatform.com";

    public static final long DELAY_1H_NANOS = 3600000000000L;

    public static class Preferences {

        public static final String FILE_NAME = "eXoPreferences";

        public static final String LAST_VISIT_TIME = "LAST_VISIT_TIME";

        public static final String SERVERS_STORAGE = "SERVERS_STORAGE";
    }

    public static class Platform {

        public static final String DOCUMENT_PERSONAL_DRIVE_NAME = "Personal Documents";

        public static final String DOCUMENT_JCR_PATH = "/rest/private/jcr";

        public static final String DOCUMENT_UPLOAD_PATH_REST = "/rest/managedocument/uploadFile/upload";

        public static final String DOCUMENT_CONTROL_PATH_REST            = "/rest/managedocument/uploadFile/control";

        public static final Double MIN_SUPPORTED_VERSION = 4.3;
    }

    public static class Share {

        public static final int MAX_ITEMS_ALLOWED = 10;

        public static final int DOCUMENT_MAX_SIZE_MB = 10;
    }

    public static class Permissions {
        public static final int REQUEST_PICK_IMAGE_FROM_GALLERY       = 8;
    }
}
