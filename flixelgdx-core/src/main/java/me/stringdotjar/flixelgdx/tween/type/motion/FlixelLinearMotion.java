/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type.motion;

import com.badlogic.gdx.math.MathUtils;

import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/** Straight-line motion from ({@code fromX}, {@code fromY}) to ({@code toX}, {@code toY}). */
public final class FlixelLinearMotion extends FlixelMotion {

  private float fromX;
  private float fromY;
  private float moveX;
  private float moveY;

  public FlixelLinearMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the motion for the tween.
   *
   * @param fromX The starting X position.
   * @param fromY The starting Y position.
   * @param toX The ending X position.
   * @param toY The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelLinearMotion setMotion(
      float fromX, float fromY, float toX, float toY, float durationOrSpeed, boolean useDuration) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.moveX = toX - fromX;
    this.moveY = toY - fromY;
    motionX = fromX;
    motionY = fromY;
    float dist = (float) Math.sqrt(moveX * moveX + moveY * moveY);
    if (tweenSettings != null) {
      if (useDuration) {
        tweenSettings.setDuration(Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR));
      } else {
        float speed = Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR);
        tweenSettings.setDuration(dist / speed);
      }
    }
    return this;
  }

  @Override
  protected void computeMotion() {
    motionX = fromX + moveX * scale;
    motionY = fromY + moveY * scale;
  }
}
