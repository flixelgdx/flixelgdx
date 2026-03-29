/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.builders;

import com.badlogic.gdx.graphics.Color;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.tween.type.FlixelColorTween;
import me.stringdotjar.flixelgdx.util.FlixelColor;

import org.jetbrains.annotations.Nullable;

/** Builder for {@link FlixelColorTween}. */
public final class FlixelColorTweenBuilder extends FlixelAbstractTweenBuilder<FlixelColorTween, FlixelColorTweenBuilder> {

  private @Nullable FlixelSprite sprite;
  private @Nullable FlixelColor fromFlixel;
  private @Nullable FlixelColor toFlixel;
  private @Nullable Color fromRaw;
  private @Nullable Color toRaw;
  private boolean useRaw;
  private @Nullable Runnable onColor;

  @Override
  protected FlixelColorTweenBuilder self() {
    return this;
  }

  public FlixelColorTweenBuilder setSprite(@Nullable FlixelSprite sprite) {
    this.sprite = sprite;
    return this;
  }

  public FlixelColorTweenBuilder fromTo(@Nullable FlixelColor from, @Nullable FlixelColor to) {
    this.useRaw = false;
    this.fromFlixel = from;
    this.toFlixel = to;
    return this;
  }

  public FlixelColorTweenBuilder fromToRaw(@Nullable Color from, @Nullable Color to) {
    this.useRaw = true;
    this.fromRaw = from;
    this.toRaw = to;
    return this;
  }

  public FlixelColorTweenBuilder setOnColor(@Nullable Runnable onColor) {
    this.onColor = onColor;
    return this;
  }

  @Override
  public FlixelColorTween start() {
    FlixelTweenSettings settings = new FlixelTweenSettings(type, ease);
    applyTo(settings);
    FlixelColorTween tween =
        manager.obtainTween(FlixelColorTween.class, () -> new FlixelColorTween(settings));
    tween.setTweenSettings(settings);
    if (useRaw) {
      tween.setColorEndpointsRaw(sprite, fromRaw, toRaw, onColor);
    } else {
      tween.setColorEndpoints(sprite, fromFlixel, toFlixel, onColor);
    }
    return (FlixelColorTween) manager.addTween(tween);
  }
}
