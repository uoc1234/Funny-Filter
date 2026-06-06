package com.otaliastudios.cameraview.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.internal.GlTextureDrawer;
import com.otaliastudios.cameraview.internal.Issue514Workaround;
import com.otaliastudios.cameraview.size.Size;

import java.nio.Buffer;


/**
 * Draws overlays through {@link Overlay}.
 *
 * - Provides a {@link Canvas} to be passed to the Overlay
 * - Lets the overlay draw there: {@link #draw(Overlay.Target)}
 * - Renders this into the current EGL window: {@link #render(long)}
 * - Applies the {@link Issue514Workaround} the correct way
 *
 * In the future we might want to use a different approach than {@link GlTextureDrawer},
 * {@link SurfaceTexture} and {@link GLES11Ext#GL_TEXTURE_EXTERNAL_OES},
 * for example by using a regular {@link GLES20#GL_TEXTURE_2D} that might
 * be filled through {@link GLES20#glTexImage2D(int, int, int, int, int, int, int, int, Buffer)}.
 *
 * The current approach has some issues, for example see {@link Issue514Workaround}.
 */
public class OverlayDrawer {

    private static final String TAG = OverlayDrawer.class.getSimpleName();
    private static final CameraLogger LOG = CameraLogger.create(TAG);

    private Overlay mOverlay;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    @VisibleForTesting GlTextureDrawer mTextureDrawer;
    private Issue514Workaround mIssue514Workaround;
    private final Object mIssue514WorkaroundLock = new Object();

    public OverlayDrawer(@NonNull Overlay overlay, @NonNull Size size) {
        mOverlay = overlay;
        mTextureDrawer = new GlTextureDrawer();
        mSurfaceTexture = new SurfaceTexture(mTextureDrawer.getTexture().getId());
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mSurface = new Surface(mSurfaceTexture);
        mIssue514Workaround = new Issue514Workaround(mTextureDrawer.getTexture().getId());
    }

    /**
     * Should be called to draw the {@link Overlay} on the given {@link Overlay.Target}.
     * This will provide a working {@link Canvas} to the overlay and also update the
     * drawn contents to a GLES texture.
     * @param target the target
     */
    // Thêm throttling để giảm frame drops
    private long mLastDrawTime = 0;
    private static final long MIN_DRAW_INTERVAL = 50; // ~20 FPS (1000ms/20) - giảm từ 30 xuống 20
    private int mSkippedFrames = 0;
    private static final int MAX_CONSECUTIVE_SKIPS = 3; // Tối đa skip 3 frame liên tiếp

    public void draw(@NonNull Overlay.Target target) {
        // Throttle drawing để tránh overdraw và giảm CPU usage
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastDrawTime < MIN_DRAW_INTERVAL) {
            mSkippedFrames++;
            // Nếu skip quá nhiều frame, force render để tránh freeze
            if (mSkippedFrames < MAX_CONSECUTIVE_SKIPS) {
                return; // Skip this frame để maintain stable framerate
            }
        }
        mLastDrawTime = currentTime;
        mSkippedFrames = 0;
        
        try {
            final Canvas surfaceCanvas;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mOverlay.getHardwareCanvasEnabled()) {
                surfaceCanvas = mSurface.lockHardwareCanvas();
            } else {
                surfaceCanvas = mSurface.lockCanvas(null);
            }
            
            if (surfaceCanvas == null) {
                LOG.w("Unable to lock canvas, skipping frame");
                return;
            }
            
            surfaceCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            try {
                mOverlay.drawOn(target, surfaceCanvas);
            } catch (IndexOutOfBoundsException e) {
                LOG.w("Got IndexOutOfBoundsException while drawing overlay, skipping frame", e);
                // Skip this frame instead of crashing
            } catch (Exception e) {
                LOG.w("Got unexpected exception while drawing overlay", e);
                // Continue execution instead of crashing
            }
            mSurface.unlockCanvasAndPost(surfaceCanvas);
        } catch (Surface.OutOfResourcesException e) {
            LOG.w("Got Surface.OutOfResourcesException while drawing video overlays", e);
            return; // Skip texture update if surface failed
        } catch (Exception e) {
            LOG.w("Got unexpected exception while setting up overlay canvas", e);
            return; // Skip texture update if setup failed
        }
        
        synchronized (mIssue514WorkaroundLock) {
            mIssue514Workaround.beforeOverlayUpdateTexImage();
            try {
                mSurfaceTexture.updateTexImage();
            } catch (IllegalStateException e) {
                LOG.w("Got IllegalStateException while updating texture contents", e);
            } catch (Exception e) {
                LOG.w("Got unexpected exception while updating texture", e);
            }
        }
        mSurfaceTexture.getTransformMatrix(mTextureDrawer.getTextureTransform());
    }

    /**
     * Returns the transform that should be used to render the drawn content.
     * This should be called after {@link #draw(Overlay.Target)} and can be modified.
     * @return the transform matrix
     */
    public float[] getTransform() {
        return mTextureDrawer.getTextureTransform();
    }

    /**
     * Renders the drawn content in the current EGL surface, assuming there is one.
     * Should be called after {@link #draw(Overlay.Target)} and any {@link #getTransform()}
     * modification.
     *
     * @param timestampUs frame timestamp
     */
    public void render(long timestampUs) {
        // Enable blending
        // Reference http://www.learnopengles.com/android-lesson-five-an-introduction-to-blending/
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        synchronized (mIssue514WorkaroundLock) {
            mTextureDrawer.draw(timestampUs);
        }
    }

    /**
     * Releases resources.
     */
    public void release() {
        if (mIssue514Workaround != null) {
            mIssue514Workaround.end();
            mIssue514Workaround = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mTextureDrawer != null) {
            mTextureDrawer.release();
            mTextureDrawer = null;
        }
    }
}
