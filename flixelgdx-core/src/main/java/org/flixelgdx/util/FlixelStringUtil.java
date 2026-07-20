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

import com.badlogic.gdx.utils.CharArray;

import org.jetbrains.annotations.NotNull;

/**
 * Handy utility class for manipulating strings more efficiently.
 */
public final class FlixelStringUtil {

  /**
   * Compares the content of two {@link CharSequence}s for equality. This works for
   * {@link String}s, {@link StringBuilder}s, and any {@link CharSequence}s.
   *
   * @param a The first {@link CharSequence} to compare.
   * @param b The second {@link CharSequence} to compare.
   * @return {@code true} if the content of the two {@link CharSequence}s is equal, {@code false} otherwise.
   */
  public static boolean contentEquals(CharSequence a, CharSequence b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }

    int len = a.length();
    if (len != b.length()) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Appends {@code value} rounded to {@code decimals} decimal places using only {@link CharArray} primitive
   * appenders, avoiding {@link Float#toString(float)} and other helpers that allocate {@link String} objects.
   *
   * <p>Non-finite values fall back to {@link CharArray#append(float)}.
   *
   * <p>When {@code decimals} is zero or negative, the value is rounded to the nearest integer and no
   * decimal point is written.
   *
   * <p>Fractional digits are zero-padded on the left so the output always has exactly {@code decimals}
   * digits after the decimal point (for example, {@code 3.05f} with {@code decimals = 2} produces
   * {@code "3.05"}, not {@code "3.5"}).
   *
   * <p>Because this method uses {@code long} arithmetic internally, meaningful precision is still
   * limited to roughly seven significant digits (the range of a {@code float}).
   *
   * @param out Destination buffer. For {@link FlixelString} callers, prefer
   *     {@link FlixelString#concatFloatRounded(float, int)} or {@link FlixelString#setFloatRounded(float, int)}
   *     instead of reaching for a raw {@link CharArray}.
   * @param value Value to format.
   * @param decimals Number of digits after the decimal point; values of zero or less produce an
   *     integer with no decimal point.
   */
  public static void appendFloatRounded(@NotNull CharArray out, float value, int decimals) {
    if (out == null) {
      return;
    }
    if (Float.isNaN(value) || Float.isInfinite(value)) {
      out.append(value);
      return;
    }
    if (decimals <= 0) {
      out.append(Math.round((double) value));
      return;
    }
    long scale = 1;
    for (int i = 0; i < decimals; i++) {
      scale *= 10;
    }
    long units = Math.round((double) value * scale);
    if (units < 0) {
      out.append('-');
      units = -units;
    }
    long whole = units / scale;
    long frac = units % scale;
    out.append(whole);
    out.append('.');
    long fracScale = scale / 10;
    while (fracScale > frac && fracScale >= 1) {
      out.append('0');
      fracScale /= 10;
    }
    if (frac > 0) {
      out.append(frac);
    }
  }

  /**
   * Appends {@code value} rounded to one decimal place (nearest tenth). Convenience wrapper for
   * {@link #appendFloatRounded(CharArray, float, int)} with {@code decimals = 1}.
   *
   * <p>Non-finite values fall back to {@link CharArray#append(float)}.
   *
   * @param out Destination buffer. For {@link FlixelString}, callers prefer
   *     {@link FlixelString#concatFloatRoundedOneDecimal(float)} or
   *     {@link FlixelString#setFloatRoundedOneDecimal(float)} instead of reaching for a raw {@link CharArray}.
   * @param value Value to format.
   */
  public static void appendFloatRoundedOneDecimal(@NotNull CharArray out, float value) {
    appendFloatRounded(out, value, 1);
  }

  private FlixelStringUtil() {}
}
