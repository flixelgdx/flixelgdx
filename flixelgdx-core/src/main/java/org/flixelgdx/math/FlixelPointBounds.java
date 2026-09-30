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
 * A pair of 2D points that a random point is picked between, one axis at a time.
 *
 * <p>An emitter rolls a fresh X between {@link #minX} and {@link #maxX}, and a fresh Y between
 * {@link #minY} and {@link #maxY}, for every particle it emits. Setting the minimum and maximum of
 * an axis to the same number turns the roll off for that axis.
 *
 * <p>Example:
 *
 * <pre>{@code
 * emitter.acceleration.set(0f, 400f);            // Constant gravity pulling down.
 * emitter.drag.set(10f, 10f, 40f, 40f);          // Each particle slows down at a random rate.
 * }</pre>
 */
public class FlixelPointBounds {

  /** The smallest X a roll can produce. */
  public float minX;

  /** The smallest Y a roll can produce. */
  public float minY;

  /** The largest X a roll can produce. */
  public float maxX;

  /** The largest Y a roll can produce. */
  public float maxY;

  /**
   * Creates bounds where both ends are the same point.
   *
   * @param x The X used for both {@link #minX} and {@link #maxX}.
   * @param y The Y used for both {@link #minY} and {@link #maxY}.
   */
  public FlixelPointBounds(float x, float y) {
    this(x, y, x, y);
  }

  /**
   * Creates bounds between two points.
   *
   * @param minX The smallest X a roll can produce.
   * @param minY The smallest Y a roll can produce.
   * @param maxX The largest X a roll can produce.
   * @param maxY The largest Y a roll can produce.
   */
  public FlixelPointBounds(float minX, float minY, float maxX, float maxY) {
    this.minX = minX;
    this.minY = minY;
    this.maxX = maxX;
    this.maxY = maxY;
  }

  /**
   * Sets both ends to the same point, so every roll returns it.
   *
   * @param x The X used for both {@link #minX} and {@link #maxX}.
   * @param y The Y used for both {@link #minY} and {@link #maxY}.
   * @return {@code this} bounds for chaining.
   */
  public FlixelPointBounds set(float x, float y) {
    return set(x, y, x, y);
  }

  /**
   * Sets both ends of these bounds.
   *
   * @param minX The smallest X a roll can produce.
   * @param minY The smallest Y a roll can produce.
   * @param maxX The largest X a roll can produce.
   * @param maxY The largest Y a roll can produce.
   * @return {@code this} bounds for chaining.
   */
  public FlixelPointBounds set(float minX, float minY, float maxX, float maxY) {
    this.minX = minX;
    this.minY = minY;
    this.maxX = maxX;
    this.maxY = maxY;
    return this;
  }

  /**
   * Returns whether these bounds hold exactly the same values as other bounds.
   *
   * @param other The bounds to compare against.
   * @return {@code true} if every minimum and maximum matches.
   */
  public boolean matches(FlixelPointBounds other) {
    return other != null && minX == other.minX && minY == other.minY && maxX == other.maxX && maxY == other.maxY;
  }
}
