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
package org.flixelgdx.functional;

import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

/**
 * Something with a tint you can read or write as a {@link FlixelColor}.
 *
 * @see org.flixelgdx.FlixelSprite
 */
public interface FlixelColorable {

  /**
   * Returns the live backing tint. Mutating it changes the tint directly; use
   * {@link #setColor(FlixelColor)} when copying from another color.
   *
   * @return The backing color; never {@code null}.
   */
  @NotNull
  FlixelColor getColor();

  /**
   * Copies RGBA from {@code color} into this tint.
   *
   * @param color The wrapper to copy from. Must not be {@code null}.
   */
  void setColor(@NotNull FlixelColor color);

  /**
   * @return Packed RGBA8888 tint.
   */
  default int getPackedColor() {
    return getColor().getColor();
  }
}
