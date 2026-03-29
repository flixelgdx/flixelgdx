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

/**
 * Chain of quadratic Bézier segments: control points are {@code start, control, end, control, end, ...}
 * (odd total count, at least three points).
 */
public final class FlixelQuadPath extends FlixelMotion {

  private final Array<Vector2> points = new Array<>();
  private float[] cumulativeT = new float[8];
  private float[] segmentLen = new float[8];
  private int numSegs;
  private float totalDistance;

  public FlixelQuadPath(@Nullable FlixelTweenSettings settings) {
    super(settings);
  }

  @Override
  public void reset() {
    points.clear();
    numSegs = 0;
    super.reset();
  }

  public FlixelQuadPath addPoint(float x, float y) {
    points.add(new Vector2(x, y));
    return this;
  }

  public FlixelQuadPath setMotion(float durationOrSpeed, boolean useDuration) {
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

  private void ensureCap(int n) {
    if (cumulativeT.length < n) {
      cumulativeT = new float[n + 8];
      segmentLen = new float[n + 8];
    }
  }

  private void updatePath() {
    if (points.size < 3 || (points.size - 1) % 2 != 0) {
      throw new IllegalStateException(
          "FlixelQuadPath needs at least 3 points and an odd point count (start, control, end, ...).");
    }
    numSegs = (points.size - 1) / 2;
    ensureCap(numSegs + 1);
    totalDistance = 0f;
    for (int i = 0; i < numSegs; i++) {
      Vector2 a = points.get(i * 2);
      Vector2 b = points.get(i * 2 + 1);
      Vector2 c = points.get(i * 2 + 2);
      float len = approxQuadLength(a, b, c);
      segmentLen[i] = len;
      totalDistance += len;
    }
    if (totalDistance <= 0f) {
      totalDistance = MathUtils.FLOAT_ROUNDING_ERROR;
    }
    float acc = 0f;
    cumulativeT[0] = 0f;
    for (int i = 0; i < numSegs; i++) {
      acc += segmentLen[i];
      cumulativeT[i + 1] = acc / totalDistance;
    }
  }

  private static float approxQuadLength(Vector2 a, Vector2 b, Vector2 c) {
    return a.dst(b) + b.dst(c) + a.dst(c) * 0.5f;
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
    if (numSegs < 1 || points.size < 3) {
      return;
    }
    float pathT = backward ? 1f - scale : scale;
    pathT = MathUtils.clamp(pathT, 0f, 1f);
    int seg = 0;
    while (seg < numSegs - 1 && pathT > cumulativeT[seg + 1]) {
      seg++;
    }
    float t0 = cumulativeT[seg];
    float t1 = cumulativeT[seg + 1];
    float u = (t1 - t0) > 1e-8f ? (pathT - t0) / (t1 - t0) : 0f;
    u = MathUtils.clamp(u, 0f, 1f);
    Vector2 a = points.get(seg * 2);
    Vector2 b = points.get(seg * 2 + 1);
    Vector2 c = points.get(seg * 2 + 2);
    float s = u;
    float r = 1f - s;
    motionX = a.x * r * r + b.x * 2f * r * s + c.x * s * s;
    motionY = a.y * r * r + b.y * 2f * r * s + c.y * s * s;
  }
}
