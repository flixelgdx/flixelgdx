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

import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.lwjgl.bgfx.BGFX;

import java.nio.ByteBuffer;

/**
 * A texture that lives in bgfx GPU memory.
 *
 * <p>The RGBA pixels are uploaded to a 2D bgfx texture on construction. Smooth (linear) versus
 * crisp (nearest) sampling is baked into the sampler flags; because bgfx sets sampling per binding
 * rather than per texture, the current flags are read back by the batch when the texture is drawn.
 */
final class FlixelBgfxTexture implements FlixelTexture {

  /** Sampler flags for nearest-neighbor (crisp) filtering: point min/mag. */
  private static final long FLAGS_NEAREST =
      BGFX.BGFX_SAMPLER_MIN_POINT | BGFX.BGFX_SAMPLER_MAG_POINT | BGFX.BGFX_SAMPLER_U_CLAMP | BGFX.BGFX_SAMPLER_V_CLAMP;

  /** Sampler flags for linear (smooth) filtering. */
  private static final long FLAGS_LINEAR = BGFX.BGFX_SAMPLER_U_CLAMP | BGFX.BGFX_SAMPLER_V_CLAMP;

  private final int width;
  private final int height;

  private short handle;

  private boolean smooth;
  private boolean destroyed;

  /**
   * Uploads an image to a new bgfx texture.
   *
   * @param image The RGBA pixels to upload.
   */
  FlixelBgfxTexture(FlixelImage image) {
    this.width = image.getWidth();
    this.height = image.getHeight();
    ByteBuffer pixels = image.getPixels();
    pixels.position(0).limit(width * height * 4);
    // Trailing 0L is the "external" texture handle, unused here (bgfx allocates the storage).
    this.handle = BGFX.bgfx_create_texture_2d(
        width, height, false, 1, BGFX.BGFX_TEXTURE_FORMAT_RGBA8,
        BGFX.BGFX_TEXTURE_NONE, BGFX.bgfx_copy(pixels), 0L);
  }

  /**
   * Wraps an already-created bgfx texture handle (for example, a render target's color texture).
   *
   * @param handle The bgfx texture handle.
   * @param width The texture width in pixels.
   * @param height The texture height in pixels.
   */
  FlixelBgfxTexture(short handle, int width, int height) {
    this.handle = handle;
    this.width = width;
    this.height = height;
  }

  /**
   * @return The bgfx texture handle for binding.
   */
  short getBgfxHandle() {
    return handle;
  }

  /**
   * @return The sampler flags matching the current smooth setting.
   */
  long getSamplerFlags() {
    return smooth ? FLAGS_LINEAR : FLAGS_NEAREST;
  }

  @Override
  public void update(int x, int y, FlixelImage image) {
    if (destroyed || handle == -1) {
      return;
    }
    ByteBuffer pixels = image.getPixels();
    pixels.position(0).limit(image.getWidth() * image.getHeight() * 4);
    BGFX.bgfx_update_texture_2d(handle, 0, 0,
        (short) x, (short) y, (short) image.getWidth(), (short) image.getHeight(),
        BGFX.bgfx_copy(pixels), 0xFFFF);
  }

  @Override
  public void destroy() {
    if (!destroyed && handle != -1) {
      BGFX.bgfx_destroy_texture(handle);
      destroyed = true;
    }
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
  }
}
