/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelCubicMotion;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelCubicMotion}. */
public final class FlixelCubicMotionBuilder extends FlixelAbstractTweenBuilder<FlixelCubicMotion, FlixelCubicMotionBuilder> {

  private @Nullable FlixelObject target;
  private float p0x;
  private float p0y;
  private float p1x;
  private float p1y;
  private float p2x;
  private float p2y;
  private float p3x;
  private float p3y;
  private float durationOrSpeed = 1f;
  private boolean useDuration = true;

  @Override
  protected FlixelCubicMotionBuilder self() {
    return this;
  }

  public FlixelCubicMotionBuilder setTarget(@Nullable FlixelObject target) {
    this.target = target;
    return this;
  }

  public FlixelCubicMotionBuilder cubic(
      float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y) {
    this.p0x = p0x;
    this.p0y = p0y;
    this.p1x = p1x;
    this.p1y = p1y;
    this.p2x = p2x;
    this.p2y = p2y;
    this.p3x = p3x;
    this.p3y = p3y;
    return this;
  }

  public FlixelCubicMotionBuilder durationOrSpeed(float v, boolean useDuration) {
    this.durationOrSpeed = v;
    this.useDuration = useDuration;
    return this;
  }

  @Override
  public FlixelCubicMotion start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelCubicMotion tween =
        manager.obtainTween(FlixelCubicMotion.class, () -> new FlixelCubicMotion(settings));
    tween.setTweenSettings(settings);
    tween.setMotion(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return (FlixelCubicMotion) manager.addTween(tween);
  }
}
