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

import android.opengl.GLES30;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * A GPU texture backed by an OpenGL ES 3.0 texture object.
 *
 * <p>The handle returned by {@link #getHandle()} is a monotonically increasing long used only to
 * distinguish one texture from another inside the batch's slot tracker; it is not the GL texture
 * name. The actual GL name is accessible through {@link #getGlTexture()} so the batch and render
 * target can bind it.
 *
 * <p>Instances are created by {@link FlixelAndroidGraphics} and must only be used on the GL
 * thread.
 */
class FlixelGlesTexture implements FlixelTexture {

  private static long nextHandle = 1L;

  private final long handle;

  private final int glTexture;
  private final int width;
  private final int height;

  private boolean smooth;

  /**
   * Creates an empty (undefined-content) texture, suitable as a render target color attachment.
   *
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @param smooth {@code true} for linear filtering, {@code false} for nearest.
   */
  FlixelGlesTexture(int width, int height, boolean smooth) {
    this.handle = nextHandle++;
    this.width = width;
    this.height = height;
    this.smooth = smooth;

    int[] ids = new int[1];
    GLES30.glGenTextures(1, ids, 0);
    glTexture = ids[0];

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture);
    // Allocate storage without uploading data; the render target fills it by drawing.
    GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
        GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, (java.nio.Buffer) null);
    applyFilter();
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
  }

  /**
   * Creates a texture and uploads RGBA pixels from a direct {@link ByteBuffer}.
   *
   * @param width Width in pixels.
   * @param height Height in pixels.
   * @param rgba Tightly packed RGBA8888 pixels, four bytes per pixel.
   * @param smooth {@code true} for linear filtering.
   */
  FlixelGlesTexture(int width, int height, @NotNull ByteBuffer rgba, boolean smooth) {
    this.handle = nextHandle++;
    this.width = width;
    this.height = height;
    this.smooth = smooth;

    int[] ids = new int[1];
    GLES30.glGenTextures(1, ids, 0);
    glTexture = ids[0];

    rgba.rewind();
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture);
    GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, width, height, 0,
        GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, rgba);
    applyFilter();
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
  }

  /**
   * Creates a compressed or uncompressed texture from pre-transcoded mip levels.
   *
   * <p>Used by {@link FlixelAndroidGraphics#createCompressedTexture} after Basis Universal
   * transcodes each mip level into the chosen GPU format. When {@code levels} is greater than
   * one, the min filter is set to a mipmap variant so the chain is used.
   *
   * @param width Base level width in pixels.
   * @param height Base level height in pixels.
   * @param levels Number of mip levels in {@code mips}.
   * @param mips Transcoded pixel data for each mip level, from largest to smallest.
   * @param glFormat The internal format constant, such as {@code GL_COMPRESSED_RGBA8_ETC2_EAC}
   *     or {@code GL_RGBA} for uncompressed fallback.
   * @param compressed {@code true} when calling {@code glCompressedTexImage2D},
   *     {@code false} to call {@code glTexImage2D}.
   */
  FlixelGlesTexture(int width, int height, int levels, @NotNull ByteBuffer[] mips,
      int glFormat, boolean compressed) {
    this.handle = nextHandle++;
    this.width = width;
    this.height = height;
    this.smooth = true;

    int[] ids = new int[1];
    GLES30.glGenTextures(1, ids, 0);
    glTexture = ids[0];

    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture);

    int mipW = width;
    int mipH = height;
    for (int i = 0; i < levels; i++) {
      ByteBuffer mip = mips[i];
      mip.rewind();
      if (compressed) {
        GLES30.glCompressedTexImage2D(GLES30.GL_TEXTURE_2D, i, glFormat,
            mipW, mipH, 0, mip.remaining(), mip);
      } else {
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, i, GLES30.GL_RGBA,
            mipW, mipH, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, mip);
      }
      mipW = Math.max(1, mipW / 2);
      mipH = Math.max(1, mipH / 2);
    }

    if (levels > 1) {
      int minFilter = smooth
          ? GLES30.GL_LINEAR_MIPMAP_LINEAR
          : GLES30.GL_NEAREST_MIPMAP_NEAREST;
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, minFilter);
      int magFilter = smooth ? GLES30.GL_LINEAR : GLES30.GL_NEAREST;
      GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, magFilter);
    } else {
      applyFilter();
    }
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
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
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture);
    applyFilter();
  }

  @Override
  public void update(int x, int y, @NotNull FlixelImage image) {
    ByteBuffer pixels = image.getPixels();
    pixels.rewind();
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexture);
    GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y,
        image.getWidth(), image.getHeight(),
        GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixels);
  }

  @Override
  public void destroy() {
    int[] ids = {glTexture};
    GLES30.glDeleteTextures(1, ids, 0);
  }

  /**
   * Returns the underlying GL texture name so the batch and framebuffer can bind it.
   *
   * @return The GL texture object name.
   */
  int getGlTexture() {
    return glTexture;
  }

  /** Applies the current smooth flag as min and mag filters on the already-bound texture. */
  private void applyFilter() {
    int filter = smooth ? GLES30.GL_LINEAR : GLES30.GL_NEAREST;
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter);
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter);
  }
}
