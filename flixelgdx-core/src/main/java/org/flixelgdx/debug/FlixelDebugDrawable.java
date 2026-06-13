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
package org.flixelgdx.debug;

import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;

/**
 * Interface for objects that can draw a debug bounding box in the
 * {@link org.flixelgdx.debug.FlixelDebugOverlay FlixelDebugOverlay}. Any class that
 * implements this will automatically appear when visual debug drawing is enabled,
 * without being hard-coded into the overlay.
 *
 * <p>{@link FlixelObject} implements this by default, using collision state to
 * pick an appropriate color. Custom objects can implement this interface to provide
 * their own debug visualization.
 */
public interface FlixelDebugDrawable {

  /** X position of the bounding box in world space. */
  float getDebugX();

  /** Y position of the bounding box in world space. */
  float getDebugY();

  /** Width of the bounding box in world pixels. */
  float getDebugWidth();

  /** Height of the bounding box in world pixels. */
  float getDebugHeight();

  /**
   * X position of the debug box in the same world space as {@link FlixelCamera} projection during
   * {@code draw} (includes scroll-factor / parallax). Defaults to {@link #getDebugX()}.
   *
   * @param camera The game camera used for this debug pass.
   */
  default float getDebugDrawX(FlixelCamera camera) {
    return getDebugX();
  }

  /**
   * Y position of the debug box in the same world space as {@link FlixelCamera} projection during
   * {@code draw}. Defaults to {@link #getDebugY()}.
   *
   * @param camera The game camera used for this debug pass.
   */
  default float getDebugDrawY(FlixelCamera camera) {
    return getDebugY();
  }

  /**
   * Returns the RGBA color for this object's debug bounding box as a 4-element
   * array: {@code [r, g, b, a]}. Implementations should return a cached array
   * rather than allocating every frame.
   */
  float[] getDebugBoundingBoxColor();
}
