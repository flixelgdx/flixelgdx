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
 * A 2D value that starts somewhere and ends somewhere else over a lifetime, with some randomness at
 * each end.
 *
 * <p>This is the two-axis version of {@link FlixelRangeBounds}, used for properties with an X and a
 * Y, such as velocity and scale. Rolling a point from {@link #start} and another from {@link #end}
 * produces a concrete {@link FlixelPointRange} to slide along, following {@link #ease}.
 *
 * <p>By convention, if {@link #end} holds the same bounds as {@link #start} (see
 * {@link #changes()}), the value should stay at its start roll for the whole lifetime.
 * {@link #active} lets the owner switch the whole property off.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Start at normal size and grow to double size.
 * emitter.scale.set(1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f);
 * }</pre>
 */
public class FlixelPointRangeBounds {

  /** The bounds the starting point is rolled from. */
  public final FlixelPointBounds start;

  /** The bounds the ending point is rolled from. */
  public final FlixelPointBounds end;

  /**
   * The curve used to move from the start point to the end point, or {@code null} for a straight
   * line. Any function from {@link FlixelEase} works, such as {@code FlixelEase::quadOut}.
   */
  @Nullable
  public FlixelEaseFunction ease;

  /** Whether the emitter applies this property to new particles at all. */
  public boolean active = true;

  /**
   * Creates a range where every value is the same point.
   *
   * @param x The X used everywhere.
   * @param y The Y used everywhere.
   */
  public FlixelPointRangeBounds(float x, float y) {
    this(x, y, x, y, x, y, x, y);
  }

  /**
   * Creates a range whose start and end are rolled from the same bounds, so the value stays
   * constant over a particle's life.
   *
   * @param startMinX The smallest starting X.
   * @param startMinY The smallest starting Y.
   * @param startMaxX The largest starting X.
   * @param startMaxY The largest starting Y.
   */
  public FlixelPointRangeBounds(float startMinX, float startMinY, float startMaxX, float startMaxY) {
    this(startMinX, startMinY, startMaxX, startMaxY, startMinX, startMinY, startMaxX, startMaxY);
  }

  /**
   * Creates a range with separate start and end bounds.
   *
   * @param startMinX The smallest starting X.
   * @param startMinY The smallest starting Y.
   * @param startMaxX The largest starting X.
   * @param startMaxY The largest starting Y.
   * @param endMinX The smallest ending X.
   * @param endMinY The smallest ending Y.
   * @param endMaxX The largest ending X.
   * @param endMaxY The largest ending Y.
   */
  public FlixelPointRangeBounds(float startMinX, float startMinY, float startMaxX, float startMaxY,
      float endMinX, float endMinY, float endMaxX, float endMaxY) {
    this.start = new FlixelPointBounds(startMinX, startMinY, startMaxX, startMaxY);
    this.end = new FlixelPointBounds(endMinX, endMinY, endMaxX, endMaxY);
  }

  /**
   * Sets every end of both bounds to one point, so every particle gets it for its whole life.
   *
   * @param x The X to use everywhere.
   * @param y The Y to use everywhere.
   * @return {@code this} range for chaining.
   */
  public FlixelPointRangeBounds set(float x, float y) {
    return set(x, y, x, y, x, y, x, y);
  }

  /**
   * Sets the start bounds and copies them to the end, so each particle keeps its rolled value for
   * its whole life.
   *
   * @param startMinX The smallest starting X.
   * @param startMinY The smallest starting Y.
   * @param startMaxX The largest starting X.
   * @param startMaxY The largest starting Y.
   * @return {@code this} range for chaining.
   */
  public FlixelPointRangeBounds set(float startMinX, float startMinY, float startMaxX, float startMaxY) {
    return set(startMinX, startMinY, startMaxX, startMaxY, startMinX, startMinY, startMaxX, startMaxY);
  }

  /**
   * Sets separate start and end bounds.
   *
   * @param startMinX The smallest starting X.
   * @param startMinY The smallest starting Y.
   * @param startMaxX The largest starting X.
   * @param startMaxY The largest starting Y.
   * @param endMinX The smallest ending X.
   * @param endMinY The smallest ending Y.
   * @param endMaxX The largest ending X.
   * @param endMaxY The largest ending Y.
   * @return {@code this} range for chaining.
   */
  public FlixelPointRangeBounds set(float startMinX, float startMinY, float startMaxX, float startMaxY,
      float endMinX, float endMinY, float endMaxX, float endMaxY) {
    start.set(startMinX, startMinY, startMaxX, startMaxY);
    end.set(endMinX, endMinY, endMaxX, endMaxY);
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
