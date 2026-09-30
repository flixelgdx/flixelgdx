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
package org.flixelgdx.util;

import org.flixelgdx.math.FlixelBounds;
import org.jetbrains.annotations.NotNull;

/**
 * A pair of colors that a random color is picked between.
 *
 * <p>This is the color version of {@link FlixelBounds}. Rolling a color means picking one random
 * point along the line from {@link #min} to {@link #max} (see {@link FlixelColor#lerp(FlixelColor,
 * float)}), so all channels move together and the results stay between the two colors instead of
 * mixing into unrelated ones. Setting both to the same color turns the randomness off.
 *
 * <p>Both colors are owned copies: {@link #set(FlixelColor, FlixelColor)} copies the components in,
 * so changing a color you passed in later does not affect these bounds.
 *
 * @param min One end of the colors a roll can produce.
 * @param max The other end of the colors a roll can produce.
 */
public record FlixelColorBounds(FlixelColor min, FlixelColor max) {

  /**
   * Creates bounds where both ends are the same color.
   *
   * @param color The color copied into both {@link #min} and {@link #max}.
   */
  public FlixelColorBounds(@NotNull FlixelColor color) {
    this(color, color);
  }

  /**
   * Creates bounds between two colors.
   *
   * @param min The color copied into {@link #min}.
   * @param max The color copied into {@link #max}.
   */
  public FlixelColorBounds(@NotNull FlixelColor min, @NotNull FlixelColor max) {
    this.min = new FlixelColor(min);
    this.max = new FlixelColor(max);
  }

  /**
   * Sets both ends to the same color, so every roll returns it.
   *
   * @param color The color copied into both ends.
   * @return {@code this} bounds for chaining.
   */
  public FlixelColorBounds set(@NotNull FlixelColor color) {
    return set(color, color);
  }

  /**
   * Sets both ends of these bounds.
   *
   * @param min The color copied into {@link #min}.
   * @param max The color copied into {@link #max}.
   * @return {@code this} bounds for chaining.
   */
  public FlixelColorBounds set(@NotNull FlixelColor min, @NotNull FlixelColor max) {
    this.min.set(min);
    this.max.set(max);
    return this;
  }

  /**
   * Returns whether these bounds hold exactly the same colors as other bounds.
   *
   * @param other The bounds to compare against.
   * @return {@code true} if every channel of both ends matches.
   */
  public boolean matches(FlixelColorBounds other) {
    return other != null && same(min, other.min) && same(max, other.max);
  }

  private static boolean same(FlixelColor a, FlixelColor b) {
    return a.r == b.r && a.g == b.g && a.b == b.b && a.a == b.a;
  }
}
