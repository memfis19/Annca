package io.memfis19.annca.internal.controller.view;

import android.app.Activity;

import io.memfis19.annca.internal.configuration.AnncaConfiguration;

/**
 * Created by memfis on 7/6/16.
 */
public interface CameraView {

    Activity getActivity();

    void updateCameraPreview(Object... parameters);

    void updateUiForMediaAction(@AnncaConfiguration.MediaAction int mediaAction);

    void updateCameraSwitcher(int numberOfCameras);

    void onPhotoTaken();

    void onVideoRecordStart(int width, int height);

    void onVideoRecordStop();

    void releaseCameraPreview();

}
