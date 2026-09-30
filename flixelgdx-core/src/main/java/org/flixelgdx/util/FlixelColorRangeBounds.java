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

import org.flixelgdx.tween.ease.FlixelEase;
import org.flixelgdx.tween.ease.FlixelEaseFunction;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A color that starts as one shade and ends as another over a particle's lifetime, with some
 * randomness at each end.
 *
 * <p>This is the color version of {@link FlixelRangeBounds}. Only the red, green, and blue channels
 * are used; transparency is controlled separately by the emitter's {@code alpha} range so the two
 * can follow different curves.
 *
 * <p>If {@link #end} holds the same colors as {@link #start}, the particle keeps its start color for
 * its whole life. Set {@link #active} to {@code false} to have the emitter skip color entirely and
 * leave particles white.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Fire: start yellow, cool down to a dark red.
 * emitter.color.set(FlixelColor.YELLOW, FlixelColor.YELLOW, FlixelColor.RED, FlixelColor.MAROON);
 * }</pre>
 */
public class FlixelColorRangeBounds {

  /** The bounds the starting color is rolled from. */
  public final FlixelColorBounds start;

  /** The bounds the ending color is rolled from. */
  public final FlixelColorBounds end;

  /**
   * The curve used to move from the start color to the end color, or {@code null} for a straight
   * line. Any function from {@link FlixelEase} works, such as {@code FlixelEase::quadOut}.
   */
  @Nullable
  public FlixelEaseFunction ease;

  /** Whether the emitter applies this property to new particles at all. */
  public boolean active = true;

  /**
   * Creates a range where every particle gets the same color for its whole life.
   *
   * @param color The color used everywhere.
   */
  public FlixelColorRangeBounds(@NotNull FlixelColor color) {
    this(color, color, color, color);
  }

  /**
   * Creates a range with separate start and end bounds.
   *
   * @param startMin One end of the starting colors.
   * @param startMax The other end of the starting colors.
   * @param endMin One end of the ending colors.
   * @param endMax The other end of the ending colors.
   */
  public FlixelColorRangeBounds(@NotNull FlixelColor startMin, @NotNull FlixelColor startMax,
      @NotNull FlixelColor endMin, @NotNull FlixelColor endMax) {
    this.start = new FlixelColorBounds(startMin, startMax);
    this.end = new FlixelColorBounds(endMin, endMax);
  }

  /**
   * Sets every end of both bounds to one color, so every particle gets it for its whole life.
   *
   * @param color The color to use everywhere.
   * @return {@code this} range for chaining.
   */
  public FlixelColorRangeBounds set(@NotNull FlixelColor color) {
    return set(color, color, color, color);
  }

  /**
   * Sets the start bounds and copies them to the end, so each particle keeps its rolled color for
   * its whole life.
   *
   * @param startMin One end of the starting colors.
   * @param startMax The other end of the starting colors.
   * @return {@code this} range for chaining.
   */
  public FlixelColorRangeBounds set(@NotNull FlixelColor startMin, @NotNull FlixelColor startMax) {
    return set(startMin, startMax, startMin, startMax);
  }

  /**
   * Sets separate start and end bounds.
   *
   * @param startMin One end of the starting colors.
   * @param startMax The other end of the starting colors.
   * @param endMin One end of the ending colors.
   * @param endMax The other end of the ending colors.
   * @return {@code this} range for chaining.
   */
  public FlixelColorRangeBounds set(@NotNull FlixelColor startMin, @NotNull FlixelColor startMax,
      @NotNull FlixelColor endMin, @NotNull FlixelColor endMax) {
    start.set(startMin, startMax);
    end.set(endMin, endMax);
    return this;
  }

  /**
   * Returns whether the color changes over a particle's life, meaning the end bounds differ from
   * the start bounds.
   *
   * @return {@code true} if {@link #end} holds different colors than {@link #start}.
   */
  public boolean changes() {
    return !start.matches(end);
  }
}
