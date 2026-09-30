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
 * A start value and an end value that something slides between over its lifetime.
 *
 * <p>Where {@link FlixelBounds} says "pick a number somewhere in here", a range holds two numbers
 * that were already picked. Something that ages, such as a particle, calls {@link #lerp(float)}
 * with how far through its life it is (from {@code 0} to {@code 1}) to get its current value. A
 * common pattern is to roll a range out of {@link FlixelRangeBounds}, then read it every frame.
 *
 * <p>{@link #active} tells the owner whether to apply the range at all. A range whose start and
 * end match has nothing to animate, so owners usually leave it inactive and skip the math.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelRange fade = new FlixelRange(1f, 0f); // Fully visible to invisible.
 * float halfway = fade.lerp(0.5f);            // 0.5
 * }</pre>
 */
public class FlixelRange {

  /** The value at the start of the lifetime. */
  public float start;

  /** The value at the end of the lifetime. */
  public float end;

  /** Whether the owner should apply this range. */
  public boolean active;

  /** Creates an inactive range where both ends are zero. */
  public FlixelRange() {}

  /**
   * Creates a range between two values. It is active only if the values differ.
   *
   * @param start The value at the start of the lifetime.
   * @param end The value at the end of the lifetime.
   */
  public FlixelRange(float start, float end) {
    set(start, end);
  }

  /**
   * Sets both ends of this range and marks it active only if they differ.
   *
   * @param start The value at the start of the lifetime.
   * @param end The value at the end of the lifetime.
   * @return {@code this} range for chaining.
   */
  public FlixelRange set(float start, float end) {
    this.start = start;
    this.end = end;
    this.active = start != end;
    return this;
  }

  /**
   * Returns the value part of the way from {@link #start} to {@link #end}.
   *
   * @param t How far along the lifetime, where {@code 0} is the start and {@code 1} is the end.
   * @return The blended value.
   */
  public float lerp(float t) {
    return start + (end - start) * t;
  }
}
