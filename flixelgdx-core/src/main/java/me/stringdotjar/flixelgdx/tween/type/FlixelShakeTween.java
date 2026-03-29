/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import com.badlogic.gdx.math.MathUtils;

import me.stringdotjar.flixelgdx.FlixelSprite;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;
import me.stringdotjar.flixelgdx.util.FlixelAxes;

import org.jetbrains.annotations.Nullable;

/**
 * Random shake on a sprite's {@link FlixelSprite#getOffsetX()} / {@link FlixelSprite#getOffsetY()}.
 */
public class FlixelShakeTween extends FlixelTween {

  protected @Nullable FlixelSprite sprite;
  protected FlixelAxes axes = FlixelAxes.XY;

  // Peak offset magnitude in pixels.
  protected float intensity = 4f;
  protected float savedOffsetX;
  protected float savedOffsetY;

  public FlixelShakeTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the {@link FlixelSprite} and axes to shake.
   *
   * @param sprite The sprite to shake.
   * @param axes The directions to shake.
   * @param intensity The intensity of the shake. This is typically a
   *   very small number, such as 0.01f.
   * @return {@code this} for chaining.
   */
  public FlixelShakeTween setShake(@Nullable FlixelSprite sprite, FlixelAxes axes, float intensity) {
    this.sprite = sprite;
    this.axes = axes != null ? axes : FlixelAxes.XY;
    this.intensity = intensity;
    return this;
  }

  @Override
  public FlixelTween start() {
    super.start();
    if (sprite != null) {
      savedOffsetX = sprite.getOffsetX();
      savedOffsetY = sprite.getOffsetY();
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (sprite == null) {
      return;
    }
    float ix = (axes == FlixelAxes.Y) ? 0f : MathUtils.random(-intensity, intensity) * (1f - scale);
    float iy = (axes == FlixelAxes.X) ? 0f : MathUtils.random(-intensity, intensity) * (1f - scale);
    sprite.setOffset(savedOffsetX + ix, savedOffsetY + iy);
  }

  @Override
  public void reset() {
    restoreOffset();
    super.reset();
    sprite = null;
    axes = FlixelAxes.XY;
    intensity = 4f;
    savedOffsetX = 0f;
    savedOffsetY = 0f;
  }

  protected void restoreOffset() {
    if (sprite != null) {
      sprite.setOffset(savedOffsetX, savedOffsetY);
    }
  }

  @Override
  public FlixelTween cancel() {
    restoreOffset();
    return super.cancel();
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (sprite == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, sprite);
    }
    return Objects.equals(object, sprite) && field.equals("shake");
  }
}
