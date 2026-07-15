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

/**
 * Handy utility class for handling strings more efficiently.
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
   * Appends {@code value} rounded to {@code decimalPlaces} decimal places using only {@link CharArray} primitive
   * appenders, avoiding {@link Float#toString(float)} and other helpers that allocate {@link String} objects.
   *
   * <p>Non-finite values are appended via {@link CharArray#append(float)} as a fallback.
   *
   * <p>If {@code decimalPlaces} is zero or negative, the value is rounded to the nearest whole number and
   * appended without a decimal point.
   *
   * <p>Fractional digits are zero-padded on the left when needed, so {@code 3.05f} with
   * {@code decimalPlaces = 2} produces {@code "3.05"}, not {@code "3.5"}.
   *
   * @param out Destination buffer; for {@link FlixelString} callers prefer {@link FlixelString#concatFloatRounded(float, int)}
   *   or {@link FlixelString#setFloatRounded(float, int)} instead of reaching for a raw {@link CharArray}.
   * @param value Value to format.
   * @param decimalPlaces Number of decimal places to format to. Values of zero or less produce a whole number.
   */
  public static void appendFloatRounded(CharArray out, float value, int decimalPlaces) {
    if (out == null) {
      return;
    }
    if (Float.isNaN(value) || Float.isInfinite(value)) {
      out.append(value);
      return;
    }
    if (decimalPlaces <= 0) {
      out.append(Math.round(value));
      return;
    }
    int scale = 1;
    for (int i = 0; i < decimalPlaces; i++) {
      scale *= 10;
    }
    long scaled = Math.round((double) value * scale);
    if (scaled < 0) {
      out.append('-');
      scaled = -scaled;
    }
    int whole = (int) (scaled / scale);
    int frac = (int) (scaled % scale);
    out.append(whole);
    out.append('.');
    // Pad the fractional part with leading zeros so it is always decimalPlaces digits wide.
    // For example, frac=5 with decimalPlaces=2 (scale=100) produces "05", not "5".
    int pow = scale / 10;
    while (pow > 1) {
      if (frac < pow) {
        out.append('0');
      }
      pow /= 10;
    }
    out.append(frac);
  }

  private FlixelStringUtil() {}
}
