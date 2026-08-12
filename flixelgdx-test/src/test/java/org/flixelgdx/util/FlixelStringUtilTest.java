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

import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.collections.FlixelCharArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
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
  void appendFloatRoundedTwoDecimalsRoundsUp() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.456f, 2);
    assertEquals("3.46", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedTwoDecimalsRoundsDown() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.454f, 2);
    assertEquals("3.45", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedTwoDecimalsZero() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 0f, 2);
    assertEquals("0.00", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedTwoDecimalsNegative() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, -2.6f, 2);
    assertEquals("-2.60", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedTwoDecimalsLeadingZeroInFraction() {
    // 3.05 rounded to 2 places must produce "3.05", not "3.5".
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.05f, 2);
    assertEquals("3.05", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedThreeDecimalsWholeNumber() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 10f, 3);
    assertEquals("10.000", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedZeroDecimalsProducesInteger() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.6f, 0);
    assertEquals("4", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedNegativeDecimalsProducesInteger() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRounded(out, 3.6f, -1);
    assertEquals("4", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedNullOutDoesNotThrow() {
    FlixelStringUtil.appendFloatRounded(null, 1.0f, 2);
  }

  @Test
  void appendFloatRoundedAppendsToExistingContent() {
    FlixelCharArray out = new FlixelCharArray();
    out.append("v:");
    FlixelStringUtil.appendFloatRounded(out, 9.5f, 2);
    assertEquals("v:9.50", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalPositiveRoundsUp() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 3.45f);
    assertEquals("3.5", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalPositiveRoundsDown() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 3.44f);
    assertEquals("3.4", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalZero() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 0f);
    assertEquals("0.0", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalNegativeValue() {
    // -2.6 * 10 = -26.0, Math.round(-26.0) = -26, so the result is "-2.6".
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, -2.6f);
    assertEquals("-2.6", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalWholeNumber() {
    FlixelCharArray out = new FlixelCharArray();
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 10f);
    assertEquals("10.0", new String(out.getItems(), 0, out.getSize()));
  }

  @Test
  void appendFloatRoundedOneDecimalNullOutDoesNotThrow() {
    FlixelStringUtil.appendFloatRoundedOneDecimal(null, 1.0f);
  }

  @Test
  void appendFloatRoundedOneDecimalAppendsToExistingContent() {
    FlixelCharArray out = new FlixelCharArray();
    out.append("fps:");
    FlixelStringUtil.appendFloatRoundedOneDecimal(out, 60.0f);
    assertEquals("fps:60.0", new String(out.getItems(), 0, out.getSize()));
  }
}
