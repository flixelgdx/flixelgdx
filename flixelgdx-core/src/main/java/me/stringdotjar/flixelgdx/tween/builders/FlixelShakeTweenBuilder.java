/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelShakeTween}. */
public final class FlixelShakeTweenBuilder extends FlixelAbstractTweenBuilder<FlixelShakeTween, FlixelShakeTweenBuilder> {

  private @Nullable FlixelSprite sprite;
  private FlixelAxes axes = FlixelAxes.XY;
  private float intensity = 4f;

  @Override
  protected FlixelShakeTweenBuilder self() {
    return this;
  }

  public FlixelShakeTweenBuilder setSprite(@Nullable FlixelSprite sprite) {
    this.sprite = sprite;
    return this;
  }

  public FlixelShakeTweenBuilder setAxes(FlixelAxes axes) {
    this.axes = axes != null ? axes : FlixelAxes.XY;
    return this;
  }

  public FlixelShakeTweenBuilder setIntensity(float intensity) {
    this.intensity = intensity;
    return this;
  }

  @Override
  public FlixelShakeTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelShakeTween tween =
        manager.obtainTween(FlixelShakeTween.class, () -> new FlixelShakeTween(settings));
    tween.setTweenSettings(settings);
    tween.setShake(sprite, axes, intensity);
    return (FlixelShakeTween) manager.addTween(tween);
  }
}
