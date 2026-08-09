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
import org.jetbrains.annotations.NotNull;

/**
 * An off-screen surface that drawing can be redirected into, then sampled as a texture.
 *
 * <p>Render targets power post-processing: the framework draws a camera (or the whole scene)
 * into one, then draws the target's texture back to the screen through a shader. Between
 * {@link #begin()} and {@link #end()}, everything the batch draws lands on this surface instead
 * of the screen.
 *
 * <p>Create one through {@link FlixelGraphicsManager#createRenderTarget(int, int)} and destroy
 * it when finished (or when the window resizes and a new size is needed).
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelRenderTarget target = Flixel.graphics.createRenderTarget(width, height);
 * target.begin();
 * // ... draw the scene ...
 * target.end();
 * batch.draw(target.getTexture(), 0, 0, width, height);
 * }</pre>
 */
public interface FlixelRenderTarget extends FlixelDestroyable {

  /**
   * Redirects subsequent drawing into this target.
   *
   * <p>Targets nest: if another target is already active when this one begins, {@link #end()}
   * returns drawing to that outer target rather than to the screen. This is what lets a camera's
   * post-processing target render inside a whole-scene global target.
   */
  void begin();

  /** Ends redirection, so drawing goes back to the previously active target (or the screen). */
  void end();

  /**
   * @return The width of this target in pixels.
   */
  int getWidth();

  /**
   * @return The height of this target in pixels.
   */
  int getHeight();

  /**
   * Returns the texture holding what has been drawn into this target.
   *
   * <p>Note that some backends render targets upside down relative to normal textures; the
   * framework's composite passes account for this, and custom code can check
   * {@link #isFlipped()}.
   *
   * @return The color texture; never {@code null}.
   */
  @NotNull
  FlixelTexture getTexture();

  /**
   * @return {@code true} when {@link #getTexture()} is stored bottom-up and must be drawn
   *     flipped vertically to appear correct.
   */
  boolean isFlipped();
}
