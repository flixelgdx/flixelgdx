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
package org.flixelgdx.graphics;

import org.flixelgdx.functional.FlixelDestroyable;

/**
 * An opaque handle to a texture that lives on the GPU.
 *
 * <p>A texture is created by the active graphics backend, usually through
 * {@link FlixelGraphicsManager#createTexture(FlixelImage)} or by loading an image through the
 * asset manager. Game code treats it as a black box: it can ask for the size, toggle smooth
 * (linear) filtering, and destroy it, but never touches the underlying GPU object directly.
 * That is what keeps the same game code working on every backend.
 *
 * <p>Most games never create textures by hand. {@link FlixelGraphic} wraps a texture with
 * reference counting and asset-manager integration, and {@link FlixelFrame} points at a
 * rectangular region inside one.
 *
 * @see FlixelGraphic
 * @see FlixelFrame
 * @see FlixelGraphicsManager#createTexture(FlixelImage)
 */
public interface FlixelTexture extends FlixelDestroyable {

  /**
   * Returns a backend-specific identifier for this texture.
   *
   * <p>This value only has meaning to the active backend (for example, a bgfx texture handle).
   * It exists for advanced interop and debugging; portable game code should never interpret it.
   *
   * @return The opaque native handle, or {@code 0} when no GPU object exists.
   */
  long getHandle();

  /**
   * Returns the texture width in pixels.
   *
   * @return The texture width in pixels.
   */
  int getWidth();

  /**
   * Returns the texture height in pixels.
   *
   * @return The texture height in pixels.
   */
  int getHeight();

  /**
   * Returns whether this texture samples with smooth (linear) filtering.
   *
   * @return {@code true} for linear filtering, {@code false} for crisp nearest-neighbor sampling.
   */
  boolean isSmooth();

  /**
   * Switches between smooth (linear) and crisp (nearest-neighbor) sampling.
   *
   * <p>Pixel-art games usually want {@code false}; high-resolution art usually wants {@code true}.
   * The framework calls this when antialiasing is toggled on a sprite.
   *
   * @param smooth {@code true} for linear filtering.
   */
  void setSmooth(boolean smooth);

  /**
   * Re-uploads pixel data into a rectangle of this texture.
   *
   * <p>Used by streaming systems such as the font glyph atlas. Backends that cannot update
   * textures in place may ignore the call.
   *
   * @param x Left edge of the destination rectangle in pixels.
   * @param y Top edge of the destination rectangle in pixels.
   * @param image Source pixels; the full image is copied.
   */
  void update(int x, int y, FlixelImage image);
}
