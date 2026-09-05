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
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelShaderProgram;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.graphics.FlixelUnsupportedRenderTarget;
import org.flixelgdx.graphics.FlixelUnsupportedShader;
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
 */
public class FlixelHtml5Graphics implements FlixelGraphicsManager {

  private static final int HEADER_SIZE = 12;

  @NotNull
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();

  @NotNull
  private final FlixelArray<FlixelWebGlRenderTarget> targetStack = new FlixelArray<>();

  private WebGLRenderingContext gl;
  private FlixelWebGlBatch batch;

  private int backBufferWidth;
  private int backBufferHeight;

  private boolean continuousRendering = true;
  private boolean renderRequested;

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

  @Override
  @NotNull
  public FlixelGraphicsApi getApi() {
    return FlixelGraphicsApi.WebGL;
  }

  @Override
  @NotNull
  public FlixelBatch getBatch() {
    return batch;
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
    if (gl != null) {
      gl.enable(WebGLRenderingContext.SCISSOR_TEST);
      gl.scissor(x, y, Math.max(1, width), Math.max(1, height));
    }
  }

  @Override
  public void clearScissor() {
    if (gl != null) {
      gl.disable(WebGLRenderingContext.SCISSOR_TEST);
    }
  }

  @Override
  public void setViewport(int x, int y, int width, int height) {
    if (gl != null) {
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
  public FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return new FlixelWebGlTexture(gl, width, height, rgba, false);
  }

  @Override
  @NotNull
  public FlixelTexture createTexture(@NotNull FlixelImage image) {
    return new FlixelWebGlTexture(gl, image.width(), image.height(), image.pixels(), false);
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

  @Override
  public void setContinuousRendering(boolean continuous) {
    continuousRendering = continuous;
  }

  @Override
  public boolean isContinuousRendering() {
    return continuousRendering;
  }

  @Override
  public void requestRendering() {
    renderRequested = true;
  }

  @Override
  public boolean consumeRenderRequest() {
    if (renderRequested) {
      renderRequested = false;
      return true;
    }
    return false;
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

  @JSBody(params = "canvas", script = "return canvas.getContext('webgl2');")
  private static native WebGLRenderingContext getWebGl2(HTMLCanvasElement canvas);
}
