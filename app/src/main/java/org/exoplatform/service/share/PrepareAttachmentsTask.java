package org.exoplatform.service.share;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.DateFormat;

import com.squareup.picasso.Picasso;

import org.exoplatform.App;
import org.exoplatform.R;
import org.exoplatform.model.DocumentInfo;
import org.exoplatform.tool.DocumentUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Performs these operations in background:
 * <ol>
 * <li>Check each file URI in mAttachmentsUris</li>
 * <li>If the file is less than 10MB, add it to mActivityPost</li>
 * <li>Stop after 10 files</li>
 * <li>Generate a bitmap for the thumbnail</li>
 * <li>Wait until the Compose fragment is ready and display the thumbnail</li>
 * </ol>
 *
 * @author paristote
 */
public class PrepareAttachmentsTask extends AsyncTask<List<Uri>, Void, PrepareAttachmentsTask.AttachmentsResult> {

    public class AttachmentsResult {
        public Bitmap thumbnail;
        public List<String> attachments;
        public String error;
        public boolean isFatalError;
    }

    public interface Listener {
        void onPrepareAttachmentsFinished(AttachmentsResult result);
    }

    private Context mContext;

    private List<Listener> mListeners;

    public PrepareAttachmentsTask(Context ctx) {
        if (ctx == null) throw new IllegalArgumentException("Context must not be null");
        mContext = ctx.getApplicationContext();
        mListeners = new ArrayList<>();
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    private Bitmap getThumbnail(File origin) {
        Bitmap thumb;
        try {
            // loads, resizes and crops the bitmap to create a square thumbnail
            thumb = Picasso.with(mContext)
                            .load(origin)
                            .resizeDimen(R.dimen.ShareActivity_Thumbnail_Size, R.dimen.ShareActivity_Thumbnail_Size)
                            .centerCrop()
                            .get();
        } catch (IOException e) {
            return null;
        }
        return thumb;
    }

    private File getFileWithRotatedBitmap(DocumentInfo info, String filename) throws IOException {
        FileOutputStream fos = null;
        try {
            // Decode bitmap from input stream
            Bitmap bm = BitmapFactory.decodeStream(info.documentData);
            // Turn the image in the correct orientation
            bm = DocumentUtils.rotateBitmapByAngle(bm, info.orientationAngle);
            File file = new File(mContext.getFilesDir(), filename);
            fos = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            return file;
        } catch (OutOfMemoryError e) {
            throw new RuntimeException("Exception while decoding/rotating the bitmap", e);
        } finally {
            try {
                // try..catch here to not break the process if close() fails
                if (fos != null)
                    fos.close();
            } catch (IOException ignored) {
            }
        }
    }

    private File getFileWithData(DocumentInfo info, String filename) throws IOException {
        FileOutputStream fileOutput = null;
        BufferedInputStream buffInput = null;
        try {
            // create temp file
            fileOutput = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
            buffInput = new BufferedInputStream(info.documentData);
            byte[] buf = new byte[1024];
            int len;
            while ((len = buffInput.read(buf)) != -1) {
                fileOutput.write(buf, 0, len);
            }
            return new File(mContext.getFilesDir(), filename);
        } finally {
            try {
                // try..catch here to not break the process if close() fails
                if (buffInput != null)
                    buffInput.close();
                if (fileOutput != null)
                    fileOutput.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected AttachmentsResult doInBackground(List<Uri>... params) {
        Set<Integer> errors = new HashSet<>();
        AttachmentsResult result = new AttachmentsResult();
        List<Uri> attachmentUris = null;
        if (params.length > 0)
            attachmentUris = params[0];
        if (attachmentUris != null && !attachmentUris.isEmpty()) {
            result.attachments = new ArrayList<>(App.Share.MAX_ITEMS_ALLOWED);
            for (Uri att : attachmentUris) {
                // Stop when we reach the maximum number of files
                if (result.attachments.size() == App.Share.MAX_ITEMS_ALLOWED) {
                    errors.add(R.string.ShareErrorTooManyFiles);
                    break;
                }
                DocumentInfo info = DocumentUtils.documentInfoFromUri(att, mContext);
                // Skip if the file cannot be read
                if (info == null) {
                    errors.add(R.string.ShareErrorCannotReadDoc);
                    continue;
                }
                // Skip if the file is more than 10MB
                if (info.documentSizeKb > (App.Share.DOCUMENT_MAX_SIZE_MB * 1024)) {
                    errors.add(R.string.ShareErrorFileTooBig);
                    continue;
                }
                // All good, let's copy this file in our app's storage
                // We must do this because some sharing apps (e.g. Google Photos)
                // will revoke the permission on the files when the activity stops,
                // therefore the service won't be able to access them
                String cleanName = DocumentUtils.cleanupFilename(info.documentName);
                String tempFileName = DateFormat.format("yyyy-MM-dd-HH:mm:ss", System.currentTimeMillis()) + "-" + cleanName;

                try {
                    // Create temp file
                    File tempFile = null;
                    if ("image/jpeg".equals(info.documentMimeType) && info.orientationAngle != DocumentUtils.ROTATION_0) {
                        // For an image with an EXIF rotation information, we get the file
                        // from the bitmap rotated back to its correct orientation
                        tempFile = getFileWithRotatedBitmap(info, tempFileName);
                    } else {
                        // Otherwise we just write the data to a file
                        tempFile = getFileWithData(info, tempFileName);
                    }
                    // add file to list
                    result.attachments.add(tempFile.getAbsolutePath());
                    if (result.thumbnail == null) {
                        result.thumbnail = getThumbnail(tempFile);
                    }
                } catch (Exception e) {
                    errors.add(R.string.ShareErrorCannotReadDoc);
                }
            }
            // Done creating the files
            // Create an error message (if any) to display in onPostExecute
            if (!errors.isEmpty()) {
                StringBuilder message;
                if (result.attachments.size() == 0) {
                    message = new StringBuilder(mContext.getString(R.string.ShareErrorAllFilesCannotShare)).append(":");
                    result.isFatalError = true;
                } else {
                    message = new StringBuilder(mContext.getString(R.string.ShareErrorSomeFilesCannotShare)).append(":");
                    result.isFatalError = false;
                }
                for (Integer errCode : errors) {
                    switch (errCode) {
                        case R.string.ShareErrorCannotReadDoc:
                        case R.string.ShareErrorFileTooBig:
                        case R.string.ShareErrorTooManyFiles:
                            message.append("\n").append(mContext.getString(errCode));
                            break;
                    }
                }
                result.error = message.toString();
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(AttachmentsResult result) {
        for (Listener listener: mListeners) {
            if (listener != null)
                listener.onPrepareAttachmentsFinished(result);
        }
    }
}
