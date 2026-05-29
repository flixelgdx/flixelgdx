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
package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import me.stringdotjar.flixelgdx.functional.FlixelAngleable;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Tweens {@link FlixelAngleable#getAngle()} toward an end angle (degrees).
 */
public class FlixelAngleTween extends FlixelTween {

  protected @Nullable FlixelAngleable angleTarget;
  protected float fromAngle;
  protected float toAngle;

  public FlixelAngleTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the {@link FlixelAngleable} and angles to tween.
   *
   * @param angleTarget The object whose angle is driven.
   * @param fromAngle Use {@link Float#NaN} to take the target's current angle at {@link #start()}.
   * @param toAngle The ending angle in degrees.
   */
  public FlixelAngleTween setAngles(@Nullable FlixelAngleable angleTarget, float fromAngle, float toAngle) {
    this.angleTarget = angleTarget;
    this.fromAngle = fromAngle;
    this.toAngle = toAngle;
    return this;
  }

  @Override
  public FlixelTween start() {
    super.start();
    if (angleTarget != null && Float.isNaN(fromAngle)) {
      fromAngle = angleTarget.getAngle();
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (angleTarget == null) {
      return;
    }
    float a = fromAngle + (toAngle - fromAngle) * scale;
    angleTarget.setAngle(a);
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (angleTarget == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, angleTarget);
    }
    return Objects.equals(object, angleTarget) && "angle".equals(field);
  }

  @Override
  public void reset() {
    super.reset();
    angleTarget = null;
    fromAngle = 0f;
    toAngle = 0f;
  }
}
