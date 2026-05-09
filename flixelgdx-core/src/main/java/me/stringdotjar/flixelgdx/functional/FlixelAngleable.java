/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.functional;

/**
 * Something with a rotation angle in degrees that angle tweens and motion integration can drive.
 *
 * @see me.stringdotjar.flixelgdx.FlixelObject
 */
public interface FlixelAngleable {

  /**
   * The angle in degrees of this object. Does not affect axis-aligned collision on
   * {@link me.stringdotjar.flixelgdx.FlixelObject}.
   *
   * @return The current angle in degrees.
   */
  float getAngle();

  /**
   * Sets the rotation angle in degrees.
   *
   * @param degrees The new angle in degrees.
   */
  void setAngle(float degrees);

  /**
   * Adds {@code deltaDegrees} to the current rotation angle.
   *
   * @param deltaDegrees Degrees to add (may be negative).
   */
  void changeAngle(float deltaDegrees);
}
