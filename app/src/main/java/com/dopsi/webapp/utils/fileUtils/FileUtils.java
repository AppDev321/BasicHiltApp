package com.dopsi.webapp.utils.fileUtils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import com.dopsi.webapp.R;
import com.dopsi.webapp.utils.AppLogger;
import com.dopsi.webapp.utils.MimeTypeUtil;
import com.dopsi.webapp.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import static com.dopsi.webapp.utils.fileUtils.CacheHelper.TAG;
import static com.dopsi.webapp.utils.fileUtils.CacheHelper.getAppPrivateDir;
import static com.dopsi.webapp.utils.fileUtils.CacheHelper.getGiffyFolder;

public class FileUtils {

    private static final String PUBLIC_DOWNLOADS = "content://downloads/public_downloads";
    public static final String APP_GIF_PATH = "/App/Media/Animated Gifs/.Sent";
    public static final String APP_CHAT_BACKGROUND_PATH = "/App/Media/Chat Background/";
    private static final String ANDROID_DOCUMENT_FOLDER = "com.android.providers.media.documents";
    public static final SimpleDateFormat IMAGE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    private static final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        String fileName = cursor.getString(0);
                        String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                        if (!TextUtils.isEmpty(path)) {
                            return path;
                        }
                    }
                } finally {
                    if (cursor != null)
                        cursor.close();
                }

                try {
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse(PUBLIC_DOWNLOADS), Long.parseLong(DocumentsContract.getDocumentId(uri)));

                    return getPathFromUri(context, contentUri, null, null);
                } catch (NumberFormatException e) {
                    //In Android 8 and Android P the id is not a number
                    return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getPathFromUri(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            List<String> segments = uri.getPathSegments();
            String path;
            if ((context.getPackageName() + ".files").equals(uri.getAuthority()) && segments.size() > 1 && segments.get(0).equals("external")) {
                path = Environment.getExternalStorageDirectory().getAbsolutePath() + uri.getPath().substring(segments.get(0).length() + 1);
            } else {
                path = getPathFromUri(context, uri, null, null);
            }
            if (path != null) {
                File file = new File(path);
                if (!file.canRead()) {
                    return null;
                }
            }
            return path;
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            if (uri.toString().contains("GIPHY")) {
                return getFilePathFromURI(context, uri, getGiffyFolder());
            } else {
                return uri.getPath();
            }
        }
        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getPathFromUri(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            return getFilePathFromURI(context, uri, getGiffyFolder());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return ANDROID_DOCUMENT_FOLDER.equals(uri.getAuthority());
    }


    private static String getFilePathFromURI(Context context, Uri contentUri, String path) {
        //copy file and send new file path
        String fileName = getFileNameFromUri(context, contentUri);
        if (fileName == null)
            return "";
        if (!TextUtils.isEmpty(fileName)) {
            File fileDestination = new File(Environment.getExternalStorageDirectory().getPath() + path);
            if (!fileDestination.exists())
                fileDestination.mkdirs();
            File copyFile = new File(fileDestination, fileName);
            if (!copyFile.exists())
                copyFileFromUriToAnotherDestination(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        try {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                fileName = path.substring(cut + 1);
                if (FilenameUtils.getExtension(fileName).isEmpty()) {
                    fileName = fileName + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(MimeUtils.guessMimeTypeFromUri(context, uri));
                }
            }
        } catch (NullPointerException e) {
            return null;
        }
        return fileName;
    }

    public static void copyFileFromUriToAnotherDestination(Context context, Uri srcUri, File fileDestination) {
      /*  try {
            // Create file upload directory if it doesn't exist

            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(fileDestination);
            IOUtils.copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {//making generic as observing security exception as well
            e.printStackTrace();
        }*/
    }

    public static Drawable getFileIcon(Context context, @NonNull String path) {

        String extension = FilenameUtils.getExtension(path);
        Drawable icon;
        switch (extension) {
            case "apk":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_apk);
                break;

            case "dmg":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_dmg);
                break;

            case "exe":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_exe);
                break;

            case "txt":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_txt);
                break;

            case "docx":
            case "doc":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_doc);
                break;

            case "rtf":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_rtf);
                break;

            case "key":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_key);
                break;

            case "rar":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_rar);
                break;

            case "zip":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_zip);
                break;

            case "xlsx":
            case "xls":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_xls);
                break;

            case "pdf":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_pdf);
                break;

            case "pptx":
            case "ppt":
                icon = ContextCompat.getDrawable(context, R.drawable.ic_file_ppt);
                break;

            default:
                icon = null;
                break;
        }
        return icon;

    }

    public static String getFileSizeByBytes(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format(Locale.ENGLISH, "%.1f %cB", bytes / 1000.0, ci.current());
    }

    public static String getFileSizeByUri(Uri uri) {
        long size = 0;
        if (uri.getPath() != null)
            size = new File(uri.getPath()).length();
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }



    public static String getOutputFilePath(String type, String filename) {
        File outputFile;
        // All type file should be store to app's private storage ie,"Android/media" for Q and above
        if (Utils.INSTANCE.getAndroidQAndAbove()) {
            outputFile = new File(getAppPrivateDir() + "/" + type + "/", ".sent");
        } else {
            outputFile = new File(Environment.getExternalStorageDirectory().getPath(),
                    CacheHelper.getScrambleMediaFolder() + "/" + type + "/" + ".sent"
            );
        }
        if (!outputFile.exists()) {
            outputFile.mkdirs();
        }
        return (outputFile.getAbsolutePath() + "/" + FilenameUtils.getBaseName(filename) + new SimpleDateFormat("_yyyyMMdd", Locale.ENGLISH).format(Calendar.getInstance().getTime()) + "_CM." + FilenameUtils.getExtension(filename));

    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }



    private static int getHeightWithAspectRatio(int width, double heightScalingFactor) {
        return (int) (width * heightScalingFactor);
    }

    private static int getWidthWithAspectRatio(int height, double widthScalingFactor) {
        return (int) (height * widthScalingFactor);
    }

    private static boolean isHeightLessThenMinimum(int height, int width) {

        final int thirtyPercentOfWidth = (30 * width) / 100;

        //We will restrict height to a minimum limit(30% of width).
        return height < thirtyPercentOfWidth;
    }

    private static boolean isWidthLessThenMinimum(int width, int height) {

        final int thirtyPercentOfHeight = (30 * height) / 100;

        //We will restrict width to a minimum limit(30% of height).
        return width < thirtyPercentOfHeight;
    }

    private static int getMinimumHeight(int width) {

        //We will restrict height to a minimum limit(30% of width).
        return (30 * width) / 100;
    }

    private static int getMinimumWidth(int height) {

        //We will restrict width to a minimum limit(30% of height).
        return (50 * height) / 100;
    }


    public static String readableFileSize(Long size) {
        if (size <= 0) return "0";
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static boolean isImageOrVideoFile(String mimeType) {
        if (mimeType == null)
            return false;
        return mimeType.startsWith(MimeTypeUtil.MIME_START_WITH_IMAGE) || mimeType.startsWith(MimeTypeUtil.MIME_START_WITH_VIDEO);

    }



    public static String getLogsPath() {
        File outputFile;
        // All type file should be store to app's private storage ie,"Android/media/Logs" for Q and above
        if (Utils.INSTANCE.getAndroidQAndAbove()) {
            outputFile = new File(getAppPrivateDir() + "/Logs");
        } else {
            outputFile = new File(Environment.getExternalStorageDirectory().getPath(),
                    CacheHelper.getScrambleMediaFolder() + "/Logs"
            );
        }
        try {
            if (!outputFile.exists()) {
                outputFile.mkdirs();
            }
        } catch (Exception e) {
            AppLogger.INSTANCE.e(TAG, "Error in creating directory for logs");
        }

        return outputFile.getAbsolutePath();
    }

    public static Boolean fileExist(Context context, Uri uri) {
        String filePath = getPath(context, uri);
        if (filePath == null)
            return false;

        File file = new File(filePath);
        return (file != null && file.exists()) ? true : false;
    }
}
