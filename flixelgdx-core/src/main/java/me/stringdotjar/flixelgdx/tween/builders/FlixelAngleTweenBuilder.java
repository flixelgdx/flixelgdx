/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.functional.FlixelAngleable;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelAngleTween;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelAngleTween}. */
public final class FlixelAngleTweenBuilder extends FlixelAbstractTweenBuilder<FlixelAngleTween, FlixelAngleTweenBuilder> {

  private @Nullable FlixelAngleable target;
  private float fromAngle = Float.NaN;
  private float toAngle = 0f;

  @Override
  protected FlixelAngleTweenBuilder self() {
    return this;
  }

  public FlixelAngleTweenBuilder setTarget(@Nullable FlixelAngleable target) {
    this.target = target;
    return this;
  }

  public FlixelAngleTweenBuilder fromAngle(float degrees) {
    this.fromAngle = degrees;
    return this;
  }

  public FlixelAngleTweenBuilder toAngle(float degrees) {
    this.toAngle = degrees;
    return this;
  }

  @Override
  public FlixelAngleTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelAngleTween tween =
        manager.obtainTween(FlixelAngleTween.class, () -> new FlixelAngleTween(settings));
    tween.setTweenSettings(settings);
    tween.setAngles(target, fromAngle, toAngle);
    return (FlixelAngleTween) manager.addTween(tween);
  }
}
