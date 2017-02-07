package io.github.memfis19.sample;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.memfis19.annca.Annca;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.utils.CameraHelper;

/**
 * Created by memfis on 11/8/16.
 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSIONS = 931;
    private static final int CAPTURE_MEDIA = 368;

    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_layout);

        activity = this;

        if (Build.VERSION.SDK_INT > 15) {
            askForPermissions(new String[]{
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CAMERA_PERMISSIONS);
        } else {
            enableCamera();
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.defaultConfiguration:
                    AnncaConfiguration.Builder builder = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
                    new Annca(builder.build()).launchCamera();
                    break;
                case R.id.photoConfiguration:
                    AnncaConfiguration.Builder photo = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
                    photo.setMediaAction(AnncaConfiguration.MEDIA_ACTION_PHOTO);
                    photo.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_LOW);
                    new Annca(photo.build()).launchCamera();
                    break;
                case R.id.videoConfiguration:
                    AnncaConfiguration.Builder video = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
                    video.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
                    video.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_HIGH);
                    new Annca(video.build()).launchCamera();
                    break;
                case R.id.videoLimitedConfiguration:
                    AnncaConfiguration.Builder videoLimited = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
                    videoLimited.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO);
                    videoLimited.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_AUTO);
                    videoLimited.setVideoFileSize(5 * 1024 * 1024);
                    videoLimited.setMinimumVideoDuration(5 * 60 * 1000);
                    new Annca(videoLimited.build()).launchCamera();
                    break;
                case R.id.universalConfiguration:
                    AnncaConfiguration.Builder universal = new AnncaConfiguration.Builder(activity, CAPTURE_MEDIA);
                    universal.setMediaAction(AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED);
                    universal.setFlashMode(AnncaConfiguration.FLASH_MODE_ON);
                    new Annca(universal.build()).launchCamera();
                    break;
                case R.id.dialogDemo:
                    DemoDialogFragment.getInstance().show(getSupportFragmentManager(), "DemoDialogFragment");
                    break;
                case R.id.customDemo:
                    if (CameraHelper.hasCamera2(MainActivity.this)) {
                        startActivity(new Intent(MainActivity.this, CustomCamera2Activity.class));
                    } else {
                        startActivity(new Intent(MainActivity.this, CustomCameraActivity.class));
                    }
                    break;
            }
        }
    };


    protected final void askForPermissions(String[] permissions, int requestCode) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[permissionsToRequest.size()]), requestCode);
        } else enableCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) return;
        enableCamera();
    }

    protected void enableCamera() {
        findViewById(R.id.defaultConfiguration).setOnClickListener(onClickListener);
        findViewById(R.id.photoConfiguration).setOnClickListener(onClickListener);
        findViewById(R.id.videoConfiguration).setOnClickListener(onClickListener);
        findViewById(R.id.videoLimitedConfiguration).setOnClickListener(onClickListener);
        findViewById(R.id.universalConfiguration).setOnClickListener(onClickListener);
        findViewById(R.id.dialogDemo).setOnClickListener(onClickListener);
        findViewById(R.id.customDemo).setOnClickListener(onClickListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_MEDIA && resultCode == RESULT_OK) {
            Toast.makeText(this, "Media captured.", Toast.LENGTH_SHORT).show();
        }
    }
}
