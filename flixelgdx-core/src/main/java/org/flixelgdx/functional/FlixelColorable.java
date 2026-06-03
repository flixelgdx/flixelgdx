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

import com.badlogic.gdx.graphics.Color;

import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;

/**
 * Something with a tint you can read or write as either a libGDX {@link Color} or a
 * {@link FlixelColor} wrapper. Implementations should keep both APIs in sync on the same backing RGBA.
 *
 * @see org.flixelgdx.FlixelSprite
 */
public interface FlixelColorable {

  /**
   * @return Packed RGBA8888 tint (see {@link Color#rgba8888(Color)}).
   */
  int getColor();

  /**
   * @return The backing libGDX color (often mutable). Must not be {@code null}.
   */
  @NotNull
  Color getGdxColor();

  /**
   * Copies RGBA from {@code color} into this tint.
   *
   * @param color The color to copy. Must not be {@code null}.
   */
  void setColor(@NotNull Color color);

  /**
   * Copies RGBA from {@code color} into this tint.
   *
   * @param color The wrapper to copy from. Must not be {@code null}.
   */
  void setColor(@NotNull FlixelColor color);
}
