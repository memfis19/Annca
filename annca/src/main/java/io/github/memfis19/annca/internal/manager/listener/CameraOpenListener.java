package io.github.memfis19.annca.internal.manager.listener;

import android.view.View;

import io.github.memfis19.annca.internal.utils.Size;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraOpenListener<CameraId> {
    void onCameraOpened(CameraId openedCameraId, Size previewSize, View view);

    void onCameraReady();

    void onCameraOpenError();
}
