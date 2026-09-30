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
package org.flixelgdx.particle;

/**
 * A pair of numbers that a random value is picked between.
 *
 * <p>An emitter rolls a fresh value between {@link #min} and {@link #max} for every particle it
 * emits. Setting both to the same number turns the roll off, so every particle gets exactly that
 * value.
 *
 * <p>Example:
 *
 * <pre>{@code
 * emitter.lifespan.set(0.5f, 1.5f); // Each particle lives between half a second and 1.5 seconds.
 * emitter.launchAngle.set(-90f);    // Every particle launches straight up.
 * }</pre>
 */
public class FlixelBounds {

  /** The smallest value a roll can produce. */
  public float min;

  /** The largest value a roll can produce. */
  public float max;

  /**
   * Creates bounds where both ends are the same value.
   *
   * @param value The value used for both {@link #min} and {@link #max}.
   */
  public FlixelBounds(float value) {
    this(value, value);
  }

  /**
   * Creates bounds between two values.
   *
   * @param min The smallest value a roll can produce.
   * @param max The largest value a roll can produce.
   */
  public FlixelBounds(float min, float max) {
    this.min = min;
    this.max = max;
  }

  /**
   * Sets both ends to the same value, so every roll returns it.
   *
   * @param value The value used for both {@link #min} and {@link #max}.
   * @return {@code this} bounds for chaining.
   */
  public FlixelBounds set(float value) {
    return set(value, value);
  }

  /**
   * Sets both ends of these bounds.
   *
   * @param min The smallest value a roll can produce.
   * @param max The largest value a roll can produce.
   * @return {@code this} bounds for chaining.
   */
  public FlixelBounds set(float min, float max) {
    this.min = min;
    this.max = max;
    return this;
  }

  /**
   * Copies both ends from other bounds.
   *
   * @param other The bounds to copy from.
   * @return {@code this} bounds for chaining.
   */
  public FlixelBounds set(FlixelBounds other) {
    return set(other.min, other.max);
  }

  /**
   * Returns whether these bounds hold exactly the same values as other bounds.
   *
   * @param other The bounds to compare against.
   * @return {@code true} if both {@link #min} and {@link #max} match.
   */
  public boolean matches(FlixelBounds other) {
    return other != null && min == other.min && max == other.max;
  }
}
