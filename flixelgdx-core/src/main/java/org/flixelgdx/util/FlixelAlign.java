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

/**
 * Bit flags that describe how something is aligned within a box.
 *
 * <p>Alignment is stored as a single {@code int} whose bits combine a vertical
 * choice ({@link #TOP}, {@link #BOTTOM}, or neither for vertical center) with a
 * horizontal choice ({@link #LEFT}, {@link #RIGHT}, or neither for horizontal
 * center). Because the pieces are independent bits, they combine with the
 * bitwise OR operator, and the common corners are provided ready-made
 * ({@link #TOP_LEFT} and friends).
 *
 * <p>Storing alignment as flags keeps it allocation free and lets rendering code
 * test one axis at a time with {@link #isLeft(int)}, {@link #isTop(int)}, and so
 * on, instead of branching over an enum.
 *
 * <p>Example:
 *
 * <pre>{@code
 * int align = FlixelAlign.TOP | FlixelAlign.RIGHT; // same as FlixelAlign.TOP_RIGHT
 * if (FlixelAlign.isTop(align)) {
 *   // anchor to the top edge
 * }
 * }</pre>
 */
public final class FlixelAlign {

  /** Centered on both axes. */
  public static final int CENTER = 1;

  /** Aligned to the top edge. */
  public static final int TOP = 2;

  /** Aligned to the bottom edge. */
  public static final int BOTTOM = 4;

  /** Aligned to the left edge. */
  public static final int LEFT = 8;

  /** Aligned to the right edge. */
  public static final int RIGHT = 16;

  /** Aligned to the top-left corner. */
  public static final int TOP_LEFT = TOP | LEFT;

  /** Aligned to the top-right corner. */
  public static final int TOP_RIGHT = TOP | RIGHT;

  /** Aligned to the bottom-left corner. */
  public static final int BOTTOM_LEFT = BOTTOM | LEFT;

  /** Aligned to the bottom-right corner. */
  public static final int BOTTOM_RIGHT = BOTTOM | RIGHT;

  /**
   * Reports whether the given alignment includes the left edge.
   *
   * @param align The alignment flags to test.
   * @return {@code true} if the {@link #LEFT} bit is set.
   */
  public static boolean isLeft(int align) {
    return (align & LEFT) != 0;
  }

  /**
   * Reports whether the given alignment includes the right edge.
   *
   * @param align The alignment flags to test.
   * @return {@code true} if the {@link #RIGHT} bit is set.
   */
  public static boolean isRight(int align) {
    return (align & RIGHT) != 0;
  }

  /**
   * Reports whether the given alignment includes the top edge.
   *
   * @param align The alignment flags to test.
   * @return {@code true} if the {@link #TOP} bit is set.
   */
  public static boolean isTop(int align) {
    return (align & TOP) != 0;
  }

  /**
   * Reports whether the given alignment includes the bottom edge.
   *
   * @param align The alignment flags to test.
   * @return {@code true} if the {@link #BOTTOM} bit is set.
   */
  public static boolean isBottom(int align) {
    return (align & BOTTOM) != 0;
  }

  /**
   * Reports whether the given alignment is centered horizontally (neither left
   * nor right).
   *
   * @param align The alignment flags to test.
   * @return {@code true} if neither {@link #LEFT} nor {@link #RIGHT} is set.
   */
  public static boolean isCenterHorizontal(int align) {
    return (align & (LEFT | RIGHT)) == 0;
  }

  /**
   * Reports whether the given alignment is centered vertically (neither top nor
   * bottom).
   *
   * @param align The alignment flags to test.
   * @return {@code true} if neither {@link #TOP} nor {@link #BOTTOM} is set.
   */
  public static boolean isCenterVertical(int align) {
    return (align & (TOP | BOTTOM)) == 0;
  }

  private FlixelAlign() {}
}
