/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.backend.android.graphics;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.util.DisplayMetrics;
import org.flixelgdx.backend.android.FlixelAndroidWindow;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGlobalShaderPipeline;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedBatch;
import org.flixelgdx.graphics.FlixelUnsupportedRenderTarget;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Android graphics backend, rendering through OpenGL ES 3.0 on a GLSurfaceView.
 *
 * <p>This class implements {@link FlixelGraphicsManager} for the Android platform. It exposes
 * {@link #onContextLost()} and {@link #onContextRestored()} for the launcher to wire into the
 * runner's context listener. It is created by
 * {@link org.flixelgdx.backend.android.FlixelAndroidLauncher} and installed as
 * {@link org.flixelgdx.Flixel#graphics} before the game loop starts. All GL calls go through
 * {@code android.opengl.GLES30} and must remain on the GL thread.
 *
 * <p>GL initialization is deferred to the first {@link #beginFrame()} call, which happens on the
 * GL thread after the EGL context is ready. Until then, all queries return safe neutral values.
 *
 * <p>Render resolution (see {@link #setRenderResolution(int, int, boolean)}) works the same as
 * the desktop backend: the scene is drawn into a fixed-size framebuffer and letterboxed to the
 * window on each frame. The global post-processing shader chain is also supported.
 *
 * <p>KTX2 / Basis Universal compressed textures are transcoded to ASTC 4x4 on devices that expose
 * {@code GL_KHR_texture_compression_astc_ldr}, falling back to ETC2 RGBA8 (universally available
 * on ES 3.0), and then to uncompressed RGBA32 as a last resort.
 */
public class FlixelAndroidGraphics implements FlixelGraphicsManager {

  /**
   * Weight applied to each new frame-time sample when smoothing the frame rate. Keeping this
   * small prevents the fps readout from jittering between frames.
   */
  private static final double FPS_SMOOTHING = 0.1;

  /** GLES internal format constant for ASTC 4x4 RGBA (extension, not in GLES30 constants). */
  private static final int GL_COMPRESSED_RGBA_ASTC_4x4_KHR = 0x93B0;

  private long lastFrameNanos = -1L;

  private double averageFps;

  @NotNull
  private final Activity activity;

  @NotNull
  private final FlixelAndroidWindow window;

  @NotNull
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();

  @NotNull
  private final FlixelArray<FlixelGlesRenderTarget> targetStack = new FlixelArray<>();

  /** Actions queued from other threads by {@link #queueMainThread(Runnable)}. */
  private final FlixelArray<Runnable> mainThreadQueue = new FlixelArray<>();

  /** The batch of queued actions currently being run, swapped with the queue each frame. */
  private final FlixelArray<Runnable> runningActions = new FlixelArray<>();

  @NotNull
  private final FlixelMatrix compositeOrtho = new FlixelMatrix();

  @NotNull
  private final FlixelGlobalShaderPipeline globalPipeline = new FlixelGlobalShaderPipeline();

  // Render resolution: when enabled, the scene is drawn into sceneTarget then stretched to the
  // window. compositeScale/OffsetX/OffsetY describe the FIT letterbox used to place the surface.
  @Nullable
  private FlixelGlesRenderTarget sceneTarget;

  @Nullable
  private FlixelGlesBatch batch;

  private float compositeScale = 1f;
  private float compositeOffsetX;
  private float compositeOffsetY;

  private int renderWidth;
  private int renderHeight;

  private float density = 1f;
  private float ppi;

  // Last known viewport in Y-down world coordinates, used by fillViewOpaque to detect full-screen.
  private int viewportX;
  private int viewportY;
  private int viewportW;
  private int viewportH;

  private boolean initialized;
  private boolean astcSupported;
  private boolean renderResolutionEnabled;
  private boolean sceneActive;

  /**
   * Creates a graphics manager for the given activity and window. GL initialization is deferred
   * until {@link #beginFrame()} is called on the GL thread.
   *
   * @param activity The host activity used for {@link DisplayMetrics} queries.
   * @param window The Android window that exposes the back-buffer dimensions.
   */
  public FlixelAndroidGraphics(@NotNull Activity activity, @NotNull FlixelAndroidWindow window) {
    this.activity = activity;
    this.window = window;
  }

  @Override
  @NotNull
  public FlixelGraphicsApi getApi() {
    return FlixelGraphicsApi.OpenGLES;
  }

  @Override
  public void beginFrame() {
    if (!initialized) {
      initGL();
    }
    runQueuedActions();

    // EMA frame-rate tracking.
    long now = System.nanoTime();
    if (lastFrameNanos >= 0) {
      double delta = (now - lastFrameNanos) / 1_000_000_000.0;
      if (delta > 0.0) {
        double instantFps = 1.0 / delta;
        averageFps = averageFps * (1.0 - FPS_SMOOTHING) + instantFps * FPS_SMOOTHING;
      }
    }
    lastFrameNanos = now;
  }

  @Override
  public void endFrame() {}

  @Override
  @NotNull
  public FlixelBatch getBatch() {
    return batch != null ? batch : FlixelUnsupportedBatch.INSTANCE;
  }

  @Override
  public void queueMainThread(@NotNull Runnable action) {
    // Callers may be on the UI thread, an asset loader thread, or a host callback, none of which
    // own the GL context. Actions are queued and run at the start of the next frame on the GL
    // thread, the same as on desktop.
    synchronized (mainThreadQueue) {
      mainThreadQueue.add(action);
    }
  }

  @Override
  public void setRenderResolution(int width, int height, boolean smooth) {
    if (width < 1 || height < 1) {
      clearRenderResolution();
      return;
    }
    if (renderResolutionEnabled && width == renderWidth && height == renderHeight) {
      if (sceneTarget != null) {
        sceneTarget.getTexture().setSmooth(smooth);
      }
      return;
    }
    renderResolutionEnabled = true;
    renderWidth = width;
    renderHeight = height;
    if (sceneTarget != null) {
      sceneTarget.destroy();
      sceneTarget = null;
    }
    if (initialized) {
      sceneTarget = new FlixelGlesRenderTarget(this, width, height, smooth);
    }
    globalPipeline.resize(this);
  }

  @Override
  public void clearRenderResolution() {
    renderResolutionEnabled = false;
    if (sceneTarget != null) {
      sceneTarget.destroy();
      sceneTarget = null;
    }
    globalPipeline.resize(this);
  }

  @Override
  public boolean isRenderResolutionEnabled() {
    return renderResolutionEnabled;
  }

  @Override
  public int getRenderWidth() {
    return renderResolutionEnabled ? renderWidth : window.getBackBufferWidth();
  }

  @Override
  public int getRenderHeight() {
    return renderResolutionEnabled ? renderHeight : window.getBackBufferHeight();
  }

  @Override
  public void beginScene() {
    if (!renderResolutionEnabled || batch == null) {
      return;
    }
    if (sceneTarget == null) {
      sceneTarget = new FlixelGlesRenderTarget(this, renderWidth, renderHeight, true);
    }
    sceneActive = true;

    float ww = Math.max(1, window.getBackBufferWidth());
    float wh = Math.max(1, window.getBackBufferHeight());
    compositeScale = Math.min(ww / renderWidth, wh / renderHeight);
    compositeOffsetX = (ww - renderWidth * compositeScale) / 2f;
    compositeOffsetY = (wh - renderHeight * compositeScale) / 2f;

    sceneTarget.begin();
    GLES30.glClearColor(0f, 0f, 0f, 0f);
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public void endScene() {
    if (!sceneActive || batch == null || sceneTarget == null) {
      return;
    }
    sceneActive = false;
    sceneTarget.end();

    float dstX = compositeOffsetX;
    float dstY = compositeOffsetY;
    float dstW = renderWidth * compositeScale;
    float dstH = renderHeight * compositeScale;

    int backW = window.getBackBufferWidth();
    int backH = window.getBackBufferHeight();
    GLES30.glViewport(0, 0, backW, backH);

    compositeOrtho.setToOrtho2DYDown(0, 0, backW, backH, isDepthZeroToOne());
    batch.setProjection(compositeOrtho);
    batch.setBlendMode(FlixelBlendMode.NONE);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.begin();
    // The scene FBO is stored bottom-up (isFlipped() == true), so flip the vertical coords.
    batch.draw(sceneTarget.getTexture(), dstX, dstY, dstW, dstH, 0f, 1f, 1f, 0f);
    batch.end();
    batch.setBlendMode(FlixelBlendMode.NORMAL);
  }

  @Override
  public void beginCameraPass() {}

  @Override
  public void clear(float r, float g, float b, float a) {
    GLES30.glClearColor(r, g, b, a);
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
  }

  @Override
  public boolean fillViewOpaque(float r, float g, float b, float a) {
    // Clearing in GL covers the whole bound framebuffer, so we can only use it when the
    // camera exactly fills the current surface.
    int surfaceW = sceneActive ? renderWidth : window.getBackBufferWidth();
    int surfaceH = sceneActive ? renderHeight : window.getBackBufferHeight();
    if (viewportX > 0 || viewportY > 0 || viewportW < surfaceW || viewportH < surfaceH) {
      return false;
    }
    clear(r, g, b, a);
    return true;
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    int surfH = sceneActive ? renderHeight : window.getBackBufferHeight();
    int glY = surfH - y - height;
    GLES30.glEnable(GLES30.GL_SCISSOR_TEST);
    GLES30.glScissor(x, glY, Math.max(1, width), Math.max(1, height));
  }

  @Override
  public void clearScissor() {
    GLES30.glDisable(GLES30.GL_SCISSOR_TEST);
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    // The framework uses Y-down viewport coordinates; GL uses Y-up from the bottom.
    int surfH = sceneActive ? renderHeight : window.getBackBufferHeight();
    int glY = surfH - y - height;
    viewportX = x;
    viewportY = y;
    viewportW = width;
    viewportH = height;
    GLES30.glViewport(x, Math.max(0, glY), Math.max(1, width), Math.max(1, height));
  }

  @Override
  public int getBackBufferWidth() {
    return window.getBackBufferWidth();
  }

  @Override
  public int getBackBufferHeight() {
    return window.getBackBufferHeight();
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(int width, int height) {
    return new FlixelGlesTexture(width, height, false);
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return new FlixelGlesTexture(width, height, rgba, false);
  }

  @Override
  @Nullable
  public FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    // Copy to a byte array because BitmapFactory requires a byte[] or a seekable input stream.
    byte[] bytes = new byte[encoded.remaining()];
    encoded.duplicate().get(bytes);

    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
    opts.inPremultiplied = false;
    opts.inScaled = false;
    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    if (bmp == null) {
      return null;
    }

    int w = bmp.getWidth();
    int h = bmp.getHeight();
    ByteBuffer pixels = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
    bmp.copyPixelsToBuffer(pixels);
    bmp.recycle();
    pixels.rewind();
    return new FlixelImage(w, h, pixels);
  }

  @Override
  @Nullable
  public FlixelTexture createCompressedTexture(@NotNull ByteBuffer container) {
    // Ensure the container is a direct buffer so FlixelBasisu's JNI can read it.
    ByteBuffer direct;
    if (container.isDirect()) {
      direct = container;
    } else {
      direct = ByteBuffer.allocateDirect(container.remaining()).order(ByteOrder.nativeOrder());
      direct.put(container.duplicate()).rewind();
    }

    long handle = FlixelBasisu.open(direct);
    if (handle == 0L) {
      return null;
    }

    try {
      int levels = FlixelBasisu.getLevels(handle);
      int baseW = FlixelBasisu.getWidth(handle);
      int baseH = FlixelBasisu.getHeight(handle);
      if (levels <= 0 || baseW <= 0 || baseH <= 0) {
        return null;
      }

      // Choose the best GPU format this device supports.
      int fmt;
      int glFormat;
      boolean compressed;
      if (astcSupported) {
        fmt = FlixelBasisu.FMT_ASTC_4x4_RGBA;
        glFormat = GL_COMPRESSED_RGBA_ASTC_4x4_KHR;
        compressed = true;
      } else {
        fmt = FlixelBasisu.FMT_ETC2_RGBA;
        glFormat = GLES30.GL_COMPRESSED_RGBA8_ETC2_EAC;
        compressed = true;
      }

      ByteBuffer[] mips = new ByteBuffer[levels];
      for (int i = 0; i < levels; i++) {
        int needed = FlixelBasisu.getTranscodedSize(handle, i, fmt);
        if (needed <= 0) {
          // Fall back to RGBA32 for this texture.
          fmt = FlixelBasisu.FMT_RGBA32;
          glFormat = GLES30.GL_RGBA;
          compressed = false;
          needed = FlixelBasisu.getTranscodedSize(handle, i, fmt);
          if (needed <= 0) {
            return null;
          }
        }
        ByteBuffer mip = ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder());
        if (!FlixelBasisu.transcode(handle, i, fmt, mip)) {
          return null;
        }
        mips[i] = mip;
      }

      return new FlixelGlesTexture(baseW, baseH, levels, mips, glFormat, compressed);
    } finally {
      FlixelBasisu.close(handle);
    }
  }

  @Override
  @NotNull
  public FlixelRenderTarget createRenderTarget(int width, int height) {
    if (!initialized) {
      return FlixelUnsupportedRenderTarget.INSTANCE;
    }
    return new FlixelGlesRenderTarget(this, width, height, true);
  }

  @Override
  @NotNull
  public FlixelShaderProgram compileShaderSource(@NotNull String vertexSource,
      @NotNull String fragmentSource) {
    int program = FlixelGlesPrograms.buildCustom(vertexSource, fragmentSource);
    return program != 0 ? new FlixelGlesShaderProgram(program) : FlixelUnsupportedShader.INSTANCE;
  }

  @Override
  public int getFps() {
    return (int) averageFps;
  }

  @Override
  @NotNull
  public FlixelGlobalShaderPipeline getGlobalShaderPipeline() {
    return globalPipeline;
  }

  @Override
  @NotNull
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  @Override
  public float getDensity() {
    return density;
  }

  @Override
  public float getPpi() {
    return ppi;
  }

  /**
   * Handles EGL context loss; GPU resources become invalid and are cleared.
   *
   * <p>The batch, scene target, and built-in programs are marked for re-creation on the next
   * {@link #beginFrame()} call. Texture handles held by the asset system reference objects that
   * are now invalid; a full game restart through the launcher is expected after context loss.
   */
  public void onContextLost() {
    initialized = false;
    batch = null;
    if (sceneTarget != null) {
      sceneTarget = null;
    }
    targetStack.clear();
    globalPipeline.dispose();
  }

  /**
   * Handles EGL context restoration; re-initializes the GL state on the current thread.
   */
  public void onContextRestored() {
    initGL();
    if (renderResolutionEnabled) {
      sceneTarget = new FlixelGlesRenderTarget(this, renderWidth, renderHeight, true);
    }
    globalPipeline.resize(this);
  }

  /**
   * Pushes a render target onto the stack, making it the active drawing surface.
   *
   * @param target The target to activate.
   */
  void pushRenderTarget(@NotNull FlixelGlesRenderTarget target) {
    targetStack.add(target);
    bindTarget(target);
  }

  /**
   * Pops the topmost render target and restores the previous surface.
   */
  void popRenderTarget() {
    if (targetStack.getSize() > 0) {
      targetStack.pop();
    }
    restoreActiveFramebuffer();
  }

  /**
   * Binds whichever framebuffer is currently active (the top of the stack, or the window).
   */
  void restoreActiveFramebuffer() {
    if (targetStack.getSize() > 0) {
      bindTarget(targetStack.get(targetStack.getSize() - 1));
    } else {
      GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
      GLES30.glViewport(0, 0, window.getBackBufferWidth(), window.getBackBufferHeight());
    }
  }

  /** Binds a render target's framebuffer and sets the GL viewport to its size. */
  /**
   * Runs every action queued by {@link #queueMainThread(Runnable)} on the GL thread.
   *
   * <p>The queue is copied out under the lock and run afterwards, so an action may safely queue
   * another action (which then runs next frame) without deadlocking or mutating the list being run.
   */
  private void runQueuedActions() {
    synchronized (mainThreadQueue) {
      if (mainThreadQueue.isEmpty()) {
        return;
      }
      for (int i = 0; i < mainThreadQueue.getSize(); i++) {
        runningActions.add(mainThreadQueue.get(i));
      }
      mainThreadQueue.clear();
    }
    for (int i = 0; i < runningActions.getSize(); i++) {
      runningActions.get(i).run();
    }
    runningActions.clear();
  }

  private void bindTarget(@NotNull FlixelGlesRenderTarget target) {
    GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, target.getFramebuffer());
    GLES30.glViewport(0, 0, target.getWidth(), target.getHeight());
  }

  /** Performs one-time GL initialization on the GL thread. */
  private void initGL() {
    // Query the maximum simultaneous texture image units; cap at 16 to stay practical.
    int[] maxUnits = new int[1];
    GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_IMAGE_UNITS, maxUnits, 0);
    int maxSlots = Math.min(maxUnits[0], 16);
    if (maxSlots < 1) {
      maxSlots = 1;
    }

    // Detect ASTC support.
    String extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS);
    astcSupported = extensions != null
        && extensions.contains("GL_KHR_texture_compression_astc_ldr");

    // Read display density from the resources; this does not require a Display reference.
    DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
    density = metrics.density;
    ppi = metrics.xdpi;

    GLES30.glDisable(GLES30.GL_DEPTH_TEST);
    GLES30.glEnable(GLES30.GL_BLEND);
    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

    batch = new FlixelGlesBatch(maxSlots);
    initialized = true;
  }
}
