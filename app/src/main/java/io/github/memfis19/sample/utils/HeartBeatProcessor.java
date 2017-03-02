package io.github.memfis19.sample.utils;

import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.memfis19.annca.internal.utils.Size;

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

    private Size previewSize = null;
    private OnFrameProcessListener onFrameProcessListener = null;
    private boolean isPrepared = false;

    private static final int SELECTION_SIZE = 150;
    private static final int MEDIANA_SIZE = 50;
    private static final float DEVIATION = 0.01f;
    private static final int FRAME_STEP_SIZE = 6;

    private List<Integer> frameValues = new ArrayList<>();
    private CircularFifoQueue<Integer> values = new CircularFifoQueue<>(SELECTION_SIZE);
    private CircularFifoQueue<Float> medianaValues = new CircularFifoQueue<>(MEDIANA_SIZE);
    private CircularFifoQueue<AbstractMap.SimpleEntry<Long, Integer>> timedValues = new CircularFifoQueue<>(SELECTION_SIZE);
    private CircularFifoQueue<Float> hrPmValues = new CircularFifoQueue<>(SELECTION_SIZE);

    private float averageHrPm = 0;

    public HeartBeatProcessor() {
    }

    public void prepare(Size previewSize, OnFrameProcessListener onFrameProcessListener) {
        if (previewSize == null)
            throw new IllegalArgumentException("Preview size can't be null.");

        imageProcessorHandlerThread.start();
        imageProcessorHandler = new Handler(imageProcessorHandlerThread.getLooper());

        this.previewSize = previewSize;
        this.onFrameProcessListener = onFrameProcessListener;
    }

    public void release() {
        imageProcessorHandlerThread.quit();
        values.clear();
        hrPmValues.clear();
        timedValues.clear();
        frameValues.clear();
        medianaValues.clear();
    }

    public void processFrame(final byte[] data) {
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
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onFrameProcessListener.onFrameProcessed(value, timeValue, averageHrPm);
                                }
                            });
                        }
                    }
                }

                int[] rgb = new int[data.length];
                decodeYUV420SP(rgb, data, previewSize.getWidth(), previewSize.getHeight());
                int sum = 0;
                for (int pixel : rgb) {
                    sum += Color.red(pixel);
                }
                frameValues.add(sum);
            }
        });
    }

    private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
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