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

  // -- contentEquals --

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

  // -- appendFloatRoundedOneDecimal --

  @Test
  void appendFloatRoundedOneDecimalPositiveRoundsUp() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 3.45f);
    assertEquals("3.5", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedOneDecimalPositiveRoundsDown() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 3.44f);
    assertEquals("3.4", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedOneDecimalZero() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 0f);
    assertEquals("0.0", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedOneDecimalNegativeValue() {
    // -2.6 * 10 = -26.0, Math.round(-26.0f) = -26, so the result is "-2.6".
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, -2.6f);
    assertEquals("-2.6", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedOneDecimalWholeNumber() {
    CharArray out = new CharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 10f);
    assertEquals("10.0", new String(out.items, 0, out.size));
  }

  @Test
  void appendFloatRoundedOneDecimalNullOutDoesNotThrow() {
    FlixelStringUtil.appendFloatRoundedOneDecimal(null, 1.0f);
  }

  @Test
  void appendFloatRoundedOneDecimalAppendsToExistingContent() {
    CharArray out = new CharArray();
    out.append("fps:");
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 60.0f);
    assertEquals("fps:60.0", new String(out.items, 0, out.size));
  }
}
