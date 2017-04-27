package io.github.memfis19.sample;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.controller.CameraController;
import io.github.memfis19.annca.internal.controller.impl.Camera2Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.ui.AnncaCameraActivity;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.utils.Size;

/**
 * Created by memfis on 2/7/17.
 */

public class CustomCamera2Activity extends AnncaCameraActivity<String> {

    private static final int REQUEST_CODE = 404;

    @AnncaConfiguration.MediaAction
    private static final int PHOTO = AnncaConfiguration.MEDIA_ACTION_PHOTO;

    @AnncaConfiguration.MediaQuality
    private static final int QUALITY = AnncaConfiguration.MEDIA_QUALITY_HIGH;

    @AnncaConfiguration.FlashMode
    private static final int FLASH = AnncaConfiguration.FLASH_MODE_AUTO;

    @Override
    protected View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        RelativeLayout customCameraLayout = (RelativeLayout) layoutInflater.inflate(R.layout.custom_camera_layout, parent, false);

        customCameraLayout.findViewById(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCameraController().takePhoto();
            }
        });

        return customCameraLayout;
    }

    @Override
    public int getRequestCode() {
        return REQUEST_CODE;
    }

    @Override
    public int getMediaAction() {
        return PHOTO;
    }

    @Override
    public int getMediaQuality() {
        return QUALITY;
    }

    @Override
    public int getVideoDuration() {
        return 1000;
    }

    @Override
    public long getVideoFileSize() {
        return 5 * 1024 * 1024;
    }

    @Override
    public int getMinimumVideoDuration() {
        return 1000;
    }

    @Override
    public int getFlashMode() {
        return FLASH;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        setCameraPreview(cameraPreview, size);
    }

    @Override
    public void updateUiForMediaAction(@AnncaConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {

    }

    @Override
    public void onPhotoTaken() {
        Toast.makeText(this, "Result file: " + String.valueOf(getCameraController().getOutputFile().toString()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {

    }

    @Override
    public void onVideoRecordStop() {

    }

    @Override
    public void releaseCameraPreview() {

    }

    @Override
    public int getCameraFace() {
        return CameraSwitchView.CAMERA_TYPE_REAR;
    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public CameraController<String> createCameraController(CameraView cameraView, ConfigurationProvider configurationProvider) {
        return new Camera2Controller(cameraView, configurationProvider);
    }

    @Override
    protected void onScreenRotation(int degrees) {

    }

    @Override
    public int getMediaResultBehaviour() {
        return AnncaConfiguration.PREVIEW;
    }
}
