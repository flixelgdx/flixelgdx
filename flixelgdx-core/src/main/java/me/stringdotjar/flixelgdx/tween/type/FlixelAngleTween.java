/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

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
