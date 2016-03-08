package org.exoplatform.model;

import android.content.Context;
import android.util.Log;

import org.exoplatform.BuildConfig;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by paristote on 3/7/16.
 */
public class DocumentInfo {
    public String      documentName;

    public long        documentSizeKb;

    public InputStream documentData;

    public String      documentMimeType;

    public int         orientationAngle = 0; // ROTATION_0

    @Override
    public String toString() {
        return String.format(Locale.US, "File %s [%s - %s KB]", documentName, documentMimeType, documentSizeKb);
    }

    public void closeDocStream() {
        if (documentData != null)
            try {
                documentData.close();
            } catch (IOException e) {
                if (BuildConfig.DEBUG)
                    Log.d("", e.getMessage(), e);
            }
    }

    /**
     * On Platform 4.1-M2, the upload service renames the uploaded file.
     * Therefore the link to this file in the activity becomes incorrect. To fix
     * this, we rename the file before upload so the same name is used in the
     * activity.
     */
    public void cleanupFilename(Context context) {
        final String TILDE_HYPHENS_COLONS_SPACES = "[~_:\\s]";
        final String MULTIPLE_HYPHENS = "-{2,}";
        final String FORBIDDEN_CHARS = "[`!@#\\$%\\^&\\*\\|;\"'<>/\\\\\\[\\]\\{\\}\\(\\)\\?,=\\+\\.]+";
        String name = documentName;
        String ext = "";
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length()) {
            ext = name.substring(lastDot); // the ext with the dot
            name = name.substring(0, lastDot); // the name before the ext
        }
        // [~_:\s] Replaces ~ _ : and spaces by -
        name = Pattern.compile(TILDE_HYPHENS_COLONS_SPACES).matcher(name).replaceAll("-");
        // [`!@#\$%\^&\*\|;"'<>/\\\[\]\{\}\(\)\?,=\+\.]+ Deletes forbidden chars
        name = Pattern.compile(FORBIDDEN_CHARS).matcher(name).replaceAll("");
        // Converts accents to regular letters
        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        // Replaces upper case characters by lower case
        name = name.toLowerCase(Locale.getDefault());
        // Remove consecutive -
        name = Pattern.compile(MULTIPLE_HYPHENS).matcher(name).replaceAll("-");
        // Save
        documentName = name + ext;
    }
}
