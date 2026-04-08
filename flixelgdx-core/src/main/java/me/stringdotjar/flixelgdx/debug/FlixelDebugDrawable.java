/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import me.stringdotjar.flixelgdx.FlixelCamera;
import me.stringdotjar.flixelgdx.FlixelObject;

/**
 * Interface for objects that can draw a debug bounding box in the
 * {@link me.stringdotjar.flixelgdx.debug.FlixelDebugOverlay}. Any class that
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
