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

/** Quadratic Bézier motion (one control point). */
public final class FlixelQuadMotion extends FlixelMotion {

  private float fromX;
  private float fromY;
  private float cx;
  private float cy;
  private float toX;
  private float toY;

  public FlixelQuadMotion(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  /**
   * Sets the motion for the tween.
   *
   * @param fromX The starting X position.
   * @param fromY The starting Y position.
   * @param cx The X coordinate of the control point.
   * @param cy The Y coordinate of the control point.
   * @param toX The ending X position.
   * @param toY The ending Y position.
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelQuadMotion setMotion(
      float fromX,
      float fromY,
      float cx,
      float cy,
      float toX,
      float toY,
      float durationOrSpeed,
      boolean useDuration) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.cx = cx;
    this.cy = cy;
    this.toX = toX;
    this.toY = toY;
    motionX = fromX;
    motionY = fromY;
    float dist = approximateQuadLength(fromX, fromY, cx, cy, toX, toY);
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

  private static float approximateQuadLength(float x0, float y0, float x1, float y1, float x2, float y2) {
    float d1 = Vector2len(x1 - x0, y1 - y0);
    float d2 = Vector2len(x2 - x1, y2 - y1);
    float d3 = Vector2len(x2 - x0, y2 - y0);
    return (d1 + d2 + d3) * 0.5f;
  }

  private static float Vector2len(float dx, float dy) {
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  @Override
  protected void computeMotion() {
    float t = scale;
    float u = 1f - t;
    motionX = fromX * u * u + cx * 2f * u * t + toX * t * t;
    motionY = fromY * u * u + cy * 2f * u * t + toY * t * t;
  }
}
