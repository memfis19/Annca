package io.github.memfis19.annca.internal.configuration;

/**
 * Created by memfis on 7/6/16.
 */
public interface ConfigurationProvider {

    int getRequestCode();

    @AnncaConfiguration.MediaAction
    int getMediaAction();

    @AnncaConfiguration.MediaQuality
    int getMediaQuality();

    int getVideoDuration();

    long getVideoFileSize();

    @AnncaConfiguration.SensorPosition
    int getSensorPosition();

    int getDegrees();

    int getMinimumVideoDuration();

    @AnncaConfiguration.FlashMode
    int getFlashMode();
}
