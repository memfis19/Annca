package io.github.memfis19.annca.internal.manager.listener;

import java.io.File;

/**
 * Created by memfis on 8/14/16.
 */
public interface CameraPhotoListener {
    void onPhotoTaken(File photoFile);

    void onPhotoTakeError();
}
