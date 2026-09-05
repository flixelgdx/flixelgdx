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
package org.flixelgdx.backend.desktop.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXStats;
import org.lwjgl.bgfx.BGFXTextureInfo;
import org.lwjgl.bgfx.BGFXTransientVertexBuffer;
import org.lwjgl.bgfx.BGFXVertexLayout;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

/**
 * The desktop graphics backend, built on bgfx.
 *
 * <p>This is the single implementation of {@link FlixelGraphicsManager} on desktop. It owns the
 * bgfx device lifecycle, the shared sprite batch, the vertex layout every quad uses, and the sprite
 * shader program. Drawing goes through bgfx's numbered views: view {@code 0} targets the screen,
 * and render targets take higher-numbered views bound to their framebuffers, tracked on a small
 * stack so per-camera and whole-scene targets nest correctly.
 *
 * <p>bgfx cannot draw without a compiled shader program. The sprite shaders are precompiled per
 * renderer backend and bundled under {@code org/flixelgdx/shaders}; build them with
 * {@code scripts/build_shaders.sh}. If the program is missing at startup, drawing degrades to a
 * no-op with a one-time warning so the game still runs (useful headless), while everything else
 * (textures, render targets, clears) works normally.
 */
public class FlixelBgfxGraphics implements FlixelGraphicsManager {

  public double smoothingFactor = 0.1;

  private long lastFrameTime = System.nanoTime();

  /** Wall-clock nanoseconds accumulated since the last stats log line, when stats logging is enabled. */
  private long statsWindowNanos;

  /** Timestamp of the previous {@code endFrame}, used to measure true end-to-end frame periods. */
  private long statsLastNanos;

  private double averageFps = 0;

  /** bgfx view id used to clear the whole back buffer at the start of a frame. */
  private static final int SCREEN_CLEAR_VIEW = 0;

  /** First view id handed out to on-screen camera passes (view 0 is reserved for the clear). */
  private static final int FIRST_SCREEN_VIEW = 1;

  /**
   * Number of view ids reserved for on-screen camera passes. bgfx renders views in ascending id
   * order, so keeping camera views below the render-target range makes each camera draw over the
   * clear and lets render targets share the frame without id collisions.
   */
  private static final int MAX_SCREEN_VIEWS = 64;

  /** First view id handed out to render targets, above the reserved on-screen range. */
  private static final int FIRST_TARGET_VIEW = MAX_SCREEN_VIEWS;

  @NotNull
  private final FlixelBgfxBatch batch = new FlixelBgfxBatch(this);

  @NotNull
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();

  /** Tasks queued from background threads, drained on the render thread at the start of each frame. */
  @NotNull
  private final FlixelArray<Runnable> mainThreadQueue = new FlixelArray<>();

  @NotNull
  private FlixelGraphicsApi api = FlixelGraphicsApi.OpenGL;

  /** Stack of active view ids; the top is where the batch currently submits. */
  @NotNull
  private final int[] viewStack = new int[16];

  /**
   * Framebuffer bound at each {@link #viewStack} level ({@code -1} is the back buffer), plus that
   * level's pixel size. When a render target (for example a global shader FBO) wraps the whole
   * camera loop, {@link #beginCameraPass()} reads these so every camera can still get its own view
   * into the same framebuffer instead of sharing one, which is what keeps per-camera zoom and scroll
   * from overwriting each other.
   */
  @NotNull
  private final short[] viewStackFrameBuffer = new short[16];

  @NotNull
  private final int[] viewStackWidth = new int[16];

  @NotNull
  private final int[] viewStackHeight = new int[16];

  @NotNull
  private final BGFXVertexLayout vertexLayout = BGFXVertexLayout.create();

  /**
   * Reused transient vertex buffer descriptor. bgfx only fills this in with a fresh allocation each
   * flush, so one instance is reused for every submission instead of allocating a new struct.
   */
  @NotNull
  private final BGFXTransientVertexBuffer transientVertices = BGFXTransientVertexBuffer.create();

  /** Scratch matrix for {@code projection * transform}, reused each flush to avoid allocation. */
  @NotNull
  private final FlixelMatrix combinedMatrix = new FlixelMatrix();

  /** Reused column-major buffer handed to {@code bgfx_set_view_transform} each flush. */
  @NotNull
  private final FloatBuffer viewTransform = BufferUtils.createFloatBuffer(16);

  /** Fixed-resolution scene surface the whole frame renders into when a render resolution is set. */
  @Nullable
  private FlixelBgfxRenderTarget sceneTarget;

  /**
   * 1x1 white texture used by {@link #forceOpaqueAlpha()} to write alpha=1 to the back buffer
   * without changing RGB, keeping sprites visible when the transparent framebuffer is enabled but
   * transparency is currently off.
   */
  @Nullable
  private FlixelBgfxTexture opaqueAlphaTexture;

  /**
   * Cached wrapper around bgfx's internal per-frame stats struct.
   *
   * <p>{@code bgfx_get_stats()} always returns a pointer to the same struct, which bgfx updates in
   * place every frame; only the Java wrapper LWJGL builds around that pointer is new each call. The
   * wrapper is therefore created once (see {@link #getBgfxStats()}) and reused, so reading stats
   * every frame for the debug overlay never allocates.
   */
  @Nullable
  private BGFXStats bgfxStats;

  /** Reused ortho matrix for the final upscale blit, rebuilt each composite to match the window. */
  @NotNull
  private final FlixelMatrix compositeOrtho = new FlixelMatrix();

  private short vertexLayoutHandle;
  private short quadIndexBuffer = -1;
  private short spriteProgram = -1;
  private short textureUniform = -1;

  private int backBufferWidth;
  private int backBufferHeight;
  private int renderWidth;
  private int renderHeight;
  private float compositeScale = 1f;
  private float compositeOffsetX;
  private float compositeOffsetY;
  private int viewportX;
  private int viewportY;
  private int viewportWidth;
  private int viewportHeight;
  private int scissorX;
  private int scissorY;
  private int scissorWidth = -1;
  private int scissorHeight = -1;
  private int nextTargetView = FIRST_TARGET_VIEW;
  private int nextScreenView = FIRST_SCREEN_VIEW;
  private int viewStackDepth;

  private int clearColor;

  /** Frames counted since the last stats log line, when stats logging is enabled. */
  private int statsWindowFrames;

  /** Frame-rate cap in fps, or {@code 0} when uncapped. Read by the runner each frame. */
  private volatile int targetFps;

  /**
   * Whether the window was opened with an alpha-capable framebuffer. When {@code true},
   * {@link #onResize(int, int)} passes {@code BGFX_RESET_TRANSPARENT_BACKBUFFER} so the OS
   * compositor receives the back-buffer alpha channel and can composite the window against the
   * desktop.
   */
  private boolean transparentFramebuffer;

  private boolean vsync = true;
  private boolean programWarned;

  /** Whether a fixed render resolution is active. See {@link #setRenderResolution(int, int, boolean)}. */
  private boolean renderResolutionEnabled;

  /** Whether the scene surface is stretched with linear filtering ({@code true}) or nearest-neighbor. */
  private boolean renderSmooth = true;

  /** True between {@link #beginScene()} and {@link #endScene()}, so viewport remapping is active. */
  private boolean sceneActive;

  /** Set when the render size or filter changed, so the scene surface is rebuilt on the next frame. */
  private boolean sceneTargetDirty;

  /** When {@code true}, per-frame bgfx CPU/GPU timing is logged once per second. Set by {@code flixel.render.stats}. */
  private boolean statsEnabled;

  /** When {@code true}, bgfx's on-screen debug stats overlay is also shown. Set by {@code flixel.render.stats=overlay}. */
  private boolean statsOverlay;

  /**
   * Sets up the vertex layout, sprite program, and texture uniform after bgfx has been
   * initialized by the runner. Called once at startup.
   *
   * @param width The initial back buffer width in pixels.
   * @param height The initial back buffer height in pixels.
   * @param transparentFramebuffer Whether the window was opened with an alpha-capable framebuffer.
   *     When {@code true}, {@link #onResize(int, int)} passes
   *     {@code BGFX_RESET_TRANSPARENT_BACKBUFFER} on every swap-chain reset so the OS compositor
   *     receives the back-buffer alpha channel.
   */
  public void onInitialized(int width, int height, boolean transparentFramebuffer) {
    this.transparentFramebuffer = transparentFramebuffer;
    this.backBufferWidth = width;
    this.backBufferHeight = height;
    this.viewportWidth = width;
    this.viewportHeight = height;
    this.viewStack[0] = SCREEN_CLEAR_VIEW;
    this.viewStackFrameBuffer[0] = -1;
    this.viewStackDepth = 1;

    this.api = apiFromRenderer(BGFX.bgfx_get_renderer_type());
    batch.setBgra(isDepthZeroToOne());
    FlixelBgfxTexture.swapRB = isDepthZeroToOne();
    BGFX.bgfx_vertex_layout_begin(vertexLayout, BGFX.bgfx_get_renderer_type());
    BGFX.bgfx_vertex_layout_add(vertexLayout, BGFX.BGFX_ATTRIB_POSITION, 2, BGFX.BGFX_ATTRIB_TYPE_FLOAT, false, false);
    BGFX.bgfx_vertex_layout_add(vertexLayout, BGFX.BGFX_ATTRIB_TEXCOORD0, 2, BGFX.BGFX_ATTRIB_TYPE_FLOAT, false, false);
    BGFX.bgfx_vertex_layout_add(vertexLayout, BGFX.BGFX_ATTRIB_COLOR0, 4, BGFX.BGFX_ATTRIB_TYPE_UINT8, true, false);
    BGFX.bgfx_vertex_layout_end(vertexLayout);
    vertexLayoutHandle = BGFX.bgfx_create_vertex_layout(vertexLayout);
    quadIndexBuffer = createQuadIndexBuffer();

    textureUniform = BGFX.bgfx_create_uniform("s_texture", BGFX.BGFX_UNIFORM_TYPE_SAMPLER, 1);
    spriteProgram = loadSpriteProgram();
    opaqueAlphaTexture = createOpaqueAlphaTexture();

    configureStats();
  }

  /**
   * Reads the {@code flixel.render.stats} system property and turns on per-frame bgfx timing when
   * requested. This is a diagnostic aid for telling CPU-bound submission apart from GPU-bound
   * fillrate: it logs one line per second with frame time, CPU submit time, GPU time, draw calls,
   * and transient buffer usage.
   *
   * <p>Recognized values are {@code true} or {@code log} (log once per second), {@code overlay}
   * (also show bgfx's built-in on-screen stats), and anything falsy (off, the default). Enable it
   * from the command line, for example:
   *
   * <pre>{@code
   * java -Dflixel.render.stats=log -jar mygame.jar
   * }</pre>
   */
  private void configureStats() {
    String value = System.getProperty("flixel.render.stats", "").trim().toLowerCase();
    statsEnabled = value.equals("true") || value.equals("log") || value.equals("overlay");
    statsOverlay = value.equals("overlay");
    if (statsOverlay) {
      BGFX.bgfx_set_debug(BGFX.BGFX_DEBUG_STATS);
    }
    if (statsEnabled) {
      Flixel.debug("Graphics", "bgfx stats logging is on (flixel.render.stats="
          + value + "). CPU submit vs GPU time is logged once per second.");
    }
  }

  /**
   * Updates cached back buffer size and resets the bgfx swap chain. Called by the runner on resize.
   *
   * @param width The new back buffer width in pixels.
   * @param height The new back buffer height in pixels.
   */
  public void onResize(int width, int height) {
    backBufferWidth = width;
    backBufferHeight = height;
    int flags = vsync ? BGFX.BGFX_RESET_VSYNC : BGFX.BGFX_RESET_NONE;
    if (transparentFramebuffer) {
      flags |= BGFX.BGFX_RESET_TRANSPARENT_BACKBUFFER;
    }
    BGFX.bgfx_reset(width, height, flags, BGFX.BGFX_TEXTURE_FORMAT_RGBA8);
  }

  @NotNull
  @Override
  public FlixelGraphicsApi getApi() {
    return api;
  }

  @Override
  public boolean isDepthZeroToOne() {
    // OpenGL and OpenGL ES map NDC depth to [-1, 1]; every other bgfx backend uses [0, 1].
    return api != FlixelGraphicsApi.OpenGL && api != FlixelGraphicsApi.OpenGLES;
  }

  @NotNull
  @Override
  public FlixelBatch getBatch() {
    return batch;
  }

  @Override
  public void queueMainThread(@Nullable Runnable action) {
    if (action == null) {
      return;
    }
    synchronized (mainThreadQueue) {
      mainThreadQueue.add(action);
    }
  }

  @Override
  public void beginFrame() {
    synchronized (mainThreadQueue) {
      for (int i = 0; i < mainThreadQueue.getSize(); i++) {
        mainThreadQueue.get(i).run();
      }
      mainThreadQueue.clear();
    }
    nextTargetView = FIRST_TARGET_VIEW;
    nextScreenView = FIRST_SCREEN_VIEW;
    viewStackDepth = 1;
    viewStack[0] = SCREEN_CLEAR_VIEW;
    // The clear view covers the whole back buffer and renders first; camera passes draw over it.
    BGFX.bgfx_set_view_mode(SCREEN_CLEAR_VIEW, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_set_view_rect(SCREEN_CLEAR_VIEW, 0, 0, Math.max(1, backBufferWidth), Math.max(1, backBufferHeight));
    BGFX.bgfx_touch(SCREEN_CLEAR_VIEW);

    long currentTime = System.nanoTime();
    long deltaTime = currentTime - lastFrameTime;
    lastFrameTime = currentTime;

    double instantFps = 1_000_000_000.0 / deltaTime;
    averageFps = (averageFps * (1 - smoothingFactor)) + (instantFps * smoothingFactor);
  }

  @Override
  public void endFrame() {
    // Advance bgfx to the next frame; 0 means "do not capture this frame".
    BGFX.bgfx_frame(0);
    if (statsEnabled) {
      logStats();
    }
  }

  /**
   * Accumulates one frame of bgfx timing and logs a summary line roughly once per second.
   *
   * <p>The key signal is CPU submit time versus GPU time. When GPU time dominates, the frame is
   * fillrate or GPU bound and CPU-side batching changes will not help; when CPU submit time
   * dominates, the render thread is the bottleneck and reducing draw calls or per-flush work pays
   * off. {@code waitSubmit} and {@code waitRender} reveal stalls where one thread waits on the other.
   */
  private void logStats() {
    long now = System.nanoTime();
    if (statsLastNanos != 0L) {
      statsWindowNanos += now - statsLastNanos;
    }
    statsLastNanos = now;
    statsWindowFrames++;
    if (statsWindowNanos < 1_000_000_000L) {
      return;
    }

    BGFXStats stats = getBgfxStats();
    double windowFps = statsWindowFrames * 1_000_000_000.0 / statsWindowNanos;
    statsWindowNanos = 0L;
    statsWindowFrames = 0;
    if (stats == null) {
      return;
    }

    // bgfx reports CPU and GPU times in separate timer units, so each converts to milliseconds
    // through its own frequency. A GPU frequency of zero means the backend cannot time the GPU.
    double cpuToMs = 1000.0 / stats.cpuTimerFreq();
    double gpuToMs = stats.gpuTimerFreq() > 0 ? 1000.0 / stats.gpuTimerFreq() : 0.0;
    double frameMs = stats.cpuTimeFrame() * cpuToMs;
    double cpuMs = (stats.cpuTimeEnd() - stats.cpuTimeBegin()) * cpuToMs;
    double gpuMs = (stats.gpuTimeEnd() - stats.gpuTimeBegin()) * gpuToMs;
    double waitSubmitMs = stats.waitSubmit() * cpuToMs;
    double waitRenderMs = stats.waitRender() * cpuToMs;
    long gpuMemMb = stats.gpuMemoryUsed() < 0 ? -1 : stats.gpuMemoryUsed() / (1024 * 1024);

    Flixel.debug("Graphics", String.format(
        "stats | fps=%.0f frame=%.2fms | cpuSubmit=%.2fms gpu=%.2fms | "
            + "waitSubmit=%.2fms waitRender=%.2fms | draws=%d peak=%d views=%d | "
            + "tvb=%dKB tib=%dKB | gpuMem=%s",
        windowFps, frameMs, cpuMs, gpuMs, waitSubmitMs, waitRenderMs,
        stats.numDraw(), stats.numDrawCallsPeak(), stats.numViews(),
        stats.transientVbUsed() / 1024, stats.transientIbUsed() / 1024,
        gpuMemMb < 0 ? "n/a" : gpuMemMb + "MB"));
  }

  @Override
  public void beginCameraPass() {
    // Each camera draws into its own bgfx view so its projection does not leak into the others.
    // bgfx applies the view transform per view for the whole frame, so sharing one view would let
    // the last camera's zoom and scroll overwrite every other camera's.
    //
    // This holds even when a render target already wraps the camera loop (for example a global
    // shader FBO): each camera still gets a fresh view, bound to that target's framebuffer, so the
    // per-camera transforms survive instead of collapsing onto the last camera's.
    int level = viewStackDepth - 1;
    int view;
    if (level == 0) {
      view = nextScreenView;
      if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
        nextScreenView++;
      }
      // On screen, draw into the render-resolution scene surface when one is active, else the back buffer.
      BGFX.bgfx_set_view_frame_buffer(view, sceneActive ? sceneFrameBuffer() : (short) -1);
    } else {
      view = nextTargetView++;
      BGFX.bgfx_set_view_frame_buffer(view, viewStackFrameBuffer[level]);
      BGFX.bgfx_set_view_rect(view, 0, 0, viewStackWidth[level], viewStackHeight[level]);
    }
    viewStack[level] = view;
    BGFX.bgfx_set_view_clear(view, BGFX.BGFX_CLEAR_NONE, 0, 1f, 0);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_touch(view);
  }

  @NotNull
  @Override
  public FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    ByteBuffer copy = ByteBuffer.allocateDirect(width * height * 4);
    rgba.position(0).limit(width * height * 4);
    copy.put(rgba).flip();
    return new FlixelBgfxTexture(new FlixelImage(width, height, copy));
  }

  @NotNull
  @Override
  public FlixelTexture createTexture(@NotNull FlixelImage image) {
    return new FlixelBgfxTexture(image);
  }

  @Nullable
  @Override
  public FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    return FlixelStbImage.decode(encoded);
  }

  @Nullable
  @Override
  public FlixelTexture createCompressedTexture(@NotNull ByteBuffer container) {
    // bgfx parses the container (KTX2, KTX, DDS, PVR) itself and keeps the compressed data on the
    // GPU. bgfx_copy takes ownership of a copy, so the caller's buffer can be released afterward.
    ByteBuffer src = container.duplicate();
    int size = src.remaining();
    if (size == 0) {
      return null;
    }
    ByteBuffer copy = ByteBuffer.allocateDirect(size);
    copy.put(src).flip();
    try (MemoryStack stack = MemoryStack.stackPush()) {
      BGFXTextureInfo info = BGFXTextureInfo.malloc(stack);
      short handle = BGFX.bgfx_create_texture(Objects.requireNonNull(BGFX.bgfx_copy(copy)),
          BGFX.BGFX_TEXTURE_NONE, 0, info);
      if (handle == -1) {
        return null;
      }
      return new FlixelBgfxTexture(handle, info.width(), info.height());
    }
  }

  @NotNull
  @Override
  public FlixelRenderTarget createRenderTarget(int width, int height) {
    return new FlixelBgfxRenderTarget(this, width, height);
  }

  @Override
  public void setRenderResolution(int width, int height, boolean smooth) {
    if (width < 1 || height < 1) {
      clearRenderResolution();
      return;
    }
    if (renderResolutionEnabled && width == renderWidth && height == renderHeight) {
      // Same size: a filter-only change can be applied to the existing surface without rebuilding it.
      if (smooth != renderSmooth) {
        renderSmooth = smooth;
        if (sceneTarget != null) {
          sceneTarget.getTexture().setSmooth(smooth);
        }
      }
      return;
    }
    renderWidth = width;
    renderHeight = height;
    renderSmooth = smooth;
    renderResolutionEnabled = true;
    // The surface is built lazily on the next frame, so this is safe to call before bgfx is ready
    // (for example from a game's constructor).
    sceneTargetDirty = true;
  }

  @Override
  public void clearRenderResolution() {
    renderResolutionEnabled = false;
    sceneActive = false;
    disposeSceneTarget();
  }

  @Override
  public boolean isRenderResolutionEnabled() {
    return renderResolutionEnabled;
  }

  @Override
  public int getRenderWidth() {
    return renderResolutionEnabled ? renderWidth : backBufferWidth;
  }

  @Override
  public int getRenderHeight() {
    return renderResolutionEnabled ? renderHeight : backBufferHeight;
  }

  @Override
  public void beginScene() {
    if (!renderResolutionEnabled) {
      return;
    }
    ensureSceneTarget();
    if (sceneTarget == null) {
      return;
    }
    sceneActive = true;
    // Work out how the fixed surface is stretched onto the current window (a FIT letterbox). Both the
    // per-camera viewport remap and the final blit below derive from this.
    float ww = Math.max(1, backBufferWidth);
    float wh = Math.max(1, backBufferHeight);
    compositeScale = Math.min(ww / renderWidth, wh / renderHeight);
    compositeOffsetX = (ww - renderWidth * compositeScale) / 2f;
    compositeOffsetY = (wh - renderHeight * compositeScale) / 2f;
    // Clear the whole surface once before any camera draws into it. A dedicated low-id view bound to
    // the scene framebuffer runs first because bgfx renders views in ascending id order.
    int clearView = nextScreenView;
    if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
      nextScreenView++;
    }
    BGFX.bgfx_set_view_frame_buffer(clearView, sceneFrameBuffer());
    BGFX.bgfx_set_view_mode(clearView, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_set_view_rect(clearView, 0, 0, renderWidth, renderHeight);
    BGFX.bgfx_set_view_clear(clearView, BGFX.BGFX_CLEAR_COLOR, 0, 1f, 0);
    BGFX.bgfx_touch(clearView);
  }

  @Override
  public void endScene() {
    if (!sceneActive) {
      return;
    }
    sceneActive = false;
    if (sceneTarget == null || spriteProgram == -1) {
      return;
    }
    // A fresh view above every camera view (so bgfx runs it last) draws the finished surface to the
    // back buffer, stretched into the letterboxed destination rectangle.
    int view = nextScreenView;
    if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
      nextScreenView++;
    }
    viewStack[0] = view;
    viewStackDepth = 1;
    BGFX.bgfx_set_view_frame_buffer(view, (short) -1);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);

    float dstX = compositeOffsetX;
    float dstY = compositeOffsetY;
    float dstW = renderWidth * compositeScale;
    float dstH = renderHeight * compositeScale;

    clearScissor();
    viewportX = 0;
    viewportY = 0;
    viewportWidth = Math.max(1, backBufferWidth);
    viewportHeight = Math.max(1, backBufferHeight);
    // Y-down composite ortho so the upscale blit matches the batch's Y-down vertex layout.
    compositeOrtho.setToOrtho2DYDown(0, 0, backBufferWidth, backBufferHeight, isDepthZeroToOne());

    FlixelTexture texture = sceneTarget.getTexture();
    batch.setProjection(compositeOrtho);
    batch.setBlendMode(FlixelBlendMode.NONE);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.begin();
    if (sceneTarget.isFlipped()) {
      // Some backends store render targets bottom-up, so flip the vertical texture coordinates.
      batch.draw(texture, dstX, dstY, dstW, dstH, 0f, 1f, 1f, 0f);
    } else {
      batch.draw(texture, dstX, dstY, dstW, dstH);
    }
    batch.end();
    batch.setBlendMode(FlixelBlendMode.NORMAL);
  }

  @Override
  public void clear(float r, float g, float b, float a) {
    clearColor = ((int) (r * 255f) << 24) | ((int) (g * 255f) << 16) | ((int) (b * 255f) << 8) | (int) (a * 255f);
    BGFX.bgfx_set_view_clear(currentView(), BGFX.BGFX_CLEAR_COLOR, clearColor, 1f, 0);
  }

  @Override
  public boolean fillViewOpaque(float r, float g, float b, float a) {
    // A bgfx view clear covers the whole view rectangle, so only skip the background quad when this
    // camera fills the entire surface. Sub-region and split-screen cameras keep drawing a quad.
    int surfaceWidth = sceneActive ? renderWidth : backBufferWidth;
    int surfaceHeight = sceneActive ? renderHeight : backBufferHeight;
    if (viewportX > 0 || viewportY > 0 || viewportWidth < surfaceWidth || viewportHeight < surfaceHeight) {
      return false;
    }
    clear(r, g, b, a);
    return true;
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    if (sceneActive) {
      // Clip rects arrive in window pixels; remap them into the render surface the same way viewports
      // are remapped, then flip to bgfx's top-left origin using the render height, not the window's.
      int rx = Math.round((x - compositeOffsetX) / compositeScale);
      int ry = Math.round((y - compositeOffsetY) / compositeScale);
      scissorX = rx;
      scissorWidth = Math.max(1, Math.round(width / compositeScale));
      scissorHeight = Math.max(1, Math.round(height / compositeScale));
      scissorY = renderHeight - ry - scissorHeight;
    } else {
      scissorX = x;
      scissorWidth = Math.max(1, width);
      scissorHeight = Math.max(1, height);
      scissorY = backBufferHeight - y - scissorHeight;
    }
  }

  @Override
  public void clearScissor() {
    scissorWidth = -1;
    scissorHeight = -1;
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    if (sceneActive) {
      // Cameras lay out their viewport in window pixels, but the scene surface is a different (fixed)
      // size, so undo the composite stretch to land in render pixels. compositeOffset/Scale describe
      // how the surface is placed on the window, so their inverse maps window space back to it.
      viewportX = Math.round((x - compositeOffsetX) / compositeScale);
      viewportY = Math.round((y - compositeOffsetY) / compositeScale);
      viewportWidth = Math.max(1, Math.round(width / compositeScale));
      viewportHeight = Math.max(1, Math.round(height / compositeScale));
    } else {
      viewportX = x;
      viewportY = y;
      viewportWidth = Math.max(1, width);
      viewportHeight = Math.max(1, height);
    }
    BGFX.bgfx_set_view_rect(currentView(), viewportX, viewportY, viewportWidth, viewportHeight);
  }

  @Override
  public int getBackBufferWidth() {
    return backBufferWidth;
  }

  @Override
  public int getBackBufferHeight() {
    return backBufferHeight;
  }

  @NotNull
  @Override
  public FlixelShaderProgram compileShaderProgram(byte @NotNull [] vertex, byte @NotNull [] fragment) {
    if (vertex.length == 0 || fragment.length == 0) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    short vsh = createShaderFromBytes(vertex);
    short fsh = createShaderFromBytes(fragment);
    if (vsh == -1 || fsh == -1) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    return new FlixelBgfxShader(BGFX.bgfx_create_program(vsh, fsh, true));
  }

  @NotNull
  @Override
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  @Override
  public boolean isVSyncEnabled() {
    return vsync;
  }

  @Override
  public void setVSync(boolean enabled) {
    if (vsync != enabled) {
      vsync = enabled;
      onResize(backBufferWidth, backBufferHeight);
    }
  }

  @Override
  public int getFps() {
    return (int) averageFps;
  }

  @Override
  public int getTargetFps() {
    return targetFps;
  }

  @Override
  public void setTargetFps(int fps) {
    targetFps = Math.max(0, fps);
  }

  /**
   * Returns the cached {@link BGFXStats} wrapper, creating it on first use.
   *
   * <p>The returned object points directly at bgfx's internal stats struct, so every field reflects
   * the most recently completed frame without another native call or allocation. Callers must not
   * hold onto stale copies of individual values across frames if they need live data; re-read the
   * fields instead. Returns {@code null} only if bgfx has not been initialized yet.
   *
   * @return The shared, live-updating bgfx stats wrapper, or {@code null} before bgfx is ready.
   */
  @Nullable
  public BGFXStats getBgfxStats() {
    if (bgfxStats == null) {
      bgfxStats = BGFX.bgfx_get_stats();
    }
    return bgfxStats;
  }

  /**
   * The compiled sprite shader program, or {@code -1} when none is available.
   *
   * <p>Exposed so the desktop debug overlay's ImGui renderer can reuse the exact same program the
   * sprite batch uses. The ImGui vertex format (position, texture coordinate, packed color) is
   * identical to the sprite layout and the sprite fragment shader already outputs
   * {@code texture * vertexColor}, which is what ImGui needs, so no separate shader is required.
   *
   * @return The bgfx sprite program handle, or {@code -1} if the shaders were not bundled.
   */
  public short getSpriteProgram() {
    return spriteProgram;
  }

  /**
   * The sampler uniform ({@code s_texture}) the sprite program reads its texture from.
   *
   * <p>Shared with the debug overlay's ImGui renderer so it can bind the font atlas and inspected
   * textures through the same uniform the sprite pipeline uses.
   *
   * @return The bgfx sampler uniform handle, or {@code -1} if it was not created.
   */
  public short getTextureUniform() {
    return textureUniform;
  }

  /**
   * Reserves a fresh, top-most on-screen bgfx view bound to the back buffer for the debug overlay to
   * draw into, and returns its id.
   *
   * <p>bgfx submits views in ascending id order, so handing out the next screen view id makes the
   * overlay draw over everything the game rendered this frame. The caller sets the view transform
   * and submits its own draws; this only allocates the view, points it at the whole back buffer, and
   * selects sequential submission mode so the overlay's draw order is preserved.
   *
   * @return The bgfx view id the overlay should submit its draws into.
   */
  public int beginOverlayView() {
    int view = nextScreenView;
    if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
      nextScreenView++;
    }
    viewStack[0] = view;
    viewStackDepth = 1;
    BGFX.bgfx_set_view_frame_buffer(view, (short) -1);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_set_view_rect(view, 0, 0, Math.max(1, backBufferWidth), Math.max(1, backBufferHeight));
    BGFX.bgfx_set_view_clear(view, BGFX.BGFX_CLEAR_NONE, 0, 1f, 0);
    BGFX.bgfx_touch(view);
    return view;
  }

  /** Directs subsequent submissions into a render target's framebuffer via a fresh view. */
  void pushRenderTarget(short frameBuffer, int width, int height) {
    int view = nextTargetView++;
    if (viewStackDepth < viewStack.length) {
      viewStack[viewStackDepth] = view;
      viewStackFrameBuffer[viewStackDepth] = frameBuffer;
      viewStackWidth[viewStackDepth] = width;
      viewStackHeight[viewStackDepth] = height;
      viewStackDepth++;
    }
    BGFX.bgfx_set_view_frame_buffer(view, frameBuffer);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_set_view_rect(view, 0, 0, width, height);
    BGFX.bgfx_set_view_clear(view, BGFX.BGFX_CLEAR_COLOR, 0, 1f, 0);
    // Touch so the target still clears even when the camera loop redirects its draws into fresh
    // per-camera views bound to this same framebuffer (see beginCameraPass).
    BGFX.bgfx_touch(view);
  }

  /** Returns submissions to the previously active view. */
  void popRenderTarget() {
    if (viewStackDepth > 1) {
      viewStackDepth--;
    }
  }

  private int currentView() {
    return viewStack[viewStackDepth - 1];
  }

  /** Rebuilds the scene surface when the render size or filter changed, then leaves it ready to use. */
  private void ensureSceneTarget() {
    if (sceneTarget != null && !sceneTargetDirty) {
      return;
    }
    disposeSceneTarget();
    sceneTarget = new FlixelBgfxRenderTarget(this, Math.max(1, renderWidth), Math.max(1, renderHeight));
    sceneTarget.getTexture().setSmooth(renderSmooth);
    sceneTargetDirty = false;
  }

  private void disposeSceneTarget() {
    if (sceneTarget != null) {
      sceneTarget.destroy();
      sceneTarget = null;
    }
  }

  /** The scene framebuffer handle, or the back-buffer sentinel when no surface exists yet. */
  private short sceneFrameBuffer() {
    return sceneTarget != null ? sceneTarget.getFrameBuffer() : (short) -1;
  }

  /**
   * Uploads and submits one batch of quads to the active view.
   *
   * @param verts Interleaved vertex floats (pos, uv, packed color) for {@code quadCount} quads.
   * @param quadCount The number of quads to draw.
   * @param texture The texture to bind, or {@code null} to skip the draw.
   * @param blend The blend mode.
   * @param shader The custom shader, or {@code null} for the default sprite program.
   * @param projection The view-projection matrix.
   * @param transform The model transform applied before projection.
   */
  void submitQuads(@NotNull float[] verts, int quadCount, @Nullable FlixelBgfxTexture texture,
      @NotNull FlixelBlendMode blend, @Nullable FlixelShader shader,
      @NotNull FlixelMatrix projection, @NotNull FlixelMatrix transform) {
    short program = resolveProgram(shader);
    if (program == -1 || texture == null) {
      if (!programWarned && program == -1) {
        programWarned = true;
        Flixel.warn("Graphics", "No sprite shader program is available. "
            + "Rendering is a no-op until compiled shaders are bundled.");
      }
      return;
    }

    int view = currentView();
    int vertexCount = quadCount * FlixelBgfxBatch.verticesPerQuad();
    int indexCount = quadCount * FlixelBgfxBatch.indicesPerQuad();

    BGFX.bgfx_alloc_transient_vertex_buffer(transientVertices, vertexCount, vertexLayout);
    transientVertices.data().asFloatBuffer().put(verts, 0, quadCount * FlixelBgfxBatch.floatsPerQuad());

    setViewTransform(view, projection, transform);

    BGFX.bgfx_set_view_rect(view, viewportX, viewportY, viewportWidth, viewportHeight);
    if (scissorWidth > 0) {
      BGFX.bgfx_set_scissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }
    BGFX.bgfx_set_transient_vertex_buffer(0, transientVertices, 0, vertexCount);
    // The index pattern never changes, so every batch draws through the shared static index buffer.
    BGFX.bgfx_set_index_buffer(quadIndexBuffer, 0, indexCount);
    BGFX.bgfx_set_texture(0, textureUniform, texture.getBgfxHandle(), (int) texture.getSamplerFlags());
    BGFX.bgfx_set_state(BGFX.BGFX_STATE_WRITE_RGB | BGFX.BGFX_STATE_WRITE_A | blendState(blend), 0);
    BGFX.bgfx_submit(view, program, 0, BGFX.BGFX_DISCARD_ALL);
  }

  private short resolveProgram(@Nullable FlixelShader shader) {
    if (shader != null) {
      FlixelShaderProgram prog = shader.getProgram();
      if (prog instanceof FlixelBgfxShader bgfxShader && bgfxShader.isValid()) {
        return bgfxShader.getProgram();
      }
    }
    return spriteProgram;
  }

  /**
   * Maps a blend mode to bgfx state blend bits.
   *
   * <p>The framework uses straight (non-premultiplied) alpha: the sprite fragment shader outputs
   * {@code texture * vertexColor} without folding alpha into the color channels. Each mode below is
   * therefore expressed so a sprite's own alpha (and tint alpha) is respected at blend time through
   * the {@code SRC_ALPHA} factor where the classic premultiplied formulation would use {@code ONE}.
   *
   * <p>The alpha channel always accumulates with a standard "over" ({@code srcA = ONE},
   * {@code dstA = 1 - srcA}) no matter what the color channels do. That keeps the alpha a camera or
   * global shader later reads out of a render target correct, so blended sprites composite properly
   * instead of punching holes or squaring their own alpha (which the old shared {@code BLEND_ALPHA}
   * state did on both channels).
   */
  private static long blendState(@NotNull FlixelBlendMode mode) {
    // Standard "over" for the alpha channel, shared by every blended mode.
    long srcA = BGFX.BGFX_STATE_BLEND_ONE;
    long dstA = BGFX.BGFX_STATE_BLEND_INV_SRC_ALPHA;
    return switch (mode) {
      // No blending at all: the source overwrites the destination, alpha included.
      case NONE -> 0L;
      // color = src * srcAlpha + dst * (1 - srcAlpha).
      case NORMAL -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_SRC_ALPHA, BGFX.BGFX_STATE_BLEND_INV_SRC_ALPHA, srcA, dstA);
      // color = src * srcAlpha + dst. Brightens; a faded sprite adds less.
      case ADD -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_SRC_ALPHA, BGFX.BGFX_STATE_BLEND_ONE, srcA, dstA);
      // color = src * dst + dst * (1 - srcAlpha). A pure multiply where opaque, unchanged where clear.
      case MULTIPLY -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_DST_COLOR, BGFX.BGFX_STATE_BLEND_INV_SRC_ALPHA, srcA, dstA);
      // color = src + dst * (1 - src). Lightens without blowing out to white the way ADD can.
      case SCREEN -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_ONE, BGFX.BGFX_STATE_BLEND_INV_SRC_COLOR, srcA, dstA);
      // color = dst - src * srcAlpha, via the reverse-subtract equation on the color channels only.
      case SUBTRACT -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_SRC_ALPHA, BGFX.BGFX_STATE_BLEND_ONE, srcA, dstA)
          | BGFX.BGFX_STATE_BLEND_EQUATION_SEPARATE(
              BGFX.BGFX_STATE_BLEND_EQUATION_REVSUB, BGFX.BGFX_STATE_BLEND_EQUATION_ADD);
      // color = max(src, dst) per channel. The MAX equation ignores the color factors.
      case LIGHTEN -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_ONE, BGFX.BGFX_STATE_BLEND_ONE, srcA, dstA)
          | BGFX.BGFX_STATE_BLEND_EQUATION_SEPARATE(
              BGFX.BGFX_STATE_BLEND_EQUATION_MAX, BGFX.BGFX_STATE_BLEND_EQUATION_ADD);
      // color = min(src, dst) per channel. The MIN equation ignores the color factors.
      case DARKEN -> BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
          BGFX.BGFX_STATE_BLEND_ONE, BGFX.BGFX_STATE_BLEND_ONE, srcA, dstA)
          | BGFX.BGFX_STATE_BLEND_EQUATION_SEPARATE(
              BGFX.BGFX_STATE_BLEND_EQUATION_MIN, BGFX.BGFX_STATE_BLEND_EQUATION_ADD);
    };
  }

  /**
   * Sets the active view's transform to {@code projection * transform}, using the reused scratch
   * matrix and buffer so no allocation happens on the flush path.
   *
   * @param view The bgfx view id to set the transform on.
   * @param projection The view-projection matrix.
   * @param transform The model transform applied before projection.
   */
  private void setViewTransform(int view, @NotNull FlixelMatrix projection, @NotNull FlixelMatrix transform) {
    combinedMatrix.set(projection);
    combinedMatrix.mul(transform);
    viewTransform.clear();
    viewTransform.put(combinedMatrix.val);
    viewTransform.flip();
    BGFX.bgfx_set_view_transform(view, null, viewTransform);
  }

  /**
   * Builds the shared static index buffer every quad batch draws through.
   *
   * <p>A quad's two triangles always wind the same way ({@code 0, 1, 2, 2, 3, 0}), so the whole
   * index buffer is built once here instead of being regenerated on every flush. Each flush binds
   * only the prefix it needs. The largest index stays within the unsigned 16-bit range because a
   * batch holds at most {@link FlixelBgfxBatch#maxQuads()} quads.
   *
   * @return The bgfx index buffer handle.
   */
  private static short createQuadIndexBuffer() {
    int quads = FlixelBgfxBatch.maxQuads();
    int indexCount = quads * FlixelBgfxBatch.indicesPerQuad();
    ByteBuffer data = MemoryUtil.memAlloc(indexCount * 2);
    ShortBuffer indices = data.asShortBuffer();
    for (int q = 0; q < quads; q++) {
      int v = q * FlixelBgfxBatch.verticesPerQuad();
      indices.put((short) v);
      indices.put((short) (v + 1));
      indices.put((short) (v + 2));
      indices.put((short) (v + 2));
      indices.put((short) (v + 3));
      indices.put((short) v);
    }
    short handle = BGFX.bgfx_create_index_buffer(BGFX.bgfx_copy(data), BGFX.BGFX_BUFFER_NONE);
    MemoryUtil.memFree(data);
    return handle;
  }

  private static short createShaderFromBytes(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
    buffer.put(bytes).flip();
    return BGFX.bgfx_create_shader(BGFX.bgfx_copy(buffer));
  }

  /** Loads the precompiled sprite shader program for the active renderer, or {@code -1} when absent. */
  private short loadSpriteProgram() {
    String dir = shaderDirFromApi(api);
    byte[] vs = readShaderResource("org/flixelgdx/shaders/" + dir + "/vs_sprite.bin");
    byte[] fs = readShaderResource("org/flixelgdx/shaders/" + dir + "/fs_sprite.bin");
    if (vs.length == 0 || fs.length == 0) {
      return -1;
    }
    short vsh = createShaderFromBytes(vs);
    short fsh = createShaderFromBytes(fs);
    if (vsh == -1 || fsh == -1) {
      return -1;
    }
    return BGFX.bgfx_create_program(vsh, fsh, true);
  }

  /** Maps a bgfx renderer type constant to the corresponding {@link FlixelGraphicsApi}. */
  private static FlixelGraphicsApi apiFromRenderer(int type) {
    if (type == BGFX.BGFX_RENDERER_TYPE_DIRECT3D11) {
      return FlixelGraphicsApi.Direct3D11;
    }
    if (type == BGFX.BGFX_RENDERER_TYPE_DIRECT3D12) {
      return FlixelGraphicsApi.Direct3D12;
    }
    if (type == BGFX.BGFX_RENDERER_TYPE_METAL) {
      return FlixelGraphicsApi.Metal;
    }
    if (type == BGFX.BGFX_RENDERER_TYPE_VULKAN) {
      return FlixelGraphicsApi.Vulkan;
    }
    if (type == BGFX.BGFX_RENDERER_TYPE_OPENGLES) {
      return FlixelGraphicsApi.OpenGLES;
    }
    return FlixelGraphicsApi.OpenGL;
  }

  /** Returns the shader subfolder that corresponds to a given graphics API. */
  private static String shaderDirFromApi(@NotNull FlixelGraphicsApi api) {
    return FlixelGraphicsManager.shaderVariantDir(api);
  }

  private static byte @NotNull [] readShaderResource(@NotNull String path) {
    try (InputStream in = FlixelBgfxGraphics.class.getResourceAsStream("/" + path)) {
      if (in == null) {
        return new byte[0];
      }
      return in.readAllBytes();
    } catch (Exception e) {
      return new byte[0];
    }
  }

  /**
   * Forces the entire back buffer's alpha channel to {@code 1} (fully opaque) without touching
   * the RGB channels.
   *
   * <p>Called every frame when the transparent framebuffer was requested in the game config but
   * desktop transparency is currently off. Without this pass, any semi-transparent sprite would
   * leave alpha values below {@code 1} in the back buffer, letting the desktop show through even
   * though the game is not in transparency mode. A full-screen quad is submitted with a
   * separate-function blend that preserves destination RGB while writing source alpha ({@code 1})
   * into the destination alpha channel.
   */
  @Override
  public void forceOpaqueAlpha() {
    if (spriteProgram == -1 || opaqueAlphaTexture == null) {
      return;
    }
    int view = nextScreenView;
    if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
      nextScreenView++;
    }
    viewStack[0] = view;
    viewStackDepth = 1;
    BGFX.bgfx_set_view_frame_buffer(view, (short) -1);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);

    float w = Math.max(1, backBufferWidth);
    float h = Math.max(1, backBufferHeight);
    // Reuse compositeOrtho and viewTransform to avoid allocation. The full-screen quad sits at
    // depth 0, which is inside NDC on both [0, 1] and [-1, 1] backends when the ortho is built
    // for the correct convention.
    compositeOrtho.setToOrtho2DYDown(0, 0, backBufferWidth, backBufferHeight, isDepthZeroToOne());
    viewTransform.clear();
    viewTransform.put(compositeOrtho.val);
    viewTransform.flip();
    BGFX.bgfx_set_view_transform(view, null, viewTransform);
    BGFX.bgfx_set_view_rect(view, 0, 0, (int) w, (int) h);

    // Build one full-screen quad into the transient vertex buffer. The vertex color is white
    // (0xFFFFFFFF) so the fragment outputs alpha=1 regardless of the texture.
    int vertexCount = FlixelBgfxBatch.verticesPerQuad();
    int indexCount = FlixelBgfxBatch.indicesPerQuad();
    BGFX.bgfx_alloc_transient_vertex_buffer(transientVertices, vertexCount, vertexLayout);
    float white = Float.intBitsToFloat(0xFFFFFFFF);
    FloatBuffer buf = transientVertices.data().asFloatBuffer();
    buf.put(0f).put(0f).put(0f).put(0f).put(white); // top-left
    buf.put(w).put(0f).put(1f).put(0f).put(white);  // top-right
    buf.put(w).put(h).put(1f).put(1f).put(white);   // bottom-right
    buf.put(0f).put(h).put(0f).put(1f).put(white);  // bottom-left

    BGFX.bgfx_set_transient_vertex_buffer(0, transientVertices, 0, vertexCount);
    BGFX.bgfx_set_index_buffer(quadIndexBuffer, 0, indexCount);
    BGFX.bgfx_set_texture(0, textureUniform, opaqueAlphaTexture.getBgfxHandle(), BGFX.BGFX_SAMPLER_NONE);
    // Blend: RGB src=ZERO dst=ONE (destination RGB preserved), alpha src=ONE dst=ZERO (forced to 1).
    long alphaForceBlend = BGFX.BGFX_STATE_BLEND_FUNC_SEPARATE(
        BGFX.BGFX_STATE_BLEND_ZERO, BGFX.BGFX_STATE_BLEND_ONE,
        BGFX.BGFX_STATE_BLEND_ONE, BGFX.BGFX_STATE_BLEND_ZERO);
    BGFX.bgfx_set_state(BGFX.BGFX_STATE_WRITE_RGB | BGFX.BGFX_STATE_WRITE_A | alphaForceBlend, 0);
    BGFX.bgfx_submit(view, spriteProgram, 0, BGFX.BGFX_DISCARD_ALL);
  }

  /** Creates a 1x1 fully opaque white texture used by the alpha-force pass. */
  @Nullable
  private static FlixelBgfxTexture createOpaqueAlphaTexture() {
    ByteBuffer pixel = MemoryUtil.memAlloc(4);
    pixel.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
    short handle = BGFX.bgfx_create_texture_2d(
        1, 1, false, 1,
        BGFX.BGFX_TEXTURE_FORMAT_RGBA8,
        BGFX.BGFX_TEXTURE_NONE,
        BGFX.bgfx_copy(pixel), 0L);
    MemoryUtil.memFree(pixel);
    if (handle == -1) {
      return null;
    }
    return new FlixelBgfxTexture(handle, 1, 1);
  }

  public boolean isRenderSmooth() {
    return renderSmooth;
  }

  public boolean isStatsEnabled() {
    return statsEnabled;
  }

  public boolean isStatsOverlay() {
    return statsOverlay;
  }
}
