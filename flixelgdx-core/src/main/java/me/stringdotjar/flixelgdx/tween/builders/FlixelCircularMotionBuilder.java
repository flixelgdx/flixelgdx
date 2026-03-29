/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelCircularMotion;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelCircularMotion}. */
public final class FlixelCircularMotionBuilder extends FlixelAbstractTweenBuilder<FlixelCircularMotion, FlixelCircularMotionBuilder> {

  private @Nullable FlixelObject target;
  private float centerX;
  private float centerY;
  private float radius = 50f;
  private float angleDeg;
  private boolean clockwise = true;
  private float durationOrSpeed = 1f;
  private boolean useDuration = true;

  @Override
  protected FlixelCircularMotionBuilder self() {
    return this;
  }

  public FlixelCircularMotionBuilder setTarget(@Nullable FlixelObject target) {
    this.target = target;
    return this;
  }

  public FlixelCircularMotionBuilder circle(float centerX, float centerY, float radius) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.radius = radius;
    return this;
  }

  public FlixelCircularMotionBuilder startAngle(float angleDeg) {
    this.angleDeg = angleDeg;
    return this;
  }

  public FlixelCircularMotionBuilder clockwise(boolean clockwise) {
    this.clockwise = clockwise;
    return this;
  }

  public FlixelCircularMotionBuilder durationOrSpeed(float v, boolean useDuration) {
    this.durationOrSpeed = v;
    this.useDuration = useDuration;
    return this;
  }

  @Override
  public FlixelCircularMotion start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelCircularMotion tween =
        manager.obtainTween(FlixelCircularMotion.class, () -> new FlixelCircularMotion(settings));
    tween.setTweenSettings(settings);
    tween.setMotion(centerX, centerY, radius, angleDeg, clockwise, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return (FlixelCircularMotion) manager.addTween(tween);
  }
}
