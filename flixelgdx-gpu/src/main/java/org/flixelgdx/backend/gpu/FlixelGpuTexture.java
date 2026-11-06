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
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.GpuBindGroup;
import io.github.stringdotjar.jwebgpu.GpuTexture;
import io.github.stringdotjar.jwebgpu.GpuTextureFormat;
import io.github.stringdotjar.jwebgpu.GpuTextureView;

/**
 * A texture on the GPU, either RGBA8 or block-compressed.
 *
 * <p>The texture owns its {@code GpuTexture}, a view of it, and up to two bind groups (one that samples with
 * nearest-neighbor filtering and one that samples smoothly). The bind groups are created the first time the
 * texture is drawn that way and then reused for every later draw, so drawing allocates nothing.
 *
 * <p>Pixel uploads go through the queue, which makes them visible to every draw recorded in the same frame, even
 * draws recorded before the upload. A texture that is replaced several times in one frame therefore shows only the
 * last version.
 *
 * <p>A compressed texture (from a KTX2 file) is rounded up to whole 4x4 blocks on the GPU, so a 50x37 image is a
 * 52x40 texture. {@link #getWidth()} and {@link #getHeight()} still report 50x37, so frames and regions measured in
 * pixels keep working, and the batch scales texture coordinates by {@link #getUScale()} and {@link #getVScale()} to
 * match. A compressed texture cannot be changed with {@link #update(int, int, FlixelImage)}.
 */
public class FlixelGpuTexture implements FlixelTexture {

  private static final FlixelLogger LOG = Flixel.log.tagged("Graphics/Textures");

  private final long id;
  private final long extraUsage;
  private final long byteSize;
  private final int width;
  private final int height;
  private final int gpuWidth;
  private final int gpuHeight;
  private final float uScale;
  private final float vScale;

  private final FlixelGpuGraphics graphics;
  private GpuTexture texture;
  private GpuTextureView view;
  private GpuBindGroup nearestGroup;
  private GpuBindGroup linearGroup;

  private int epoch;

  private String format = "RGBA8";

  private boolean smooth;
  private boolean destroyed;
  private boolean compressed;

  /**
   * Creates an empty (transparent) texture.
   *
   * @param graphics The renderer that owns the texture.
   * @param width The width in pixels.
   * @param height The height in pixels.
   * @param extraUsage Additional {@code GpuTextureUsage} flags, such as {@code RENDER_ATTACHMENT}.
   */
  FlixelGpuTexture(@NotNull FlixelGpuGraphics graphics, int width, int height, long extraUsage) {
    this.graphics = graphics;
    this.width = width;
    this.height = height;
    this.extraUsage = extraUsage;
    this.gpuWidth = width;
    this.gpuHeight = height;
    this.uScale = 1f;
    this.vScale = 1f;
    this.byteSize = (long) width * height * 4L;
    this.epoch = graphics.getDeviceEpoch();
    this.texture = graphics.createRawTexture(width, height, GpuTextureFormat.RGBA8_UNORM, 1, extraUsage);
    this.view = texture.createView(null);
    this.id = graphics.registerTexture(this);
  }

  /**
   * Creates a texture and uploads pixels into it.
   *
   * @param graphics The renderer that owns the texture.
   * @param image The RGBA pixels to upload.
   */
  FlixelGpuTexture(@NotNull FlixelGpuGraphics graphics, @NotNull FlixelImage image) {
    this(graphics, image.getWidth(), image.getHeight(), 0L);
    graphics.writePixels(texture, 0, 0, width, height, image.getPixels());
  }

  /**
   * Creates a texture from a prepared image, uploading every mip level.
   *
   * @param graphics The renderer that owns the texture.
   * @param image The image, in a format the device supports.
   */
  FlixelGpuTexture(@NotNull FlixelGpuGraphics graphics, @NotNull FlixelGpuCompressedImage image) {
    this.graphics = graphics;
    this.width = image.getWidth();
    this.height = image.getHeight();
    this.gpuWidth = image.getGpuWidth();
    this.gpuHeight = image.getGpuHeight();
    this.uScale = (float) width / gpuWidth;
    this.vScale = (float) height / gpuHeight;
    this.extraUsage = 0L;
    this.byteSize = image.getByteSize();
    this.compressed = image.getFormat().isCompressed();
    this.format = image.getFormat().name();
    this.epoch = graphics.getDeviceEpoch();
    this.texture = graphics.createRawTexture(gpuWidth, gpuHeight, image.getFormat().getGpuFormat(), image.getLevels(),
        0L);
    this.view = texture.createView(null);
    this.id = graphics.registerTexture(this);
    for (int level = 0; level < image.getLevels(); level++) {
      graphics.writeLevel(texture, image, level);
    }
  }

  @Override
  public void update(int x, int y, @NotNull FlixelImage image) {
    if (destroyed) {
      return;
    }
    refresh();
    if (compressed) {
      LOG.warn("A compressed texture cannot be updated with new pixels; the update was ignored.");
      return;
    }
    graphics.writePixels(texture, x, y, image.getWidth(), image.getHeight(), image.getPixels());
  }

  @Override
  public void destroy() {
    if (destroyed) {
      return;
    }
    destroyed = true;
    graphics.unregisterTexture(this);
    if (nearestGroup != null) {
      nearestGroup.release();
      nearestGroup = null;
    }
    if (linearGroup != null) {
      linearGroup.release();
      linearGroup = null;
    }
    view.release();
    // Release instead of destroying: the GPU frees the memory once the frames still using it are done.
    texture.release();
  }

  /**
   * Replaces the GPU objects with empty ones when the renderer has moved to a new device.
   *
   * <p>After a device loss every object of the old device is dead, and the pixels were never kept in memory. The
   * texture therefore comes back transparent, with the same size and id, so drawing stays valid and the game can
   * upload the pixels again. A compressed texture comes back as a transparent RGBA8 texture of the same size, which
   * {@link #update(int, int, FlixelImage)} can then fill. It costs one integer comparison when nothing was lost.
   */
  private void refresh() {
    int current = graphics.getDeviceEpoch();
    if (epoch == current) {
      return;
    }
    epoch = current;
    // The objects belong to a dead device, but their handles still need to be given back.
    if (nearestGroup != null) {
      nearestGroup.release();
      nearestGroup = null;
    }
    if (linearGroup != null) {
      linearGroup.release();
      linearGroup = null;
    }
    view.release();
    texture.release();
    texture = graphics.createRawTexture(gpuWidth, gpuHeight, GpuTextureFormat.RGBA8_UNORM, 1, extraUsage);
    view = texture.createView(null);
    compressed = false;
  }

  /**
   * Returns the bind group (uniform slot, this texture and the right sampler) for the current filtering mode,
   * creating it on first use.
   *
   * @return The bind group for bind group index 0.
   */
  @NotNull
  GpuBindGroup bindGroup() {
    refresh();
    if (smooth) {
      if (linearGroup == null) {
        linearGroup = graphics.createTextureGroup(view, true);
      }
      return linearGroup;
    }
    if (nearestGroup == null) {
      nearestGroup = graphics.createTextureGroup(view, false);
    }
    return nearestGroup;
  }

  /**
   * Returns the underlying GPU texture.
   *
   * @return The texture.
   */
  @NotNull
  public GpuTexture getGpuTexture() {
    refresh();
    return texture;
  }

  /**
   * Returns the view the shaders sample through.
   *
   * @return The texture view.
   */
  @NotNull
  public GpuTextureView getView() {
    refresh();
    return view;
  }

  @Override
  public long getHandle() {
    return id;
  }

  /**
   * Returns how many bytes of GPU memory the texture's pixels take, counting every mip level.
   *
   * @return The size in bytes.
   */
  @Override
  public long getByteSize() {
    return byteSize;
  }

  /**
   * Returns the width of the texture on the GPU, which is {@link #getWidth()} rounded up to whole blocks for a
   * compressed texture.
   *
   * @return The GPU width in pixels.
   */
  @NotNull
  @Override
  public String getFormat() {
    return format;
  }

  public int getGpuWidth() {
    return gpuWidth;
  }

  /**
   * Returns the height of the texture on the GPU, which is {@link #getHeight()} rounded up to whole blocks for a
   * compressed texture.
   *
   * @return The GPU height in pixels.
   */
  public int getGpuHeight() {
    return gpuHeight;
  }

  /**
   * Returns the factor that turns a horizontal texture coordinate measured against {@link #getWidth()} into one
   * measured against {@link #getGpuWidth()}.
   *
   * @return {@code getWidth() / getGpuWidth()}, which is 1 unless the texture was rounded up to whole blocks.
   */
  public float getUScale() {
    return uScale;
  }

  /**
   * Returns the factor that turns a vertical texture coordinate measured against {@link #getHeight()} into one
   * measured against {@link #getGpuHeight()}.
   *
   * @return {@code getHeight() / getGpuHeight()}, which is 1 unless the texture was rounded up to whole blocks.
   */
  public float getVScale() {
    return vScale;
  }

  /**
   * Returns whether the pixels are stored in a block-compressed format.
   *
   * @return {@code true} for a texture loaded from a compressed KTX2 file, until a device loss replaces it.
   */
  public boolean isCompressed() {
    return compressed;
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

  /**
   * Returns whether {@link #destroy()} has been called.
   *
   * @return {@code true} when the GPU resources are released.
   */
  public boolean isDestroyed() {
    return destroyed;
  }

  /**
   * Returns the renderer-wide unique id of this texture, the same value as {@link #getHandle()}.
   *
   * @return The id.
   */
  public long getId() {
    return id;
  }
}
