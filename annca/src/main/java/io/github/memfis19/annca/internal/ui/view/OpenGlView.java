package io.github.memfis19.annca.internal.ui.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

/**
 * Created by memfis on 3/9/17.
 */

public class OpenGlView extends GLSurfaceView {

    public interface Callback {
        void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);

        void onSurfaceTextureChanged(SurfaceTexture surfaceTexture, int width, int height);
    }

    private OpenGlRenderer renderer;
    private Callback callback;

    public OpenGlView(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        renderer = new OpenGlRenderer(this, callback);
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public SurfaceTexture getSurfaceTexture() {
        return renderer.getSurfaceTexture();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void onPause() {
        renderer.onPause();

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        renderer.onResume();
    }
}
