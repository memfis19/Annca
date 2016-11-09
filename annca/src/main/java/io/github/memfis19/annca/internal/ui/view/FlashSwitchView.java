package io.github.memfis19.annca.internal.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import io.github.memfis19.annca.R;


/**
 * Created by memfis on 7/6/16.
 */
public class FlashSwitchView extends ImageButton {

    private FlashMode currentMode = FlashMode.FLASH_AUTO;
    private FlashModeSwitchListener switchListener;
    private Drawable flashOnDrawable;
    private Drawable flashOffDrawable;
    private Drawable flashAutoDrawable;

    private int tintColor = Color.WHITE;

    public enum FlashMode {
        FLASH_ON, FLASH_OFF, FLASH_AUTO
    }

    public interface FlashModeSwitchListener {
        void onFlashModeChanged(FlashMode mode);
    }

    public FlashSwitchView(@NonNull Context context) {
        this(context, null);
    }

    public FlashSwitchView(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        flashOnDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_on_white_24dp);
        flashOffDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_off_white_24dp);
        flashAutoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flash_auto_white_24dp);
        init();
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        setOnClickListener(new FlashButtonClickListener());
        setIcon();
    }

    private void setIcon() {
        if (FlashMode.FLASH_OFF == currentMode) {
            setImageDrawable(flashOffDrawable);
        } else if (FlashMode.FLASH_ON == currentMode) {
            setImageDrawable(flashOnDrawable);
        } else setImageDrawable(flashAutoDrawable);

    }

    private void setIconsTint(@ColorInt int tintColor) {
        this.tintColor = tintColor;
        flashOnDrawable.setColorFilter(tintColor, PorterDuff.Mode.MULTIPLY);
        flashOffDrawable.setColorFilter(tintColor, PorterDuff.Mode.MULTIPLY);
        flashAutoDrawable.setColorFilter(tintColor, PorterDuff.Mode.MULTIPLY);
    }

    public void setFlashMode(@NonNull FlashMode mode) {
        this.currentMode = mode;
        setIcon();
    }

    public FlashMode getCurrentFlasMode() {
        return currentMode;
    }

    public void setFlashSwitchListener(@NonNull FlashModeSwitchListener switchListener) {
        this.switchListener = switchListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (Build.VERSION.SDK_INT > 10) {
            if (enabled) {
                setAlpha(1f);
            } else {
                setAlpha(0.5f);
            }
        }
    }

    private class FlashButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (FlashMode.FLASH_AUTO == currentMode) {
                currentMode = FlashMode.FLASH_OFF;
            } else if (FlashMode.FLASH_OFF == currentMode) {
                currentMode = FlashMode.FLASH_ON;
            } else if (FlashMode.FLASH_ON == currentMode) {
                currentMode = FlashMode.FLASH_AUTO;
            }
            setIcon();
            if (switchListener != null) {
                switchListener.onFlashModeChanged(currentMode);
            }
        }
    }
}
