package io.github.memfis19.annca.internal.ui.camera;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.controller.impl.Camera1Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.manager.impl.Camera1Manager;
import io.github.memfis19.annca.internal.ui.BaseCameraActivity;
import io.github.memfis19.annca.internal.ui.view.AutoFitSurfaceView;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.ui.view.MediaActionSwitchView;
import io.github.memfis19.annca.internal.utils.CameraHelper;

/**
 * Created by memfis on 7/6/16.
 */
@SuppressWarnings("deprecation")
public class Camera1Activity extends BaseCameraActivity implements CameraView {

    private AutoFitSurfaceView autoFitSurfaceView;

    private List<CharSequence> videoQualities;
    private List<CharSequence> photoQualities;

    @AnncaConfiguration.CameraFace
    private int cameraFace = AnncaConfiguration.CAMERA_FACE_REAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraController = new Camera1Controller(this, this);
        cameraController.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        controlPanel.lockControls();
        controlPanel.allowRecord(false);

        cameraController.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        controlPanel.lockControls();
        controlPanel.allowRecord(false);

        cameraController.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cameraController.onDestroy();
    }

    //--------------------------------controls callbacks--------------------------------------------
    @Override
    protected void onTakePhotoEvent() {
        cameraController.takePhoto();
    }

    @Override
    protected void onStartRecordingEvent() {
        cameraController.startVideoRecord();
    }

    @Override
    protected void onStopRecordingEvent() {
        cameraController.stopVideoRecord();
    }

    @Override
    protected void onMediaActionChangedEvent(@MediaActionSwitchView.MediaActionState int mediaActionState) {

    }

    @Override
    protected void onCameraTypeChangedEvent(@CameraSwitchView.CameraType int cameraType) {
        controlPanel.lockControls();
        controlPanel.allowRecord(false);

        cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? AnncaConfiguration.CAMERA_FACE_FRONT : AnncaConfiguration.CAMERA_FACE_REAR;

        cameraController.switchCamera(cameraFace);
    }

    //-------------------------CameraView implementation---------------------------

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Object... parameters) {
        controlPanel.unLockControls();
        controlPanel.allowRecord(true);

        Camera.Size previewSize = (Camera.Size) parameters[0];

        autoFitSurfaceView = new AutoFitSurfaceView(this, (SurfaceHolder.Callback) parameters[1]);
        putPreviewToContainer(autoFitSurfaceView);

        previewContainer.setAspectRatio(previewSize.height / (double) previewSize.width);
//        autoFitSurfaceView.setAspectRatio(previewSize.width, previewSize.height);
    }

    @Override
    public void updateUiForMediaAction(@AnncaConfiguration.MediaAction int mediaAction) {
    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {
        controlPanel.allowCameraSwitching(numberOfCameras > 1);
    }

    @Override
    public void onPhotoTaken() {
        startPreviewActivity();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {
        controlPanel.onStartVideoRecord(cameraController.getOutputFile());
    }

    @Override
    public void onVideoRecordStop() {
        controlPanel.allowRecord(false);
        controlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        clearPreviewContainer();
    }

    @Override
    protected CharSequence[] getVideoQualityOptions() {
        videoQualities = new ArrayList<>();

        if (getMinimumVideoDuration() > 0)
            videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_AUTO, (Integer) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()));

        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, (Integer) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()));
        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, (Integer) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()));
        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOW, (Integer) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()));

        CharSequence[] array = new CharSequence[videoQualities.size()];
        videoQualities.toArray(array);

        return array;
    }

    @Override
    protected CharSequence[] getPhotoQualityOptions() {
        photoQualities = new ArrayList<>();
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGHEST, Camera1Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGHEST)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, Camera1Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGH)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, Camera1Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_MEDIUM)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOWEST, Camera1Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_LOWEST)));

        CharSequence[] array = new CharSequence[photoQualities.size()];
        photoQualities.toArray(array);

        return array;
    }

    @Override
    protected int getVideoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_AUTO) checkedIndex = 0;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_LOW) checkedIndex = 3;
        return checkedIndex;
    }

    @Override
    protected int getPhotoOptionCheckedIndex() {
        int checkedIndex = -1;
        if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_HIGHEST) checkedIndex = 0;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_HIGH) checkedIndex = 1;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_MEDIUM) checkedIndex = 2;
        else if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_LOWEST) checkedIndex = 3;
        return checkedIndex;
    }

    @Override
    protected DialogInterface.OnClickListener getVideoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                newQuality = ((VideoQualityOption) videoQualities.get(i)).getMediaQuality();
            }
        };
    }

    @Override
    protected DialogInterface.OnClickListener getPhotoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                newQuality = ((PhotoQualityOption) photoQualities.get(i)).getMediaQuality();
            }
        };
    }

    private class PhotoQualityOption implements CharSequence {

        @AnncaConfiguration.MediaQuality
        private int mediaQuality;
        private String title;
        private Camera.Size size;

        public PhotoQualityOption(@AnncaConfiguration.MediaQuality int mediaQuality, Camera.Size size) {
            this.mediaQuality = mediaQuality;
            this.size = size;

            title = String.valueOf(size.width) + " x " + String.valueOf(size.height);
        }

        public int getMediaQuality() {
            return mediaQuality;
        }

        @Override
        public int length() {
            return title.length();
        }

        @Override
        public char charAt(int i) {
            return title.charAt(i);
        }

        @Override
        public CharSequence subSequence(int i, int i1) {
            return title.subSequence(i, i1);
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private class VideoQualityOption implements CharSequence {

        private String title;

        @AnncaConfiguration.MediaQuality
        private int mediaQuality;
        private CamcorderProfile camcorderProfile;
        private int videoDuration;

        public VideoQualityOption(@AnncaConfiguration.MediaQuality int mediaQuality, int cameraId, long maxFileSize, int baseDuration) {
            this.mediaQuality = mediaQuality;

            long minutes = TimeUnit.SECONDS.toMinutes(baseDuration);
            long seconds = baseDuration - minutes * 60;

            if (mediaQuality == AnncaConfiguration.MEDIA_QUALITY_AUTO) {
                title = "Auto " + ", (" + (minutes > 10 ? minutes : ("0" + minutes)) + ":" + (seconds > 10 ? seconds : ("0" + seconds)) + " min)";
            } else {
                camcorderProfile = CameraHelper.getCamcorderProfile(mediaQuality, cameraId);
                videoDuration = (int) CameraHelper.calculateApproximateVideoDuration(camcorderProfile, maxFileSize);

                minutes = TimeUnit.SECONDS.toMinutes(videoDuration);
                seconds = videoDuration - minutes * 60;

                title = String.valueOf(camcorderProfile.videoFrameWidth)
                        + " x " + String.valueOf(camcorderProfile.videoFrameHeight)
                        + ", (" + (minutes > 10 ? minutes : ("0" + minutes)) + ":" + (seconds > 10 ? seconds : ("0" + seconds)) + " min)";
            }
        }

        public int getMediaQuality() {
            return mediaQuality;
        }

        @Override
        public int length() {
            return title.length();
        }

        @Override
        public char charAt(int i) {
            return title.charAt(i);
        }

        @Override
        public CharSequence subSequence(int i, int i1) {
            return title.subSequence(i, i1);
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
