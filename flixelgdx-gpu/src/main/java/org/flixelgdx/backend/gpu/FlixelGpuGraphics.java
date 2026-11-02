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
package org.flixelgdx.backend.gpu;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.collections.FlixelLongMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGlobalShaderPipeline;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.flixelgdx.graphics.wgsl.FlixelWgsl;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuAdapter;
import io.github.stringdotjar.jwebgpu.GpuAdapterInfo;
import io.github.stringdotjar.jwebgpu.GpuBindGroup;
import io.github.stringdotjar.jwebgpu.GpuBuffer;
import io.github.stringdotjar.jwebgpu.GpuBufferDescriptor;
import io.github.stringdotjar.jwebgpu.GpuBufferUsage;
import io.github.stringdotjar.jwebgpu.GpuCommandBuffer;
import io.github.stringdotjar.jwebgpu.GpuCommandEncoder;
import io.github.stringdotjar.jwebgpu.GpuCompositeAlphaMode;
import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuExtent3D;
import io.github.stringdotjar.jwebgpu.GpuFeatureName;
import io.github.stringdotjar.jwebgpu.GpuIndexFormat;
import io.github.stringdotjar.jwebgpu.GpuLimits;
import io.github.stringdotjar.jwebgpu.GpuLoadOp;
import io.github.stringdotjar.jwebgpu.GpuPresentMode;
import io.github.stringdotjar.jwebgpu.GpuQueue;
import io.github.stringdotjar.jwebgpu.GpuRenderPassColorAttachment;
import io.github.stringdotjar.jwebgpu.GpuRenderPassDescriptor;
import io.github.stringdotjar.jwebgpu.GpuRenderPassEncoder;
import io.github.stringdotjar.jwebgpu.GpuRenderPipeline;
import io.github.stringdotjar.jwebgpu.GpuStoreOp;
import io.github.stringdotjar.jwebgpu.GpuSurface;
import io.github.stringdotjar.jwebgpu.GpuSurfaceCapabilities;
import io.github.stringdotjar.jwebgpu.GpuSurfaceConfiguration;
import io.github.stringdotjar.jwebgpu.GpuSurfaceGetCurrentTextureStatus;
import io.github.stringdotjar.jwebgpu.GpuSurfaceTexture;
import io.github.stringdotjar.jwebgpu.GpuTexelCopyBufferLayout;
import io.github.stringdotjar.jwebgpu.GpuTexelCopyTextureInfo;
import io.github.stringdotjar.jwebgpu.GpuTexture;
import io.github.stringdotjar.jwebgpu.GpuTextureAspect;
import io.github.stringdotjar.jwebgpu.GpuTextureDescriptor;
import io.github.stringdotjar.jwebgpu.GpuTextureDimension;
import io.github.stringdotjar.jwebgpu.GpuTextureFormat;
import io.github.stringdotjar.jwebgpu.GpuTextureUsage;
import io.github.stringdotjar.jwebgpu.GpuTextureView;

import java.nio.ByteBuffer;

/**
 * The one WebGPU renderer that every platform shares.
 *
 * <p>This class implements {@link FlixelGraphicsManager} on top of {@code jwebgpu-api} only. The platform
 * (desktop, Android, web) supplies the pieces that differ: it installs a jwebgpu backend, creates a
 * {@code GpuSurface} for its window, runs a {@link FlixelGpuBootstrap}, and then hands the finished device to
 * {@link #initialize}. From then on a frame is {@link #beginFrame()}, drawing through {@link #getBatch()}, and
 * {@link #endFrame()}.
 *
 * <h2>How a frame is recorded</h2>
 *
 * <p>The whole frame goes into one command encoder. Nothing reaches the GPU until {@link #endFrame()} submits it,
 * but {@code queue.writeBuffer(...)} takes effect <em>before</em> the commands submitted after it. So every draw
 * copies its vertices and uniforms to its own slice of a ring buffer and binds that slice with an offset, instead
 * of reusing one spot. When a ring fills up the renderer submits the commands recorded so far, rewinds the rings
 * and carries on; the queue keeps the order straight.
 *
 * <p>The old API drew into numbered "views". Here the equivalent is a stack of <em>levels</em>: level 0 is the
 * screen (or the scaled scene surface) and every {@link FlixelRenderTarget#begin()} pushes a level. A level
 * remembers whether it still needs clearing, and a render pass is opened lazily on the first draw using a clear
 * or load operation accordingly. That is how {@link #clear(float, float, float, float)} works: it never issues a
 * clear command, it only changes how the next pass begins.
 *
 * <h2>Coordinates</h2>
 *
 * <p>Everything is top-left origin with Y pointing down, in pixels. Render targets are stored top-down too, so
 * they are never flipped. Clip-space depth runs from 0 to 1 ({@link #isDepthZeroToOne()} is {@code true}).
 */
public class FlixelGpuGraphics implements FlixelGraphicsManager {

  private static final FlixelLogger LOG = Flixel.log.tagged("Graphics");
  // The built-in sprite shader. The preamble from FlixelWgsl is put in front of it like any other shader.
  private static final String SPRITE_SHADER = """
      // The built-in sprite shader: the texel times the vertex tint. Premultiplication is left to the blend mode.

      @fragment
      fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
        return textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
      }
      """;

  private static final long VERTEX_RING_BYTES = 4L << 20;
  private static final long UNIFORM_RING_BYTES = 1L << 20;
  private static final long INDEX_RING_BYTES = 1L << 20;
  private static final int MAX_LEVELS = 16;
  private static final int SPARE = MAX_LEVELS;
  private static final int GLOBALS_FLOATS = FlixelWgsl.GLOBALS_SIZE / 4;

  /** The smoothing factor of the frame rate average. */
  public double smoothingFactor = 0.1;

  private long lastFrameNanos = System.nanoTime();
  private long frameStartNanos;
  private long ringEpoch;
  private long nextTextureId = 1L;
  private double averageFps;

  private final Object gpuLock = new Object();
  private final FlixelStatsLog statsLog = new FlixelStatsLog();
  private final FlixelGpuBatch batch = new FlixelGpuBatch(this);
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();
  private final FlixelArray<Runnable> mainThreadQueue = new FlixelArray<>();
  private final FlixelGlobalShaderPipeline pipeline = new FlixelGlobalShaderPipeline();
  private final FlixelLongMap<FlixelGpuTexture> textures = new FlixelLongMap<>();
  private final FlixelGpuStats stats = new FlixelGpuStats();
  private final FlixelMatrix combined = new FlixelMatrix();
  private final FlixelMatrix compositeOrtho = new FlixelMatrix();
  private final FlixelMatrix identity = new FlixelMatrix();
  private final float[] globals = new float[GLOBALS_FLOATS];
  private final float[] opaqueVertices = new float[FlixelGpuBatch.floatsPerQuad()];
  private final int[] dynamicOffset = new int[1];
  private final GpuCommandBuffer[] submitArray = new GpuCommandBuffer[1];

  private final GpuRenderPassDescriptor passDescriptor = new GpuRenderPassDescriptor(1);
  private final GpuSurfaceTexture surfaceTexture = new GpuSurfaceTexture();
  private final GpuSurfaceConfiguration surfaceConfig = new GpuSurfaceConfiguration();
  private final GpuSurfaceCapabilities capabilities = new GpuSurfaceCapabilities(16, 8, 8);
  private final GpuTextureDescriptor textureDescriptor = new GpuTextureDescriptor();
  private final GpuTexelCopyTextureInfo copyDestination = new GpuTexelCopyTextureInfo();
  private final GpuTexelCopyBufferLayout copyLayout = new GpuTexelCopyBufferLayout();
  private final GpuExtent3D copyExtent = new GpuExtent3D();
  private final GpuAdapterInfo adapterInfo = new GpuAdapterInfo();
  private final GpuLimits limits = new GpuLimits();

  private final GpuTextureView[] levelView = new GpuTextureView[MAX_LEVELS + 1];
  private final GpuTextureFormat[] levelFormat = new GpuTextureFormat[MAX_LEVELS + 1];
  private final FlixelGpuRenderTarget[] levelTarget = new FlixelGpuRenderTarget[MAX_LEVELS + 1];
  private final int[] levelWidth = new int[MAX_LEVELS + 1];
  private final int[] levelHeight = new int[MAX_LEVELS + 1];
  private final int[] viewX = new int[MAX_LEVELS + 1];
  private final int[] viewY = new int[MAX_LEVELS + 1];
  private final int[] viewW = new int[MAX_LEVELS + 1];
  private final int[] viewH = new int[MAX_LEVELS + 1];
  private final int[] cutX = new int[MAX_LEVELS + 1];
  private final int[] cutY = new int[MAX_LEVELS + 1];
  private final int[] cutW = new int[MAX_LEVELS + 1];
  private final int[] cutH = new int[MAX_LEVELS + 1];
  private final float[] clearColor = new float[(MAX_LEVELS + 1) * 4];
  private final boolean[] clearPending = new boolean[MAX_LEVELS + 1];
  private final boolean[] cutEnabled = new boolean[MAX_LEVELS + 1];

  private FlixelGraphicsApi api = FlixelGraphicsApi.WEBGPU;
  private GpuTextureFormat surfaceFormat = GpuTextureFormat.RGBA8_UNORM;

  private GpuAdapter adapter;
  private GpuDevice device;
  private GpuQueue queue;
  private GpuSurface surface;
  private GpuCommandEncoder encoder;
  private GpuRenderPassEncoder pass;
  private GpuCommandBuffer commands;
  private GpuTexture headlessTexture;
  private GpuTextureView headlessView;
  private GpuTextureView surfaceView;
  private GpuBuffer quadIndices;
  private FlixelGpuRing vertexRing;
  private FlixelGpuRing uniformRing;
  private FlixelGpuRing indexRing;
  private FlixelGpuPipelines pipelines;
  private FlixelGpuShaderProgram spriteProgram;
  private FlixelGpuTexture opaqueTexture;
  private FlixelGpuRenderTarget sceneTarget;
  private FlixelImageDecoder decoder;
  private volatile FlixelBasisTranscoder basisTranscoder;
  private Runnable eventPump;
  private GpuRenderPipeline lastPipeline;

  private int deviceEpoch;
  private int uniformAlignment = 256;
  private int backBufferWidth;
  private int backBufferHeight;
  private int renderWidth;
  private int renderHeight;
  private int sceneWidth;
  private int sceneHeight;
  private int depth;
  private int passLevel;
  private float renderScale = 1f;
  private float compositeScale = 1f;
  private float compositeOffsetX;
  private float compositeOffsetY;

  private volatile int targetFps;
  private volatile int blockFormats = FlixelGpuBlockFormat.RGBA8.bit();

  private boolean transparentFramebuffer;
  private boolean vsync = true;
  private boolean surfaceDirty;
  private boolean surfaceDetached;
  private boolean frameActive;
  private boolean passOpen;
  private boolean stateDirty;
  private boolean renderResolutionEnabled;
  private boolean windowedRenderResolution;
  private boolean renderSmooth = true;
  private boolean sceneActive;
  private boolean sceneTargetDirty;
  private boolean statsEnabled;
  private boolean ringWarned;

  /** Creates a renderer. Call {@link #initialize} before the first frame. */
  public FlixelGpuGraphics() {
    String value = System.getProperty("flixel.render.stats", "").trim().toLowerCase();
    statsEnabled = value.equals("true") || value.equals("log");
  }

  /**
   * Builds every GPU resource the renderer needs, once the platform has a device.
   *
   * @param adapter The adapter the device came from.
   * @param device The device to render with.
   * @param surface The surface to present to, or {@code null} to render off-screen into a texture (for tests
   *     and tools).
   * @param width The initial back buffer width in pixels.
   * @param height The initial back buffer height in pixels.
   * @param transparent Whether the window's alpha channel should reach the desktop compositor.
   * @param decoder How to decode encoded image files, or {@code null} when the platform loads pixels itself.
   */
  public void initialize(@NotNull GpuAdapter adapter, @NotNull GpuDevice device, @Nullable GpuSurface surface,
      int width, int height, boolean transparent, @Nullable FlixelImageDecoder decoder) {
    this.adapter = adapter;
    this.device = device;
    this.surface = surface;
    this.surfaceDetached = false;
    this.deviceEpoch++;
    this.decoder = decoder;
    this.transparentFramebuffer = transparent;
    this.backBufferWidth = Math.max(1, width);
    this.backBufferHeight = Math.max(1, height);
    this.queue = device.getQueue();

    adapter.getInfo(adapterInfo);
    api = apiOf(adapterInfo);
    if (device.getLimits(limits) != null && limits.minUniformBufferOffsetAlignment > 0) {
      uniformAlignment = Math.max(limits.minUniformBufferOffsetAlignment, 16);
    }
    LOG.info("GPU: {} ({}), {} backend.", adapterInfo.device, adapterInfo.description, adapterInfo.backendType);
    blockFormats = queryBlockFormats(device);

    pipelines = new FlixelGpuPipelines(device);
    vertexRing = new FlixelGpuRing(device, GpuBufferUsage.VERTEX, VERTEX_RING_BYTES, "flixel vertices");
    uniformRing = new FlixelGpuRing(device, GpuBufferUsage.UNIFORM, UNIFORM_RING_BYTES, "flixel uniforms");
    indexRing = new FlixelGpuRing(device, GpuBufferUsage.INDEX, INDEX_RING_BYTES, "flixel overlay indices");
    quadIndices = createQuadIndexBuffer();

    spriteProgram = pipelines.createSpriteProgram(SPRITE_SHADER, this);
    opaqueTexture = createOpaqueTexture();
    configureSurface();
    surfaceDirty = false;
  }

  private static int queryBlockFormats(GpuDevice device) {
    int mask = 0;
    FlixelGpuBlockFormat[] all = FlixelGpuBlockFormat.values();
    for (FlixelGpuBlockFormat flixelGpuBlockFormat : all) {
      GpuFeatureName feature = flixelGpuBlockFormat.getFeature();
      if (feature == null || device.hasFeature(feature)) {
        mask |= flixelGpuBlockFormat.bit();
      }
    }
    return mask;
  }

  private GpuBuffer createQuadIndexBuffer() {
    int quads = FlixelGpuBatch.maxQuads();
    short[] indices = new short[quads * FlixelGpuBatch.indicesPerQuad()];
    for (int q = 0; q < quads; q++) {
      int v = q * FlixelGpuBatch.verticesPerQuad();
      int i = q * FlixelGpuBatch.indicesPerQuad();
      indices[i] = (short) v;
      indices[i + 1] = (short) (v + 1);
      indices[i + 2] = (short) (v + 2);
      indices[i + 3] = (short) (v + 2);
      indices[i + 4] = (short) (v + 3);
      indices[i + 5] = (short) v;
    }
    GpuBufferDescriptor d = new GpuBufferDescriptor();
    d.label = "flixel quad indices";
    d.usage = GpuBufferUsage.INDEX | GpuBufferUsage.COPY_DST;
    d.size = (long) indices.length * 2L;
    GpuBuffer buffer = device.createBuffer(d);
    queue.writeBuffer(buffer, 0L, indices, 0, indices.length);
    return buffer;
  }

  private FlixelGpuTexture createOpaqueTexture() {
    FlixelImage pixel = new FlixelImage(1, 1);
    pixel.fill(0xFFFFFFFF);
    return new FlixelGpuTexture(this, pixel);
  }

  private static FlixelGraphicsApi apiOf(GpuAdapterInfo info) {
    if (info.backendType == null) {
      return FlixelGraphicsApi.WEBGPU;
    }
    return switch (info.backendType) {
      case D3D11 -> FlixelGraphicsApi.DIRECT3D11;
      case D3D12 -> FlixelGraphicsApi.DIRECT3D12;
      case METAL -> FlixelGraphicsApi.METAL;
      case VULKAN -> FlixelGraphicsApi.VULKAN;
      case OPENGL -> FlixelGraphicsApi.OPENGL;
      case OPENGLES -> FlixelGraphicsApi.OPENGL_ES;
      default -> FlixelGraphicsApi.WEBGPU;
    };
  }

  private void configureSurface() {
    if (surface == null) {
      surfaceFormat = GpuTextureFormat.RGBA8_UNORM;
      if (headlessView != null) {
        headlessView.release();
        headlessTexture.release();
      }
      headlessTexture = createRawTexture(backBufferWidth, backBufferHeight, GpuTextureFormat.RGBA8_UNORM, 1,
          GpuTextureUsage.RENDER_ATTACHMENT | GpuTextureUsage.COPY_SRC);
      headlessView = headlessTexture.createView(null);
      return;
    }
    capabilities.reset();
    surface.getCapabilities(adapter, capabilities);
    surfaceFormat = chooseFormat();
    surfaceConfig.reset();
    surfaceConfig.device = device;
    surfaceConfig.format = surfaceFormat;
    surfaceConfig.usage = GpuTextureUsage.RENDER_ATTACHMENT;
    surfaceConfig.width = backBufferWidth;
    surfaceConfig.height = backBufferHeight;
    surfaceConfig.alphaMode = chooseAlphaMode();
    surfaceConfig.presentMode = choosePresentMode();
    surface.configure(surfaceConfig);
  }

  private GpuTextureFormat chooseFormat() {
    GpuTextureFormat fallback = null;
    for (int i = 0; i < capabilities.formatCount; i++) {
      GpuTextureFormat f = capabilities.formats[i];
      if (f == GpuTextureFormat.BGRA8_UNORM || f == GpuTextureFormat.RGBA8_UNORM) {
        return f;
      }
      if (fallback == null) {
        fallback = f;
      }
    }
    return fallback != null ? fallback : GpuTextureFormat.BGRA8_UNORM;
  }

  private GpuCompositeAlphaMode chooseAlphaMode() {
    boolean unpremultiplied = false;
    boolean premultiplied = false;
    boolean opaque = false;
    for (int i = 0; i < capabilities.alphaModeCount; i++) {
      GpuCompositeAlphaMode m = capabilities.alphaModes[i];
      unpremultiplied |= m == GpuCompositeAlphaMode.UNPREMULTIPLIED;
      premultiplied |= m == GpuCompositeAlphaMode.PREMULTIPLIED;
      opaque |= m == GpuCompositeAlphaMode.OPAQUE;
    }
    if (transparentFramebuffer) {
      if (unpremultiplied) {
        return GpuCompositeAlphaMode.UNPREMULTIPLIED;
      }
      if (premultiplied) {
        return GpuCompositeAlphaMode.PREMULTIPLIED;
      }
    }
    return opaque ? GpuCompositeAlphaMode.OPAQUE : GpuCompositeAlphaMode.AUTO;
  }

  private GpuPresentMode choosePresentMode() {
    boolean immediate = false;
    boolean mailbox = false;
    for (int i = 0; i < capabilities.presentModeCount; i++) {
      GpuPresentMode m = capabilities.presentModes[i];
      immediate |= m == GpuPresentMode.IMMEDIATE;
      mailbox |= m == GpuPresentMode.MAILBOX;
    }
    if (vsync) {
      return GpuPresentMode.FIFO;
    }
    if (immediate) {
      return GpuPresentMode.IMMEDIATE;
    }
    return mailbox ? GpuPresentMode.MAILBOX : GpuPresentMode.FIFO;
  }

  /**
   * Tells the renderer the window changed size. The surface is reconfigured at the start of the next frame.
   *
   * @param width The new back buffer width in pixels.
   * @param height The new back buffer height in pixels.
   */
  public void onResize(int width, int height) {
    backBufferWidth = Math.max(1, width);
    backBufferHeight = Math.max(1, height);
    surfaceDirty = true;
  }

  /**
   * Stops presenting to the current surface without touching the device or any GPU resource.
   *
   * <p>A platform whose window can disappear while the app keeps running (Android destroys the surface of an app
   * that goes to the background) calls this <em>before</em> it releases the surface. Until
   * {@link #attachSurface} is called, {@link #beginFrame()} starts no frame and {@link #isFrameActive()} stays
   * {@code false}, so nothing is drawn. Textures, shaders and render targets stay valid, because they belong to the
   * device and not to the surface.
   *
   * <p>The caller still owns the {@link GpuSurface} and must release it itself; this method only unconfigures it.
   * Do not call it while a frame is being recorded.
   */
  public void detachSurface() {
    if (surface == null) {
      return;
    }
    frameActive = false;
    passOpen = false;
    if (surfaceView != null) {
      surfaceView.release();
      surfaceView = null;
    }
    surface.unconfigure();
    surface = null;
    surfaceDetached = true;
  }

  /**
   * Starts presenting to a new surface after {@link #detachSurface()}, keeping every other resource.
   *
   * <p>The surface is configured right away with the given size, using the formats and present mode that the
   * surface supports. The caller keeps ownership of the surface and releases it after calling
   * {@link #detachSurface()} again.
   *
   * @param newSurface The surface to present to.
   * @param width The back buffer width in pixels.
   * @param height The back buffer height in pixels.
   */
  public void attachSurface(@NotNull GpuSurface newSurface, int width, int height) {
    surface = newSurface;
    surfaceDetached = false;
    backBufferWidth = Math.max(1, width);
    backBufferHeight = Math.max(1, height);
    configureSurface();
    surfaceDirty = false;
  }

  @NotNull
  @Override
  public FlixelGraphicsApi getApi() {
    return api;
  }

  @Override
  public boolean isDepthZeroToOne() {
    return true;
  }

  @NotNull
  @Override
  public FlixelBatch getBatch() {
    return batch;
  }

  @NotNull
  @Override
  public FlixelGlobalShaderPipeline getGlobalShaderPipeline() {
    return pipeline;
  }

  @Override
  public void queueMainThread(@NotNull Runnable action) {
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
    long now = System.nanoTime();
    long delta = now - lastFrameNanos;
    lastFrameNanos = now;
    frameStartNanos = now;
    double instantFps = 1_000_000_000.0 / Math.max(1L, delta);
    averageFps = (averageFps * (1 - smoothingFactor)) + (instantFps * smoothingFactor);

    stats.beginFrame();
    if (surfaceDirty) {
      configureSurface();
      surfaceDirty = false;
    }
    frameActive = acquireBackBuffer();
    if (!frameActive) {
      return;
    }
    vertexRing.reset();
    uniformRing.reset();
    indexRing.reset();
    ringEpoch++;
    encoder = encoder == null ? device.createCommandEncoder(null) : device.createCommandEncoder(null, encoder);
    passOpen = false;
    sceneActive = false;
    depth = 1;
    initLevel(0, surfaceView, surfaceFormat, backBufferWidth, backBufferHeight, null);
    setClear(0, 0f, 0f, 0f, 1f);
  }

  private boolean acquireBackBuffer() {
    if (surfaceDetached) {
      return false;
    }
    if (surface == null) {
      surfaceView = headlessView;
      return true;
    }
    surface.getCurrentTexture(surfaceTexture);
    GpuSurfaceGetCurrentTextureStatus status = surfaceTexture.status;
    if (status == GpuSurfaceGetCurrentTextureStatus.SUCCESS_SUBOPTIMAL) {
      surfaceDirty = true;
    } else if (status != GpuSurfaceGetCurrentTextureStatus.SUCCESS_OPTIMAL) {
      if (status == GpuSurfaceGetCurrentTextureStatus.OUTDATED || status == GpuSurfaceGetCurrentTextureStatus.LOST) {
        surfaceDirty = true;
      }
      return false;
    }
    if (surfaceTexture.texture == null) {
      return false;
    }
    surfaceView = surfaceView == null ? surfaceTexture.texture.createView(null)
        : surfaceTexture.texture.createView(null, surfaceView);
    return true;
  }

  @Override
  public void endFrame() {
    if (!frameActive) {
      return;
    }
    while (depth > 1) {
      popTarget();
    }
    if (sceneActive) {
      endScene();
    }
    finishLevel(0);
    endPass();
    submitCommands();
    long presentStart = System.nanoTime();
    if (surface != null) {
      surface.present();
    }
    long end = System.nanoTime();
    stats.cpuMs = (presentStart - frameStartNanos) / 1_000_000f;
    stats.presentMs = (end - presentStart) / 1_000_000f;
    stats.vertexBytes = (int) vertexRing.getAllocator().getUsed();
    stats.uniformBytes = (int) uniformRing.getAllocator().getUsed();
    stats.pipelines = pipelines.getPipelineCount();
    stats.textures = textures.getSize();
    stats.ringBytes = VERTEX_RING_BYTES + UNIFORM_RING_BYTES + INDEX_RING_BYTES;
    frameActive = false;
    if (statsEnabled) {
      statsLog.tick(end, stats, api);
    }
  }

  @Override
  public void beginCameraPass() {
    // A camera pass continues drawing into the current level. The clear (if any) is already queued on the level,
    // so there is nothing to start here; each camera sets its own viewport right after this call.
  }

  private void initLevel(int level, GpuTextureView view, GpuTextureFormat format, int width, int height,
      @Nullable FlixelGpuRenderTarget target) {
    levelView[level] = view;
    levelFormat[level] = format;
    levelTarget[level] = target;
    levelWidth[level] = width;
    levelHeight[level] = height;
    viewX[level] = 0;
    viewY[level] = 0;
    viewW[level] = width;
    viewH[level] = height;
    cutEnabled[level] = false;
    clearPending[level] = false;
    stateDirty = true;
  }

  private void setClear(int level, float r, float g, float b, float a) {
    clearPending[level] = true;
    int at = level * 4;
    clearColor[at] = r;
    clearColor[at + 1] = g;
    clearColor[at + 2] = b;
    clearColor[at + 3] = a;
  }

  private void copyLevel(int from, int to) {
    levelView[to] = levelView[from];
    levelFormat[to] = levelFormat[from];
    levelTarget[to] = levelTarget[from];
    levelWidth[to] = levelWidth[from];
    levelHeight[to] = levelHeight[from];
    viewX[to] = viewX[from];
    viewY[to] = viewY[from];
    viewW[to] = viewW[from];
    viewH[to] = viewH[from];
    cutX[to] = cutX[from];
    cutY[to] = cutY[from];
    cutW[to] = cutW[from];
    cutH[to] = cutH[from];
    cutEnabled[to] = cutEnabled[from];
    clearPending[to] = clearPending[from];
    System.arraycopy(clearColor, from * 4, clearColor, to * 4, 4);
    stateDirty = true;
  }

  /**
   * Makes sure a render pass is open on the current level, beginning one with the level's pending load
   * operation if needed.
   */
  private void ensurePass() {
    if (passOpen) {
      return;
    }
    int level = depth - 1;
    GpuRenderPassColorAttachment color = passDescriptor.colorAttachments[0];
    color.view = levelView[level];
    color.storeOp = GpuStoreOp.STORE;
    if (clearPending[level]) {
      color.loadOp = GpuLoadOp.CLEAR;
      color.clearValue.r = clearColor[level * 4];
      color.clearValue.g = clearColor[level * 4 + 1];
      color.clearValue.b = clearColor[level * 4 + 2];
      color.clearValue.a = clearColor[level * 4 + 3];
      clearPending[level] = false;
    } else {
      color.loadOp = GpuLoadOp.LOAD;
    }
    passDescriptor.colorAttachmentCount = 1;
    pass = pass == null ? encoder.beginRenderPass(passDescriptor) : encoder.beginRenderPass(passDescriptor, pass);
    passOpen = true;
    passLevel = level;
    stateDirty = true;
    lastPipeline = null;
    stats.passes++;
  }

  private void endPass() {
    if (passOpen) {
      pass.end();
      passOpen = false;
    }
  }

  /** Runs a clear-only pass when a level was cleared but never drawn to, so the clear is not lost. */
  private void finishLevel(int level) {
    if (clearPending[level] && levelView[level] != null) {
      endPass();
      int saved = depth;
      depth = level + 1;
      ensurePass();
      endPass();
      depth = saved;
    }
  }

  private void submitCommands() {
    commands = commands == null ? encoder.finish(null) : encoder.finish(null, commands);
    submitArray[0] = commands;
    queue.submit(submitArray, 1);
    stats.submits++;
  }

  /**
   * Submits the commands recorded so far and rewinds the rings, so a ring that ran full can be filled again.
   *
   * <p>The queue runs writes and submits in order, so the new frame data cannot overwrite data the submitted
   * commands still read. The render pass, if one was open, is reopened with a load operation on the next draw.
   */
  public void restartCommands() {
    if (!frameActive) {
      return;
    }
    endPass();
    submitCommands();
    encoder = device.createCommandEncoder(null, encoder);
    vertexRing.reset();
    uniformRing.reset();
    indexRing.reset();
    ringEpoch++;
  }

  @NotNull
  @Override
  public FlixelTexture createTexture(int width, int height) {
    return new FlixelGpuTexture(this, Math.max(1, width), Math.max(1, height), 0L);
  }

  @NotNull
  @Override
  public FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    ByteBuffer copy = ByteBuffer.allocateDirect(width * height * 4);
    rgba.position(0).limit(width * height * 4);
    copy.put(rgba).flip();
    return new FlixelGpuTexture(this, new FlixelImage(width, height, copy));
  }

  @NotNull
  @Override
  public FlixelTexture createTexture(@NotNull FlixelImage image) {
    return new FlixelGpuTexture(this, image);
  }

  @Nullable
  @Override
  public FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    return decoder != null ? decoder.decode(encoded) : null;
  }

  /**
   * Uploads a KTX2 file, keeping it compressed on the GPU.
   *
   * <p>This is {@link #prepareCompressedTexture(ByteBuffer)} followed by
   * {@link #createTexture(FlixelGpuCompressedImage)} on the calling thread. When the file cannot be used, the reason
   * is logged and {@code null} is returned.
   *
   * @param container The whole KTX2 file.
   * @return The texture, or {@code null} when the file is invalid or needs something this device or platform lacks.
   */
  @Nullable
  @Override
  public FlixelTexture createCompressedTexture(@NotNull ByteBuffer container) {
    FlixelGpuCompressedImage image;
    try {
      image = prepareCompressedTexture(container);
    } catch (IllegalArgumentException e) {
      LOG.warn("Could not use a compressed texture: {}", e.getMessage());
      return null;
    }
    return createTexture(image);
  }

  /**
   * Reads a KTX2 file into an image that is ready to upload, transcoding Basis Universal data for this GPU.
   *
   * <p>This is the slow half of loading a compressed texture, and it only uses the CPU, so an asset loader runs it
   * on a background thread and then calls {@link #createTexture(FlixelGpuCompressedImage)} on the render thread.
   * The target format depends on what the current device supports (see {@link #isBlockFormatSupported}); see
   * {@link FlixelKtx2} for the files that can be read.
   *
   * <pre>{@code
   * // On a loading thread:
   * FlixelGpuCompressedImage image = graphics.prepareCompressedTexture(fileBytes);
   * // Later, on the render thread:
   * FlixelTexture texture = graphics.createTexture(image);
   * }</pre>
   *
   * @param container The whole KTX2 file. A file in a GPU format is uploaded straight from this buffer, so it must
   *     stay unchanged until {@link #createTexture(FlixelGpuCompressedImage)} runs.
   * @return The prepared image.
   * @throws IllegalArgumentException When the file cannot be used. The message says why.
   */
  @NotNull
  public FlixelGpuCompressedImage prepareCompressedTexture(@NotNull ByteBuffer container) {
    return FlixelKtx2.decode(container, blockFormats, basisTranscoder);
  }

  /**
   * Uploads a prepared image, with all of its mip levels.
   *
   * @param image An image from {@link #prepareCompressedTexture(ByteBuffer)}.
   * @return The texture. Drawing it works like any other texture; see {@link FlixelGpuTexture} for how its size is
   *     reported.
   */
  @NotNull
  public FlixelTexture createTexture(@NotNull FlixelGpuCompressedImage image) {
    return new FlixelGpuTexture(this, image);
  }

  /**
   * Returns whether the current device can sample textures in a format.
   *
   * <p>The answer can change after a device loss, because the new device may come from another adapter.
   *
   * @param format The format.
   * @return {@code true} if textures in that format can be created and drawn.
   */
  public boolean isBlockFormatSupported(@NotNull FlixelGpuBlockFormat format) {
    return (blockFormats & format.bit()) != 0;
  }

  @NotNull
  @Override
  public FlixelRenderTarget createRenderTarget(int width, int height) {
    return new FlixelGpuRenderTarget(this, width, height);
  }

  GpuTexture createRawTexture(int width, int height, GpuTextureFormat format, int levels, long extraUsage) {
    synchronized (gpuLock) {
      textureDescriptor.reset();
      textureDescriptor.label = "flixel texture";
      textureDescriptor.usage = GpuTextureUsage.TEXTURE_BINDING | GpuTextureUsage.COPY_DST | extraUsage;
      textureDescriptor.dimension = GpuTextureDimension.D2;
      textureDescriptor.size.width = width;
      textureDescriptor.size.height = height;
      textureDescriptor.size.depthOrArrayLayers = 1;
      textureDescriptor.format = format;
      textureDescriptor.mipLevelCount = levels;
      return device.createTexture(textureDescriptor);
    }
  }

  void writeLevel(GpuTexture target, FlixelGpuCompressedImage image, int level) {
    FlixelGpuBlockFormat format = image.getFormat();
    // Every level is copied as whole blocks, so the copy covers the level's padded size.
    int blocksWide = format.blocks(image.getWidth() >> level);
    int blocksHigh = format.blocks(image.getHeight() >> level);
    ByteBuffer data = image.getData();
    synchronized (gpuLock) {
      data.limit(image.getOffset(level) + image.getSize(level)).position(image.getOffset(level));
      copyDestination.reset();
      copyDestination.texture = target;
      copyDestination.mipLevel = level;
      copyDestination.aspect = GpuTextureAspect.ALL;
      copyLayout.reset();
      copyLayout.bytesPerRow = blocksWide * format.getBlockBytes();
      copyLayout.rowsPerImage = blocksHigh;
      copyExtent.width = blocksWide * format.getBlockSize();
      copyExtent.height = blocksHigh * format.getBlockSize();
      copyExtent.depthOrArrayLayers = 1;
      queue.writeTexture(copyDestination, data, copyLayout, copyExtent);
      data.clear();
    }
  }

  void writePixels(GpuTexture target, int x, int y, int width, int height, ByteBuffer pixels) {
    synchronized (gpuLock) {
      pixels.position(0).limit(width * height * 4);
      copyDestination.reset();
      copyDestination.texture = target;
      copyDestination.origin.x = x;
      copyDestination.origin.y = y;
      copyDestination.aspect = GpuTextureAspect.ALL;
      copyLayout.reset();
      copyLayout.bytesPerRow = width * 4;
      copyLayout.rowsPerImage = height;
      copyExtent.width = width;
      copyExtent.height = height;
      copyExtent.depthOrArrayLayers = 1;
      queue.writeTexture(copyDestination, pixels, copyLayout, copyExtent);
    }
  }

  GpuBindGroup createTextureGroup(GpuTextureView view, boolean smooth) {
    synchronized (gpuLock) {
      return pipelines.createTextureGroup(uniformRing.getBuffer(), view, smooth);
    }
  }

  long registerTexture(FlixelGpuTexture texture) {
    synchronized (gpuLock) {
      long id = nextTextureId++;
      textures.put(id, texture);
      stats.textureBytes += texture.getByteSize();
      return id;
    }
  }

  void unregisterTexture(FlixelGpuTexture texture) {
    synchronized (gpuLock) {
      textures.remove(texture.getId());
      stats.textureBytes -= texture.getByteSize();
    }
  }

  /**
   * Looks up a live texture by the id {@link FlixelTexture#getHandle()} returned.
   *
   * @param id The texture id.
   * @return The texture, or {@code null} when it was destroyed or never existed.
   */
  @Nullable
  public FlixelGpuTexture getTexture(long id) {
    synchronized (gpuLock) {
      return textures.get(id);
    }
  }

  void pushTarget(@NotNull FlixelGpuRenderTarget target) {
    if (!frameActive || depth >= MAX_LEVELS) {
      return;
    }
    endPass();
    int level = depth++;
    FlixelGpuTexture tex = target.getGpuTexture();
    initLevel(level, tex.getView(), GpuTextureFormat.RGBA8_UNORM, tex.getWidth(), tex.getHeight(), target);
    setClear(level, 0f, 0f, 0f, 0f);
  }

  void popTarget() {
    if (!frameActive || depth <= 1) {
      return;
    }
    finishLevel(depth - 1);
    endPass();
    depth--;
    stateDirty = true;
  }

  @Override
  public void setRenderResolution(int width, int height) {
    if (width < 1 || height < 1) {
      clearRenderResolution();
      return;
    }
    renderWidth = width;
    renderHeight = height;
    renderResolutionEnabled = true;
    if (!windowedRenderResolution && !Flixel.window.isFullscreen()) {
      Flixel.window.setSize(width, height);
    }
  }

  @Override
  public void clearRenderResolution() {
    renderResolutionEnabled = false;
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
  public void setRenderScale(float scale) {
    renderScale = Math.max(MIN_RENDER_SCALE, Math.min(MAX_RENDER_SCALE, scale));
  }

  @Override
  public float getRenderScale() {
    return renderScale;
  }

  @Override
  public void setRenderSmooth(boolean smooth) {
    renderSmooth = smooth;
    if (sceneTarget != null) {
      sceneTarget.getTexture().setSmooth(smooth);
    }
  }

  @Override
  public boolean isRenderSmooth() {
    return renderSmooth;
  }

  @Override
  public int getSceneWidth() {
    return sceneSize(true);
  }

  @Override
  public int getSceneHeight() {
    return sceneSize(false);
  }

  /**
   * Returns one side of the surface the scene is drawn into this frame.
   *
   * <p>In fullscreen with a render resolution set, the scene is drawn at that resolution and stretched to the
   * monitor. Otherwise (windowed, where the window already is the resolution, or no resolution set) it starts
   * from the back buffer. A platform that cannot resize its own window turns on
   * {@link #setWindowedRenderResolution(boolean)}, and then a set resolution always applies. The render scale is
   * applied on top of either.
   */
  private int sceneSize(boolean horizontal) {
    boolean fixed = renderResolutionEnabled && (windowedRenderResolution || Flixel.window.isFullscreen());
    int base;
    if (horizontal) {
      base = fixed ? renderWidth : backBufferWidth;
    } else {
      base = fixed ? renderHeight : backBufferHeight;
    }
    return Math.max(1, Math.round(base * renderScale));
  }

  @Override
  public void beginScene() {
    if (!frameActive) {
      return;
    }
    int width = sceneSize(true);
    int height = sceneSize(false);
    if (width == backBufferWidth && height == backBufferHeight) {
      disposeSceneTarget();
      return;
    }
    if (width != sceneWidth || height != sceneHeight) {
      sceneWidth = width;
      sceneHeight = height;
      sceneTargetDirty = true;
    }
    ensureSceneTarget();
    endPass();
    copyLevel(0, SPARE);
    FlixelGpuTexture tex = sceneTarget.getGpuTexture();
    initLevel(0, tex.getView(), GpuTextureFormat.RGBA8_UNORM, sceneWidth, sceneHeight, sceneTarget);
    setClear(0, 0f, 0f, 0f, 0f);
    sceneActive = true;
    float ww = Math.max(1, backBufferWidth);
    float wh = Math.max(1, backBufferHeight);
    compositeScale = Math.min(ww / sceneWidth, wh / sceneHeight);
    compositeOffsetX = (ww - sceneWidth * compositeScale) / 2f;
    compositeOffsetY = (wh - sceneHeight * compositeScale) / 2f;
  }

  @Override
  public void endScene() {
    if (!sceneActive) {
      return;
    }
    while (depth > 1) {
      popTarget();
    }
    finishLevel(0);
    endPass();
    sceneActive = false;
    FlixelGpuRenderTarget scene = sceneTarget;
    copyLevel(SPARE, 0);
    viewX[0] = 0;
    viewY[0] = 0;
    viewW[0] = Math.max(1, backBufferWidth);
    viewH[0] = Math.max(1, backBufferHeight);
    cutEnabled[0] = false;
    if (scene == null) {
      return;
    }
    float dstX = compositeOffsetX;
    float dstY = compositeOffsetY;
    float dstW = sceneWidth * compositeScale;
    float dstH = sceneHeight * compositeScale;
    compositeOrtho.setToOrtho2DYDown(0, 0, backBufferWidth, backBufferHeight, true);
    FlixelTexture texture = scene.getTexture();
    batch.setProjection(compositeOrtho);
    batch.setBlendMode(FlixelBlendMode.NONE);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.begin();
    batch.draw(texture, dstX, dstY, dstW, dstH);
    batch.end();
    batch.setBlendMode(FlixelBlendMode.NORMAL);
  }

  private void ensureSceneTarget() {
    if (sceneTarget != null && !sceneTargetDirty) {
      return;
    }
    disposeSceneTarget();
    sceneTarget = new FlixelGpuRenderTarget(this, sceneWidth, sceneHeight);
    sceneTarget.getTexture().setSmooth(renderSmooth);
    sceneTargetDirty = false;
  }

  private void disposeSceneTarget() {
    if (sceneTarget != null) {
      sceneTarget.destroy();
      sceneTarget = null;
    }
  }

  @Override
  public void clear(float r, float g, float b, float a) {
    if (!frameActive) {
      return;
    }
    int level = depth - 1;
    if (passOpen && passLevel == level) {
      endPass();
    }
    setClear(level, r, g, b, a);
  }

  @Override
  public boolean fillViewOpaque(float r, float g, float b, float a) {
    if (!frameActive) {
      return false;
    }
    int level = depth - 1;
    if (viewX[level] > 0 || viewY[level] > 0 || viewW[level] < levelWidth[level]
        || viewH[level] < levelHeight[level]) {
      return false;
    }
    clear(r, g, b, a);
    return true;
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    if (!frameActive) {
      return;
    }
    int level = depth - 1;
    if (sceneActive) {
      int rx = Math.round((x - compositeOffsetX) / compositeScale);
      int ry = Math.round((y - compositeOffsetY) / compositeScale);
      cutX[level] = rx;
      cutW[level] = Math.max(1, Math.round(width / compositeScale));
      cutH[level] = Math.max(1, Math.round(height / compositeScale));
      cutY[level] = sceneHeight - ry - cutH[level];
    } else {
      cutX[level] = x;
      cutW[level] = Math.max(1, width);
      cutH[level] = Math.max(1, height);
      cutY[level] = backBufferHeight - y - cutH[level];
    }
    cutEnabled[level] = true;
    stateDirty = true;
  }

  @Override
  public void clearScissor() {
    if (!frameActive) {
      return;
    }
    cutEnabled[depth - 1] = false;
    stateDirty = true;
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    if (!frameActive) {
      return;
    }
    int level = depth - 1;
    if (sceneActive) {
      viewX[level] = Math.round((x - compositeOffsetX) / compositeScale);
      viewY[level] = Math.round((y - compositeOffsetY) / compositeScale);
      viewW[level] = Math.max(1, Math.round(width / compositeScale));
      viewH[level] = Math.max(1, Math.round(height / compositeScale));
    } else {
      viewX[level] = x;
      viewY[level] = y;
      viewW[level] = Math.max(1, width);
      viewH[level] = Math.max(1, height);
    }
    stateDirty = true;
  }

  @Override
  public int getBackBufferWidth() {
    return backBufferWidth;
  }

  @Override
  public int getBackBufferHeight() {
    return backBufferHeight;
  }

  /**
   * Forces the whole back buffer's alpha channel to {@code 1} (fully opaque) without touching the color
   * channels.
   *
   * <p>Called every frame when a transparent framebuffer was requested but transparency is currently off.
   * Without it, a semi-transparent sprite would leave alpha below {@code 1} in the back buffer and let the
   * desktop show through. A full-screen quad is drawn with a blend that keeps the destination color and writes
   * the source alpha.
   */
  @Override
  public void forceOpaqueAlpha() {
    if (!frameActive || sceneActive || opaqueTexture == null || depth != 1 || !transparentFramebuffer) {
      return;
    }
    viewX[0] = 0;
    viewY[0] = 0;
    viewW[0] = backBufferWidth;
    viewH[0] = backBufferHeight;
    cutEnabled[0] = false;
    stateDirty = true;
    float w = backBufferWidth;
    float h = backBufferHeight;
    compositeOrtho.setToOrtho2DYDown(0, 0, w, h, true);
    float white = Float.intBitsToFloat(0xFFFFFFFF);
    putVertex(opaqueVertices, 0, 0f, 0f, 0f, 0f, white);
    putVertex(opaqueVertices, 1, w, 0f, 1f, 0f, white);
    putVertex(opaqueVertices, 2, w, h, 1f, 1f, white);
    putVertex(opaqueVertices, 3, 0f, h, 0f, 1f, white);
    submitQuads(opaqueVertices, 1, opaqueTexture, FlixelGpuBlend.FORCE_ALPHA, null, compositeOrtho, identity);
  }

  private static void putVertex(float[] dst, int index, float x, float y, float u, float v, float color) {
    int at = index * 5;
    dst[at] = x;
    dst[at + 1] = y;
    dst[at + 2] = u;
    dst[at + 3] = v;
    dst[at + 4] = color;
  }

  /**
   * Records one batch of quads into the current render pass.
   *
   * @param verts Interleaved vertex floats (x, y, u, v, packed color) for {@code quadCount} quads.
   * @param quadCount The number of quads to draw.
   * @param texture The texture to draw with, or {@code null} to skip the draw.
   * @param blend A blend id from {@link FlixelGpuBlend}.
   * @param shader The custom shader, or {@code null} for the sprite shader.
   * @param projection The view-projection matrix.
   * @param transform The model transform applied before projection.
   */
  void submitQuads(float @NotNull [] verts, int quadCount, @Nullable FlixelGpuTexture texture, int blend,
      @Nullable FlixelShader shader, @NotNull FlixelMatrix projection, @NotNull FlixelMatrix transform) {
    if (!frameActive || texture == null || texture.isDestroyed() || quadCount <= 0) {
      return;
    }
    FlixelGpuShaderProgram program = resolveProgram(shader);
    int level = depth - 1;
    GpuRenderPipeline gpuPipeline;
    synchronized (gpuLock) {
      gpuPipeline = pipelines.get(blend, levelFormat[level], program);
    }
    if (gpuPipeline == null) {
      return;
    }
    combined.set(projection);
    combined.mul(transform);
    System.arraycopy(combined.val, 0, globals, 0, 16);
    globals[16] = texture.getGpuWidth();
    globals[17] = texture.getGpuHeight();
    globals[18] = 1f / Math.max(1, texture.getGpuWidth());
    globals[19] = 1f / Math.max(1, texture.getGpuHeight());

    int floatCount = quadCount * FlixelGpuBatch.floatsPerQuad();
    long vertexOffset = -1L;
    long globalsOffset = -1L;
    int shaderOffset = -1;
    for (int attempt = 0; attempt < 2; attempt++) {
      vertexOffset = vertexRing.write(queue, verts, floatCount, 4);
      globalsOffset = uniformRing.write(queue, globals, GLOBALS_FLOATS, uniformAlignment);
      shaderOffset = program.uniformOffset(this);
      if (vertexOffset >= 0L && globalsOffset >= 0L && shaderOffset >= 0) {
        break;
      }
      if (attempt == 0) {
        restartCommands();
      }
    }
    if (vertexOffset < 0L || globalsOffset < 0L || shaderOffset < 0) {
      if (!ringWarned) {
        ringWarned = true;
        LOG.warn("A single draw is larger than the GPU ring buffers; it was skipped.");
      }
      return;
    }

    ensurePass();
    applyState();
    if (gpuPipeline != lastPipeline) {
      pass.setPipeline(gpuPipeline);
      lastPipeline = gpuPipeline;
    }
    dynamicOffset[0] = (int) globalsOffset;
    pass.setBindGroup(0, texture.bindGroup(), dynamicOffset, 1);
    GpuBindGroup shaderGroup = program.getBindGroup();
    if (shaderGroup != null) {
      dynamicOffset[0] = shaderOffset;
      pass.setBindGroup(1, shaderGroup, dynamicOffset, 1);
    }
    pass.setVertexBuffer(0, vertexRing.getBuffer(), vertexOffset, (long) quadCount * 80L);
    pass.setIndexBuffer(quadIndices, GpuIndexFormat.UINT16, 0L, (long) quadCount * 12L);
    pass.drawIndexed(quadCount * FlixelGpuBatch.indicesPerQuad(), 1, 0, 0, 0);
    stats.draws++;
    stats.triangles += quadCount * 2;
    stats.vertices += quadCount * 4;
  }

  /** Applies the viewport and scissor of the current level to the open pass when they changed. */
  private void applyState() {
    if (!stateDirty) {
      return;
    }
    stateDirty = false;
    int level = depth - 1;
    int w = levelWidth[level];
    int h = levelHeight[level];
    int vx = clamp(viewX[level], 0, w - 1);
    int vy = clamp(viewY[level], 0, h - 1);
    int vw = clamp(viewW[level], 1, w - vx);
    int vh = clamp(viewH[level], 1, h - vy);
    pass.setViewport(vx, vy, vw, vh, 0f, 1f);
    if (cutEnabled[level]) {
      int sx = clamp(cutX[level], 0, w - 1);
      int sy = clamp(cutY[level], 0, h - 1);
      pass.setScissorRect(sx, sy, clamp(cutW[level], 1, w - sx), clamp(cutH[level], 1, h - sy));
    } else {
      pass.setScissorRect(0, 0, w, h);
    }
  }

  private static int clamp(int value, int min, int max) {
    return value < min ? min : Math.min(value, Math.max(min, max));
  }

  @NotNull
  private FlixelGpuShaderProgram resolveProgram(@Nullable FlixelShader shader) {
    if (shader != null && shader.getProgram() instanceof FlixelGpuShaderProgram program && program.isValid()) {
      return program;
    }
    return spriteProgram;
  }

  /**
   * Starts a draw into the back buffer for the debug overlay.
   *
   * <p>Call after the game's frame has been drawn. It makes sure the scene has been composited and the back
   * buffer is the target, and resets the viewport and scissor to the whole window.
   *
   * @return {@code true} when overlay draws can follow, {@code false} when this frame is not being rendered.
   */
  public boolean beginOverlay() {
    if (!frameActive) {
      return false;
    }
    if (sceneActive) {
      endScene();
    }
    while (depth > 1) {
      popTarget();
    }
    viewX[0] = 0;
    viewY[0] = 0;
    viewW[0] = backBufferWidth;
    viewH[0] = backBufferHeight;
    cutEnabled[0] = false;
    stateDirty = true;
    return true;
  }

  /**
   * Copies vertices into the vertex ring for an overlay draw.
   *
   * @param data The vertex bytes, from position to limit. The remaining count must be a multiple of four.
   * @return The ring offset, or {@code -1} when the ring is full; call {@link #restartCommands()} and try again.
   */
  public long writeOverlayVertices(@NotNull ByteBuffer data) {
    return frameActive ? vertexRing.write(queue, data, 4) : -1L;
  }

  /**
   * Copies indices into the overlay index ring.
   *
   * @param data The index bytes, from position to limit. The remaining count must be a multiple of four.
   * @return The ring offset, or {@code -1} when the ring is full; call {@link #restartCommands()} and try again.
   */
  public long writeOverlayIndices(@NotNull ByteBuffer data) {
    return frameActive ? indexRing.write(queue, data, 4) : -1L;
  }

  /**
   * Draws indexed triangles in the sprite vertex layout into the back buffer, with the normal blend mode.
   *
   * @param texture The texture to sample.
   * @param vertexOffset The offset returned by {@link #writeOverlayVertices(ByteBuffer)}.
   * @param vertexBytes The byte size of the vertices.
   * @param indexOffset The offset returned by {@link #writeOverlayIndices(ByteBuffer)}.
   * @param indexBytes The byte size of the indices.
   * @param index32 Whether the indices are 32-bit instead of 16-bit.
   * @param firstIndex The first index to draw, counted in indices.
   * @param indexCount The number of indices to draw.
   * @param projection The matrix mapping the overlay's coordinates to clip space.
   * @param clipX The scissor rectangle's left edge in pixels.
   * @param clipY The scissor rectangle's top edge in pixels.
   * @param clipW The scissor rectangle's width in pixels.
   * @param clipH The scissor rectangle's height in pixels.
   */
  public void drawOverlay(@NotNull FlixelGpuTexture texture, long vertexOffset, long vertexBytes, long indexOffset,
      long indexBytes, boolean index32, int firstIndex, int indexCount, @NotNull FlixelMatrix projection, int clipX,
      int clipY, int clipW, int clipH) {
    if (!frameActive || texture.isDestroyed()) {
      return;
    }
    GpuRenderPipeline gpuPipeline = pipelines.get(FlixelGpuBlend.NORMAL, levelFormat[0], spriteProgram);
    if (gpuPipeline == null) {
      return;
    }
    System.arraycopy(projection.val, 0, globals, 0, 16);
    globals[16] = texture.getGpuWidth();
    globals[17] = texture.getGpuHeight();
    globals[18] = 1f / Math.max(1, texture.getGpuWidth());
    globals[19] = 1f / Math.max(1, texture.getGpuHeight());
    long globalsOffset = uniformRing.write(queue, globals, GLOBALS_FLOATS, uniformAlignment);
    if (globalsOffset < 0L) {
      return;
    }
    ensurePass();
    applyState();
    if (gpuPipeline != lastPipeline) {
      pass.setPipeline(gpuPipeline);
      lastPipeline = gpuPipeline;
    }
    dynamicOffset[0] = (int) globalsOffset;
    pass.setBindGroup(0, texture.bindGroup(), dynamicOffset, 1);
    pass.setVertexBuffer(0, vertexRing.getBuffer(), vertexOffset, vertexBytes);
    pass.setIndexBuffer(indexRing.getBuffer(), index32 ? GpuIndexFormat.UINT32 : GpuIndexFormat.UINT16, indexOffset,
        indexBytes);
    int w = levelWidth[0];
    int h = levelHeight[0];
    int sx = clamp(clipX, 0, w - 1);
    int sy = clamp(clipY, 0, h - 1);
    pass.setScissorRect(sx, sy, clamp(clipW, 1, w - sx), clamp(clipH, 1, h - sy));
    pass.drawIndexed(indexCount, 1, firstIndex, 0, 0);
    stateDirty = true;
    stats.draws++;
    stats.triangles += indexCount / 3;
  }

  @NotNull
  @Override
  public FlixelShaderProgram compileShaderProgram(@NotNull String name) {
    String path = "shaders/" + name + ".wgsl";
    // Read through the asset system, which knows where each platform keeps the game's files: the assets folder or
    // the packaged resources on desktop and Android, and the preloaded assets on the web.
    String source = null;
    if (Flixel.assets != null) {
      FlixelFile file = Flixel.assets.resolveFile(path);
      if (file != null && file.exists()) {
        source = file.readString();
      }
    }
    if (source == null) {
      LOG.error("Shader '{}' was not found. Put it at {} in the resources or the assets folder.", name, path);
      return FlixelUnsupportedShader.INSTANCE;
    }
    return compile(source, name);
  }

  @NotNull
  @Override
  public FlixelShaderProgram compileShaderSource(@NotNull String wgslSource) {
    return compile(wgslSource, "shader");
  }

  private FlixelShaderProgram compile(String source, String label) {
    if (device == null) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    FlixelGpuShaderProgram program;
    synchronized (gpuLock) {
      program = pipelines.compile(source, label, uniformRing.getBuffer(), eventPump, this);
    }
    return program != null ? program : FlixelUnsupportedShader.INSTANCE;
  }

  /**
   * Returns how many devices this renderer has been initialized with.
   *
   * <p>It goes up by one every time {@link #initialize} runs again after a device loss. Textures and shader programs
   * remember the value they were created with, so they can tell that their GPU objects belong to a dead device.
   *
   * @return The device generation, starting at 1 after the first initialization.
   */
  int getDeviceEpoch() {
    return deviceEpoch;
  }

  void forgetProgram(FlixelGpuShaderProgram program) {
    synchronized (gpuLock) {
      if (pipelines != null) {
        pipelines.forget(program.getId());
      }
    }
  }

  /**
   * Returns how far the rings have been rewound, so shaders know when their uploaded uniforms are gone.
   *
   * @return A number that changes whenever the rings are rewound.
   */
  long getRingEpoch() {
    return ringEpoch;
  }

  /**
   * Copies a shader's uniform bytes into the uniform ring.
   *
   * @param data The bytes.
   * @param size How many bytes to copy.
   * @return The offset to bind, or {@code -1} when the ring is full.
   */
  int uploadUniforms(byte[] data, int size) {
    int padded = (size + 3) & ~3;
    long at = uniformRing.write(queue, data, padded > data.length ? data.length : padded, uniformAlignment);
    return (int) at;
  }

  @NotNull
  @Override
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  @Override
  public boolean isVSync() {
    return vsync;
  }

  @Override
  public void setVSync(boolean enabled) {
    if (vsync != enabled) {
      vsync = enabled;
      surfaceDirty = true;
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
   * Gives the renderer a way to let pending GPU callbacks run while it waits for a shader to be validated.
   *
   * <p>The desktop passes {@code instance::processEvents}. The web leaves it unset, because the browser delivers
   * callbacks on its own, and shader errors then show up in the log a moment later.
   *
   * @param pump The pump, or {@code null}.
   */
  public void setEventPump(@Nullable Runnable pump) {
    this.eventPump = pump;
  }

  /**
   * Releases every GPU resource. The renderer cannot be used afterwards.
   */
  public void dispose() {
    if (device == null) {
      return;
    }
    frameActive = false;
    pipeline.dispose();
    disposeSceneTarget();
    if (opaqueTexture != null) {
      opaqueTexture.destroy();
      opaqueTexture = null;
    }
    if (spriteProgram != null) {
      spriteProgram.destroy();
      spriteProgram = null;
    }
    pipelines.release();
    quadIndices.release();
    vertexRing.release();
    uniformRing.release();
    indexRing.release();
    if (headlessView != null) {
      headlessView.release();
      headlessTexture.release();
      headlessView = null;
    }
    if (surfaceView != null && surface != null) {
      surfaceView.release();
      surfaceView = null;
    }
    if (pass != null) {
      pass.release();
    }
    if (encoder != null) {
      encoder.release();
    }
    if (commands != null) {
      commands.release();
    }
    if (surface != null) {
      surface.unconfigure();
      surface.release();
    }
    queue.release();
    device.destroy();
    device.release();
    adapter.release();
    device = null;
  }

  /**
   * Returns the frame counters the debug overlay shows.
   *
   * @return The live stats object.
   */
  @NotNull
  public FlixelGpuStats getStats() {
    return stats;
  }

  /**
   * Returns the description of the GPU in use.
   *
   * @return The adapter info, filled in by {@link #initialize}.
   */
  @NotNull
  public GpuAdapterInfo getAdapterInfo() {
    return adapterInfo;
  }

  /**
   * Returns the format of the back buffer.
   *
   * @return The surface format.
   */
  @NotNull
  public GpuTextureFormat getSurfaceFormat() {
    return surfaceFormat;
  }

  /**
   * Returns the device, for advanced use such as custom pipelines.
   *
   * @return The device, or {@code null} before {@link #initialize}.
   */
  @Nullable
  public GpuDevice getDevice() {
    return device;
  }

  /**
   * Returns the queue.
   *
   * @return The queue, or {@code null} before {@link #initialize}.
   */
  @Nullable
  public GpuQueue getQueue() {
    return queue;
  }

  /**
   * Returns whether a frame is being recorded, which is {@code false} when the surface could not give a back
   * buffer this frame.
   *
   * @return {@code true} between a successful {@link #beginFrame()} and {@link #endFrame()}.
   */
  public boolean isFrameActive() {
    return frameActive;
  }

  /**
   * Returns the image decoder.
   *
   * @return The decoder, or {@code null}.
   */
  @Nullable
  public FlixelImageDecoder getImageDecoder() {
    return decoder;
  }

  /**
   * Sets how encoded images are decoded.
   *
   * @param decoder The decoder.
   */
  public void setImageDecoder(@Nullable FlixelImageDecoder decoder) {
    this.decoder = decoder;
  }

  /**
   * Returns the Basis Universal transcoder used for supercompressed KTX2 files.
   *
   * @return The transcoder, or {@code null} when the platform has none.
   */
  @Nullable
  public FlixelBasisTranscoder getBasisTranscoder() {
    return basisTranscoder;
  }

  /**
   * Sets the Basis Universal transcoder used for supercompressed KTX2 files.
   *
   * <p>A platform that ships the native transcoder installs it here once, before any asset loads. Without one,
   * KTX2 files in a GPU format still load, but Basis Universal files are rejected.
   *
   * @param basisTranscoder The transcoder, or {@code null} for none.
   */
  public void setBasisTranscoder(@Nullable FlixelBasisTranscoder basisTranscoder) {
    this.basisTranscoder = basisTranscoder;
  }

  /**
   * Returns whether the once-a-second statistics log is on ({@code -Dflixel.render.stats=log}).
   *
   * @return {@code true} when stats logging is enabled.
   */
  public boolean isStatsEnabled() {
    return statsEnabled;
  }

  /**
   * Turns the once-a-second statistics log on or off.
   *
   * @param statsEnabled Whether to log.
   */
  public void setStatsEnabled(boolean statsEnabled) {
    this.statsEnabled = statsEnabled;
  }

  /**
   * Returns whether a set render resolution also applies outside fullscreen.
   *
   * @return {@code true} if the scene is always drawn at the render resolution.
   * @see #setWindowedRenderResolution(boolean)
   */
  public boolean isWindowedRenderResolution() {
    return windowedRenderResolution;
  }

  /**
   * Makes a set render resolution apply outside fullscreen too.
   *
   * <p>By default a render resolution only takes effect in fullscreen: in a window, the window itself is resized to
   * that resolution instead. A platform whose window the game cannot resize (such as a browser canvas, which the
   * page sizes) turns this on, so the scene is drawn at the render resolution and stretched to fit, like a
   * fullscreen monitor.
   *
   * @param windowedRenderResolution Whether a render resolution always applies.
   */
  public void setWindowedRenderResolution(boolean windowedRenderResolution) {
    this.windowedRenderResolution = windowedRenderResolution;
  }

  /** Logs the frame statistics once per second when {@code flixel.render.stats} is on. */
  private static final class FlixelStatsLog {

    private long nextLog;

    @SuppressWarnings("DefaultLocale")
    void tick(long now, FlixelGpuStats stats, FlixelGraphicsApi api) {
      if (now < nextLog) {
        return;
      }
      nextLog = now + 1_000_000_000L;
      LOG.debug("{}: cpu {} ms, present {} ms, {} draws, {} triangles, {} passes, {} submits, vertex ring {} KB,"
          + " uniform ring {} KB, {} pipelines.", api, String.format("%.2f", stats.cpuMs),
          String.format("%.2f", stats.presentMs), stats.draws, stats.triangles, stats.passes, stats.submits,
          stats.vertexBytes / 1024, stats.uniformBytes / 1024, stats.pipelines);
    }
  }
}
