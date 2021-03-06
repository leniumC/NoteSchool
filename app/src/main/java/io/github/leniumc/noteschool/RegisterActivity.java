package io.github.leniumc.noteschool;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String SERVER_IP = "http://192.168.0.105/NoteSchool/";

    private static final int READ_REQUEST_CODE = 42;
    private String avatarPath = "";
    private TextView avatarFilename;
    private EditText studentIdEditText, usernameEditText, passwordEditText,
            gradeEditText, descriptionEditText;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        avatarFilename = findViewById(R.id.avatar_file_name);
        studentIdEditText = findViewById(R.id.student_id);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        gradeEditText = findViewById(R.id.grade);
        descriptionEditText = findViewById(R.id.description);
    }

    public void register(View view) {
        verifyStoragePermissions(this);
        if (studentIdEditText.getText().toString().trim().length() == 0 ||
                usernameEditText.getText().toString().trim().length() == 0 ||
                passwordEditText.getText().toString().trim().length() == 0 ||
                gradeEditText.getText().toString().trim().length() == 0) {
            Toast.makeText(this, "信息未填写完整", Toast.LENGTH_SHORT).show();
        } else if (avatarPath.equals("")) {
            Toast.makeText(this, "未选择头像", Toast.LENGTH_SHORT).show();
        } else {
            makeRequest(getApplicationContext(), avatarPath,
                    studentIdEditText.getText().toString(),
                    usernameEditText.getText().toString(),
                    passwordEditText.getText().toString(),
                    gradeEditText.getText().toString(),
                    descriptionEditText.getText().toString());
        }
    }

    public void makeRequest(final Context context, final String filePath,
                            String studentId, String username, String password,
                            String grade, String description) {
        byte[] bytesOfMessage = new byte[0];
        try {
            bytesOfMessage = password.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] digest = md.digest(bytesOfMessage);
        StringBuffer sb = new StringBuffer();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        password = sb.toString();
        try {
            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            String uploadId =
                    new MultipartUploadRequest(context, SERVER_IP + "create_user.php")
                            .setUtf8Charset()
                            .addParameter("user_id", studentId)
                            .addParameter("user_password", password)
                            .addParameter("user_grade", grade)
                            .addParameter("user_name", username)
                            .addParameter("user_description", description)
                            .setNotificationConfig(new UploadNotificationConfig()
                                    .setTitleForAllStatuses(new File(filePath).getName()))
                            .setDelegate(new UploadStatusDelegate() {
                                @Override
                                public void onProgress(Context context, UploadInfo uploadInfo) {

                                }

                                @Override
                                public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
                                    exception.printStackTrace();
                                }

                                @Override
                                public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                    int integer = Integer.parseInt(serverResponse.getBodyAsString());
                                    if (integer == -1 || integer == 1) {
                                        Toast.makeText(context, "连接错误", Toast.LENGTH_LONG).show();
                                    } else if (integer == 2) {
                                        Toast.makeText(context, "用户ID已注册", Toast.LENGTH_LONG).show();
                                    } else if (integer == 4) {
                                        Toast.makeText(context, "姓名不合法", Toast.LENGTH_LONG).show();
                                    } else if (integer == 6) {
                                        Toast.makeText(context, "简介过长", Toast.LENGTH_LONG).show();
                                    } else if (integer == 7) {
                                        Toast.makeText(context, "图片文件过大", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(context, "success", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onCancelled(Context context, UploadInfo uploadInfo) {

                                }
                            })
                            .addFileToUpload(filePath, "uploaded_file")
                            .setMaxRetries(2)
                            .startUpload();
        } catch (Exception exc) {
            Log.e("AndroidUploadService", exc.getMessage(), exc);
        }
    }

    public void addAvatar(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择头像"), READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = resultData.getData();
            String path = getPath(this, uri);
            if (path != null) {
                File file = new File(path);
                avatarPath = path;
                avatarFilename.setText(file.getName());
            } else {
                new AlertDialog.Builder(this).setTitle("文件错误")
                        .setMessage("文件选择有误，请尝试更换文件或文件目录")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {}
                        });
            }
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
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

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
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
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
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
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
