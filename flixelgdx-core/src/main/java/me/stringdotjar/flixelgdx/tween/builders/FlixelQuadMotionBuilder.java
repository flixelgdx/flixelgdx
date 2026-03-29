/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.motion.FlixelQuadMotion;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelQuadMotion}. */
public final class FlixelQuadMotionBuilder extends FlixelAbstractTweenBuilder<FlixelQuadMotion, FlixelQuadMotionBuilder> {

  private @Nullable FlixelObject target;
  private float fromX;
  private float fromY;
  private float cx;
  private float cy;
  private float toX;
  private float toY;
  private float durationOrSpeed = 1f;
  private boolean useDuration = true;

  @Override
  protected FlixelQuadMotionBuilder self() {
    return this;
  }

  public FlixelQuadMotionBuilder setTarget(@Nullable FlixelObject target) {
    this.target = target;
    return this;
  }

  public FlixelQuadMotionBuilder quad(float fromX, float fromY, float cx, float cy, float toX, float toY) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.cx = cx;
    this.cy = cy;
    this.toX = toX;
    this.toY = toY;
    return this;
  }

  public FlixelQuadMotionBuilder durationOrSpeed(float v, boolean useDuration) {
    this.durationOrSpeed = v;
    this.useDuration = useDuration;
    return this;
  }

  @Override
  public FlixelQuadMotion start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelQuadMotion tween =
        manager.obtainTween(FlixelQuadMotion.class, () -> new FlixelQuadMotion(settings));
    tween.setTweenSettings(settings);
    tween.setMotion(fromX, fromY, cx, cy, toX, toY, durationOrSpeed, useDuration);
    tween.setMotionObject(target);
    return (FlixelQuadMotion) manager.addTween(tween);
  }
}
