package io.github.memfis19.annca.internal.controller.impl;

import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;

import java.io.File;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.manager.CameraManager;
import io.github.memfis19.annca.internal.manager.impl.Camera1Manager;
import io.github.memfis19.annca.internal.manager.listener.CameraCloseListener;
import io.github.memfis19.annca.internal.manager.listener.CameraOpenListener;
import io.github.memfis19.annca.internal.manager.listener.CameraPhotoListener;
import io.github.memfis19.annca.internal.manager.listener.CameraVideoListener;
import io.github.memfis19.annca.internal.utils.CameraHelper;

/**
 * Created by memfis on 7/7/16.
 */

@SuppressWarnings("deprecation")
public class Camera1Controller implements io.github.memfis19.annca.internal.controller.CameraController<Integer>,
        CameraOpenListener<Integer, Camera.Size, SurfaceHolder.Callback>, CameraPhotoListener, CameraCloseListener<Integer>, CameraVideoListener<Camera.Size> {

    private final static String TAG = "Camera1Controller";

    private CameraView cameraView;
    private ConfigurationProvider configurationProvider;
    private CameraManager<Integer, Camera.Size, SurfaceHolder.Callback> cameraManager;

    private File outputFile;

    private Integer currentCameraId;

    public Camera1Controller(CameraView cameraView, ConfigurationProvider configurationProvider) {
        this.cameraView = cameraView;
        this.configurationProvider = configurationProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        cameraManager = Camera1Manager.getInstance();
        cameraManager.initializeCameraManager(configurationProvider, cameraView.getActivity());
        currentCameraId = cameraManager.getFaceBackCameraId();
    }

    @Override
    public void onResume() {
        cameraManager.openCamera(currentCameraId, this);
    }

    @Override
    public void onPause() {
        cameraManager.closeCamera(null);
    }

    @Override
    public void onDestroy() {
        cameraManager.releaseCameraManager();
    }

    @Override
    public void takePhoto() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), AnncaConfiguration.MEDIA_ACTION_PHOTO);
        cameraManager.takePhoto(outputFile, this);
    }

    @Override
    public void startVideoRecord() {
        outputFile = CameraHelper.getOutputMediaFile(cameraView.getActivity(), AnncaConfiguration.MEDIA_ACTION_VIDEO);
        cameraManager.startVideoRecord(outputFile, this);
    }

    @Override
    public void stopVideoRecord() {
        cameraManager.stopVideoRecord();
    }

    @Override
    public boolean isVideoRecording() {
        return cameraManager.isVideoRecording();
    }

    @Override
    public void switchCamera(@AnncaConfiguration.CameraFace final int cameraFace) {
        currentCameraId = cameraManager.getCurrentCameraId().equals(cameraManager.getFaceFrontCameraId()) ?
                cameraManager.getFaceBackCameraId() : cameraManager.getFaceFrontCameraId();

        cameraManager.closeCamera(this);
    }

    @Override
    public void switchQuality() {
        cameraManager.closeCamera(this);
    }

    @Override
    public int getNumberOfCameras() {
        return cameraManager.getNumberOfCameras();
    }

    @Override
    public int getMediaAction() {
        return configurationProvider.getMediaAction();
    }

    @Override
    public int getCameraOrientation() {
        return 0;
    }

    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public Integer getCurrentCameraId() {
        return currentCameraId;
    }

    //----------------Private internal methods-------------------

    @Override
    public void onCameraOpened(Integer cameraId, Camera.Size previewSize, SurfaceHolder.Callback surfaceCallback) {
        cameraView.updateCameraSwitcher(getNumberOfCameras());
        cameraView.updateUiForMediaAction(configurationProvider.getMediaAction());
        cameraView.updateCameraPreview(previewSize, surfaceCallback);
    }

    @Override
    public void onCameraOpenError() {
    }

    @Override
    public void onPhotoTaken(File photoFile) {
        cameraView.onPhotoTaken();
    }

    @Override
    public void onPhotoTakeError() {
    }

    @Override
    public void onCameraClosed(Integer closedCameraId) {
        cameraView.releaseCameraPreview();

        cameraManager.openCamera(currentCameraId, this);
    }

    @Override
    public void onVideoRecordStarted(Camera.Size videoSize) {
        cameraView.onVideoRecordStart(videoSize.width, videoSize.height);
    }

    @Override
    public void onVideoRecordStopped(File videoFile) {
        cameraView.onVideoRecordStop();
    }

    @Override
    public void onVideoRecordError() {

    }
}
