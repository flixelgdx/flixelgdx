/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.math;

/**
 * Static math helpers used across FlixelGDX.
 *
 * <p>These are the general-purpose math routines games reach for every frame:
 * clamping, interpolation, angle work, and a handful of game-feel helpers
 * ({@link #approach(float, float, float)}, {@link #wrap(float, float, float)},
 * {@link #remap}). Everything here is allocation-free and safe to call inside
 * update or render loops.
 *
 * <p>Trigonometry ({@link #sin(float)} / {@link #cos(float)}) is served from a
 * precomputed lookup table. That trades a little accuracy for a large speed win,
 * which is the right call for gameplay math (movement, oscillation, orbiting)
 * where being off by a fraction of a degree is invisible. When you need full
 * precision, call {@link Math#sin(double)} directly instead.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Ease a health bar toward its target without overshooting.
 * displayed = FlixelMathUtil.approach(displayed, target, 120f * elapsed);
 *
 * // Keep an angle in the -180..180 range.
 * float a = FlixelMathUtil.wrap(angle, -180f, 180f);
 * }</pre>
 */
public final class FlixelMathUtil {

  /**
   * The mathematical constant pi (the ratio of a circle's circumference to its
   * diameter) as a {@code float}.
   */
  public static final float PI = (float) Math.PI;

  /**
   * Two times {@link #PI}, i.e. a full turn in radians.
   */
  public static final float PI2 = PI * 2f;

  /**
   * Half of {@link #PI}, i.e. a quarter turn in radians.
   */
  public static final float HALF_PI = PI / 2f;

  /**
   * Multiplier that converts an angle in degrees to radians.
   */
  public static final float DEG_TO_RAD = PI / 180f;

  /**
   * Multiplier that converts an angle in radians to degrees.
   */
  public static final float RAD_TO_DEG = 180f / PI;

  /**
   * A tiny tolerance used as the default when comparing floats for near
   * equality (see {@link #isEqual(float, float)}).
   */
  public static final float FLOAT_ROUNDING_ERROR = 0.000001f;

  private static final int SIN_BITS = 14;
  private static final int SIN_MASK = ~(-1 << SIN_BITS);
  private static final int SIN_COUNT = SIN_MASK + 1;
  private static final float RAD_TO_INDEX = SIN_COUNT / PI2;
  private static final float[] SIN_TABLE = new float[SIN_COUNT];

  static {
    for (int i = 0; i < SIN_COUNT; i++) {
      SIN_TABLE[i] = (float) Math.sin((i + 0.5f) / SIN_COUNT * PI2);
    }
    // Pin the exact cardinal angles so results at 0, 90, 180, and 270 degrees
    // are exact rather than table-approximated.
    for (int i = 0; i < 360; i += 90) {
      SIN_TABLE[(int) (i * DEG_TO_RAD * RAD_TO_INDEX) & SIN_MASK] = (float) Math.sin(i * DEG_TO_RAD);
    }
  }

  /**
   * Returns the sine of the given angle using a fast lookup table.
   *
   * @param radians The angle, in radians.
   * @return The approximate sine of {@code radians}, in the range -1 to 1.
   */
  public static float sin(float radians) {
    return SIN_TABLE[(int) (radians * RAD_TO_INDEX) & SIN_MASK];
  }

  /**
   * Returns the cosine of the given angle using a fast lookup table.
   *
   * @param radians The angle, in radians.
   * @return The approximate cosine of {@code radians}, in the range -1 to 1.
   */
  public static float cos(float radians) {
    return SIN_TABLE[(int) ((radians + HALF_PI) * RAD_TO_INDEX) & SIN_MASK];
  }

  /**
   * Returns the sine of an angle given in degrees, using the fast lookup table.
   *
   * @param degrees The angle, in degrees.
   * @return The approximate sine of {@code degrees}, in the range -1 to 1.
   */
  public static float sinDeg(float degrees) {
    return sin(degrees * DEG_TO_RAD);
  }

  /**
   * Returns the cosine of an angle given in degrees, using the fast lookup
   * table.
   *
   * @param degrees The angle, in degrees.
   * @return The approximate cosine of {@code degrees}, in the range -1 to 1.
   */
  public static float cosDeg(float degrees) {
    return cos(degrees * DEG_TO_RAD);
  }

  /**
   * Returns the angle, in radians, of the vector from the origin to
   * {@code (x, y)}.
   *
   * <p>This is a thin, full-precision wrapper over {@link Math#atan2(double,
   * double)} that returns a {@code float}, provided for convenience alongside
   * the rest of the framework's angle helpers.
   *
   * @param y The vertical component.
   * @param x The horizontal component.
   * @return The angle in radians, in the range -pi to pi.
   */
  public static float atan2(float y, float x) {
    return (float) Math.atan2(y, x);
  }

  /**
   * Clamps an {@code int} to the inclusive range {@code [min, max]}.
   *
   * @param value The value to clamp.
   * @param min The lowest allowed result.
   * @param max The highest allowed result.
   * @return {@code value} pinned into the range.
   */
  public static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    return Math.min(value, max);
  }

  /**
   * Clamps a {@code float} to the inclusive range {@code [min, max]}.
   *
   * @param value The value to clamp.
   * @param min The lowest allowed result.
   * @param max The highest allowed result.
   * @return {@code value} pinned into the range.
   */
  public static float clamp(float value, float min, float max) {
    if (value < min) {
      return min;
    }
    return Math.min(value, max);
  }

  /**
   * Linearly interpolates between two values.
   *
   * @param from The value returned when {@code t} is 0.
   * @param to The value returned when {@code t} is 1.
   * @param t The interpolation factor, normally in the range 0 to 1.
   * @return The interpolated value.
   */
  public static float lerp(float from, float to, float t) {
    return from + (to - from) * t;
  }

  /**
   * Linearly interpolates between two angles (in degrees), taking the shortest
   * way around the circle.
   *
   * <p>Unlike a plain {@link #lerp(float, float, float)}, this handles the wrap
   * from 359 degrees to 0 degrees correctly, so rotating from 350 to 10 sweeps
   * 20 degrees forward instead of 340 degrees backward.
   *
   * @param fromDeg The starting angle, in degrees.
   * @param toDeg The target angle, in degrees.
   * @param t The interpolation factor, normally in the range 0 to 1.
   * @return The interpolated angle, in degrees.
   */
  public static float lerpAngle(float fromDeg, float toDeg, float t) {
    float delta = ((toDeg - fromDeg) % 360f + 540f) % 360f - 180f;
    return fromDeg + delta * t;
  }

  /**
   * Moves a value toward a target by at most {@code maxDelta}, never
   * overshooting.
   *
   * <p>This is the classic "ease without a spring" helper: call it every frame
   * with {@code maxDelta = speed * elapsed} to slide a value toward a goal at a
   * constant rate and stop exactly on it.
   *
   * @param value The current value.
   * @param target The value to move toward.
   * @param maxDelta The largest step allowed this call (should be non-negative).
   * @return The value stepped toward {@code target}, clamped so it never passes it.
   */
  public static float approach(float value, float target, float maxDelta) {
    if (value < target) {
      return Math.min(value + maxDelta, target);
    }
    return Math.max(value - maxDelta, target);
  }

  /**
   * Wraps a value into the half-open range {@code [min, max)}, cycling instead
   * of clamping.
   *
   * <p>Where {@link #clamp(float, float, float)} pins a value to the edge, this
   * wraps it around, which is what you want for angles or looping coordinates.
   *
   * @param value The value to wrap.
   * @param min The inclusive lower bound.
   * @param max The exclusive upper bound (must be greater than {@code min}).
   * @return {@code value} wrapped into the range.
   */
  public static float wrap(float value, float min, float max) {
    float range = max - min;
    return min + ((value - min) % range + range) % range;
  }

  /**
   * Remaps a value from one range into another, preserving its relative
   * position.
   *
   * <p>For example, remapping 5 from {@code [0, 10]} into {@code [0, 100]}
   * yields 50. The result is not clamped, so inputs outside the source range
   * map outside the destination range.
   *
   * @param value The value to remap.
   * @param fromMin The lower bound of the source range.
   * @param fromMax The upper bound of the source range.
   * @param toMin The lower bound of the destination range.
   * @param toMax The upper bound of the destination range.
   * @return The remapped value.
   */
  public static float remap(float value, float fromMin, float fromMax, float toMin, float toMax) {
    return toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin);
  }

  /**
   * Snaps a value to the nearest multiple of {@code step} (grid snapping).
   *
   * @param value The value to snap.
   * @param step The grid spacing. A step of 0 or less returns {@code value}
   *     unchanged.
   * @return The nearest multiple of {@code step}.
   */
  public static float snap(float value, float step) {
    if (step <= 0f) {
      return value;
    }
    return Math.round(value / step) * step;
  }

  /**
   * Returns the sign of a value as -1, 0, or 1.
   *
   * @param value The value to inspect.
   * @return -1 if negative, 1 if positive, and 0 if exactly zero.
   */
  public static int signOf(float value) {
    return Float.compare(value, 0f);
  }

  /**
   * Reports whether two floats are equal within the default tolerance
   * {@link #FLOAT_ROUNDING_ERROR}.
   *
   * @param a The first value.
   * @param b The second value.
   * @return {@code true} if the values are nearly equal.
   */
  public static boolean isEqual(float a, float b) {
    return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
  }

  /**
   * Reports whether two floats are equal within a small tolerance.
   *
   * <p>Prefer this over {@code ==} when comparing results of floating-point
   * math, which rarely land on exact values.
   *
   * @param a The first value.
   * @param b The second value.
   * @param epsilon The largest difference still considered equal.
   * @return {@code true} if the values differ by no more than {@code epsilon}.
   */
  public static boolean isEqual(float a, float b, float epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

  /**
   * Rounds a float value to a specified number of decimal places.
   *
   * @param value The float value to round.
   * @param decimalPlaces The number of decimal places to round to.
   * @return The rounded float value.
   */
  public static float round(float value, int decimalPlaces) {
    float scale = (float) Math.pow(10, decimalPlaces);
    return Math.round(value * scale) / scale;
  }

  private FlixelMathUtil() {}
}
