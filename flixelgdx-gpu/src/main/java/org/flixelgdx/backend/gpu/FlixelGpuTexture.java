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

import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.GpuBindGroup;
import io.github.stringdotjar.jwebgpu.GpuTexture;
import io.github.stringdotjar.jwebgpu.GpuTextureView;

/**
 * An RGBA8 texture on the GPU.
 *
 * <p>The texture owns its {@code GpuTexture}, a view of it, and up to two bind groups (one that samples with
 * nearest-neighbor filtering and one that samples smoothly). The bind groups are created the first time the
 * texture is drawn that way and then reused for every later draw, so drawing allocates nothing.
 *
 * <p>Pixel uploads go through the queue, which makes them visible to every draw recorded in the same frame, even
 * draws recorded before the upload. A texture that is replaced several times in one frame therefore shows only the
 * last version.
 */
public class FlixelGpuTexture implements FlixelTexture {

  private final long id;
  private final int width;
  private final int height;

  private final FlixelGpuGraphics graphics;
  private GpuTexture texture;
  private GpuTextureView view;
  private GpuBindGroup nearestGroup;
  private GpuBindGroup linearGroup;

  private boolean smooth;
  private boolean destroyed;

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
    this.texture = graphics.createRawTexture(width, height, extraUsage);
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

  @Override
  public void update(int x, int y, @NotNull FlixelImage image) {
    if (destroyed) {
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
   * Returns the bind group (uniform slot, this texture and the right sampler) for the current filtering mode,
   * creating it on first use.
   *
   * @return The bind group for bind group index 0.
   */
  @NotNull
  GpuBindGroup bindGroup() {
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
    return texture;
  }

  /**
   * Returns the view the shaders sample through.
   *
   * @return The texture view.
   */
  @NotNull
  public GpuTextureView getView() {
    return view;
  }

  @Override
  public long getHandle() {
    return id;
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
