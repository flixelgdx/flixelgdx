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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.webgl.WebGLRenderingContext;

import java.nio.ByteBuffer;

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

  @NotNull
  private final FlixelArray<FlixelDisplayMode> displayModes = new FlixelArray<>();

  private WebGLRenderingContext gl;
  private FlixelWebGlBatch batch;

  private int backBufferWidth;
  private int backBufferHeight;

  private boolean webGpuAvailable;

  /**
   * Creates the WebGL2 context on the given canvas and builds the sprite batch.
   *
   * @param canvas The canvas to render into.
   */
  public void initialize(HTMLCanvasElement canvas) {
    webGpuAvailable = webGpuSupported();
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
  public FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  /**
   * Returns whether the browser advertises WebGPU support. Rendering still uses WebGL2; this is a
   * capability probe for the framework and future work.
   *
   * @return {@code true} if {@code navigator.gpu} is present.
   */
  public boolean isWebGpuAvailable() {
    return webGpuAvailable;
  }

  @JSBody(params = "canvas", script = "return canvas.getContext('webgl2');")
  private static native WebGLRenderingContext getWebGl2(HTMLCanvasElement canvas);

  @JSBody(script = "return !!(navigator.gpu);")
  private static native boolean webGpuSupported();
}
