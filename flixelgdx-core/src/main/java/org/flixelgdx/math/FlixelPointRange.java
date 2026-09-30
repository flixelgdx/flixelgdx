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
 * A start point and an end point that something slides between over its lifetime.
 *
 * <p>This is the two-axis version of {@link FlixelRange}, used for properties with an X and a Y,
 * such as velocity and scale. Call {@link #lerpX(float)} and {@link #lerpY(float)} with how far
 * through its life something is to get its current value on each axis.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelPointRange grow = new FlixelPointRange(1f, 1f, 2f, 2f); // Normal size to double size.
 * float width = grow.lerpX(0.25f) * 16f;                       // 1.25 times a 16 pixel frame.
 * }</pre>
 */
public class FlixelPointRange {

  /** The X value at the start of the lifetime. */
  public float startX;

  /** The Y value at the start of the lifetime. */
  public float startY;

  /** The X value at the end of the lifetime. */
  public float endX;

  /** The Y value at the end of the lifetime. */
  public float endY;

  /** Whether the owner should apply this range. */
  public boolean active;

  /** Creates an inactive range where every value is zero. */
  public FlixelPointRange() {}

  /**
   * Creates a range between two points. It is active only if the points differ.
   *
   * @param startX The X value at the start of the lifetime.
   * @param startY The Y value at the start of the lifetime.
   * @param endX The X value at the end of the lifetime.
   * @param endY The Y value at the end of the lifetime.
   */
  public FlixelPointRange(float startX, float startY, float endX, float endY) {
    set(startX, startY, endX, endY);
  }

  /**
   * Sets both ends of this range and marks it active only if they differ.
   *
   * @param startX The X value at the start of the lifetime.
   * @param startY The Y value at the start of the lifetime.
   * @param endX The X value at the end of the lifetime.
   * @param endY The Y value at the end of the lifetime.
   * @return {@code this} range for chaining.
   */
  public FlixelPointRange set(float startX, float startY, float endX, float endY) {
    this.startX = startX;
    this.startY = startY;
    this.endX = endX;
    this.endY = endY;
    this.active = startX != endX || startY != endY;
    return this;
  }

  /**
   * Returns the X value part of the way from {@link #startX} to {@link #endX}.
   *
   * @param t How far along the lifetime, where {@code 0} is the start and {@code 1} is the end.
   * @return The blended X value.
   */
  public float lerpX(float t) {
    return startX + (endX - startX) * t;
  }

  /**
   * Returns the Y value part of the way from {@link #startY} to {@link #endY}.
   *
   * @param t How far along the lifetime, where {@code 0} is the start and {@code 1} is the end.
   * @return The blended Y value.
   */
  public float lerpY(float t) {
    return startY + (endY - startY) * t;
  }
}
