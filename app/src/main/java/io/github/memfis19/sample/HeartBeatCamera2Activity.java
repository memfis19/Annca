package io.github.memfis19.sample;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.nio.ByteBuffer;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.controller.CameraController;
import io.github.memfis19.annca.internal.controller.impl.Camera2Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.manager.impl.ParametersHandler;
import io.github.memfis19.annca.internal.manager.listener.CameraPreviewCallback;
import io.github.memfis19.annca.internal.ui.AnncaCameraActivity;
import io.github.memfis19.annca.internal.utils.Size;
import io.github.memfis19.annca.internal.utils.Utils;
import io.github.memfis19.sample.utils.GraphView;
import io.github.memfis19.sample.utils.HeartBeatProcessor;

/**
 * Created by memfis on 2/7/17.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class HeartBeatCamera2Activity extends AnncaCameraActivity<String> {

    private GraphView graphView;
    private HeartBeatProcessor heartBeatProcessor;

    private CaptureRequest.Builder parameters;
    private Size size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        heartBeatProcessor = new HeartBeatProcessor(this, true, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        heartBeatProcessor.release();
    }

    private static final int REQUEST_CODE = 404;

    @AnncaConfiguration.MediaAction
    private static final int PHOTO = AnncaConfiguration.MEDIA_ACTION_PHOTO;

    @AnncaConfiguration.MediaQuality
    private static final int QUALITY = AnncaConfiguration.MEDIA_QUALITY_LOWEST;

    @AnncaConfiguration.FlashMode
    private static final int FLASH = AnncaConfiguration.FLASH_MODE_OFF;

    @Override
    protected View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        graphView = new GraphView(this);

        graphView.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, Utils.convertDipToPixels(this, 100));
        graphView.setLayoutParams(layoutParams);


        return graphView;
    }

    @Override
    protected void onCameraControllerReady() {
        super.onCameraControllerReady();
        try {
            getCameraController().getCameraManager().handleParameters(new ParametersHandler<CaptureRequest.Builder>() {
                @Override
                public CaptureRequest.Builder getParameters(CaptureRequest.Builder params) {
                    parameters = params;

                    params.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

                    heartBeatProcessor.prepare(size.getWidth(), size.getHeight(), new HeartBeatProcessor.OnFrameProcessListener() {
                        @Override
                        public void onFrameProcessed(int value, long time, float averageHrPm) {
                            graphView.drawPoint(value, time, averageHrPm);
                        }
                    });

                    getCameraController().getCameraManager().setPreviewCallback(new CameraPreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data) {
                        }

                        @Override
                        public void onPreviewFrame(ByteBuffer byteBuffer) {
                            heartBeatProcessor.processFrame(byteBuffer);
                        }
                    });

                    return params;
                }
            });
        } catch (Exception e) {
            Log.e("Camera", "Error", e);
        }
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
        this.size = size;
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
    public CameraController<String> createCameraController(CameraView cameraView, ConfigurationProvider configurationProvider) {
        return new Camera2Controller(cameraView, configurationProvider);
    }

    @Override
    protected void onScreenRotation(int degrees) {

    }
}
