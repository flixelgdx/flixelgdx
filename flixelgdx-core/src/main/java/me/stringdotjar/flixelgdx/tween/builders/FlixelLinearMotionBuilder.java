/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelLinearMotion;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelLinearMotion}. */
public final class FlixelLinearMotionBuilder extends FlixelAbstractTweenBuilder<FlixelLinearMotion, FlixelLinearMotionBuilder> {

  private @Nullable FlixelObject target;
  private float fromX;
  private float fromY;
  private float toX;
  private float toY;
  private float durationOrSpeed = 1f;
  private boolean useDuration = true;

  @Override
  protected FlixelLinearMotionBuilder self() {
    return this;
  }

  public FlixelLinearMotionBuilder setTarget(@Nullable FlixelObject target) {
    this.target = target;
    return this;
  }

  public FlixelLinearMotionBuilder line(float fromX, float fromY, float toX, float toY) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.toX = toX;
    this.toY = toY;
    return this;
  }

  public FlixelLinearMotionBuilder durationOrSpeed(float v, boolean useDuration) {
    this.durationOrSpeed = v;
    this.useDuration = useDuration;
    return this;
  }

  @Override
  public FlixelLinearMotion start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelLinearMotion tween =
        manager.obtainTween(FlixelLinearMotion.class, () -> new FlixelLinearMotion(settings));
    tween.setTweenSettings(settings);
    tween.setMotion(fromX, fromY, toX, toY, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return (FlixelLinearMotion) manager.addTween(tween);
  }
}
