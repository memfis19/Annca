package io.github.memfis19.annca.internal.manager.listener;

import io.github.memfis19.annca.internal.utils.Size;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraOpenListener<CameraId, SurfaceListener> {
    void onCameraOpened(CameraId openedCameraId, Size previewSize, SurfaceListener surfaceListener);

    void onCameraReady();

    void onCameraOpenError();
}
