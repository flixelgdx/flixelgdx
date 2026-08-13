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
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelShaderSource;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
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

  private long lastFrameTime = System.nanoTime();

  /** Wall-clock nanoseconds accumulated since the last stats log line, when stats logging is enabled. */
  private long statsWindowNanos;

  /** Timestamp of the previous {@code endFrame}, used to measure true end-to-end frame periods. */
  private long statsLastNanos;

  private double averageFps = 0;
  private double smoothingFactor = 0.1;

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

  private short vertexLayoutHandle;
  private short quadIndexBuffer = -1;
  private short spriteProgram = -1;
  private short textureUniform = -1;

  private int backBufferWidth;
  private int backBufferHeight;
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

  private boolean vsync = true;
  private boolean programWarned;

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
   */
  public void onInitialized(int width, int height) {
    this.backBufferWidth = width;
    this.backBufferHeight = height;
    this.viewportWidth = width;
    this.viewportHeight = height;
    this.viewStack[0] = SCREEN_CLEAR_VIEW;
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
      Flixel.info("Graphics", "bgfx stats logging is on (flixel.render.stats="
          + value + "). CPU submit vs GPU time is logged once per second.");
    }
  }

  /** Updates cached back buffer size and resets the bgfx swap chain. Called by the runner on resize. */
  public void onResize(int width, int height) {
    backBufferWidth = width;
    backBufferHeight = height;
    BGFX.bgfx_reset(width, height, vsync ? BGFX.BGFX_RESET_VSYNC : BGFX.BGFX_RESET_NONE,
        BGFX.BGFX_TEXTURE_FORMAT_RGBA8);
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

    BGFXStats stats = BGFX.bgfx_get_stats();
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

    Flixel.info("Graphics", String.format(
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
    // Only top-level (screen) passes are isolated here. When a render target is already active (for
    // example a global shader FBO that wraps every camera), we keep drawing into that target's view
    // rather than popping it off the stack.
    if (viewStackDepth != 1) {
      return;
    }
    int view = nextScreenView;
    if (nextScreenView < MAX_SCREEN_VIEWS - 1) {
      nextScreenView++;
    }
    viewStack[0] = view;
    // Draw in submission order (painter's algorithm). Without this, bgfx sorts draws within the view
    // to minimize state changes, which reorders 2D layers - the flash overlay ends up under the scene.
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
      short handle = BGFX.bgfx_create_texture(BGFX.bgfx_copy(copy), BGFX.BGFX_TEXTURE_NONE, 0, info);
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
  public void clear(float r, float g, float b, float a) {
    clearColor = ((int) (r * 255f) << 24) | ((int) (g * 255f) << 16) | ((int) (b * 255f) << 8) | (int) (a * 255f);
    BGFX.bgfx_set_view_clear(currentView(), BGFX.BGFX_CLEAR_COLOR, clearColor, 1f, 0);
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    scissorX = x;
    scissorWidth = Math.max(1, width);
    scissorHeight = Math.max(1, height);
    scissorY = backBufferHeight - y - scissorHeight;
  }

  @Override
  public void clearScissor() {
    scissorWidth = -1;
    scissorHeight = -1;
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    viewportX = x;
    viewportY = y;
    viewportWidth = Math.max(1, width);
    viewportHeight = Math.max(1, height);
    BGFX.bgfx_set_view_rect(currentView(), x, y, viewportWidth, viewportHeight);
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
  public FlixelShader compileShader(@NotNull FlixelShaderSource source) {
    byte[] vs = source.bgfxVertex();
    byte[] fs = source.bgfxFragment();
    if (vs == null || fs == null || vs.length == 0 || fs.length == 0) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    short vsh = createShaderFromBytes(vs);
    short fsh = createShaderFromBytes(fs);
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

  /** Directs subsequent submissions into a render target's framebuffer via a fresh view. */
  void pushRenderTarget(short frameBuffer, int width, int height) {
    int view = nextTargetView++;
    if (viewStackDepth < viewStack.length) {
      viewStack[viewStackDepth++] = view;
    }
    BGFX.bgfx_set_view_frame_buffer(view, frameBuffer);
    BGFX.bgfx_set_view_mode(view, BGFX.BGFX_VIEW_MODE_SEQUENTIAL);
    BGFX.bgfx_set_view_rect(view, 0, 0, width, height);
    BGFX.bgfx_set_view_clear(view, BGFX.BGFX_CLEAR_COLOR, 0, 1f, 0);
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
    if (shader instanceof FlixelBgfxShader bgfxShader && bgfxShader.isValid()) {
      return bgfxShader.getProgram();
    }
    return spriteProgram;
  }

  /** Maps a blend mode to bgfx state blend bits. */
  private static long blendState(@NotNull FlixelBlendMode mode) {
    return switch (mode) {
      case NONE -> 0L;
      case ADD -> BGFX.BGFX_STATE_BLEND_ADD;
      case NORMAL, MULTIPLY, SCREEN, SUBTRACT, LIGHTEN, DARKEN -> BGFX.BGFX_STATE_BLEND_ALPHA;
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
    if (api == FlixelGraphicsApi.Direct3D11 || api == FlixelGraphicsApi.Direct3D12) {
      return "dx11";
    }
    if (api == FlixelGraphicsApi.Metal) {
      return "metal";
    }
    if (api == FlixelGraphicsApi.Vulkan) {
      return "spirv";
    }
    return "glsl";
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
}
