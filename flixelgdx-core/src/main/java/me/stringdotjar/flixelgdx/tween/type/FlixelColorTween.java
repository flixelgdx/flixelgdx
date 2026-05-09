/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.graphics.Color;

import me.stringdotjar.flixelgdx.functional.FlixelColorable;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelColor;

import org.jetbrains.annotations.Nullable;

/**
 * Interpolates between two colors and optionally applies tint to a {@link FlixelColorable}.
 */
public class FlixelColorTween extends FlixelTween {

  protected final Color workFrom = new Color();
  protected final Color workTo = new Color();
  protected final Color workOut = new Color();

  @Nullable protected FlixelColor fromFlixel;
  @Nullable protected FlixelColor toFlixel;
  protected boolean useRawColor;
  @Nullable protected FlixelColorable colorTarget;
  @Nullable protected Runnable onColor;

  public FlixelColorTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Tween between two {@link FlixelColor} endpoints.
   *
   * @param target The tint target to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpoints(@Nullable FlixelColorable target, @Nullable FlixelColor from, @Nullable FlixelColor to, @Nullable Runnable onColor) {
    this.useRawColor = false;
    this.colorTarget = target;
    this.fromFlixel = from;
    this.toFlixel = to;
    this.onColor = onColor;
    return this;
  }

  /**
   * Tween between two libGDX {@link Color} values (copied into internal buffers).
   *
   * @param target The tint target to tween.
   * @param from The starting color.
   * @param to The ending color.
   * @param onColor The callback to run when the tween is complete.
   * @return {@code this} for chaining.
   */
  public FlixelColorTween setColorEndpointsRaw(@Nullable FlixelColorable target, @Nullable Color from, @Nullable Color to, @Nullable Runnable onColor) {
    this.useRawColor = true;
    this.colorTarget = target;
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

    if (colorTarget != null) {
      colorTarget.setColor(workOut);
    }
    if (onColor != null) {
      onColor.run();
    }
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (colorTarget == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, colorTarget);
    }
    return Objects.equals(object, colorTarget) && "color".equals(field);
  }

  @Override
  public void reset() {
    super.reset();
    colorTarget = null;
    fromFlixel = null;
    toFlixel = null;
    onColor = null;
    useRawColor = false;
  }
}
