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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGlobalShaderPipeline;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedRenderTarget;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The web graphics backend, rendering through WebGL2.
 *
 * <p>The browser exposes two GPU APIs: the newer WebGPU and the widely supported WebGL. This
 * backend renders through WebGL2, which every current browser ships, so games run on the broadest
 * set of machines. It still probes for WebGPU through {@code navigator.gpu} and remembers whether it
 * is present so the framework and future work can tell what the machine supports, but drawing itself
 * goes through the WebGL2 sprite {@link FlixelWebGlBatch}.
 *
 * <p>The context is created in {@link #initialize(HTMLCanvasElement)} once the runner has the
 * canvas. From then on {@link #getBatch()} hands game code the shared batch, {@link #clear} wipes
 * the frame to a color, and {@link #createTexture} uploads pixels to the GPU. The WebGL2 context is
 * accessed through the {@link WebGLRenderingContext} type because the methods this batch uses are
 * shared with WebGL1; the {@code #version 300 es} shaders still require the underlying context to be
 * WebGL2, which is what is requested.
 *
 * <p>This backend fully supports both the global post-processing shader pipeline
 * ({@link #getGlobalShaderPipeline()}) and fixed render resolution
 * ({@link #setRenderResolution(int, int, boolean)}).
 * Both features use off-screen {@link FlixelWebGlRenderTarget} surfaces so games look the same on
 * every platform without any platform-specific game code.
 *
 * @see FlixelGlobalShaderPipeline
 */
public class FlixelHtml5Graphics implements FlixelGraphicsManager {

  /**
   * Weight applied to each new frame-time sample when smoothing the frame rate. A small value keeps
   * the reported FPS steady instead of jittering with every frame.
   */
  private static final double FPS_SMOOTHING = 0.1;

  private static final int HEADER_SIZE = 12;

  /** Timestamp of the previous frame in milliseconds, or a negative value before the first frame. */
  private double lastFrameTime = -1.0;

  /** Exponentially smoothed frame rate, updated once per frame in {@link #beginFrame()}. */
  private double averageFps;

  @NotNull
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();

  @NotNull
  private final FlixelArray<FlixelWebGlRenderTarget> targetStack = new FlixelArray<>();

  @NotNull
  private final FlixelGlobalShaderPipeline pipeline = new FlixelGlobalShaderPipeline();

  /** Reused ortho matrix for the final scene upscale blit, rebuilt each composite to match the window. */
  @NotNull
  private final FlixelMatrix compositeOrtho = new FlixelMatrix();

  private WebGLRenderingContext gl;
  private FlixelWebGlBatch batch;

  /** Fixed-resolution scene surface the whole frame renders into when a render resolution is set. */
  @Nullable
  private FlixelWebGlRenderTarget sceneTarget;

  private int backBufferWidth;
  private int backBufferHeight;

  /** Fixed render width in pixels, active only when {@link #renderResolutionEnabled} is true. */
  private int renderWidth;

  /** Fixed render height in pixels, active only when {@link #renderResolutionEnabled} is true. */
  private int renderHeight;

  /** Scale applied to the scene surface when stretching it to fill the window. */
  private float compositeScale = 1f;

  /** Horizontal offset in window pixels where the scaled scene surface begins (letterbox left gap). */
  private float compositeOffsetX;

  /** Vertical offset in window pixels where the scaled scene surface begins (letterbox bottom gap). */
  private float compositeOffsetY;

  /** Whether a fixed render resolution is active. See {@link #setRenderResolution(int, int, boolean)}. */
  private boolean renderResolutionEnabled;

  /** Whether the scene surface is stretched with linear filtering or nearest-neighbor. */
  private boolean renderSmooth = true;

  /** True between {@link #beginScene()} and {@link #endScene()}, so viewport remapping is active. */
  private boolean sceneActive;

  /** Set when the render size or filter changed, so the scene surface is rebuilt on the next frame. */
  private boolean sceneTargetDirty;

  /**
   * Creates the WebGL2 context on the given canvas and builds the sprite batch.
   *
   * @param canvas The canvas to render into.
   */
  public void initialize(HTMLCanvasElement canvas) {
    gl = getWebGl2(canvas);
    if (gl == null) {
      throw new IllegalStateException("WebGL2 is not available in this browser.");
    }
    backBufferWidth = canvas.getWidth();
    backBufferHeight = canvas.getHeight();

    gl.disable(WebGLRenderingContext.DEPTH_TEST);
    gl.enable(WebGLRenderingContext.BLEND);
    gl.viewport(0, 0, backBufferWidth, backBufferHeight);

    batch = new FlixelWebGlBatch(gl);
  }

  /**
   * Records a new canvas size and updates the WebGL viewport to match.
   *
   * <p>When a render resolution is set the scene target is sized to the fixed resolution, so no
   * rebuild is needed here; only the letterbox math updates on the next {@link #beginScene()} call.
   * The global shader pipeline FBOs are resized via {@code Flixel.graphics.resizeGlobalShaders()},
   * which {@code FlixelGame.resize(...)} calls after this method returns.
   *
   * @param width The new width in pixels.
   * @param height The new height in pixels.
   */
  public void onResized(int width, int height) {
    backBufferWidth = width;
    backBufferHeight = height;
    if (gl != null) {
      gl.viewport(0, 0, width, height);
    }
  }

  /**
   * Samples the frame rate at the start of each frame. The runner calls this once per
   * {@code requestAnimationFrame} tick, so the delta between calls is one displayed frame; the
   * result is smoothed so the readout in the debug overlay stays steady.
   */
  @Override
  public void beginFrame() {
    double now = nowMillis();
    if (lastFrameTime >= 0.0) {
      double delta = now - lastFrameTime;
      if (delta > 0.0) {
        double instantFps = 1000.0 / delta;
        averageFps = (averageFps * (1.0 - FPS_SMOOTHING)) + (instantFps * FPS_SMOOTHING);
      }
    }
    lastFrameTime = now;
  }

  @Override
  @NotNull
  public FlixelGlobalShaderPipeline getGlobalShaderPipeline() {
    return pipeline;
  }

  @Override
  @NotNull
  public FlixelGraphicsApi getApi() {
    return FlixelGraphicsApi.WebGL;
  }

  @Override
  public int getFps() {
    return (int) averageFps;
  }

  @Override
  @NotNull
  public FlixelBatch getBatch() {
    return batch;
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
    // The surface is built lazily on the next beginScene() call so this is safe to call before
    // the context is ready (for example from a game constructor).
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
    // Work out how the fixed surface is stretched onto the current window (a FIT letterbox).
    // Both the per-camera viewport remap and the final blit derive from this.
    float ww = Math.max(1, backBufferWidth);
    float wh = Math.max(1, backBufferHeight);
    compositeScale = Math.min(ww / renderWidth, wh / renderHeight);
    compositeOffsetX = (ww - renderWidth * compositeScale) / 2f;
    compositeOffsetY = (wh - renderHeight * compositeScale) / 2f;
    // Redirect all subsequent draws into the scene surface and clear it.
    sceneTarget.begin();
    gl.clearColor(0f, 0f, 0f, 0f);
    gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
  }

  @Override
  public void endScene() {
    if (!sceneActive) {
      return;
    }
    sceneActive = false;
    if (sceneTarget == null || batch == null) {
      return;
    }
    // Return drawing to the screen.
    sceneTarget.end();

    float dstX = compositeOffsetX;
    float dstY = compositeOffsetY;
    float dstW = renderWidth * compositeScale;
    float dstH = renderHeight * compositeScale;

    // Restore the full back buffer viewport for the blit pass.
    clearScissor();
    gl.viewport(0, 0, Math.max(1, backBufferWidth), Math.max(1, backBufferHeight));
    compositeOrtho.setToOrtho2DYDown(0, 0, backBufferWidth, backBufferHeight, isDepthZeroToOne());

    batch.setProjection(compositeOrtho);
    batch.setBlendMode(FlixelBlendMode.NONE);
    batch.setColor(1f, 1f, 1f, 1f);
    batch.begin();
    // WebGL render targets are stored bottom-up, so flip the vertical UVs when blitting.
    if (sceneTarget.isFlipped()) {
      batch.draw(sceneTarget.getTexture(), dstX, dstY, dstW, dstH, 0f, 1f, 1f, 0f);
    } else {
      batch.draw(sceneTarget.getTexture(), dstX, dstY, dstW, dstH);
    }
    batch.end();
    batch.setBlendMode(FlixelBlendMode.NORMAL);
  }

  @Override
  public void clear(float r, float g, float b, float a) {
    if (gl != null) {
      gl.clearColor(r, g, b, a);
      gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
    }
  }

  @Override
  public void setScissor(int x, int y, int width, int height) {
    if (gl == null) {
      return;
    }
    int sx;
    int sy;
    int sw;
    int sh;
    if (sceneActive) {
      // Clip rects arrive in window pixels (bottom-left origin). Undo the composite stretch to
      // map them into the fixed render surface so sprites clip correctly inside the scene FBO.
      sx = Math.round((x - compositeOffsetX) / compositeScale);
      sy = Math.round((y - compositeOffsetY) / compositeScale);
      sw = Math.max(1, Math.round(width / compositeScale));
      sh = Math.max(1, Math.round(height / compositeScale));
    } else {
      sx = x;
      sy = y;
      sw = Math.max(1, width);
      sh = Math.max(1, height);
    }
    gl.enable(WebGLRenderingContext.SCISSOR_TEST);
    gl.scissor(sx, sy, sw, sh);
  }

  @Override
  public void clearScissor() {
    if (gl != null) {
      gl.disable(WebGLRenderingContext.SCISSOR_TEST);
    }
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    if (gl == null) {
      return;
    }
    if (sceneActive) {
      // Cameras lay out their viewport in window pixels, but the scene surface is a different
      // (fixed) size, so undo the composite stretch to land in render pixels.
      int rx = Math.round((x - compositeOffsetX) / compositeScale);
      int ry = Math.round((y - compositeOffsetY) / compositeScale);
      int rw = Math.max(1, Math.round(width / compositeScale));
      int rh = Math.max(1, Math.round(height / compositeScale));
      gl.viewport(rx, ry, rw, rh);
    } else {
      gl.viewport(x, y, width, height);
    }
  }

  @Override
  public int getBackBufferWidth() {
    return backBufferWidth;
  }

  @Override
  public int getBackBufferHeight() {
    return backBufferHeight;
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(int width, int height) {
    return new FlixelWebGlTexture(gl, width, height, false);
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return new FlixelWebGlTexture(gl, width, height, rgba, false);
  }

  /**
   * Returns the WebGL rendering context used by this backend.
   *
   * <p>Extensions that need to call WebGL directly (for example, a video backend that uploads
   * frames through {@code texSubImage2D} without a Java-side copy) can retrieve the context here
   * by casting {@link Flixel#graphics} to {@code FlixelHtml5Graphics}.
   *
   * @return The WebGL2 rendering context, or {@code null} before {@link #initialize} is called.
   */
  @Nullable
  public WebGLRenderingContext getGl() {
    return gl;
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(@NotNull FlixelImage image) {
    return new FlixelWebGlTexture(gl, image.getWidth(), image.getHeight(), image.getPixels(), false);
  }

  @Override
  @NotNull
  public FlixelRenderTarget createRenderTarget(int width, int height) {
    if (gl == null) {
      return FlixelUnsupportedRenderTarget.INSTANCE;
    }
    return new FlixelWebGlRenderTarget(this, gl, width, height);
  }

  @Override
  @NotNull
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  /**
   * Redirects drawing into a render target, remembering the previous surface so it can be restored.
   *
   * @param target The target to make active.
   */
  void pushRenderTarget(@NotNull FlixelWebGlRenderTarget target) {
    targetStack.add(target);
    bindTarget(target);
  }

  /** Ends the innermost render target, returning drawing to the enclosing target or the screen. */
  void popRenderTarget() {
    if (targetStack.getSize() > 0) {
      targetStack.pop();
    }
    restoreActiveFramebuffer();
  }

  /** Binds whichever surface is currently on top of the render-target stack, or the screen if none. */
  void restoreActiveFramebuffer() {
    if (targetStack.getSize() > 0) {
      bindTarget(targetStack.get(targetStack.getSize() - 1));
    } else {
      gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);
      gl.viewport(0, 0, backBufferWidth, backBufferHeight);
    }
  }

  /** Binds a render target's framebuffer and matches the viewport to its size. */
  private void bindTarget(@NotNull FlixelWebGlRenderTarget target) {
    gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, target.getFramebuffer());
    gl.viewport(0, 0, target.getWidth(), target.getHeight());
  }

  @Override
  @Nullable
  public FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    // A browser can only decode an encoded image asynchronously, so it cannot happen here in a
    // synchronous call. FlixelHtml5AssetManager starts a createImageBitmap Promise for each image
    // queued through the asset manager, and once the Promise resolves it stores the raw RGBA pixels
    // behind a small "FLXI" header (magic, then width and height as little-endian 32-bit integers).
    // This method just unpacks that, which needs no real decoding. Bytes without the header are a
    // genuinely encoded image (raw PNG, JPEG, etc.) that the web backend cannot decode synchronously,
    // so it returns null.
    int base = encoded.position();
    if (encoded.remaining() < HEADER_SIZE
        || encoded.get(base) != 'F' || encoded.get(base + 1) != 'L'
        || encoded.get(base + 2) != 'X' || encoded.get(base + 3) != 'I') {
      return null;
    }
    int width = readLittleEndianInt(encoded, base + 4);
    int height = readLittleEndianInt(encoded, base + 8);
    if (width <= 0 || height <= 0) {
      return null;
    }
    int pixelBytes = width * height * 4;
    if (encoded.remaining() < HEADER_SIZE + pixelBytes) {
      return null;
    }
    // Slice the existing heap buffer rather than allocating a direct buffer. ByteBuffer.allocateDirect
    // is not supported under TeaVM's wasmGC target (which uses the browser GC for all memory) and
    // throws OutOfMemoryError. A heap slice avoids an extra copy too, since toView() in
    // FlixelWebGlTexture already copies the bytes out to a byte[] for WebGL.
    ByteBuffer src = encoded.duplicate();
    src.position(base + HEADER_SIZE);
    src.limit(base + HEADER_SIZE + pixelBytes);
    return new FlixelImage(width, height, src.slice().order(ByteOrder.nativeOrder()));
  }

  @Override
  @NotNull
  public FlixelShaderProgram compileShaderSource(@NotNull String vertexSource, @NotNull String fragmentSource) {
    if (gl == null) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    WebGLProgram program = FlixelWebGlPrograms.build(gl, vertexSource, fragmentSource);
    return program != null ? new FlixelWebGlShaderProgram(gl, program) : FlixelUnsupportedShader.INSTANCE;
  }

  @Override
  @NotNull
  public FlixelShaderProgram compileShaderProgram(@NotNull String name) {
    // The web variant is raw GLSL text, not the bgfx bytecode the other backends load. A browser
    // cannot read classpath resources, so the build plugin copies these ESSL files into the web
    // assets and the preloader caches them; here they are read straight from that warm cache.
    FlixelFile vertexFile = Flixel.files.internal("shaders/" + name + "/essl/vs.glsl");
    FlixelFile fragmentFile = Flixel.files.internal("shaders/" + name + "/essl/fs.glsl");
    if (!vertexFile.exists() || !fragmentFile.exists()) {
      return FlixelUnsupportedShader.INSTANCE;
    }
    return compileShaderSource(vertexFile.readString(), fragmentFile.readString());
  }

  /**
   * Reads a little-endian 32-bit integer from a byte buffer at an absolute offset.
   *
   * @param buffer The buffer to read from.
   * @param offset The absolute byte offset of the value.
   * @return The decoded integer.
   */
  private static int readLittleEndianInt(ByteBuffer buffer, int offset) {
    return (buffer.get(offset) & 0xFF)
        | ((buffer.get(offset + 1) & 0xFF) << 8)
        | ((buffer.get(offset + 2) & 0xFF) << 16)
        | ((buffer.get(offset + 3) & 0xFF) << 24);
  }

  /** Rebuilds the scene surface when the render size or filter changed, then leaves it ready to use. */
  private void ensureSceneTarget() {
    if (sceneTarget != null && !sceneTargetDirty) {
      return;
    }
    disposeSceneTarget();
    if (gl == null) {
      return;
    }
    sceneTarget = new FlixelWebGlRenderTarget(this, gl, Math.max(1, renderWidth), Math.max(1, renderHeight));
    sceneTarget.getTexture().setSmooth(renderSmooth);
    sceneTargetDirty = false;
  }

  private void disposeSceneTarget() {
    if (sceneTarget != null) {
      sceneTarget.destroy();
      sceneTarget = null;
    }
  }

  @JSBody(params = "canvas", script = "return canvas.getContext('webgl2');")
  private static native WebGLRenderingContext getWebGl2(HTMLCanvasElement canvas);

  @JSBody(script = "return (window.performance && window.performance.now) ? window.performance.now() : Date.now();")
  private static native double nowMillis();
}
