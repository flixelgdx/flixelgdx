/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween.type.motion;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import me.stringdotjar.flixelgdx.tween.FlixelTween;
import me.stringdotjar.flixelgdx.tween.settings.FlixelTweenSettings;

import org.jetbrains.annotations.Nullable;

/** Piecewise-linear path through a polyline. */
public final class FlixelLinearPath extends FlixelMotion {

  private final Array<Vector2> points = new Array<>();
  private float[] cumulativeT = new float[8];
  private int pointCount;
  private float totalDistance;

  public FlixelLinearPath(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  @Override
  public void reset() {
    points.clear();
    pointCount = 0;
    super.reset();
  }

  public FlixelLinearPath addPoint(float x, float y) {
    points.add(new Vector2(x, y));
    return this;
  }

  /**
   * Sets the motion for the tween.
   *
   * @param durationOrSpeed The duration or speed of the motion.
   * @param useDuration If true, {@code durationOrSpeed} is seconds; if false, pixels per second.
   * @return {@code this} for chaining.
   */
  public FlixelLinearPath setMotion(float durationOrSpeed, boolean useDuration) {
    updatePath();
    if (tweenSettings != null) {
      if (useDuration) {
        tweenSettings.setDuration(Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR));
      } else {
        float speed = Math.max(durationOrSpeed, MathUtils.FLOAT_ROUNDING_ERROR);
        tweenSettings.setDuration(totalDistance / speed);
      }
    }
    return this;
  }

  private void ensureCumulativeCapacity(int n) {
    if (cumulativeT.length < n) {
      cumulativeT = new float[n + 8];
    }
  }

  private void updatePath() {
    if (points.size < 2) {
      throw new IllegalStateException("FlixelLinearPath needs at least two points.");
    }
    pointCount = points.size;
    totalDistance = 0f;
    for (int i = 1; i < points.size; i++) {
      totalDistance += points.get(i - 1).dst(points.get(i));
    }
    if (totalDistance <= 0f) {
      totalDistance = MathUtils.FLOAT_ROUNDING_ERROR;
    }
    ensureCumulativeCapacity(pointCount);
    float acc = 0f;
    cumulativeT[0] = 0f;
    for (int i = 1; i < pointCount; i++) {
      acc += points.get(i - 1).dst(points.get(i));
      cumulativeT[i] = acc / totalDistance;
    }
  }

  @Override
  public FlixelTween start() {
    if (points.size > 0) {
      Vector2 p = points.first();
      motionX = p.x;
      motionY = p.y;
    }
    return super.start();
  }

  @Override
  protected void computeMotion() {
    if (points.size < 2) {
      return;
    }
    float pathT = backward ? 1f - scale : scale;
    pathT = MathUtils.clamp(pathT, 0f, 1f);
    int seg = 0;
    while (seg < pointCount - 2 && pathT > cumulativeT[seg + 1]) {
      seg++;
    }
    float t0 = cumulativeT[seg];
    float t1 = cumulativeT[seg + 1];
    float u = (t1 - t0) > 1e-8f ? (pathT - t0) / (t1 - t0) : 0f;
    u = MathUtils.clamp(u, 0f, 1f);
    Vector2 a = points.get(seg);
    Vector2 b = points.get(seg + 1);
    motionX = a.x + (b.x - a.x) * u;
    motionY = a.y + (b.y - a.y) * u;
  }
}
