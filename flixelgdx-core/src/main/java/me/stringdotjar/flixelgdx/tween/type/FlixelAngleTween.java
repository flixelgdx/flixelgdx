/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type;

import java.util.Objects;

import me.stringdotjar.flixelgdx.FlixelObject;
import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/**
 * Tweens {@link FlixelObject#getAngle()} toward an end angle (degrees).
 */
public class FlixelAngleTween extends FlixelTween {

  protected @Nullable FlixelObject sprite;
  protected float fromAngle;
  protected float toAngle;

  public FlixelAngleTween(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the {@link FlixelObject} and angles to tween.
   *
   * @param fromAngle Use {@link Float#NaN} to take the sprite's current angle at {@link #start()}.
   */
  public FlixelAngleTween setAngles(@Nullable FlixelObject sprite, float fromAngle, float toAngle) {
    this.sprite = sprite;
    this.fromAngle = fromAngle;
    this.toAngle = toAngle;
    return this;
  }

  @Override
  public FlixelTween start() {
    super.start();
    if (sprite != null && Float.isNaN(fromAngle)) {
      fromAngle = sprite.getAngle();
    }
    return this;
  }

  @Override
  protected void updateTweenValues() {
    if (sprite == null) {
      return;
    }
    float a = fromAngle + (toAngle - fromAngle) * scale;
    sprite.setAngle(a);
  }

  @Override
  public boolean isTweenOf(Object object, String field) {
    if (sprite == null) {
      return false;
    }
    if (field == null || field.isEmpty()) {
      return Objects.equals(object, sprite);
    }
    return Objects.equals(object, sprite) && "angle".equals(field);
  }

  @Override
  public void reset() {
    super.reset();
    sprite = null;
    fromAngle = 0f;
    toAngle = 0f;
  }
}
