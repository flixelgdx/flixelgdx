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

import org.flixelgdx.GdxHeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelStringTest {

  @Test
  void defaultConstructorCreatesEmptyBuffer() {
    FlixelString fs = new FlixelString();
    assertTrue(fs.isEmpty());
    assertEquals(0, fs.length());
  }

  @Test
  void charSequenceConstructorCopiesContent() {
    FlixelString fs = new FlixelString("hello");
    assertEquals("hello", fs.toString());
  }

  @Test
  void charSequenceConstructorWithNullStoresLiteralNull() {
    FlixelString fs = new FlixelString(null);
    assertEquals("null", fs.toString());
  }

  @Test
  void setCharSequenceReplacesContent() {
    FlixelString fs = new FlixelString("old");
    fs.set("new");
    assertEquals("new", fs.toString());
  }

  @Test
  void setNullCharSequenceStoresLiteralNull() {
    FlixelString fs = new FlixelString("x");
    fs.set((CharSequence) null);
    assertEquals("null", fs.toString());
  }

  @Test
  void setFlixelStringCopiesOtherBuffer() {
    FlixelString a = new FlixelString("source");
    FlixelString b = new FlixelString(8);
    b.set(a);
    assertEquals("source", b.toString());
  }

  @Test
  void setNullFlixelStringStoresLiteralNull() {
    FlixelString fs = new FlixelString("x");
    fs.set(null);
    assertEquals("null", fs.toString());
  }

  @Test
  void setIntWritesDecimalRepresentation() {
    FlixelString fs = new FlixelString();
    fs.set(42);
    assertEquals("42", fs.toString());
  }

  @Test
  void setNegativeIntWritesDecimalRepresentation() {
    FlixelString fs = new FlixelString();
    fs.set(-7);
    assertEquals("-7", fs.toString());
  }

  @Test
  void setLongWritesDecimalRepresentation() {
    FlixelString fs = new FlixelString();
    fs.set(123456789L);
    assertEquals("123456789", fs.toString());
  }

  @Test
  void setBooleanTrue() {
    FlixelString fs = new FlixelString();
    fs.set(true);
    assertEquals("true", fs.toString());
  }

  @Test
  void setBooleanFalse() {
    FlixelString fs = new FlixelString();
    fs.set(false);
    assertEquals("false", fs.toString());
  }

  @Test
  void setCharWritesSingleCharacter() {
    FlixelString fs = new FlixelString();
    fs.set('Z');
    assertEquals("Z", fs.toString());
  }

  @Test
  void clearResetsLengthToZero() {
    FlixelString fs = new FlixelString("content");
    fs.clear();
    assertEquals(0, fs.length());
    assertTrue(fs.isEmpty());
  }

  @Test
  void isEmptyReturnsFalseAfterSet() {
    FlixelString fs = new FlixelString();
    fs.set("x");
    assertFalse(fs.isEmpty());
  }

  @Test
  void concatCharSequenceAppendsWithoutClearing() {
    FlixelString fs = new FlixelString("hello");
    fs.concat(" world");
    assertEquals("hello world", fs.toString());
  }

  @Test
  void concatFlixelStringAppendsOtherBuffer() {
    FlixelString a = new FlixelString("foo");
    FlixelString b = new FlixelString("bar");
    a.concat(b);
    assertEquals("foobar", a.toString());
  }

  @Test
  void concatIntAppendsDecimalRepresentation() {
    FlixelString fs = new FlixelString("score:");
    fs.concat(99);
    assertEquals("score:99", fs.toString());
  }

  @Test
  void concatBooleanAppendsTextRepresentation() {
    FlixelString fs = new FlixelString("active=");
    fs.concat(true);
    assertEquals("active=true", fs.toString());
  }

  @Test
  void concatCharAppendsCharacter() {
    FlixelString fs = new FlixelString("A");
    fs.concat('B');
    assertEquals("AB", fs.toString());
  }

  @Test
  void setAndConcatChainReturnsSameInstance() {
    FlixelString fs = new FlixelString();
    FlixelString result = fs.set("a").concat("b").concat("c");
    assertEquals(fs, result);
    assertEquals("abc", fs.toString());
  }

  @Test
  void setFloatRoundedFormatsToOnePlaceRoundingUp() {
    FlixelString fs = new FlixelString();
    fs.setFloatRounded(3.45f, 1);
    assertEquals("3.5", fs.toString());
  }

  @Test
  void setFloatRoundedFormatsToOnePlaceRoundingDown() {
    FlixelString fs = new FlixelString();
    fs.setFloatRounded(3.44f, 1);
    assertEquals("3.4", fs.toString());
  }

  @Test
  void setFloatRoundedTwoDecimalPlacesLeadingZero() {
    // Fractional part is 5 (0.05), which must be padded to "05" not "5".
    FlixelString fs = new FlixelString();
    fs.setFloatRounded(3.05f, 2);
    assertEquals("3.05", fs.toString());
  }

  @Test
  void setFloatRoundedZeroDecimalPlaces() {
    FlixelString fs = new FlixelString();
    fs.setFloatRounded(3.7f, 0);
    assertEquals("4", fs.toString());
  }

  @Test
  void concatFloatRoundedAppends() {
    FlixelString fs = new FlixelString("fps:");
    fs.concatFloatRounded(60.0f, 1);
    assertEquals("fps:60.0", fs.toString());
  }

  @Test
  void setFloatRoundedNegativeValue() {
    // -1.6 * 10 = -16.0, Math.round(-16.0) = -16, result "-1.6".
    FlixelString fs = new FlixelString();
    fs.setFloatRounded(-1.6f, 1);
    assertEquals("-1.6", fs.toString());
  }

  @Test
  void lengthReflectsCurrentContent() {
    FlixelString fs = new FlixelString("abc");
    assertEquals(3, fs.length());
  }

  @Test
  void charAtReturnsCorrectCharacters() {
    FlixelString fs = new FlixelString("XYZ");
    assertEquals('X', fs.charAt(0));
    assertEquals('Y', fs.charAt(1));
    assertEquals('Z', fs.charAt(2));
  }

  @Test
  void copyContentToNewStringMatchesToString() {
    FlixelString fs = new FlixelString("hello");
    assertEquals(fs.toString(), fs.copyContentToNewString());
  }

  @Test
  void copyContentToNewStringOnEmptyBufferReturnsEmptyString() {
    FlixelString fs = new FlixelString();
    assertEquals("", fs.copyContentToNewString());
  }
}
