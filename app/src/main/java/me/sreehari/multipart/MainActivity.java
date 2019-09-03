package me.sreehari.multipart;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.bikomobile.multipart.Multipart;
import com.bikomobile.multipart.MultipartRequest;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<Uri> dataUri = new ArrayList<Uri>();
    private static final int REQUEST_FILE_CODE = 1998;
    private Button mPickFiles;
    private Button mUploadFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        mPickFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckPermissions();

            }
        });

        mUploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callUploadApi();
            }
        });

    }

    private void callUploadApi() {
        final String url = "Your API URL";
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading..");
        progressDialog.show();
        Multipart multipart = new Multipart(MainActivity.this);

        for (int i = 0; i < dataUri.size(); i++) {
            multipart.addFile(getMimeType(dataUri.get(i)), "files", getFileName(dataUri.get(i)), dataUri.get(i));
        }
        multipart.addParam("param name", "add your params here");

        MultipartRequest multipartRequest = multipart.getRequest(url, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                progressDialog.dismiss();
                try {
                    JSONObject responseobj = new JSONObject(new String(response.data, HttpHeaderParser.parseCharset(response.headers)));
                    Log.e("Response", "" + response);
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.e("Error", "uploading data");
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(multipartRequest);
    }

    private void initViews() {
        mPickFiles = findViewById(R.id.pick_files);
        mUploadFiles = findViewById(R.id.upload_files);
    }

    private void CheckPermissions() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        pickFiles();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Permission to access files is needed to read and upload dataset, please try again");
                        builder.setTitle("Permission needed");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                CheckPermissions();

                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                        builder.show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {/* ... */}
                }).check();
    }

    private void pickFiles() {

        Intent fileManagerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        //Choose any file
        fileManagerIntent.setType("*/*");
        fileManagerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(fileManagerIntent, REQUEST_FILE_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_CODE) {
            if (data != null) { // checking empty selection
                if (data.getClipData() != null) {
                    // checking multiple selection or not
                    dataUri.clear();
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
//                        Log.d("Uri", "" + uri);
                        dataUri.add(uri);
                    }
                    if (!dataUri.isEmpty()) {
                        Log.d("Selection", "" + dataUri);
                    }
                } else {
                    dataUri.clear();
                    Uri uri = data.getData();
//                    Log.d("Uri", "" + uri);
                    dataUri.add(uri);
                }
            }
        }
    }

    private String getMimeType(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = mime.getExtensionFromMimeType(cR.getType(uri));
        cR.getType(uri);
        return type;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
//        Log.d("File Name inside req", "" + result);
        return result;
    }
}

