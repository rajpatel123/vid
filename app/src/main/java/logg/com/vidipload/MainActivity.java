package logg.com.vidipload;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_VIDEO_CAPTURE = 300;
    private static final int READ_REQUEST_CODE = 200;
    private static ProgressDialog pDialog;
    private Uri uri;
    private String pathToStoredVideo;
    private VideoView displayRecordedVideo;
    private static final String SERVER_PATH = " http://akwebtech.com/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayRecordedVideo = (VideoView)findViewById(R.id.video_display);
        Button captureVideoButton = (Button)findViewById(R.id.capture_video);
        captureVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent videoCaptureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if(videoCaptureIntent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(videoCaptureIntent, REQUEST_VIDEO_CAPTURE);
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_VIDEO_CAPTURE){
            uri = data.getData();
            if(EasyPermissions.hasPermissions(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                displayRecordedVideo.setVideoURI(uri);
                displayRecordedVideo.start();

                pathToStoredVideo = getRealPathFromURIPath(uri, MainActivity.this);
                Log.d(TAG, "Recorded Video Path " + pathToStoredVideo);
                //Store the video to your server
                uploadVideoToServer(pathToStoredVideo);

            }else{
                EasyPermissions.requestPermissions(MainActivity.this, getString(R.string.read_file), READ_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    private String getFileDestinationPath(){
        String generatedFilename = String.valueOf(System.currentTimeMillis());
        String filePathEnvironment = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        File directoryFolder = new File(filePathEnvironment + "/video/");
        if(!directoryFolder.exists()){
            directoryFolder.mkdir();
        }
        Log.d(TAG, "Full path " + filePathEnvironment + "/video/" + generatedFilename + ".mp4");
        return filePathEnvironment + "/video/" + generatedFilename + ".mp4";
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, MainActivity.this);
    }
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(uri != null){
            if(EasyPermissions.hasPermissions(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                displayRecordedVideo.setVideoURI(uri);
                displayRecordedVideo.start();

                pathToStoredVideo = getRealPathFromURIPath(uri, MainActivity.this);
                Log.d(TAG, "Recorded Video Path " + pathToStoredVideo);
                //Store the video to your server
                uploadVideoToServer(pathToStoredVideo);

            }
        }
    }
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "User has denied requested permission");
    }

    private void uploadVideoToServer(String pathToVideoFile){
        File videoFile = new File(pathToVideoFile);
        RequestBody videoBody = RequestBody.create(
                okhttp3.MultipartBody.FORM, "file");

RequestBody cid = RequestBody.create(
        okhttp3.MultipartBody.FORM, "23");

        MultipartBody.Part vFile = MultipartBody.Part.createFormData("file", videoFile.getName(), videoBody);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_PATH)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        VideoInterface vInterface = retrofit.create(VideoInterface.class);
        showProgressDialog(this);
        Call<UploadResponse>  serverCom = vInterface.uploadVideoToServer(cid,"video",vFile);
        serverCom.enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
               dismissProgressDialog();
                UploadResponse uploadResponse = response.body();
                 if (response.code()==200){
                     if (uploadResponse!=null && Integer.parseInt(uploadResponse.getStatus())==1){
                         Toast.makeText(MainActivity.this, uploadResponse.getMessage(), Toast.LENGTH_SHORT).show();
                     }else{
                         Toast.makeText(MainActivity.this, "Unable to uplaod, try again later", Toast.LENGTH_SHORT).show();
                     }

                 }
            }
            @Override
            public void onFailure(Call<UploadResponse> call, Throwable t) {
                dismissProgressDialog();
                Log.d(TAG, "Error message " + t.getMessage());
            }
        });
    }
    private String getRealPathFromURIPath(Uri contentURI, Activity activity) {
        Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }

    public static void showProgressDialog(Context context) {
        if (pDialog != null) {
            pDialog.dismiss();
        }
        try {
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...\n Uploading to cloud");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void showProgressDialogCancelable(Context context) {
//        if (pDialog != null) {
//            pDialog.dismiss();
//        }
//        pDialog = new ProgressDialog(context);
//        pDialog.setMessage(context.getString(R.string.loading) + "\nPlease wait");
//        pDialog.setIndeterminate(true);
//        pDialog.setCancelable(true);
//        pDialog.setCanceledOnTouchOutside(false);
//        pDialog.show();
//    }

    public static void showProgressDialog(Context context, String message) {
        if (pDialog != null) {
            pDialog.dismiss();
        }
        pDialog = new ProgressDialog(context);
        pDialog.setMessage(message);
        pDialog.setIndeterminate(true);
        pDialog.setCancelable(false);
        pDialog.setCanceledOnTouchOutside(false);
        pDialog.show();
    }


    public static void dismissProgressDialog() {
        try {
            if (null != pDialog && pDialog.isShowing()) {
                pDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}