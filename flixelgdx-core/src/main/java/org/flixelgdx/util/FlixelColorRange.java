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

import org.flixelgdx.math.FlixelRange;
import org.jetbrains.annotations.NotNull;

/**
 * A start color and an end color that something blends between over its lifetime.
 *
 * <p>This is the color version of {@link FlixelRange}. Both colors are owned copies, so changing a
 * color you passed to {@link #set(FlixelColor, FlixelColor)} later does not affect the range. Call
 * {@link #lerp(float, FlixelColor)} with how far through its life something is to write its
 * current color into a color you already own, which avoids creating a new color every frame.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelColorRange cool = new FlixelColorRange();
 * cool.set(FlixelColor.YELLOW, FlixelColor.RED); // Hot yellow to red.
 * cool.lerp(0.5f, sprite.getColor());             // Halfway between, written into the tint.
 * }</pre>
 */
public class FlixelColorRange {

  /** The color at the start of the lifetime. */
  public final FlixelColor start = new FlixelColor(FlixelColor.WHITE);

  /** The color at the end of the lifetime. */
  public final FlixelColor end = new FlixelColor(FlixelColor.WHITE);

  /** Whether the owner should apply this range. */
  public boolean active;

  /**
   * Copies both ends of this range and marks it active only if they differ.
   *
   * @param start The color copied into {@link #start}.
   * @param end The color copied into {@link #end}.
   * @return {@code this} range for chaining.
   */
  public FlixelColorRange set(@NotNull FlixelColor start, @NotNull FlixelColor end) {
    this.start.set(start);
    this.end.set(end);
    active = start.r != end.r || start.g != end.g || start.b != end.b || start.a != end.a;
    return this;
  }

  /**
   * Writes the color part of the way from {@link #start} to {@link #end} into another color.
   *
   * @param t How far along the lifetime, where {@code 0} is the start and {@code 1} is the end.
   * @param out The color to write the result into. All four channels are overwritten.
   * @return {@code out}, for chaining.
   */
  public FlixelColor lerp(float t, @NotNull FlixelColor out) {
    out.r = start.r + (end.r - start.r) * t;
    out.g = start.g + (end.g - start.g) * t;
    out.b = start.b + (end.b - start.b) * t;
    out.a = start.a + (end.a - start.a) * t;
    return out;
  }
}
