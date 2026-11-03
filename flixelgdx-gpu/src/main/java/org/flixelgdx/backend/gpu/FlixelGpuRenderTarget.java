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

import org.flixelgdx.graphics.FlixelRenderTarget;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.GpuTextureUsage;

/**
 * An off-screen RGBA8 surface that the renderer can draw into and later sample as a texture.
 *
 * <p>{@link #begin()} redirects drawing here and clears the target to transparent black; {@link #end()} returns
 * drawing to whatever was active before, so targets nest. The texture is stored top-down like every other
 * texture, so {@link #isFlipped()} is always {@code false}.
 */
public class FlixelGpuRenderTarget implements FlixelRenderTarget {

  private final FlixelGpuGraphics graphics;
  private final FlixelGpuTexture texture;

  FlixelGpuRenderTarget(@NotNull FlixelGpuGraphics graphics, int width, int height) {
    this.graphics = graphics;
    this.texture = new FlixelGpuTexture(graphics, Math.max(1, width), Math.max(1, height),
        GpuTextureUsage.RENDER_ATTACHMENT | GpuTextureUsage.COPY_SRC);
  }

  @Override
  public void begin() {
    graphics.pushTarget(this);
  }

  @Override
  public void end() {
    graphics.popTarget();
  }

  @Override
  public void destroy() {
    texture.destroy();
  }

  @Override
  public int getWidth() {
    return texture.getWidth();
  }

  @Override
  public int getHeight() {
    return texture.getHeight();
  }

  @NotNull
  @Override
  public FlixelTexture getTexture() {
    return texture;
  }

  @Override
  public boolean isFlipped() {
    return false;
  }

  /**
   * Returns the GPU-backed texture, for the renderer.
   *
   * @return The texture.
   */
  @NotNull
  FlixelGpuTexture getGpuTexture() {
    return texture;
  }
}
