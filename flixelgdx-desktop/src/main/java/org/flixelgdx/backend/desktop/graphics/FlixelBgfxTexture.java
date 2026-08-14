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
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * A texture that lives in bgfx GPU memory.
 *
 * <p>The RGBA pixels are uploaded to a 2D bgfx texture on construction. Smooth (linear) versus
 * crisp (nearest) sampling is baked into the sampler flags; because bgfx sets sampling per binding
 * rather than per texture, the current flags are read back by the batch when the texture is drawn.
 *
 * <p>On Vulkan (and any other non-OpenGL backend), the bgfx texture pipeline introduces a red/blue
 * channel inversion relative to the RGBA byte layout that stb_image produces. {@link #swapRB} is
 * set to {@code true} by {@link FlixelBgfxGraphics} for those backends, and every pixel upload
 * then swaps the red and blue bytes on the CPU before handing the data to bgfx. This is a one-time
 * cost per texture at load time, not per frame.
 */
public class FlixelBgfxTexture implements FlixelTexture {

  /**
   * Sampler flags for nearest-neighbor (crisp) filtering: point min, mag, and mip.
   *
   * <p>{@link BGFX#BGFX_SAMPLER_POINT} sets all three of the point bits, including the mip filter.
   * The mip bit matters even though these textures have no mipmaps: leaving the mip filter on its
   * linear default asks OpenGL to sample between mip levels that do not exist, which makes the
   * texture incomplete and sample as solid black on some drivers. Selecting point mip filtering
   * keeps the texture complete, so crisp textures (and the shared white pixel used for solid fills)
   * render correctly.
   */
  private static final long FLAGS_NEAREST =
      BGFX.BGFX_SAMPLER_POINT | BGFX.BGFX_SAMPLER_U_CLAMP | BGFX.BGFX_SAMPLER_V_CLAMP;

  /** Sampler flags for linear (smooth) filtering. */
  private static final long FLAGS_LINEAR = BGFX.BGFX_SAMPLER_U_CLAMP | BGFX.BGFX_SAMPLER_V_CLAMP;

  /**
   * When {@code true}, every pixel upload swaps the red and blue bytes before sending data to bgfx.
   * Set once at startup by {@link FlixelBgfxGraphics} for backends that invert the R/B channel
   * order during texture sampling (currently all non-OpenGL backends).
   */
  static boolean swapRB;

  private final int width;
  private final int height;

  private short handle;

  private boolean smooth;
  private boolean destroyed;

  /**
   * Uploads an image to a new bgfx texture.
   *
   * <p>Pixels are uploaded as RGBA8. On non-OpenGL backends (Vulkan, Metal, Direct3D), the bgfx
   * pipeline introduces a red/blue channel inversion relative to the RGBA byte order that
   * stb_image always produces, so this constructor swaps the R and B bytes on the CPU first when
   * {@link #swapRB} is {@code true}. The trailing {@code 0L} parameter is the external texture
   * handle; passing zero lets bgfx allocate the GPU-side storage itself.
   *
   * @param image The RGBA pixels to upload.
   */
  FlixelBgfxTexture(FlixelImage image) {
    this.width = image.width();
    this.height = image.height();
    ByteBuffer pixels = image.pixels();
    pixels.position(0).limit(width * height * 4);
    if (swapRB) {
      ByteBuffer swapped = swapRedBlue(pixels, width * height);
      this.handle = BGFX.bgfx_create_texture_2d(
          width, height, false, 1, BGFX.BGFX_TEXTURE_FORMAT_RGBA8,
          BGFX.BGFX_TEXTURE_NONE, BGFX.bgfx_copy(swapped), 0L);
      MemoryUtil.memFree(swapped);
    } else {
      this.handle = BGFX.bgfx_create_texture_2d(
          width, height, false, 1, BGFX.BGFX_TEXTURE_FORMAT_RGBA8,
          BGFX.BGFX_TEXTURE_NONE, BGFX.bgfx_copy(pixels), 0L);
    }
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
    int count = image.width() * image.height();
    ByteBuffer pixels = image.pixels();
    pixels.position(0).limit(count * 4);
    if (swapRB) {
      ByteBuffer swapped = swapRedBlue(pixels, count);
      BGFX.bgfx_update_texture_2d(handle, 0, 0,
          (short) x, (short) y, (short) image.width(), (short) image.height(),
          BGFX.bgfx_copy(swapped), 0xFFFF);
      MemoryUtil.memFree(swapped);
    } else {
      BGFX.bgfx_update_texture_2d(handle, 0, 0,
          (short) x, (short) y, (short) image.width(), (short) image.height(),
          BGFX.bgfx_copy(pixels), 0xFFFF);
    }
  }

  /**
   * Returns a malloc-backed {@link ByteBuffer} whose R and B bytes are swapped relative to
   * {@code src}.
   *
   * <p>stb_image always delivers pixels as RGBA, but certain bgfx backends invert the red and blue
   * channels during sampling. This method compensates on the CPU at load time so the GPU sees the
   * correct channel layout. The original {@code src} buffer is left unmodified.
   *
   * <p>The returned buffer is allocated via {@link MemoryUtil#memAlloc} and must be freed by the
   * caller with {@link MemoryUtil#memFree} once bgfx has consumed the data.
   *
   * @param src The source RGBA pixel buffer (position 0, at least {@code pixelCount * 4} bytes).
   * @param pixelCount The number of pixels to process.
   * @return A malloc-backed buffer containing the same pixels with R and B bytes swapped.
   */
  private static ByteBuffer swapRedBlue(ByteBuffer src, int pixelCount) {
    ByteBuffer dst = MemoryUtil.memAlloc(pixelCount * 4);
    for (int i = 0; i < pixelCount; i++) {
      int base = i * 4;
      dst.put(base, src.get(base + 2)); // R slot <- B value
      dst.put(base + 1, src.get(base + 1)); // G unchanged
      dst.put(base + 2, src.get(base));     // B slot <- R value
      dst.put(base + 3, src.get(base + 3)); // A unchanged
    }
    return dst;
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

  public boolean isDestroyed() {
    return destroyed;
  }
}
