package io.github.memfis19.annca.internal.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import io.github.memfis19.annca.internal.utils.Size;

/**
 * Layout that adjusts to maintain a specific aspect ratio.
 */
public class AspectFrameLayout extends FrameLayout {

    private static final String TAG = "AspectFrameLayout";

    private double targetAspectRatio = -1.0;        // initially use default window size

    private Size size = null;
    private int actualPreviewWidth;
    private int actualPreviewHeight;

    public AspectFrameLayout(Context context) {
        super(context);
    }

    public AspectFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }

        if (targetAspectRatio != aspectRatio) {
            targetAspectRatio = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (size != null) {
            setMeasuredDimension(size.getWidth(), size.getHeight());
            return;
        }

        if (targetAspectRatio > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            // padding
            int horizontalPadding = getPaddingLeft() + getPaddingRight();
            int verticalPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizontalPadding;
            initialHeight -= verticalPadding;

            double viewAspectRatio = (double) initialWidth / initialHeight;
            double aspectDifference = targetAspectRatio / viewAspectRatio - 1;

            if (Math.abs(aspectDifference) < 0.01) {
                //no changes
            } else {
                if (aspectDifference > 0) {
                    initialHeight = (int) (initialWidth / targetAspectRatio);
                } else {
                    initialWidth = (int) (initialHeight * targetAspectRatio);
                }
                initialWidth += horizontalPadding;
                initialHeight += verticalPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (size != null && getChildAt(0) != null) {
            getChildAt(0).layout(0, 0, actualPreviewWidth, actualPreviewHeight);
        } else super.onLayout(changed, l, t, r, b);
    }

    public void setCustomSize(final Size size, Size previewSize) {
        if (targetAspectRatio <= 0) {
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                    AspectFrameLayout.this.size = size;

                    actualPreviewWidth = getMeasuredWidth();
                    actualPreviewHeight = getMeasuredHeight();

                    if (actualPreviewHeight < actualPreviewWidth)
                        AspectFrameLayout.this.size = new Size(actualPreviewHeight, actualPreviewHeight);
                    else
                        AspectFrameLayout.this.size = new Size(actualPreviewWidth, actualPreviewWidth);

                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.width = size.getWidth();
                    layoutParams.height = size.getHeight();

                    setLayoutParams(layoutParams);
                    requestLayout();
                }
            });
            setAspectRatio(previewSize.getHeight() / (double) previewSize.getWidth());
        }
    }

    public int getCroppSize() {
        return size.getHeight();
    }

    public void getCameraViewLocation(int[] location) {
        if (getChildAt(0) != null) {
            getChildAt(0).getLocationInWindow(location);
        } else getLocationInWindow(location);
    }
}
