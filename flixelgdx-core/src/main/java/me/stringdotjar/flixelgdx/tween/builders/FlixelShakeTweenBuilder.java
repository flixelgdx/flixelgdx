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
import me.stringdotjar.flixelgdx.tween.type.FlixelShakeTween.ShakeUnit;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.Nullable;

/**
 * Fluent builder for {@link FlixelShakeTween}. Defaults are {@link ShakeUnit#FRACTION} with intensity
 * {@code 0.05f}, no fade-out taper.
 */
public final class FlixelShakeTweenBuilder extends FlixelAbstractTweenBuilder<FlixelShakeTween, FlixelShakeTweenBuilder> {

  private @Nullable FlixelSprite sprite;
  private FlixelAxes axes = FlixelAxes.XY;
  private float intensity = 0.05f;
  private ShakeUnit shakeUnit = ShakeUnit.FRACTION;
  private boolean fadeOut = false;

  @Override
  protected FlixelShakeTweenBuilder self() {
    return this;
  }

  public FlixelShakeTweenBuilder setSprite(@Nullable FlixelSprite sprite) {
    this.sprite = sprite;
    return self();
  }

  public FlixelShakeTweenBuilder setAxes(FlixelAxes axes) {
    this.axes = axes != null ? axes : FlixelAxes.XY;
    return self();
  }

  /**
   * Maximum shake amount. Meaning depends on the {@link #setShakeUnit(ShakeUnit)} setting.
   * Default {@code 0.05f} is appropriate for {@link ShakeUnit#FRACTION}.
   */
  public FlixelShakeTweenBuilder setIntensity(float intensity) {
    this.intensity = intensity;
    return self();
  }

  /**
   * {@link ShakeUnit#FRACTION} by default (sprite size scaled). Use {@link ShakeUnit#PIXELS} for
   * absolute pixel half-range.
   */
  public FlixelShakeTweenBuilder setShakeUnit(ShakeUnit shakeUnit) {
    this.shakeUnit = shakeUnit != null ? shakeUnit : ShakeUnit.FRACTION;
    return self();
  }

  /**
   * When true, shake amplitude tapers to zero over the tween duration. When false, full range
   * each frame until completion.
   */
  public FlixelShakeTweenBuilder setFadeOut(boolean fadeOut) {
    this.fadeOut = fadeOut;
    return self();
  }

  @Override
  public FlixelShakeTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelShakeTween tween =
        manager.obtainTween(FlixelShakeTween.class, () -> new FlixelShakeTween(settings));
    tween.setTweenSettings(settings);
    tween.setShake(sprite, axes, intensity);
    tween.setShakeUnit(shakeUnit);
    tween.setFadeOut(fadeOut);
    return (FlixelShakeTween) manager.addTween(tween);
  }
}
