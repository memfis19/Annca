package io.github.memfis19.annca.internal.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import io.github.memfis19.annca.R;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.ui.preview.PreviewActivity;
import io.github.memfis19.annca.internal.ui.view.CameraControlPanel;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.ui.view.FlashSwitchView;
import io.github.memfis19.annca.internal.ui.view.MediaActionSwitchView;
import io.github.memfis19.annca.internal.ui.view.RecordButton;
import io.github.memfis19.annca.internal.utils.Size;
import io.github.memfis19.annca.internal.utils.Utils;

/**
 * Created by memfis on 12/1/16.
 */

public abstract class BaseAnncaActivity<CameraId> extends AnncaCameraActivity<CameraId>
        implements
        RecordButton.RecordButtonListener,
        FlashSwitchView.FlashModeSwitchListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener,
        CameraSwitchView.OnCameraTypeChangeListener, CameraControlPanel.SettingsClickListener {

    private CameraControlPanel cameraControlPanel;
    private AlertDialog settingsDialog;

    protected static final int REQUEST_PREVIEW_CODE = 1001;

    public static final int ACTION_CONFIRM = 900;
    public static final int ACTION_RETAKE = 901;
    public static final int ACTION_CANCEL = 902;

    protected int requestCode = -1;

    @AnncaConfiguration.MediaAction
    protected int mediaAction = AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED;
    @AnncaConfiguration.MediaQuality
    protected int mediaQuality = AnncaConfiguration.MEDIA_QUALITY_MEDIUM;

    protected int videoDuration = -1;
    protected long videoFileSize = -1;
    protected int minimumVideoDuration = -1;

    @MediaActionSwitchView.MediaActionState
    protected int currentMediaActionState;

    @CameraSwitchView.CameraType
    protected int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    @AnncaConfiguration.MediaQuality
    protected int newQuality = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onProcessBundle(Bundle savedInstanceState) {
        super.onProcessBundle(savedInstanceState);

        extractConfiguration(getIntent().getExtras());
        currentMediaActionState = mediaAction == AnncaConfiguration.MEDIA_ACTION_VIDEO ?
                MediaActionSwitchView.ACTION_VIDEO : MediaActionSwitchView.ACTION_PHOTO;
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);
    }

    private void extractConfiguration(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(AnncaConfiguration.Arguments.REQUEST_CODE))
                requestCode = bundle.getInt(AnncaConfiguration.Arguments.REQUEST_CODE);

            if (bundle.containsKey(AnncaConfiguration.Arguments.MEDIA_ACTION)) {
                switch (bundle.getInt(AnncaConfiguration.Arguments.MEDIA_ACTION)) {
                    case AnncaConfiguration.MEDIA_ACTION_PHOTO:
                        mediaAction = AnncaConfiguration.MEDIA_ACTION_PHOTO;
                        break;
                    case AnncaConfiguration.MEDIA_ACTION_VIDEO:
                        mediaAction = AnncaConfiguration.MEDIA_ACTION_VIDEO;
                        break;
                    default:
                        mediaAction = AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED;
                        break;
                }
            }

            if (bundle.containsKey(AnncaConfiguration.Arguments.MEDIA_QUALITY)) {
                switch (bundle.getInt(AnncaConfiguration.Arguments.MEDIA_QUALITY)) {
                    case AnncaConfiguration.MEDIA_QUALITY_AUTO:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_AUTO;
                        break;
                    case AnncaConfiguration.MEDIA_QUALITY_HIGHEST:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_HIGHEST;
                        break;
                    case AnncaConfiguration.MEDIA_QUALITY_HIGH:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_HIGH;
                        break;
                    case AnncaConfiguration.MEDIA_QUALITY_MEDIUM:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                    case AnncaConfiguration.MEDIA_QUALITY_LOW:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_LOW;
                        break;
                    case AnncaConfiguration.MEDIA_QUALITY_LOWEST:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_LOWEST;
                        break;
                    default:
                        mediaQuality = AnncaConfiguration.MEDIA_QUALITY_MEDIUM;
                        break;
                }
            }

            if (bundle.containsKey(AnncaConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(AnncaConfiguration.Arguments.VIDEO_DURATION);

            if (bundle.containsKey(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE);

            if (bundle.containsKey(AnncaConfiguration.Arguments.MINIMUM_VIDEO_DURATION))
                minimumVideoDuration = bundle.getInt(AnncaConfiguration.Arguments.MINIMUM_VIDEO_DURATION);
        }
    }

    @Override
    View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        cameraControlPanel = (CameraControlPanel) layoutInflater.inflate(R.layout.user_control_layout, parent, false);

        if (cameraControlPanel != null) {
            cameraControlPanel.setup(getMediaAction());
            cameraControlPanel.setRecordButtonListener(this);
            cameraControlPanel.setFlashModeSwitchListener(this);
            cameraControlPanel.setOnMediaActionStateChangeListener(this);
            cameraControlPanel.setOnCameraTypeChangeListener(this);
            cameraControlPanel.setMaxVideoDuration(getVideoDuration());
            cameraControlPanel.setMaxVideoFileSize(getVideoFileSize());
            cameraControlPanel.setSettingsClickListener(this);
        }

        return cameraControlPanel;
    }

    @Override
    public void onSettingsClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (currentMediaActionState == MediaActionSwitchView.ACTION_VIDEO) {
            builder.setSingleChoiceItems(getVideoQualityOptions(), getVideoOptionCheckedIndex(), getVideoOptionSelectedListener());
            if (getVideoFileSize() > 0)
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title),
                        "(Max " + String.valueOf(getVideoFileSize() / (1024 * 1024) + " MB)")));
            else
                builder.setTitle(String.format(getString(R.string.settings_video_quality_title), ""));
        } else {
            builder.setSingleChoiceItems(getPhotoQualityOptions(), getPhotoOptionCheckedIndex(), getPhotoOptionSelectedListener());
            builder.setTitle(R.string.settings_photo_quality_title);
        }

        builder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (newQuality > 0 && newQuality != mediaQuality) {
                    mediaQuality = newQuality;
                    dialogInterface.dismiss();
                    cameraControlPanel.lockControls();
                    getCameraController().switchQuality();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        settingsDialog = builder.create();
        settingsDialog.show();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(settingsDialog.getWindow().getAttributes());
        layoutParams.width = Utils.convertDipToPixels(this, 350);
        layoutParams.height = Utils.convertDipToPixels(this, 350);
        settingsDialog.getWindow().setAttributes(layoutParams);
    }

    @Override
    public void onCameraTypeChanged(@CameraSwitchView.CameraType int cameraType) {
        if (currentCameraType == cameraType) return;
        currentCameraType = cameraType;

        cameraControlPanel.lockControls();
        cameraControlPanel.allowRecord(false);

        int cameraFace = cameraType == CameraSwitchView.CAMERA_TYPE_FRONT
                ? AnncaConfiguration.CAMERA_FACE_FRONT : AnncaConfiguration.CAMERA_FACE_REAR;

        getCameraController().switchCamera(cameraFace);
    }

    @Override
    public void onFlashModeChanged(@FlashSwitchView.FlashMode int mode) {

    }

    @Override
    public void onMediaActionChanged(int mediaActionState) {
        if (currentMediaActionState == mediaActionState) return;
        currentMediaActionState = mediaActionState;
    }

    @Override
    public void onTakePhotoButtonPressed() {
        getCameraController().takePhoto();
    }

    @Override
    public void onStartRecordingButtonPressed() {
        getCameraController().startVideoRecord();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        getCameraController().stopVideoRecord();
    }

    @Override
    protected void onScreenRotation(int degrees) {
        cameraControlPanel.rotateControls(degrees);
        rotateSettingsDialog(degrees);
    }

    @Override
    public int getRequestCode() {
        return requestCode;
    }

    @Override
    public int getMediaAction() {
        return mediaAction;
    }

    @Override
    public int getMediaQuality() {
        return mediaQuality;
    }

    @Override
    public int getVideoDuration() {
        return videoDuration;
    }

    @Override
    public long getVideoFileSize() {
        return videoFileSize;
    }


    @Override
    public int getMinimumVideoDuration() {
        return minimumVideoDuration / 1000;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        cameraControlPanel.unLockControls();
        cameraControlPanel.allowRecord(true);

        setCameraPreview(cameraPreview, size);
    }

    @Override
    public void updateUiForMediaAction(@AnncaConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {
        cameraControlPanel.allowCameraSwitching(numberOfCameras > 1);
    }

    @Override
    public void onPhotoTaken() {
        startPreviewActivity();
    }

    @Override
    public void onVideoRecordStart(int width, int height) {
        cameraControlPanel.onStartVideoRecord(getCameraController().getOutputFile());
    }

    @Override
    public void onVideoRecordStop() {
        cameraControlPanel.allowRecord(false);
        cameraControlPanel.onStopVideoRecord();
        startPreviewActivity();
    }

    @Override
    public void releaseCameraPreview() {
        clearCameraPreview();
    }

    private void startPreviewActivity() {
        Intent intent = PreviewActivity.newIntent(this,
                getMediaAction(), getCameraController().getOutputFile().toString());
        startActivityForResult(intent, REQUEST_PREVIEW_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PREVIEW_CODE) {
                if (PreviewActivity.isResultConfirm(data)) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(AnncaConfiguration.Arguments.FILE_PATH,
                            PreviewActivity.getMediaFilePatch(data));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else if (PreviewActivity.isResultCancel(data)) {
                    setResult(RESULT_CANCELED);
                    finish();
                } else if (PreviewActivity.isResultRetake(data)) {
                    //ignore, just proceed the camera
                }
            }
        }
    }

    private void rotateSettingsDialog(int degrees) {
        if (settingsDialog != null && settingsDialog.isShowing() && Build.VERSION.SDK_INT > 10) {
            ViewGroup dialogView = (ViewGroup) settingsDialog.getWindow().getDecorView();
            for (int i = 0; i < dialogView.getChildCount(); i++) {
                dialogView.getChildAt(i).setRotation(degrees);
            }
        }
    }

    protected abstract CharSequence[] getVideoQualityOptions();

    protected abstract CharSequence[] getPhotoQualityOptions();

    protected abstract int getVideoOptionCheckedIndex();

    protected abstract int getPhotoOptionCheckedIndex();

    protected abstract DialogInterface.OnClickListener getVideoOptionSelectedListener();

    protected abstract DialogInterface.OnClickListener getPhotoOptionSelectedListener();
}
