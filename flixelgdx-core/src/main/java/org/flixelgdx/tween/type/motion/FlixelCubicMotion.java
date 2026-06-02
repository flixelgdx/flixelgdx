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

/** Cubic Bézier motion (two control points). */
public final class FlixelCubicMotion extends FlixelMotion {

  private float p0x;
  private float p0y;
  private float p1x;
  private float p1y;
  private float p2x;
  private float p2y;
  private float p3x;
  private float p3y;

  public FlixelCubicMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the motion for the tween.
   *
   * @param p0x The X coordinate of the first control point.
   * @param p0y The Y coordinate of the first control point.
   * @param p1x The X coordinate of the second control point.
   * @param p1y The Y coordinate of the second control point.
   * @param p2x The X coordinate of the third control point.
   * @param p2y The Y coordinate of the third control point.
   * @param p3x The X coordinate of the fourth control point.
   * @param p3y The Y coordinate of the fourth control point.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelCubicMotion setMotion(
      float p0x,
      float p0y,
      float p1x,
      float p1y,
      float p2x,
      float p2y,
      float p3x,
      float p3y,
      float durationOrSpeed,
      boolean useDuration) {
    this.p0x = p0x;
    this.p0y = p0y;
    this.p1x = p1x;
    this.p1y = p1y;
    this.p2x = p2x;
    this.p2y = p2y;
    this.p3x = p3x;
    this.p3y = p3y;
    motionX = p0x;
    motionY = p0y;
    float dist =
        dist(p0x, p0y, p1x, p1y) + dist(p1x, p1y, p2x, p2y) + dist(p2x, p2y, p3x, p3y);
    if (tweenSettings != null) {
      if (useDuration) {
        tweenSettings.setDuration(Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR));
      } else {
        float speed = Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR);
        tweenSettings.setDuration(dist / speed);
      }
    }
    return this;
  }

  private static float dist(float ax, float ay, float bx, float by) {
    float dx = bx - ax;
    float dy = by - ay;
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  @Override
  protected void computeMotion() {
    float t = scale;
    float u = 1f - t;
    float tt = t * t;
    float uu = u * u;
    float uuu = uu * u;
    float ttt = tt * t;
    motionX = uuu * p0x + 3f * uu * t * p1x + 3f * u * tt * p2x + ttt * p3x;
    motionY = uuu * p0y + 3f * uu * t * p1y + 3f * u * tt * p2y + ttt * p3y;
  }
}
