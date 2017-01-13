package io.github.memfis19.annca.internal.manager.impl;

/**
 * Created by memfis on 1/13/17.
 */
public interface ParametersHandler<CameraParameters> {
    CameraParameters getParameters(CameraParameters cameraParameters);
}
