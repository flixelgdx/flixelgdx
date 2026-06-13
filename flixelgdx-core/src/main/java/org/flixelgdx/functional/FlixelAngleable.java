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
package org.flixelgdx.functional;

/**
 * Something with a rotation angle in degrees that angle tweens and motion integration can drive.
 *
 * @see org.flixelgdx.FlixelObject
 */
public interface FlixelAngleable {

  /**
   * The angle in degrees of this object. Does not affect axis-aligned collision on
   * {@link org.flixelgdx.FlixelObject FlixelObject}.
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
