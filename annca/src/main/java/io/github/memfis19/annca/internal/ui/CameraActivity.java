package io.github.memfis19.annca.internal.ui;

import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.ui.view.MediaActionSwitchView;
import io.github.memfis19.annca.internal.utils.Utils;

/**
 * Created by memfis on 7/18/16.
 */
public abstract class CameraActivity extends AppCompatActivity
        implements ConfigurationProvider, SensorEventListener {

    protected static final int REQUEST_PREVIEW_CODE = 1001;

    public static final int ACTION_CONFIRM = 900;
    public static final int ACTION_RETAKE = 901;
    public static final int ACTION_CANCEL = 902;

    private SensorManager sensorManager = null;

    protected int requestCode = -1;

    @AnncaConfiguration.MediaAction
    protected int mediaAction = AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED;
    @AnncaConfiguration.MediaQuality
    protected int mediaQuality = AnncaConfiguration.MEDIA_QUALITY_MEDIUM;

    protected int videoDuration = -1;
    protected long videoFileSize = -1;

    private int degrees = -1;

    @AnncaConfiguration.SensorPosition
    protected int sensorPosition = AnncaConfiguration.SENSOR_POSITION_UNSPECIFIED;

    @AnncaConfiguration.DeviceDefaultOrientation
    protected int deviceDefaultOrientation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        int defaultOrientation = Utils.getDeviceDefaultOrientation(this);

        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            deviceDefaultOrientation = AnncaConfiguration.ORIENTATION_LANDSCAPE;
        } else if (defaultOrientation == Configuration.ORIENTATION_PORTRAIT) {
            deviceDefaultOrientation = AnncaConfiguration.ORIENTATION_PORTRAIT;
        }

        extractConfiguration(savedInstanceState != null ? savedInstanceState : getIntent().getExtras());
    }

    private void extractConfiguration(Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(AnncaConfiguration.Arguments.REQUEST_CODE))
                requestCode = bundle.getInt(AnncaConfiguration.Arguments.REQUEST_CODE);

            if (bundle.containsKey(AnncaConfiguration.Arguments.MEDIA_ACTION))
                mediaAction = bundle.getInt(AnncaConfiguration.Arguments.MEDIA_ACTION);

            if (bundle.containsKey(AnncaConfiguration.Arguments.MEDIA_QUALITY))
                mediaQuality = bundle.getInt(AnncaConfiguration.Arguments.MEDIA_QUALITY);

            if (bundle.containsKey(AnncaConfiguration.Arguments.VIDEO_DURATION))
                videoDuration = bundle.getInt(AnncaConfiguration.Arguments.VIDEO_DURATION);

            if (bundle.containsKey(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE))
                videoFileSize = bundle.getLong(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(AnncaConfiguration.Arguments.REQUEST_CODE, requestCode);
        outState.putInt(AnncaConfiguration.Arguments.MEDIA_ACTION, requestCode);
        outState.putInt(AnncaConfiguration.Arguments.MEDIA_QUALITY, requestCode);
        outState.putInt(AnncaConfiguration.Arguments.VIDEO_DURATION, requestCode);
        outState.putLong(AnncaConfiguration.Arguments.VIDEO_FILE_SIZE, requestCode);
    }

    protected abstract void onTakePhotoEvent();

    protected abstract void onStartRecordingEvent();

    protected abstract void onStopRecordingEvent();

    protected abstract void onMediaActionChangedEvent(@MediaActionSwitchView.MediaActionState int mediaActionState);

    protected abstract void onCameraTypeChangedEvent(@CameraSwitchView.CameraType int cameraType);

    protected abstract void onScreenRotation(int degrees);

    protected abstract void onSettingsEvent();

    //--------------------------params getters------------------------------------------------------
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
    public int getSensorPosition() {
        return sensorPosition;
    }

    @Override
    public int getDegrees() {
        return degrees;
    }

    @Override
    public int getMinimumVideoDuration() {
        return 60 * 5;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (sensorEvent.values[0] < 4 && sensorEvent.values[0] > -4) {
                    if (sensorEvent.values[1] > 0) {
                        // UP
                        sensorPosition = AnncaConfiguration.SENSOR_POSITION_UP;
                        degrees = deviceDefaultOrientation == AnncaConfiguration.ORIENTATION_PORTRAIT ? 0 : 90;
                    } else if (sensorEvent.values[1] < 0) {
                        // UP SIDE DOWN
                        sensorPosition = AnncaConfiguration.SENSOR_POSITION_UP_SIDE_DOWN;
                        degrees = deviceDefaultOrientation == AnncaConfiguration.ORIENTATION_PORTRAIT ? 180 : 270;
                    }
                } else if (sensorEvent.values[1] < 4 && sensorEvent.values[1] > -4) {
                    if (sensorEvent.values[0] > 0) {
                        // LEFT
                        sensorPosition = AnncaConfiguration.SENSOR_POSITION_LEFT;
                        degrees = deviceDefaultOrientation == AnncaConfiguration.ORIENTATION_PORTRAIT ? 90 : 180;
                    } else if (sensorEvent.values[0] < 0) {
                        // RIGHT
                        sensorPosition = AnncaConfiguration.SENSOR_POSITION_RIGHT;
                        degrees = deviceDefaultOrientation == AnncaConfiguration.ORIENTATION_PORTRAIT ? 270 : 0;
                    }
                }
                onScreenRotation(degrees);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
