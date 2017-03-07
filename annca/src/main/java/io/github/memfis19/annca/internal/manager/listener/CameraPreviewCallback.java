package io.github.memfis19.annca.internal.manager.listener;

import java.nio.ByteBuffer;

/**
 * Created by memfis on 3/6/17.
 */

public interface CameraPreviewCallback {
    void onPreviewFrame(byte[] data);

    void onPreviewFrame(ByteBuffer byteBuffer);
}
