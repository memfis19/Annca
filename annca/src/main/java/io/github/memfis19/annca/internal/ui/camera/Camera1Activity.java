package io.github.memfis19.annca.internal.ui.camera;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.OutputStreamWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;
import io.github.memfis19.annca.internal.configuration.ConfigurationProvider;
import io.github.memfis19.annca.internal.controller.CameraController;
import io.github.memfis19.annca.internal.controller.impl.Camera1Controller;
import io.github.memfis19.annca.internal.controller.view.CameraView;
import io.github.memfis19.annca.internal.manager.impl.CameraHandler;
import io.github.memfis19.annca.internal.manager.impl.ParametersHandler;
import io.github.memfis19.annca.internal.ui.BaseAnncaActivity;
import io.github.memfis19.annca.internal.ui.model.PhotoQualityOption;
import io.github.memfis19.annca.internal.ui.model.VideoQualityOption;
import io.github.memfis19.annca.internal.utils.CameraHelper;
import io.github.memfis19.annca.internal.utils.Utils;

/**
 * Created by memfis on 7/6/16.
 */
@SuppressWarnings("deprecation")
public class Camera1Activity extends BaseAnncaActivity<Integer> {

    private HandlerThread imageProcessorHandlerThread = new HandlerThread("ImageProcessor", Process.THREAD_PRIORITY_BACKGROUND);
    private Handler imageProcessorHandler;
    private Handler uiHandler = new Handler();

    private Long minimumValue = 0l;
    private float deviation = 0.1f;

    private List<Long> selection = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageProcessorHandlerThread.start();
        imageProcessorHandler = new Handler(imageProcessorHandlerThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        imageProcessorHandlerThread.quit();
    }

    private GraphView graphView;

    @Override
    protected View getUserContentView(LayoutInflater layoutInflater, ViewGroup parent) {
        RelativeLayout parentContent = (RelativeLayout) super.getUserContentView(layoutInflater, parent);
        graphView = new GraphView(this);

        graphView.setBackgroundColor(Color.WHITE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, Utils.convertDipToPixels(this, 100));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        parentContent.addView(graphView, layoutParams);

        return parentContent;
    }

    @Override
    public CameraController<Integer> createCameraController(CameraView cameraView, ConfigurationProvider configurationProvider) {
        return new Camera1Controller(cameraView, configurationProvider);
    }

    private OutputStreamWriter outputStreamWriter;

    @Override
    protected void onResume() {
        super.onResume();

//        try {
//            File path = Environment.getExternalStorageDirectory();
//            File file = new File(path, "camera.txt");
//            FileOutputStream stream = new FileOutputStream(file);
//            outputStreamWriter = new OutputStreamWriter(stream);
//        } catch (Exception e) {
//            Log.e("File", "Error", e);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        try {
//            outputStreamWriter.close();
//        } catch (Exception e) {
//            Log.e("File", "Error", e);
//        }
    }

    private Camera.Parameters parameters;
    private Camera.Size size;
    private BitmapFactory.Options options = new BitmapFactory.Options();

    @Override
    protected void onCameraControllerReady() {
        super.onCameraControllerReady();
        try {
            getCameraController().getCameraManager().handleParameters(new ParametersHandler<Camera.Parameters>() {
                @Override
                public Camera.Parameters getParameters(Camera.Parameters params) {
                    parameters = params;
                    size = parameters.getPreviewSize();
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                    return params;
                }
            });
            getCameraController().getCameraManager().handleCamera(new CameraHandler<Camera>() {
                @Override
                public void handleCamera(Camera camera) {
                    camera.setPreviewCallback(new Camera.PreviewCallback() {

                        private int currentFrame = 0;
                        private List<Map.Entry<int[], int[]>> frames = new ArrayList<>();

                        @Override
                        public void onPreviewFrame(final byte[] data, final Camera camera) {
                            try {
                                imageProcessorHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (frames.size() > 6) {
                                            int[] currentFrameArray = frames.get(currentFrame - 6).getKey();
                                            int[] previousFrameArray = frames.get(currentFrame - 6).getValue();

                                            if (currentFrameArray.length == previousFrameArray.length) {
                                                int[] result = new int[currentFrameArray.length];

                                                for (int i = 0; i < currentFrameArray.length; ++i)
                                                    result[i] = (previousFrameArray[i] ^ currentFrameArray[i]);

                                                frames.remove(currentFrame - 6);
                                                currentFrame--;

                                                int sum = 0;
                                                for (int pixel : result) {
                                                    int b = Color.blue(pixel);
                                                    int r = Color.red(pixel);
                                                    int g = Color.green(pixel);
                                                    sum += Color.blue(pixel) + Color.red(pixel) + Color.green(pixel);
                                                }
                                                sum = sum / result.length;

                                                final int value = sum;
//                                                try {
//                                                    outputStreamWriter.write(System.currentTimeMillis() + "," + sum + "\n");
//                                                } catch (Exception error) {
//                                                }
                                                graphView.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        graphView.drawPoint(value);
                                                    }
                                                });

                                            }
                                        }
                                        int[] rgb = new int[data.length];
                                        decodeYUV420SP(rgb, data, size.width, size.height);
                                        currentFrame++;
                                        frames.add(new AbstractMap.SimpleEntry<>(rgb, new int[1]));
                                        if (frames.size() > 6) {
                                            frames.get(currentFrame - 6).setValue(rgb);
                                        }
                                    }

                                });
                            } catch (Exception e) {
                                Log.e("Camera", "Error", e);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e("Camera", "Error", e);
        }
    }

    public class GraphView extends View {

        private List<Point> pointsToDraw = new ArrayList<>();
        private List<Integer> values = new ArrayList<>();
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private int topPart = 0;
        private int hrPm = 0;

        private static final int SIZE = 200;

        public GraphView(Context context) {
            super(context);
            init();
        }

        public GraphView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public GraphView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(5);
            paint.setTextSize(30);
        }

        void drawPoint(int value) {
            if (values.size() > SIZE) {
                pointsToDraw.clear();
                values.clear();
                path.reset();
                minimumValue = 0l;
            }
            values.add(value);


            int height = getHeight();
            int width = getWidth();

            topPart = height / 2;

            try {
                List<Integer> newValues = values;
                int maxValue = Collections.max(newValues) + 1;
                int minValue = Collections.min(newValues) + 1;

                if (minimumValue < minValue) {
                    minimumValue = (long) minValue;
                    selection.clear();
                    hrPm = 0;
                }

                if (minimumValue - value <= minimumValue * deviation) {
                    selection.add(System.currentTimeMillis());

                    hrPm = (int) ((selection.get(selection.size() - 1) - selection.get(0)) / (selection.size())) / 2;
                }

                long total = (Math.abs((long) maxValue) + Math.abs((long) minValue));
                topPart = (int) (height - height * Math.abs((long) maxValue) / total);
                int bottomPart = height - topPart;

                pointsToDraw.clear();
                for (int i = 0; i < newValues.size(); ++i) {
                    float x = width * i / SIZE;
                    float y = newValues.get(i) > 0 ? bottomPart + topPart * Math.abs((long) newValues.get(i)) / Math.abs((long) maxValue) : bottomPart * Math.abs((long) newValues.get(i)) / Math.abs((long) minValue);
                    pointsToDraw.add(new Point((int) x, (int) y));
                }

                postInvalidate();
            } catch (Exception error) {
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawText(String.valueOf(hrPm), 0, String.valueOf(hrPm).length(), 100, 100, paint);
            for (int i = 0; i < pointsToDraw.size() - 1; ++i) {
                canvas.drawLine(pointsToDraw.get(i).x, pointsToDraw.get(i).y, pointsToDraw.get(i + 1).x, pointsToDraw.get(i + 1).y, paint);
            }
        }
    }


    void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    @Override
    protected CharSequence[] getVideoQualityOptions() {
        List<CharSequence> videoQualities = new ArrayList<>();

        if (getMinimumVideoDuration() > 0)
            videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_AUTO, CameraHelper.getCamcorderProfile(AnncaConfiguration.MEDIA_QUALITY_AUTO, getCameraController().getCurrentCameraId()), getMinimumVideoDuration()));

        CamcorderProfile camcorderProfile = CameraHelper.getCamcorderProfile(AnncaConfiguration.MEDIA_QUALITY_HIGH, getCameraController().getCurrentCameraId());
        double videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, getVideoFileSize());
        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, camcorderProfile, videoDuration));

        camcorderProfile = CameraHelper.getCamcorderProfile(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, getCameraController().getCurrentCameraId());
        videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, getVideoFileSize());
        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, camcorderProfile, videoDuration));

        camcorderProfile = CameraHelper.getCamcorderProfile(AnncaConfiguration.MEDIA_QUALITY_LOW, getCameraController().getCurrentCameraId());
        videoDuration = CameraHelper.calculateApproximateVideoDuration(camcorderProfile, getVideoFileSize());
        videoQualities.add(new VideoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOW, camcorderProfile, videoDuration));

        CharSequence[] array = new CharSequence[videoQualities.size()];
        videoQualities.toArray(array);

        return array;
    }

    @Override
    protected CharSequence[] getPhotoQualityOptions() {
        List<CharSequence> photoQualities = new ArrayList<>();
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGHEST, getCameraController().getCameraManager().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGHEST)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_HIGH, getCameraController().getCameraManager().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_HIGH)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_MEDIUM, getCameraController().getCameraManager().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_MEDIUM)));
        photoQualities.add(new PhotoQualityOption(AnncaConfiguration.MEDIA_QUALITY_LOWEST, getCameraController().getCameraManager().getPhotoSizeForQuality(AnncaConfiguration.MEDIA_QUALITY_LOWEST)));

        CharSequence[] array = new CharSequence[photoQualities.size()];
        photoQualities.toArray(array);

        return array;
    }

}
