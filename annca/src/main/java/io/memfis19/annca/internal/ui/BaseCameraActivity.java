package io.memfis19.annca.internal.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import io.memfis19.annca.R;
import io.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.memfis19.annca.internal.controller.CameraController;
import io.memfis19.annca.internal.ui.preview.PreviewActivity;
import io.memfis19.annca.internal.ui.view.AspectFrameLayout;
import io.memfis19.annca.internal.ui.view.CameraControlPanel;
import io.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.memfis19.annca.internal.ui.view.FlashSwitchView;
import io.memfis19.annca.internal.ui.view.MediaActionSwitchView;
import io.memfis19.annca.internal.ui.view.RecordButton;
import io.memfis19.annca.internal.utils.Utils;

/**
 * Created by memfis on 7/6/16.
 */
public abstract class BaseCameraActivity extends CameraActivity
        implements
        RecordButton.RecordButtonListener,
        FlashSwitchView.FlashModeSwitchListener,
        MediaActionSwitchView.OnMediaActionStateChangeListener,
        CameraSwitchView.OnCameraTypeChangeListener, CameraControlPanel.SettingsClickListener {

    protected CameraController cameraController;
    protected AspectFrameLayout previewContainer;
    protected CameraControlPanel controlPanel;
    private AlertDialog settingsDialog;

    @MediaActionSwitchView.MediaActionState
    protected int currentMediaActionState;

    @CameraSwitchView.CameraType
    protected int currentCameraType = CameraSwitchView.CAMERA_TYPE_REAR;

    @AnncaConfiguration.MediaQuality
    protected int newQuality = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentMediaActionState = mediaAction == AnncaConfiguration.MEDIA_ACTION_VIDEO ?
                MediaActionSwitchView.ACTION_VIDEO : MediaActionSwitchView.ACTION_PHOTO;

        View decorView = getWindow().getDecorView();

        if (Build.VERSION.SDK_INT > 15) {
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

        setContentView(R.layout.base_camera_layout);

        previewContainer = (AspectFrameLayout) findViewById(R.id.previewContainer);

        controlPanel = (CameraControlPanel) findViewById(R.id.controlPanel);
        if (controlPanel != null) {
            controlPanel.setup(getMediaAction());
            controlPanel.setRecordButtonListener(this);
            controlPanel.setFlashModeSwitchListener(this);
            controlPanel.setOnMediaActionStateChangeListener(this);
            controlPanel.setOnCameraTypeChangeListener(this);
            controlPanel.setMaxVideoDuration(getVideoDuration());
            controlPanel.setMaxVideoFileSize(getVideoFileSize());
            controlPanel.setSettingsClickListener(this);
        }
    }

    protected final void putPreviewToContainer(View child) {
        if (previewContainer == null || child == null) return;
        previewContainer.removeAllViews();
        previewContainer.addView(child);
    }

    protected final void clearPreviewContainer() {
        if (previewContainer != null)
            previewContainer.removeAllViews();
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

    //----------------------------controls callbacks------------------------------------------------
    @Override
    public void onTakePhotoButtonPressed() {
        onTakePhotoEvent();
    }

    @Override
    public void onStartRecordingButtonPressed() {
        onStartRecordingEvent();
    }

    @Override
    public void onStopRecordingButtonPressed() {
        onStopRecordingEvent();
    }

    @Override
    public void onFlashModeChanged(FlashSwitchView.FlashMode mode) {
    }

    @Override
    public void onMediaActionChanged(@MediaActionSwitchView.MediaActionState int actionState) {
        if (currentMediaActionState == actionState) return;
        currentMediaActionState = actionState;
        onMediaActionChangedEvent(currentMediaActionState);
    }

    @Override
    public void onCameraTypeChanged(@CameraSwitchView.CameraType int cameraType) {
        if (currentCameraType == cameraType) return;
        currentCameraType = cameraType;
        onCameraTypeChangedEvent(cameraType);
    }

    protected void startPreviewActivity() {
        Intent intent = PreviewActivity.newIntent(this,
                getMediaAction(), cameraController.getOutputFile().toString());
        startActivityForResult(intent, REQUEST_PREVIEW_CODE);
    }

    @Override
    protected void onScreenRotation(int degrees) {
        controlPanel.rotateControls(degrees);
        rotateSettingsDialog(degrees);
    }

    @Override
    public void onSettingsClick() {
        onSettingsEvent();
    }

    @Override
    protected void onSettingsEvent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (getMediaAction() == AnncaConfiguration.MEDIA_ACTION_VIDEO) {
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
                    controlPanel.lockControls();
                    cameraController.switchQuality();
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
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(settingsDialog.getWindow().getAttributes());
        lp.width = Utils.convertDipToPixels(this, 350);
        lp.height = Utils.convertDipToPixels(this, 350);
        settingsDialog.getWindow().setAttributes(lp);
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
