package org.exoplatform.tool;

/*
 * Copyright (C) 2003-${YEAR} eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.exoplatform.App;
import org.exoplatform.model.DocumentInfo;
import org.exoplatform.model.SocialActivity;
import org.exoplatform.model.UploadInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import okhttp3.Cookie;

public class DocumentUtils {

  private static final String LOG_TAG = DocumentUtils.class.getName();

  public static String encodeDocumentUrl(String urlString) {
    try {

      URL url = new URL(urlString);
      URI uri = new URI(url.getProtocol(),
                        url.getUserInfo(),
                        url.getHost(),
                        url.getPort(),
                        url.getPath(),
                        url.getQuery(),
                        url.getRef());

      return uri.toASCIIString();

    } catch (MalformedURLException e) {
      Log.d(DocumentUtils.class.getSimpleName(), e.getMessage(), e);
      return null;
    } catch (URISyntaxException e) {
      Log.d(DocumentUtils.class.getSimpleName(), e.getMessage(), e);
      return null;
    }

  }

  /**
   * Creates a folder at the JCR destination url, with Webdav.
   *
   * @param uploadInfo Upload information
   * @return true if the folder exists or was created, false otherwise
   */
  public static boolean createFolder(UploadInfo uploadInfo) {
    HttpResponse response;
    try {
      String folderUrl = uploadInfo.jcrUrl + "/" + uploadInfo.folder;
      String destination = encodeDocumentUrl(folderUrl);
      StringBuilder cookieString = new StringBuilder();
      for (Cookie c : ExoHttpClient.cookiesForUrl(destination)) {
        cookieString.append(c.name()).append("=").append(c.value()).append(";");
      }
      WebdavMethod propfind = new WebdavMethod("PROPFIND", destination);
      // TODO use okhttp
      propfind.addHeader("Cookie", cookieString.toString());
      response = new DefaultHttpClient().execute(propfind);
      int status = response.getStatusLine().getStatusCode();
      if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
        return true;
      } else {
          try {
            String stringUrl = new StringBuilder(PlatformUtils.getPlatformDomain())
                    .append(App.Platform.CREATE_FOLDER_PATH_REST)
                    .toString();
            Uri createFolderUri = Uri.parse(stringUrl);
            createFolderUri = createFolderUri.buildUpon()
                    .appendQueryParameter("workspaceName", PlatformUtils.getPlatformInfo().defaultWorkSpaceName)
                    .appendQueryParameter("driveName", uploadInfo.drive)
                    .appendQueryParameter("currentFolder", uploadInfo.folder)
                    .appendQueryParameter("folderName", "mobile")
                    .build();
            HttpGet createFolderReq = new HttpGet(createFolderUri.toString());
            cookieString = new StringBuilder();

            for (okhttp3.Cookie c : ExoHttpClient.cookiesForUrl(stringUrl.toString())) {
              cookieString.append(c.name()).append("=").append(c.value()).append(";");
            }
            createFolderReq.addHeader("Cookie", cookieString.toString());
            // Execute the request and retrieve the status code
            HttpResponse createFolder = new DefaultHttpClient().execute(createFolderReq);

            status = createFolder.getStatusLine().getStatusCode();
            return status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES;

          } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            return false;
          }
        }
    } catch (IOException e) {
      Log.e(LOG_TAG, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Returns a DocumentInfo with info coming from the file at the given URI
   * 
   * @param document the URI of a file or a content
   * @param context the context used to get the content resolver
   * @return a DocumentInfo or null if an error occurs
   */
  public static DocumentInfo documentInfoFromUri(Uri document, Context context) {
    if (document == null)
      return null;

    if (document.toString().startsWith("content://")) {
      /*
       * Some apps send fake content:// URI with real file:// URI inside E.g.
       * open ASTRO File Manager > View File > Share :
       * content://authority/-1/1/file:///sdcard/path/file.jpg/ACTUAL/123 Then
       * we extract the real URI and pass it to documentFromFileUri(...)
       */
      String decodedUri = Uri.decode(document.toString());
      int fileIdx = decodedUri.indexOf("file://");
      if (fileIdx > -1) {
        long id = -1;
        try {
          id = ContentUris.parseId(document);
        } catch (NumberFormatException | UnsupportedOperationException e) {
          Log.e(LOG_TAG, e.getMessage(), e);
        }
        String fileUri = decodedUri.substring(fileIdx);
        fileUri = fileUri.replaceAll("(/ACTUAL/)(" + id + ")", "");
        return documentFromFileUri(Uri.parse(fileUri));
      } else {
        return documentFromContentUri(document, context);
      }
    } else if (document.toString().startsWith("file://")) {
      return documentFromFileUri(document);
    } else {
      return null; // other formats not supported
    }
  }

  /**
   * Gets a DocumentInfo with info coming from the document at the given URI.
   * 
   * @param contentUri the content URI of the document (content:// ...)
   * @param context the context used to get the content resolver
   * @return a DocumentInfo or null if an error occurs
   */
  public static DocumentInfo documentFromContentUri(Uri contentUri, Context context) {
    if (contentUri == null)
      return null;

    try {
      ContentResolver cr = context.getContentResolver();
      Cursor c = cr.query(contentUri, null, null, null, null);
      if (c == null)
        return null;
      int sizeIndex = c.getColumnIndex(OpenableColumns.SIZE);
      int nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
      int orientIndex = c.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION);
      c.moveToFirst();

      DocumentInfo document = new DocumentInfo();
      document.documentName = c.getString(nameIndex);
      document.documentSizeKb = c.getLong(sizeIndex) / 1024;
      document.documentData = cr.openInputStream(contentUri);
      document.documentMimeType = cr.getType(contentUri);
      if (orientIndex != -1) { // if found orientation column
        document.orientationAngle = c.getInt(orientIndex);
      }
      c.close();
      return document;
    } catch (FileNotFoundException e) {
      Log.d(LOG_TAG, e.getMessage(), e);
    } catch (Exception e) {
      Log.e(LOG_TAG, "Cannot retrieve the content at " + contentUri);
      Log.d(LOG_TAG, e.getMessage() + "\n" + Log.getStackTraceString(e));
    }
    return null;
  }

  /**
   * Gets a DocumentInfo with info coming from the file at the given URI.
   * 
   * @param fileUri the file URI (file:// ...)
   * @return a DocumentInfo or null if an error occurs
   */
  public static DocumentInfo documentFromFileUri(Uri fileUri) {
    if (fileUri == null)
      return null;

    try {
      URI uri = new URI(fileUri.toString());
      File file = new File(uri);

      DocumentInfo document = new DocumentInfo();
      document.documentName = file.getName();
      document.documentSizeKb = file.length() / 1024;
      document.documentData = new FileInputStream(file);
      // Guess the mime type in 2 ways
      try {
        // 1) by inspecting the file's first bytes
        document.documentMimeType = URLConnection.guessContentTypeFromStream(document.documentData);
      } catch (IOException e) {
        document.documentMimeType = null;
      }
      if (document.documentMimeType == null) {
        // 2) if it fails, by stripping the extension of the filename
        // and getting the mime type from it
        String extension = "";
        int dotPos = document.documentName.lastIndexOf('.');
        if (0 <= dotPos)
          extension = document.documentName.substring(dotPos + 1);
        document.documentMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
      }
      // Get the orientation angle from the EXIF properties
      if ("image/jpeg".equals(document.documentMimeType))
        document.orientationAngle = getExifOrientationAngleFromFile(file.getAbsolutePath());
      return document;
    } catch (URISyntaxException | FileNotFoundException e) {
      Log.e(LOG_TAG, "Cannot retrieve the file at " + fileUri);
      Log.d(LOG_TAG, e.getMessage() + "\n" + Log.getStackTraceString(e));
    }
    return null;
  }

  /**
   * Delete the Files at the given paths
   * 
   * @param files a list of file paths
   * @return true if all files were deleted, false otherwise
   */
  public static boolean deleteLocalFiles(List<String> files) {
    boolean result = true;
    if (files != null) {
      for (String filePath : files) {
        File f = new File(filePath);
        boolean del = f.delete();
        Log.d(LOG_TAG, "File " + f.getName() + " deleted: " + (del ? "YES" : "NO"));
        result &= del;
      }
    }
    return result;
  }

  /**
   * On Platform 4.1-M2, the upload service renames the uploaded file. Therefore
   * the link to this file in the activity becomes incorrect. To fix this, we
   * rename the file before upload so the same name is used in the activity.
   * 
   * @param originalName the name to clean
   * @return a String without forbidden characters
   */
  public static String cleanupFilename(String originalName) {
    final String TILDE_HYPHENS_COLONS_SPACES = "[~_:\\s]";
    final String MULTIPLE_HYPHENS = "-{2,}";
    final String FORBIDDEN_CHARS = "[`!@#\\$%\\^&\\*\\|;\"'<>/\\\\\\[\\]\\{\\}\\(\\)\\?,=\\+\\.]+";
    String name = originalName;
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
    // Locale loc = new
    // Locale(SettingUtils.getPrefsLanguage(getApplicationContext()));
    name = name.toLowerCase(Locale.getDefault());
    // Remove consecutive -
    name = Pattern.compile(MULTIPLE_HYPHENS).matcher(name).replaceAll("-");
    // Save
    return (name + ext);
  }

  public static final int ROTATION_0   = 0;

  public static final int ROTATION_90  = 90;

  public static final int ROTATION_180 = 180;

  public static final int ROTATION_270 = 270;

  /**
   * Get the EXIF orientation of the given file
   * 
   * @param filePath the file's path used to create the ExifInterface
   * @return an int in DocumentUtils.ROTATION_[0 , 90 , 180 , 270]
   */
  public static int getExifOrientationAngleFromFile(String filePath) {
    int ret = ROTATION_0;
    try {
      ret = new ExifInterface(filePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
      switch (ret) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        ret = ROTATION_90;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        ret = ROTATION_180;
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        ret = ROTATION_270;
        break;
      default:
        break;
      }
    } catch (IOException e) {
      Log.d(DocumentUtils.class.getSimpleName(), e.getMessage(), e);
    }
    return ret;
  }

  /**
   * Rotate the bitmap at its correct orientation
   * 
   * @param filePath the file where the bitmap is stored
   * @param source the bitmap itself
   * @return the bitmap rotated with
   *         {@link DocumentUtils#rotateBitmapByAngle(Bitmap, int)}
   */
  public static Bitmap rotateBitmapToNormal(String filePath, Bitmap source) {
    Bitmap ret = source;

    int orientation = getExifOrientationAngleFromFile(filePath);
    // Sometimes we get an orientation = 1
    // To avoid a 1º rotation,
    // we rotate only when the orientation is exactly 90º or 180º or 270º
    if (orientation == ROTATION_90 || orientation == ROTATION_180 || orientation == ROTATION_270) {
      ret = rotateBitmapByAngle(source, orientation);
    }
    return ret;
  }

  /**
   * Rotate the bitmap by a certain angle. Uses {@link Matrix#postRotate(float)}
   * 
   * @param source the bitmap to rotate
   * @param angle the rotation angle
   * @return a new rotated bitmap
   */
  public static Bitmap rotateBitmapByAngle(Bitmap source, int angle) {
    Bitmap ret = source;
    int w, h;
    w = source.getWidth();
    h = source.getHeight();
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    try {
      ret = Bitmap.createBitmap(source, 0, 0, w, h, matrix, true);
    } catch (OutOfMemoryError e) {
      Log.d(DocumentUtils.class.getSimpleName(), e.getMessage(), e);
    }
    return ret;
  }

  private static String permissionForCode(final int permCode) {
    String permission;
    switch (permCode) {
    case App.Permissions.REQUEST_PICK_IMAGE_FROM_GALLERY:
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        // On Jelly Bean and after, return the actual READ_EXTERNAL_STORAGE
        // permission
        permission = permissionReadExternalStorage();
      else
        // Otherwise returning WRITE_EXTERNAL_STORAGE implicitly grants
        // READ_EXTERNAL_STORAGE
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
      break;
    default:
      throw new IllegalArgumentException("Given permission code is incorrect: " + permCode);
    }
    return permission;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private static String permissionReadExternalStorage() {
    return Manifest.permission.READ_EXTERNAL_STORAGE;
  }

  /**
   * Check whether the application needs to request the permission required by
   * the activity. If yes, then the permission is requested (via
   * {@link ActivityCompat#requestPermissions(Activity, String[], int)}).
   * 
   * @param caller The activity that requires the permission. Must implement
   *          {@link OnRequestPermissionsResultCallback}.
   * @param permissionCode The code defined internally, e.g.
   *          {@link org.exoplatform.App.Permissions#REQUEST_PICK_IMAGE_FROM_GALLERY}
   *          .
   * @return true if the permission has been requested <br/>
   *         false if the permission was already granted
   */
  public static boolean didRequestPermission(Activity caller, int permissionCode) {
    if (caller == null || !(caller instanceof OnRequestPermissionsResultCallback))
      throw new IllegalArgumentException("Caller activity must implement OnRequestPermissionsResultCallback");

    boolean res = false;
    String permission = permissionForCode(permissionCode);
    int check = ContextCompat.checkSelfPermission(caller, permission);
    if (check != PackageManager.PERMISSION_GRANTED) {
      res = true;
      ActivityCompat.requestPermissions(caller, new String[] { permission }, permissionCode);
    }
    return res;
  }

  /**
   * Check whether the request for the specified permission should be explained
   * to the user. Calls
   * {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)}
   * .
   * 
   * @param activity The activity that requires the permission.
   * @param permCode The code defined internally, e.g.
   *          {@link org.exoplatform.App.Permissions#REQUEST_PICK_IMAGE_FROM_GALLERY}
   *          .
   * @return true if the user should receive more information about the
   *         permission request
   */
  public static boolean shouldDisplayExplanation(Activity activity, int permCode) {
    if (activity == null)
      throw new IllegalArgumentException("Caller activity must not be null");
    String permission = permissionForCode(permCode);
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
  }
}
