package io.github.memfis19.annca.internal.manager.impl;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.gles.Drawable2d;
import io.github.memfis19.annca.internal.gles.EglCore;
import io.github.memfis19.annca.internal.gles.GlUtil;
import io.github.memfis19.annca.internal.gles.ScaledDrawable2d;
import io.github.memfis19.annca.internal.gles.Sprite2d;
import io.github.memfis19.annca.internal.gles.Texture2dProgram;
import io.github.memfis19.annca.internal.gles.WindowSurface;
import io.github.memfis19.annca.internal.manager.listener.CameraCloseListener;
import io.github.memfis19.annca.internal.manager.listener.CameraOpenListener;
import io.github.memfis19.annca.internal.manager.listener.CameraPhotoListener;
import io.github.memfis19.annca.internal.manager.listener.CameraPreviewCallback;
import io.github.memfis19.annca.internal.manager.listener.CameraVideoListener;
import io.github.memfis19.annca.internal.utils.CameraHelper;
import io.github.memfis19.annca.internal.utils.Size;

/**
 * Created by memfis on 8/14/16.
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class OpenGlCamera1Manager extends BaseCameraManager<Integer, Camera.Parameters, Camera>
        implements SurfaceHolder.Callback, Camera.PictureCallback {

    private static final String TAG = "Camera1Manager";

    private Camera camera;

    private Surface surface;
    private SurfaceView surfaceView;

    private static OpenGlCamera1Manager currentInstance;

    private int orientation;
    private int displayRotation = 0;

    private File outputPath;
    private CameraVideoListener videoListener;
    private CameraPhotoListener photoListener;
    private CameraOpenListener<Integer> cameraOpenListener;

    private CameraPreviewCallback cameraPreviewCallback;

    private OpenGlCamera1Manager() {

    }

    public static OpenGlCamera1Manager getInstance() {
        if (currentInstance == null) currentInstance = new OpenGlCamera1Manager();
        return currentInstance;
    }

    private EglCore eglCore;
    private WindowSurface windowSurface;
    private Texture2dProgram texture2dProgram;
    private SurfaceTexture cameraTexture;
    private final ScaledDrawable2d scaledDrawable2d =
            new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
    private final Sprite2d sprite2d = new Sprite2d(scaledDrawable2d);

    private int windowSurfaceWidth;
    private int windowSurfaceHeight;
    private float positionX, positionY;
    //    private int mCameraPreviewWidth, mCameraPreviewHeight;
    private float[] displayProjectionMatrix = new float[16];

    private void initOpenGl() {
        eglCore = new EglCore(null, 0);
    }

    @Override
    public void openCamera(final Integer cameraId,
                           final CameraOpenListener<Integer> cameraOpenListener) {
        this.currentCameraId = cameraId;
        this.cameraOpenListener = cameraOpenListener;
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    camera = Camera.open(cameraId);
                    prepareCameraOutputs();
                    if (cameraOpenListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpened(cameraId, previewSize, surfaceView);
                            }
                        });
                    }
                } catch (Exception error) {
                    Log.d(TAG, "Can't open camera: " + error.getMessage());
                    if (cameraOpenListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraOpenListener.onCameraOpenError();
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void closeCamera(final CameraCloseListener<Integer> cameraCloseListener) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    if (cameraCloseListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                cameraCloseListener.onCameraClosed(currentCameraId);
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void setFlashMode(@AnncaConfiguration.FlashMode int flashMode) {
        setFlashMode(camera, camera.getParameters(), flashMode);
    }

    @Override
    public void takePhoto(File photoFile, CameraPhotoListener cameraPhotoListener) {
        this.outputPath = photoFile;
        this.photoListener = cameraPhotoListener;
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                setCameraPhotoQuality(camera);
                camera.takePicture(null, null, currentInstance);
            }
        });
    }

    @Override
    public void startVideoRecord(final File videoFile, CameraVideoListener cameraVideoListener) {
        if (isVideoRecording) return;

        this.outputPath = videoFile;
        this.videoListener = cameraVideoListener;

        if (videoListener != null)
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (prepareVideoRecorder()) {
                        videoRecorder.start();
                        isVideoRecording = true;
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStarted(videoSize);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void stopVideoRecord() {
        if (isVideoRecording)
            backgroundHandler.post(new Runnable() {
                @Override
                public void run() {

                    try {
                        if (videoRecorder != null) videoRecorder.stop();
                    } catch (Exception ignore) {
                        // ignore illegal state.
                        // appear in case time or file size reach limit and stop already called.
                    }

                    isVideoRecording = false;
                    releaseVideoRecorder();

                    if (videoListener != null) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                videoListener.onVideoRecordStopped(outputPath);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void releaseCameraManager() {
        super.releaseCameraManager();
    }

    @Override
    public void initializeCameraManager(ConfigurationProvider configurationProvider, Context context) {
        super.initializeCameraManager(configurationProvider, context);

        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(this);

        initOpenGl();

        numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i;
                faceBackCameraOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i;
                faceFrontCameraOrientation = cameraInfo.orientation;
            }
        }
    }

    @Override
    public Size getPhotoSizeForQuality(@AnncaConfiguration.MediaQuality int mediaQuality) {
        return CameraHelper.getPictureSize(Size.fromList(camera.getParameters().getSupportedPictureSizes()), mediaQuality);
    }

    @Override
    protected void prepareCameraOutputs() {
        try {
            if (configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_AUTO) {
                camcorderProfile = CameraHelper.getCamcorderProfile(currentCameraId, configurationProvider.getVideoFileSize(), configurationProvider.getMinimumVideoDuration());
            } else
                camcorderProfile = CameraHelper.getCamcorderProfile(configurationProvider.getMediaQuality(), currentCameraId);

            List<Size> previewSizes = Size.fromList(camera.getParameters().getSupportedPreviewSizes());
            List<Size> pictureSizes = Size.fromList(camera.getParameters().getSupportedPictureSizes());
            List<Size> videoSizes;
            if (Build.VERSION.SDK_INT > 10)
                videoSizes = Size.fromList(camera.getParameters().getSupportedVideoSizes());
            else videoSizes = previewSizes;

            videoSize = CameraHelper.getSizeWithClosestRatio(
                    (videoSizes == null || videoSizes.isEmpty()) ? previewSizes : videoSizes,
                    camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);

            photoSize = CameraHelper.getPictureSize(
                    (pictureSizes == null || pictureSizes.isEmpty()) ? previewSizes : pictureSizes,
                    configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_AUTO
                            ? AnncaConfiguration.MEDIA_QUALITY_HIGHEST : configurationProvider.getMediaQuality());

            if (configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED) {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, photoSize.getWidth(), photoSize.getHeight());
            } else {
                previewSize = CameraHelper.getSizeWithClosestRatio(previewSizes, videoSize.getWidth(), videoSize.getHeight());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while setup camera sizes.");
        }
    }

    @Override
    protected boolean prepareVideoRecorder() {
        videoRecorder = new MediaRecorder();
        try {
            camera.lock();
            camera.unlock();
            videoRecorder.setCamera(camera);

            videoRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            videoRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            videoRecorder.setOutputFormat(camcorderProfile.fileFormat);
            videoRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
            videoRecorder.setVideoSize(videoSize.getWidth(), videoSize.getHeight());
            videoRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            videoRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            videoRecorder.setAudioEncodingBitRate(camcorderProfile.audioBitRate);
            videoRecorder.setAudioChannels(camcorderProfile.audioChannels);
            videoRecorder.setAudioSamplingRate(camcorderProfile.audioSampleRate);
            videoRecorder.setAudioEncoder(camcorderProfile.audioCodec);

            videoRecorder.setOutputFile(outputPath.toString());

            if (configurationProvider.getVideoFileSize() > 0) {
                videoRecorder.setMaxFileSize(configurationProvider.getVideoFileSize());

                videoRecorder.setOnInfoListener(this);
            }
            if (configurationProvider.getVideoDuration() > 0) {
                videoRecorder.setMaxDuration(configurationProvider.getVideoDuration());

                videoRecorder.setOnInfoListener(this);
            }

            videoRecorder.setOrientationHint(getVideoOrientation(configurationProvider.getSensorPosition()));
            videoRecorder.setPreviewDisplay(surface);

            videoRecorder.prepare();

            return true;
        } catch (IllegalStateException error) {
            Log.e(TAG, "IllegalStateException preparing MediaRecorder: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "IOException preparing MediaRecorder: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error during preparing MediaRecorder: " + error.getMessage());
        }

        releaseVideoRecorder();
        return false;
    }

    @Override
    protected void onMaxDurationReached() {
        stopVideoRecord();
    }

    @Override
    protected void onMaxFileSizeReached() {
        stopVideoRecord();
    }

    @Override
    protected void releaseVideoRecorder() {
        super.releaseVideoRecorder();

        try {
            camera.lock(); // lock camera for later use
        } catch (Exception ignore) {
        }
    }

    //------------------------Implementation------------------

    private void startPreview(SurfaceTexture surfaceTexture) {
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(currentCameraId, cameraInfo);
            int cameraRotationOffset = cameraInfo.orientation;

            Camera.Parameters parameters = camera.getParameters();
            setAutoFocus(camera, parameters);
            setFlashMode(configurationProvider.getFlashMode());

            if (configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_PHOTO
                    || configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED)
                turnPhotoCameraFeaturesOn(camera, parameters);
            else if (configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_PHOTO)
                turnVideoCameraFeaturesOn(camera, parameters);

            int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break; // Natural orientation
                case Surface.ROTATION_90:
                    degrees = 90;
                    break; // Landscape left
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;// Upside down
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;// Landscape right
            }

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                displayRotation = (cameraRotationOffset + degrees) % 360;
                displayRotation = (360 - displayRotation) % 360; // compensate
            } else {
                displayRotation = (cameraRotationOffset - degrees + 360) % 360;
            }

            this.camera.setDisplayOrientation(displayRotation);

            if (Build.VERSION.SDK_INT > 13
                    && (configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_VIDEO
                    || configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED)) {
//                parameters.setRecordingHint(true);
            }

            if (Build.VERSION.SDK_INT > 14
                    && parameters.isVideoStabilizationSupported()
                    && (configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_VIDEO
                    || configurationProvider.getMediaAction() == AnncaConfiguration.MEDIA_ACTION_UNSPECIFIED)) {
                parameters.setVideoStabilization(true);
            }

            parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
            parameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());

            camera.setParameters(parameters);
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();

            if (cameraOpenListener != null) cameraOpenListener.onCameraReady();
        } catch (IOException error) {
            Log.d(TAG, "Error setting camera preview: " + error.getMessage());
        } catch (Exception ignore) {
            Log.d(TAG, "Error starting camera preview: " + ignore.getMessage());
        }
    }

    private void turnPhotoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            if (Build.VERSION.SDK_INT > 13)
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
    }

    private void turnVideoCameraFeaturesOn(Camera camera, Camera.Parameters parameters) {
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        camera.setParameters(parameters);
    }

    @Override
    public void handleCamera(CameraHandler<Camera> cameraHandler) {
        cameraHandler.handleCamera(camera);
    }

    @Override
    public boolean handleParameters(ParametersHandler<Camera.Parameters> parameters) {
        try {
            camera.setParameters(parameters.getParameters(camera.getParameters()));
            return true;
        } catch (Throwable ignore) {
        }
        return false;
    }

    @Override
    public void setPreviewCallback(final CameraPreviewCallback cameraPreviewCallback) {
        if (cameraPreviewCallback == null) return;
        this.cameraPreviewCallback = cameraPreviewCallback;
    }

    private void setAutoFocus(Camera camera, Camera.Parameters parameters) {
        try {
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            }
        } catch (Exception ignore) {
        }
    }

    private void setFlashMode(Camera camera, Camera.Parameters parameters, @AnncaConfiguration.FlashMode int flashMode) {
        try {
            switch (flashMode) {
                case AnncaConfiguration.FLASH_MODE_AUTO:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
                case AnncaConfiguration.FLASH_MODE_ON:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    break;
                case AnncaConfiguration.FLASH_MODE_OFF:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    break;
                default:
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    break;
            }
            camera.setParameters(parameters);
        } catch (Exception ignore) {
        }
    }

    private void setCameraPhotoQuality(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        parameters.setPictureFormat(PixelFormat.JPEG);

        if (configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_LOW) {
            parameters.setJpegQuality(50);
        } else if (configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_MEDIUM) {
            parameters.setJpegQuality(75);
        } else if (configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_HIGH) {
            parameters.setJpegQuality(100);
        } else if (configurationProvider.getMediaQuality() == AnncaConfiguration.MEDIA_QUALITY_HIGHEST) {
            parameters.setJpegQuality(100);
        }
        parameters.setPictureSize(photoSize.getWidth(), photoSize.getHeight());

        camera.setParameters(parameters);
    }

    @Override
    protected int getPhotoOrientation(@AnncaConfiguration.SensorPosition int sensorPosition) {
        int rotate;
        if (currentCameraId.equals(faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + configurationProvider.getDegrees()) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - configurationProvider.getDegrees()) % 360;
        }

        if (rotate == 0) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        } else if (rotate == 90) {
            orientation = ExifInterface.ORIENTATION_ROTATE_90;
        } else if (rotate == 180) {
            orientation = ExifInterface.ORIENTATION_ROTATE_180;
        } else if (rotate == 270) {
            orientation = ExifInterface.ORIENTATION_ROTATE_270;
        }

        return orientation;

    }

    @Override
    protected int getVideoOrientation(@AnncaConfiguration.SensorPosition int sensorPosition) {
        int degrees = 0;
        switch (sensorPosition) {
            case AnncaConfiguration.SENSOR_POSITION_UP:
                degrees = 0;
                break; // Natural orientation
            case AnncaConfiguration.SENSOR_POSITION_LEFT:
                degrees = 90;
                break; // Landscape left
            case AnncaConfiguration.SENSOR_POSITION_UP_SIDE_DOWN:
                degrees = 180;
                break;// Upside down
            case AnncaConfiguration.SENSOR_POSITION_RIGHT:
                degrees = 270;
                break;// Landscape right
        }

        int rotate;
        if (currentCameraId.equals(faceFrontCameraId)) {
            rotate = (360 + faceFrontCameraOrientation + degrees) % 360;
        } else {
            rotate = (360 + faceBackCameraOrientation - degrees) % 360;
        }
        return rotate;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Surface surface = holder.getSurface();
        this.surface = holder.getSurface();
        windowSurface = new WindowSurface(eglCore, surface, false);
        windowSurface.makeCurrent();

        // Create and configure the SurfaceTexture, which will receive frames from the
        // camera.  We set the textured rect's program to render from it.
        texture2dProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
        int textureId = texture2dProgram.createTextureObject();
        cameraTexture = new SurfaceTexture(textureId);
        sprite2d.setTexture(textureId);

        boolean newSurface = true;
        if (!newSurface) {
            // This Surface was established on a previous run, so no surfaceChanged()
            // message is forthcoming.  Finish the surface setup now.
            //
            // We could also just call this unconditionally, and perhaps do an unnecessary
            // bit of reallocating if a surface-changed message arrives.
            windowSurfaceWidth = windowSurface.getWidth();
            windowSurfaceHeight = windowSurface.getHeight();
            finishSurfaceSetup();
        }

        final ByteBuffer pixelBuf = ByteBuffer.allocateDirect(previewSize.getWidth() * previewSize.getHeight() * 4);
        pixelBuf.order(ByteOrder.LITTLE_ENDIAN);

        cameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                cameraTexture.updateTexImage();

                if (cameraPreviewCallback != null) {
                    pixelBuf.clear();
                    pixelBuf.rewind();

                    GLES20.glReadPixels(0, 0, previewSize.getWidth(), previewSize.getHeight(),
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuf);

                    byte[] argb = new byte[pixelBuf.remaining()];
                    pixelBuf.get(argb);

                    cameraPreviewCallback.onPreviewFrame(argb);
                }

                draw();
            }
        });
    }

    private void draw() {
        GlUtil.checkGlError("draw start");

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        sprite2d.draw(texture2dProgram, displayProjectionMatrix);
        windowSurface.swapBuffers();

        GlUtil.checkGlError("draw done");
    }

    private void finishSurfaceSetup() {
        int width = windowSurfaceWidth;
        int height = windowSurfaceHeight;
        Log.d(TAG, "finishSurfaceSetup size=" + width + "x" + height +
                " camera=" + previewSize.getWidth() + "x" + previewSize.getHeight());

        // Use full window.
        GLES20.glViewport(0, 0, width, height);

        // Simple orthographic projection, with (0,0) in lower-left corner.
        Matrix.orthoM(displayProjectionMatrix, 0, 0, width, 0, height, -1, 1);

        // Default position is center of screen.
        positionX = width / 2.0f;
        positionY = height / 2.0f;

        updateGeometry();

        // Ready to go, start the camera.
        Log.d(TAG, "starting camera preview");
        startPreview(cameraTexture);
    }

    private void releaseGl() {
        GlUtil.checkGlError("releaseGl start");

        if (windowSurface != null) {
            windowSurface.release();
            windowSurface = null;
        }
        if (texture2dProgram != null) {
            texture2dProgram.release();
            texture2dProgram = null;
        }
        GlUtil.checkGlError("releaseGl done");

        eglCore.makeNothingCurrent();
    }

    private static final int DEFAULT_ZOOM_PERCENT = 0;      // 0-100
    private static final int DEFAULT_SIZE_PERCENT = 100;     // 0-100
    private static final int DEFAULT_ROTATE_PERCENT = 75;    // 0-100

    private void updateGeometry() {
        int width = windowSurfaceWidth;
        int height = windowSurfaceHeight;

        int smallDim = Math.min(width, height);
        // Max scale is a bit larger than the screen, so we can show over-size.
        float scaled = smallDim * (DEFAULT_SIZE_PERCENT / 100.0f) * 1.25f;
        float cameraAspect = (float) previewSize.getWidth() / previewSize.getHeight();
        int newWidth = Math.round(scaled * cameraAspect);
        int newHeight = Math.round(scaled);

        float zoomFactor = 1.0f - (DEFAULT_ZOOM_PERCENT / 100.0f);
        int rotAngle = Math.round(360 * (DEFAULT_ROTATE_PERCENT / 100.0f));

        sprite2d.setScale(newWidth, newHeight);
        sprite2d.setPosition(positionX, positionY);
        sprite2d.setRotation(rotAngle);
        scaledDrawable2d.setScale(zoomFactor);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surface = holder.getSurface();

        windowSurfaceWidth = width;
        windowSurfaceHeight = height;
        finishSurfaceSetup();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseGl();
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        File pictureFile = outputPath;
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions.");
            return;
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (FileNotFoundException error) {
            Log.e(TAG, "File not found: " + error.getMessage());
        } catch (IOException error) {
            Log.e(TAG, "Error accessing file: " + error.getMessage());
        } catch (Throwable error) {
            Log.e(TAG, "Error saving file: " + error.getMessage());
        }

        try {
            ExifInterface exif = new ExifInterface(pictureFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + getPhotoOrientation(configurationProvider.getSensorPosition()));
            exif.saveAttributes();

            if (photoListener != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        photoListener.onPhotoTaken(outputPath);
                    }
                });
            }
        } catch (Throwable error) {
            Log.e(TAG, "Can't save exif info: " + error.getMessage());
        }
    }
}
