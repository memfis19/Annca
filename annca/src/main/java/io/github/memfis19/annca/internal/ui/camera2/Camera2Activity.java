package io.github.memfis19.annca.internal.ui.camera2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.CamcorderProfile;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;

import java.util.concurrent.TimeUnit;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.controller.impl.Camera2Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.manager.impl.Camera2Manager;
import io.github.memfis19.annca.internal.ui.BaseCameraActivity;
import io.github.memfis19.annca.internal.ui.view.AutoFitTextureView;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.ui.view.MediaActionSwitchView;
import io.github.memfis19.annca.internal.utils.CameraHelper;

/**
 * Created by memfis on 7/6/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends BaseCameraActivity implements CameraView {

    private static final String TAG = "Camera2Activity";

    private AutoFitTextureView autoFitTextureView;
    private Size previewSize;
    private int degrees;

    private CharSequence[] videoQualities;
    private CharSequence[] photoQualities;

    @AnncaConfiguration.CameraFace
    private int cameraFace = AnncaConfiguration.CAMERA_FACE_REAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraController = new Camera2Controller(this, this);
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
        cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? AnncaConfiguration.CAMERA_FACE_FRONT : AnncaConfiguration.CAMERA_FACE_REAR;

        controlPanel.lockControls();
        controlPanel.allowRecord(false);

        cameraController.switchCamera(cameraFace);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Object... parameters) {
        controlPanel.unLockControls();
        controlPanel.allowRecord(true);

        previewSize = (Size) parameters[0];

        autoFitTextureView = new AutoFitTextureView(this, (TextureView.SurfaceTextureListener) parameters[1]);
        putPreviewToContainer(autoFitTextureView);

        previewContainer.setAspectRatio(previewSize.getHeight() / (double) previewSize.getWidth());
//        autoFitTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
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
//        configureTransform(autoFitTextureView.getWidth(), autoFitTextureView.getHeight());
        controlPanel.onStartVideoRecord(cameraController.getOutputFile());
//        configureTransform(width, height);
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == autoFitTextureView) {
            return;
        }

        Matrix matrix = new Matrix();

        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getWidth(), previewSize.getHeight());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());

        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.max(
                (float) viewHeight / previewSize.getHeight(),
                (float) viewWidth / previewSize.getWidth());

        if (cameraFace == AnncaConfiguration.CAMERA_FACE_REAR)
            matrix.postScale(scale, scale, centerX, centerY);
        else {
            if (degrees == 90 || degrees == 270)
                matrix.postScale(-scale, scale, centerX, centerY);
            else
                matrix.postScale(scale, -scale, centerX, centerY);
        }

        int rotation = degrees + getVideoOrientation(getSensorPosition());
        matrix.postRotate(Math.abs(degrees), centerX, centerY);

        autoFitTextureView.setTransform(matrix);
    }

    protected int getVideoOrientation(@AnncaConfiguration.SensorPosition int sensorPosition) {
        int degrees = 0;
        switch (sensorPosition) {
            case AnncaConfiguration.SENSOR_POSITION_UP:
                degrees = 0;
                break; // Natural orientation
            case AnncaConfiguration.SENSOR_POSITION_LEFT:
                degrees = 90;
                break; // Landscape left
            case AnncaConfiguration.SENSOR_POSITION_UP_SIDE_DOWN:
                degrees = 180;
                break;// Upside down
            case AnncaConfiguration.SENSOR_POSITION_RIGHT:
                degrees = 270;
                break;// Landscape right
        }

        int rotate;
        if (cameraFace == AnncaConfiguration.CAMERA_FACE_FRONT) {
            rotate = (360 + 270 + degrees) % 360;
        } else {
            rotate = (360 + 90 - degrees) % 360;
        }
        Log.d(TAG, "getVideoOrientation: " + String.valueOf(rotate));
        return rotate;
    }

    @Override
    protected void onScreenRotation(int degrees) {
        super.onScreenRotation(degrees);
        this.degrees = degrees;
    }

    @Override
    public void onVideoRecordStop() {
        controlPanel.allowRecord(false);
        controlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        autoFitTextureView = null;
        clearPreviewContainer();
    }

    @Override
    public int getMediaAction() {
        return currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO
                ? AnncaConfiguration.MEDIA_ACTION_VIDEO : AnncaConfiguration.MEDIA_ACTION_PHOTO;
    }

    @Override
    protected CharSequence[] getVideoQualityOptions() {
        videoQualities = new CharSequence[]{
                new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_AUTO, (String) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()),
                new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, (String) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()),
                new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, (String) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration()),
                new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOW, (String) cameraController.getCurrentCameraId(), getVideoFileSize(), getMinimumVideoDuration())
        };
        return videoQualities;
    }

    @Override
    protected CharSequence[] getPhotoQualityOptions() {
        photoQualities = new CharSequence[]{
                new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGHEST, Camera2Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGHEST)),
                new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, Camera2Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGH)),
                new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, Camera2Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_MEDIUM)),
                new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOWEST, Camera2Manager.getInstance().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_LOWEST))
        };
        return photoQualities;
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
                newQuality = ((VideoQualityOption) videoQualities[i]).getMediaQuality();
            }
        };
    }

    @Override
    protected DialogInterface.OnClickListener getPhotoOptionSelectedListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                newQuality = ((PhotoQualityOption) photoQualities[i]).getMediaQuality();
            }
        };
    }

    private class PhotoQualityOption implements CharSequence {

        @AnncaConfiguration.MediaQuality
        private int mediaQuality;
        private String title;
        private Size size;

        public PhotoQualityOption(@AnncaConfiguration.MediaQuality int mediaQuality, Size size) {
            this.mediaQuality = mediaQuality;
            this.size = size;

            title = String.valueOf(size.getWidth()) + " x " + String.valueOf(size.getHeight());
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

        public VideoQualityOption(@AnncaConfiguration.MediaQuality int mediaQuality, String cameraId, long maxFileSize, int baseDuration) {
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
