package io.github.memfis19.sample.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by memfis on 3/2/17.
 */
public class GraphView extends View {

    private List<Point> pointsToDraw = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int topPart = 0;
    private float averageHrPm = 0;

    private static final int SIZE = 150;
    private CircularFifoQueue<Integer> values = new CircularFifoQueue<>(SIZE);

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

    public void drawPoint(final int value, final long time, float averageHrPm) {
        values.add(value);
        this.averageHrPm = averageHrPm;

        int height = getHeight();
        int width = getWidth();

        topPart = height / 2;

        try {
            long maxValue = Collections.max(values) + 1;
            long minValue = Collections.min(values) + 1;

            long total = (Math.abs(maxValue) + Math.abs(minValue));
            topPart = (int) (height - height * Math.abs(maxValue) / total);
            int bottomPart = height - topPart;

            pointsToDraw.clear();
            for (int i = 0; i < values.size(); ++i) {
                float x = width * i / SIZE;
                float y = values.get(i) > 0 ? bottomPart + topPart * Math.abs((long) values.get(i)) / Math.abs(maxValue) : bottomPart * Math.abs((long) values.get(i)) / Math.abs(minValue);
                pointsToDraw.add(new Point((int) x, (int) y));
            }

            postInvalidate();
        } catch (Exception error) {
            Log.e("", "", error);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLine(0, topPart, getWidth(), topPart, paint);
        canvas.drawText(String.valueOf(averageHrPm), 0, String.valueOf(averageHrPm).length(), 100, 100, paint);
        for (int i = 0; i < pointsToDraw.size() - 1; ++i) {
            canvas.drawLine(pointsToDraw.get(i).x, pointsToDraw.get(i).y, pointsToDraw.get(i + 1).x, pointsToDraw.get(i + 1).y, paint);
        }
    }
}
