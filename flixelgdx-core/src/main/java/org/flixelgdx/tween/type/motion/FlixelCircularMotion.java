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
package org.flixelgdx.tween.type.motion;

import com.badlogic.gdx.math.MathUtils;

import org.flixelgdx.tween.settings.FlixelTweenSettings;
import org.jetbrains.annotations.Nullable;

/** Motion along a circular arc (angles in degrees for the API, radians internally). */
public final class FlixelCircularMotion extends FlixelMotion {

  private float centerX;
  private float centerY;
  private float radius;
  private float angleStartRad;
  private float angleSweepRad;

  public FlixelCircularMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the motion for the tween.
   *
   * @param centerX The X coordinate of the center of the circle.
   * @param centerY The Y coordinate of the center of the circle.
   * @param radius The radius of the circle.
   * @param angleDeg The start angle on the circle (degrees).
   * @param clockwise The sweep direction.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelCircularMotion setMotion(
      float centerX,
      float centerY,
      float radius,
      float angleDeg,
      boolean clockwise,
      float durationOrSpeed,
      boolean useDuration) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.radius = radius;
    this.angleStartRad = angleDeg * MathUtils.degreesToRadians;
    this.angleSweepRad = MathUtils.PI2 * (clockwise ? 1f : -1f);
    float a = angleStartRad;
    motionX = centerX + MathUtils.cos(a) * radius;
    motionY = centerY + MathUtils.sin(a) * radius;
    float circumference = radius * MathUtils.PI2;
    if (tweenSettings != null) {
      if (useDuration) {
        tweenSettings.setDuration(Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR));
      } else {
        float speed = Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR);
        tweenSettings.setDuration(circumference / speed);
      }
    }
    return this;
  }

  @Override
  protected void computeMotion() {
    float a = angleStartRad + angleSweepRad * scale;
    motionX = centerX + MathUtils.cos(a) * radius;
    motionY = centerY + MathUtils.sin(a) * radius;
  }
}
