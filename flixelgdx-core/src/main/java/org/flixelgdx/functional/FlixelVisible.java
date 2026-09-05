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

/**
 * Something that can be shown or hidden for drawing. Matches the usual {@code visible} flag on
 * {@link org.flixelgdx.FlixelBasic FlixelBasic}.
 */
public interface FlixelVisible {

  /**
   * Returns {@code true} when this object is drawn; {@code false} when hidden.
   *
   * @return {@code true} if this object is visible and drawn, {@code false} if hidden.
   */
  boolean isVisible();

  /**
   * Sets whether this object is visible and drawn.
   *
   * @param visible {@code true} to show, {@code false} to hide.
   */
  void setVisible(boolean visible);

  /** Flips between visible and hidden. */
  void toggleVisible();
}
