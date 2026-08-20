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

import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import java.nio.ByteBuffer;

/**
 * A GPU texture backed by a WebGL texture object.
 *
 * <p>The framework refers to a texture through the {@code long} handle from {@link #getHandle()},
 * which the batch uses only to tell one texture from another so it can flush when the bound texture
 * changes. WebGL identifies textures with an opaque object rather than a number, so this class keeps
 * that object internally and hands out its own increasing handle for identity, exposing the real
 * WebGL object to the batch through {@link #getGlTexture()}.
 */
public class FlixelWebGlTexture implements FlixelTexture {

  private static long nextHandle = 1L;

  private final long handle;

  private final WebGLRenderingContext gl;
  private final WebGLTexture texture;

  private final int width;
  private final int height;

  private boolean smooth;

  /**
   * Uploads RGBA pixels into a new WebGL texture.
   *
   * @param gl The rendering context.
   * @param width Texture width in pixels.
   * @param height Texture height in pixels.
   * @param rgba The tightly packed RGBA8888 pixels, four bytes per pixel.
   * @param smooth {@code true} for linear filtering, {@code false} for nearest.
   */
  public FlixelWebGlTexture(WebGLRenderingContext gl, int width, int height, ByteBuffer rgba, boolean smooth) {
    this.handle = nextHandle++;
    this.gl = gl;
    this.width = width;
    this.height = height;
    this.smooth = smooth;
    this.texture = gl.createTexture();

    gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
    gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA, width, height, 0,
        WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, toView(rgba));
    applyFilter();
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S,
        WebGLRenderingContext.CLAMP_TO_EDGE);
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T,
        WebGLRenderingContext.CLAMP_TO_EDGE);
  }

  @Override
  public long getHandle() {
    return handle;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public boolean isSmooth() {
    return smooth;
  }

  @Override
  public void setSmooth(boolean smooth) {
    this.smooth = smooth;
    gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
    applyFilter();
  }

  @Override
  public void update(int x, int y, FlixelImage image) {
    gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);
    gl.texSubImage2D(WebGLRenderingContext.TEXTURE_2D, 0, x, y, image.width(), image.height(),
        WebGLRenderingContext.RGBA, WebGLRenderingContext.UNSIGNED_BYTE, toView(image.pixels()));
  }

  @Override
  public void destroy() {
    gl.deleteTexture(texture);
  }

  /**
   * Returns the underlying WebGL texture object so the batch can bind it.
   *
   * @return The WebGL texture.
   */
  public WebGLTexture getGlTexture() {
    return texture;
  }

  /** Applies the current smoothing choice as the minification and magnification filters. */
  private void applyFilter() {
    int filter = smooth ? WebGLRenderingContext.LINEAR : WebGLRenderingContext.NEAREST;
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, filter);
    gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, filter);
  }

  /**
   * Copies RGBA bytes out of a buffer into a browser {@code Uint8Array} that WebGL can read.
   *
   * @param rgba The RGBA pixel bytes.
   * @return A typed-array view of a copy of the bytes.
   */
  private static Uint8Array toView(ByteBuffer rgba) {
    ByteBuffer readable = rgba.duplicate();
    readable.rewind();
    byte[] bytes = new byte[readable.remaining()];
    readable.get(bytes);
    return Uint8Array.create(Int8Array.copyFromJavaArray(bytes).getBuffer());
  }
}
