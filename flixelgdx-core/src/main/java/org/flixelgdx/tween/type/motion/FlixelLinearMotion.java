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

/** Straight-line motion from ({@code fromX}, {@code fromY}) to ({@code toX}, {@code toY}). */
public final class FlixelLinearMotion extends FlixelMotion {

  private float fromX;
  private float fromY;
  private float moveX;
  private float moveY;

  public FlixelLinearMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the motion for the tween.
   *
   * @param fromX The starting X position.
   * @param fromY The starting Y position.
   * @param toX The ending X position.
   * @param toY The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelLinearMotion setMotion(
      float fromX, float fromY, float toX, float toY, float durationOrSpeed, boolean useDuration) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.moveX = toX - fromX;
    this.moveY = toY - fromY;
    motionX = fromX;
    motionY = fromY;
    float dist = (float) Math.sqrt(moveX * moveX + moveY * moveY);
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

  @Override
  protected void computeMotion() {
    motionX = fromX + moveX * scale;
    motionY = fromY + moveY * scale;
  }
}
