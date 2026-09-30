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

import org.flixelgdx.tween.ease.FlixelEase;
import org.flixelgdx.tween.ease.FlixelEaseFunction;
import org.jetbrains.annotations.Nullable;

/**
 * A value that starts somewhere and ends somewhere else over a lifetime, with some randomness at
 * each end.
 *
 * <p>Range bounds are the "recipe" side of a {@link FlixelRange}: they hold two {@link FlixelBounds},
 * one for where a value may start and one for where it may end. Rolling a value from
 * {@link #start} and another from {@link #end} produces a concrete {@link FlixelRange} that an
 * object then slides along as it ages, following {@link #ease} (a straight line when
 * {@code null}). Particle emitters use this to make each particle fade out, shrink, or slow down
 * a little differently.
 *
 * <p>By convention, if {@link #end} holds the same bounds as {@link #start} (see
 * {@link #changes()}), the value should stay at its start roll for the whole lifetime instead of
 * rolling a second one. {@link #active} lets the owner switch the whole property off.
 *
 * <p>Example:
 *
 * <pre>{@code
 * emitter.alpha.set(1f, 1f, 0f, 0f);  // Start fully visible, fade to invisible.
 * emitter.alpha.ease = FlixelEase::quadIn; // Stay visible longer, then fade quickly.
 * emitter.speed.set(100f, 250f);      // A random speed that stays the same all life.
 * }</pre>
 */
public class FlixelRangeBounds {

  /** The bounds the starting value is rolled from. */
  public final FlixelBounds start;

  /** The bounds the ending value is rolled from. */
  public final FlixelBounds end;

  /**
   * The curve used to move from the start value to the end value, or {@code null} for a straight
   * line. Any function from {@link FlixelEase} works, such as {@code FlixelEase::quadOut}.
   */
  @Nullable
  public FlixelEaseFunction ease;

  /** Whether the emitter applies this property to new particles at all. */
  public boolean active = true;

  /**
   * Creates a range where every value is the same.
   *
   * @param value The value used for both ends of both bounds.
   */
  public FlixelRangeBounds(float value) {
    this(value, value, value, value);
  }

  /**
   * Creates a range whose start and end are rolled from the same bounds, so the value stays
   * constant over a particle's life.
   *
   * @param startMin The smallest starting value.
   * @param startMax The largest starting value.
   */
  public FlixelRangeBounds(float startMin, float startMax) {
    this(startMin, startMax, startMin, startMax);
  }

  /**
   * Creates a range with separate start and end bounds.
   *
   * @param startMin The smallest starting value.
   * @param startMax The largest starting value.
   * @param endMin The smallest ending value.
   * @param endMax The largest ending value.
   */
  public FlixelRangeBounds(float startMin, float startMax, float endMin, float endMax) {
    this.start = new FlixelBounds(startMin, startMax);
    this.end = new FlixelBounds(endMin, endMax);
  }

  /**
   * Sets every end of both bounds to one value, so every particle gets it for its whole life.
   *
   * @param value The value to use everywhere.
   * @return {@code this} range for chaining.
   */
  public FlixelRangeBounds set(float value) {
    return set(value, value, value, value);
  }

  /**
   * Sets the start bounds and copies them to the end, so each particle keeps its rolled value for
   * its whole life.
   *
   * @param startMin The smallest starting value.
   * @param startMax The largest starting value.
   * @return {@code this} range for chaining.
   */
  public FlixelRangeBounds set(float startMin, float startMax) {
    return set(startMin, startMax, startMin, startMax);
  }

  /**
   * Sets separate start and end bounds.
   *
   * @param startMin The smallest starting value.
   * @param startMax The largest starting value.
   * @param endMin The smallest ending value.
   * @param endMax The largest ending value.
   * @return {@code this} range for chaining.
   */
  public FlixelRangeBounds set(float startMin, float startMax, float endMin, float endMax) {
    start.set(startMin, startMax);
    end.set(endMin, endMax);
    return this;
  }

  /**
   * Returns whether the value changes over a particle's life, meaning the end bounds differ from
   * the start bounds.
   *
   * @return {@code true} if {@link #end} holds different bounds than {@link #start}.
   */
  public boolean changes() {
    return !start.matches(end);
  }
}
