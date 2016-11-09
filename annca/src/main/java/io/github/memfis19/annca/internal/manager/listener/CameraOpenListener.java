package io.github.memfis19.annca.internal.manager.listener;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraOpenListener<CameraId, Size, SurfaceListener> {
    void onCameraOpened(CameraId openedCameraId, Size previewSize, SurfaceListener surfaceListener);

    void onCameraOpenError();
}
