/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.graphics.Color;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelColor;

import org.jetbrains.annotations.Nullable;

/**
 * Interpolates between two colors and optionally applies tint to a {@link FlixelSprite}.
 */
public class FlixelColorTween extends FlixelTween {

  protected final Color workFrom = new Color();
  protected final Color workTo = new Color();
  protected final Color workOut = new Color();

  @Nullable  protected FlixelColor fromFlixel;
  @Nullable protected FlixelColor toFlixel;
  protected boolean useRawColor;
  @Nullable protected FlixelSprite sprite;
  @Nullable protected Runnable onColor;

  public FlixelColorTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Tween between two {@link FlixelColor} endpoints.
   *
   * @param sprite The sprite to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpoints(@Nullable FlixelSprite sprite, @Nullable FlixelColor from, @Nullable FlixelColor to, @Nullable Runnable onColor) {
    this.useRawColor = false;
    this.sprite = sprite;
    this.fromFlixel = from;
    this.toFlixel = to;
    this.onColor = onColor;
    return this;
  }

  /**
   * Tween between two libGDX {@link Color} values (copied into internal buffers).
   *
   * @param sprite The sprite to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpointsRaw(@Nullable FlixelSprite sprite, @Nullable Color from, @Nullable Color to, @Nullable Runnable onColor) {
    this.useRawColor = true;
    this.sprite = sprite;
    this.fromFlixel = null;
    this.toFlixel = null;
    this.onColor = onColor;
    if (from != null) {
      workFrom.set(from);
    }
    if (to != null) {
      workTo.set(to);
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (useRawColor) {
      workOut.set(workFrom).lerp(workTo, scale);
    } else {
      if (fromFlixel == null || toFlixel == null) {
        return;
      }
      workOut.set(fromFlixel.getGdxColor()).lerp(toFlixel.getGdxColor(), scale);
    }

    if (sprite != null) {
      sprite.setColor(workOut);
    }
    if (onColor != null) {
      onColor.run();
    }
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (sprite == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, sprite);
    }
    return Objects.equals(object, sprite) && "color".equals(field);
  }

  @Override
  public void reset() {
    super.reset();
    sprite = null;
    fromFlixel = null;
    toFlixel = null;
    onColor = null;
    useRawColor = false;
  }
}
