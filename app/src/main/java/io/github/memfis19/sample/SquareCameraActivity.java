package io.github.memfis19.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.FileOutputStream;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.controller.CameraController;
import io.github.memfis19.annca.internal.controller.impl.Camera1Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.ui.AnncaCameraActivity;
import io.github.memfis19.annca.internal.ui.view.CameraSwitchView;
import io.github.memfis19.annca.internal.utils.Size;

/**
 * Created by memfis on 2/7/17.
 */

public class SquareCameraActivity extends AnncaCameraActivity<Integer> {

    private static final int REQUEST_CODE = 404;

    private RelativeLayout customCameraLayout;

    private int PREVIEW_SIZE = 0;
    private int PHOTO_CROP_SIZE = 0;
    private int VIDEO_CROP_SIZE = 0;

    private boolean ffmpegSupported = true;

    @AnncaConfiguration.MediaAction
    private static final int PHOTO = AnncaConfiguration.MEDIA_ACTION_VIDEO;

    @AnncaConfiguration.MediaQuality
    private static final int QUALITY = AnncaConfiguration.MEDIA_QUALITY_HIGHEST;

    @AnncaConfiguration.FlashMode
    private static final int FLASH = AnncaConfiguration.FLASH_MODE_AUTO;

    private boolean isVideoRecording = false;

    @Override
    protected View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        customCameraLayout = (RelativeLayout) layoutInflater.inflate(R.layout.custom_camera_layout, parent, false);

        customCameraLayout.findViewById(R.id.take_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCameraController().takePhoto();
            }
        });

        customCameraLayout.findViewById(R.id.record_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isVideoRecording) {
                    isVideoRecording = true;
                    getCameraController().startVideoRecord();
                } else {
                    isVideoRecording = false;
                    getCameraController().stopVideoRecord();
                }
            }
        });

        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                    ffmpegSupported = false;
                }

                @Override
                public void onSuccess() {
                    ffmpegSupported = true;
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            ffmpegSupported = false;
        }

        return customCameraLayout;
    }

    @Override
    public int getRequestCode() {
        return REQUEST_CODE;
    }

    @Override
    public int getMediaAction() {
        return PHOTO;
    }

    @Override
    public int getMediaQuality() {
        return QUALITY;
    }

    @Override
    public int getVideoDuration() {
        return 1000;
    }

    @Override
    public long getVideoFileSize() {
        return 5 * 1024 * 1024;
    }

    @Override
    public int getMinimumVideoDuration() {
        return 1000;
    }

    @Override
    public int getFlashMode() {
        return FLASH;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void updateCameraPreview(Size size, View cameraPreview) {
        if (size.getHeight() < size.getWidth()) PREVIEW_SIZE = size.getHeight();
        else PREVIEW_SIZE = size.getWidth();

        setCustomCameraPreview(cameraPreview, size, new Size(PREVIEW_SIZE, PREVIEW_SIZE));
    }

    @Override
    public void updateUiForMediaAction(@AnncaConfiguration.MediaAction int mediaAction) {

    }

    @Override
    public void updateCameraSwitcher(int numberOfCameras) {

    }

    @Override
    public void onPhotoTaken() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Photo processing!!!");
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Size size = getCameraController().getCameraManager().getPhotoSizeForQuality(QUALITY);
                if (size.getHeight() < size.getWidth()) PHOTO_CROP_SIZE = size.getHeight();
                else PHOTO_CROP_SIZE = size.getWidth();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(getCameraController().getOutputFile().toString(), options);

                int width = options.outWidth;
                int height = options.outHeight;

                int croppedWidth = (width > height) ? height : width;
                int croppedHeight = (width > height) ? height : width;

                Bitmap toCrop = BitmapFactory.decodeFile(getCameraController().getOutputFile().toString());
                Bitmap cropped = Bitmap.createBitmap(toCrop, 0, 0, croppedWidth, croppedHeight, null, true);

                toCrop.recycle();

                FileOutputStream out = null;
                try {
                    ExifInterface exif = new ExifInterface(getCameraController().getOutputFile().getAbsolutePath());
                    String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);

                    Bitmap scaled = Bitmap.createScaledBitmap(cropped, PHOTO_CROP_SIZE, PHOTO_CROP_SIZE, true);
                    cropped.recycle();

                    out = new FileOutputStream(getCameraController().getOutputFile());
                    scaled.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                    scaled.recycle();

                    ExifInterface newExif = new ExifInterface(getCameraController().getOutputFile().getAbsolutePath());
                    newExif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
                    newExif.saveAttributes();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(SquareCameraActivity.this, "Photo is ready", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("", "");
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (Exception e) {
                        Log.e("", "");
                    }
                }
            }
        }).start();
    }


    @Override
    public void onVideoRecordStart(int width, int height) {
        if (height < width) VIDEO_CROP_SIZE = height;
        else VIDEO_CROP_SIZE = width;
    }

    @Override
    public void onVideoRecordStop() {
        if (!ffmpegSupported) return;
        FFmpeg ffmpeg = FFmpeg.getInstance(this);

        String in = getCameraController().getOutputFile().getAbsolutePath();
        String out = getCameraController().getOutputFile().getAbsolutePath().replace(".mp4", "_crop.mp4");
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Video processing!!!");

        String[] cmd = {"-i", in, "-filter:v", "crop=" + VIDEO_CROP_SIZE + ":" + VIDEO_CROP_SIZE + ":" + 0 + ":" + 0, "-c:a", "copy", out};
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {
                    super.onStart();
                    progressDialog.show();
                }

                @Override
                public void onSuccess(String message) {
                    super.onSuccess(message);
                }

                @Override
                public void onProgress(String message) {
                    super.onProgress(message);
                    progressDialog.setMessage(message);
                }

                @Override
                public void onFailure(String message) {
                    super.onFailure(message);
                }

                @Override
                public void onFinish() {
                    super.onFinish();
                    progressDialog.dismiss();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.e("", "");
        }
    }

    @Override
    public int getCameraFace() {
        return CameraSwitchView.CAMERA_TYPE_FRONT;
    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public void releaseCameraPreview() {

    }

    @Override
    public CameraController<Integer> createCameraController(CameraView cameraView, ConfigurationProvider configurationProvider) {
        return new Camera1Controller(cameraView, configurationProvider);
    }

    @Override
    protected void onScreenRotation(int degrees) {

    }

    @Override
    public int getMediaResultBehaviour() {
        return AnncaConfiguration.PREVIEW;
    }
}
