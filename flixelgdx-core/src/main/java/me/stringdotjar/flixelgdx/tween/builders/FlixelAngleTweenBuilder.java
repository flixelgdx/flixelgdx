/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelAngleTween;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelAngleTween}. */
public final class FlixelAngleTweenBuilder extends FlixelAbstractTweenBuilder<FlixelAngleTween, FlixelAngleTweenBuilder> {

  private @Nullable FlixelObject sprite;
  private float fromAngle = Float.NaN;
  private float toAngle = 0f;

  @Override
  protected FlixelAngleTweenBuilder self() {
    return this;
  }

  public FlixelAngleTweenBuilder setSprite(@Nullable FlixelObject sprite) {
    this.sprite = sprite;
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
    tween.setAngles(sprite, fromAngle, toAngle);
    return (FlixelAngleTween) manager.addTween(tween);
  }
}
