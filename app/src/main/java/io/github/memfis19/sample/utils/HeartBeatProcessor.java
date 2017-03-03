package io.github.memfis19.sample.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.Vibrator;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by memfis on 3/2/17.
 */

public class HeartBeatProcessor {

    public interface OnFrameProcessListener {
        void onFrameProcessed(final int value, final long time, float averageHrPm);
    }

    private HandlerThread imageProcessorHandlerThread = new HandlerThread("HeartBeatProcessor", Process.THREAD_PRIORITY_BACKGROUND);
    private Handler imageProcessorHandler;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    private int width = 0;
    private int height = 0;
    private OnFrameProcessListener onFrameProcessListener = null;
    private boolean isPrepared = false;

    private static final int SELECTION_SIZE = 300;
    private static final int MEDIANA_SIZE = 150;
    private static final float DEVIATION = 0.01f;
    private static final int FRAME_STEP_SIZE = 6;

    private Context context;
    private RenderScript renderScript;
    private Vibrator vibrator;
    private boolean useRenderScript = false;
    private boolean vibrate = false;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

    private List<Integer> frameValues = new ArrayList<>();
    private CircularFifoQueue<Integer> values = new CircularFifoQueue<>(SELECTION_SIZE);
    private CircularFifoQueue<Float> medianaValues = new CircularFifoQueue<>(MEDIANA_SIZE);
    private CircularFifoQueue<AbstractMap.SimpleEntry<Long, Integer>> timedValues = new CircularFifoQueue<>(SELECTION_SIZE);
    private CircularFifoQueue<Float> hrPmValues = new CircularFifoQueue<>(SELECTION_SIZE);

    private float averageHrPm = 0;

    public HeartBeatProcessor() {
        isPrepared = false;
    }

    /***
     * Construct HeartBeatProcessor which is able to use RenderScript for YUV-RGB conversion
     * @param context
     */
    public HeartBeatProcessor(Context context, boolean useRenderScript, boolean vibrate) {
        this.context = context;
        this.useRenderScript = useRenderScript;
        this.vibrate = vibrate;

        isPrepared = false;
    }

    public void prepare(int width, int height, OnFrameProcessListener onFrameProcessListener) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Preview size can't be <= 0.");

        this.width = width;
        this.height = height;

        imageProcessorHandlerThread.start();
        imageProcessorHandler = new Handler(imageProcessorHandlerThread.getLooper());

        if (context != null) renderScript = RenderScript.create(context);
        if (context != null && vibrate)
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        this.onFrameProcessListener = onFrameProcessListener;

        isPrepared = true;
    }

    public void release() {
        isPrepared = false;

        if (renderScript != null) {
            renderScript.destroy();
            renderScript.finish();
        }

        context = null;

        imageProcessorHandlerThread.quit();
        values.clear();
        hrPmValues.clear();
        timedValues.clear();
        frameValues.clear();
        medianaValues.clear();
    }

    public void processFrame(final byte[] data) {
        if (!isPrepared)
            throw new IllegalStateException("HeartBeatProcessor is not prepared. Call prepare before using.");

        imageProcessorHandler.post(new Runnable() {
            @Override
            public void run() {
                if (frameValues.size() > FRAME_STEP_SIZE) {
                    int currentFrameValue = frameValues.get(frameValues.size() - 1);
                    int previousFrameValue = frameValues.get(frameValues.size() - 1 - FRAME_STEP_SIZE);

                    final long timeValue = System.currentTimeMillis();
                    final int value = (currentFrameValue - previousFrameValue);

                    final float subtraction = Math.abs(currentFrameValue - previousFrameValue);
                    medianaValues.add(subtraction);
                    float sum = 0;
                    for (Float currentValue : medianaValues) {
                        sum += currentValue;
                    }
                    final float mediana = sum / (float) medianaValues.size();

                    if (subtraction >= mediana) {
                        values.add(value);
                        timedValues.add(new AbstractMap.SimpleEntry<>(timeValue, value));

                        long maxValue = Collections.max(values) + 1;
                        long minValue = Collections.min(values) + 1;

                        if (values.size() >= SELECTION_SIZE - 5) {
//                                float sum = 0;
//                                for (Integer currentValue : values) {
//                                    sum += currentValue;
//                                }
//                                final float mediana = sum / (float) values.size();

                            int hearbeat = 0;
                            for (Integer currentValue : values) {
                                if (currentValue <= minValue * DEVIATION) hearbeat++;
                            }
                            float hrPm = (timedValues.get(timedValues.size() - 1).getKey() - timedValues.get(0).getKey()) / hearbeat / 2;
                            hrPmValues.add(hrPm);

                            averageHrPm = 0;
                            for (Float tmp : hrPmValues) {
                                averageHrPm += tmp;
                            }
                            averageHrPm = averageHrPm / hrPmValues.size();
                        }

                        if (onFrameProcessListener != null) {
                            if (vibrator != null) vibrator.vibrate(10);
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onFrameProcessListener.onFrameProcessed(value, timeValue, averageHrPm);
                                }
                            });
                        }
                    }
                }

                int[] rgb;
                if (context != null && renderScript != null && useRenderScript) {
                    rgb = renderScriptYUVToRGB(data, width, height);
                } else {
                    rgb = new int[data.length];
                    decodeYUVToRGB(rgb, data, width, height);
                }

                int sum = 0;
                for (int pixel : rgb) {
                    sum += Color.red(pixel);
                }
                frameValues.add(sum);
            }
        });
    }

    private int[] renderScriptYUVToRGB(byte[] yuvByteArray, int width, int height) {
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript));

        Type.Builder yuvType = new Type.Builder(renderScript, Element.U8(renderScript)).setX(yuvByteArray.length);
        Allocation in = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(renderScript, Element.RGBA_8888(renderScript)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(yuvByteArray);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        byte[] tmp = new byte[out.getBytesSize()];
        out.copyTo(tmp);

        int[] colorArray = new int[tmp.length / 3];
        for (int i = 0; i < tmp.length; i += 3) {
//            int color = Color.rgb(tmp[i], tmp[i + 1], tmp[i + 2]);
            colorArray[i / 3] = (0xFF << 24) | (tmp[i] << 16) | (tmp[i + 1] << 8) | tmp[i + 2];
        }
        yuvToRgbIntrinsic.destroy();

        return colorArray;
    }

    private void decodeYUVToRGB(int[] rgb, byte[] yuv420sp, int width, int height) {
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

}