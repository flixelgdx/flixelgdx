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

import org.flixelgdx.GdxHeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelStringUtilTest {

  @Test
  void contentEqualsSameReferenceReturnsTrue() {
    String s = "hello";
    assertTrue(FlixelStringUtil.contentEquals(s, s));
  }

  @Test
  void contentEqualsEqualStringsReturnsTrue() {
    assertTrue(FlixelStringUtil.contentEquals("abc", "abc"));
  }

  @Test
  void contentEqualsDifferentStringsReturnsFalse() {
    assertFalse(FlixelStringUtil.contentEquals("abc", "xyz"));
  }

  @Test
  void contentEqualsDifferentLengthsReturnsFalse() {
    assertFalse(FlixelStringUtil.contentEquals("ab", "abc"));
  }

  @Test
  void contentEqualsNullFirstArgumentReturnsFalse() {
    assertFalse(FlixelStringUtil.contentEquals(null, "abc"));
  }

  @Test
  void contentEqualsNullSecondArgumentReturnsFalse() {
    assertFalse(FlixelStringUtil.contentEquals("abc", null));
  }

  @Test
  void contentEqualsBothNullReturnsTrue() {
    // Both are the same reference (null == null), so the same-reference check fires first.
    assertTrue(FlixelStringUtil.contentEquals(null, null));
  }

  @Test
  void contentEqualsEmptyStringsReturnsTrue() {
    assertTrue(FlixelStringUtil.contentEquals("", ""));
  }

  @Test
  void contentEqualsEmptyAndNonEmptyReturnsFalse() {
    assertFalse(FlixelStringUtil.contentEquals("", "a"));
  }

  @Test
  void contentEqualsMixedCharSequenceTypes() {
    assertTrue(FlixelStringUtil.contentEquals("test", new StringBuilder("test")));
  }

  @Test
  void appendFloatRoundedPositiveRoundsUp() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.45f, 1);
    assertEquals("3.5", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedPositiveRoundsDown() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.44f, 1);
    assertEquals("3.4", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedZero() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 0f, 1);
    assertEquals("0.0", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedNegativeValue() {
    // -2.6 * 10 = -26.0, Math.round(-26.0) = -26, so the result is "-2.6".
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, -2.6f, 1);
    assertEquals("-2.6", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedWholeNumber() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 10f, 1);
    assertEquals("10.0", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedNullOutDoesNotThrow() {
    FlixelStringUtil.appendFloatRounded(null, 1.0f, 1);
  }

  @Test
  void appendFloatRoundedAppendsToExistingContent() {
    CharArray out = new CharArray();
    out.append("fps:");
    FlixelStringUtil.appendFloatRounded(out, 60.0f, 1);
    assertEquals("fps:60.0", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedTwoDecimalPlaces() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.456f, 2);
    assertEquals("3.46", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedTwoDecimalPlacesLeadingZero() {
    // Fractional part is 5 (0.05), which must be padded to "05" not "5".
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.05f, 2);
    assertEquals("3.05", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedThreeDecimalPlaces() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 1.2345f, 3);
    assertEquals("1.235", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedZeroDecimalPlaces() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.7f, 0);
    assertEquals("4", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedNegativeDecimalPlaces() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.7f, -1);
    assertEquals("4", new String(out.items, 0, out.size));
  }
}
