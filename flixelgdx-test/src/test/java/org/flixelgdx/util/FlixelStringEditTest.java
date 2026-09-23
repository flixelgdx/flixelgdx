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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelStringEditTest {

  @Test
  void insertCharAtStart() {
    FlixelString fs = new FlixelString("ello");
    fs.insert(0, 'H');
    assertEquals("Hello", fs.toString());
  }

  @Test
  void insertCharInMiddle() {
    FlixelString fs = new FlixelString("Helo");
    fs.insert(2, 'l');
    assertEquals("Hello", fs.toString());
  }

  @Test
  void insertCharAtEnd() {
    FlixelString fs = new FlixelString("Hell");
    fs.insert(4, 'o');
    assertEquals("Hello", fs.toString());
  }

  @Test
  void insertCharPastLengthThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.insert(4, 'z'));
  }

  @Test
  void insertCharSequenceAtStart() {
    FlixelString fs = new FlixelString("world");
    fs.insert(0, "hello ");
    assertEquals("hello world", fs.toString());
  }

  @Test
  void insertCharSequenceInMiddle() {
    FlixelString fs = new FlixelString("Hlo");
    fs.insert(1, "el");
    assertEquals("Hello", fs.toString());
  }

  @Test
  void insertCharSequenceAtEnd() {
    FlixelString fs = new FlixelString("Hell");
    fs.insert(4, "o!");
    assertEquals("Hello!", fs.toString());
  }

  @Test
  void insertEmptyCharSequenceIsNoOp() {
    FlixelString fs = new FlixelString("abc");
    fs.insert(1, "");
    assertEquals("abc", fs.toString());
  }

  @Test
  void insertNullCharSequenceInsertsLiteralNull() {
    FlixelString fs = new FlixelString("ab");
    fs.insert(1, (CharSequence) null);
    assertEquals("anullb", fs.toString());
  }

  @Test
  void insertCharSequencePastLengthThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.insert(4, "z"));
  }

  @Test
  void insertCausesBufferGrowth() {
    FlixelString fs = new FlixelString(1);
    fs.set("ad");
    fs.insert(1, "bc");
    assertEquals("abcd", fs.toString());
  }

  @Test
  void deleteFromStart() {
    FlixelString fs = new FlixelString("Hello world");
    fs.delete(0, 6);
    assertEquals("world", fs.toString());
  }

  @Test
  void deleteFromMiddle() {
    FlixelString fs = new FlixelString("Hexxxllo");
    fs.delete(2, 5);
    assertEquals("Hello", fs.toString());
  }

  @Test
  void deleteFromEnd() {
    FlixelString fs = new FlixelString("Hello!!!");
    fs.delete(5, 8);
    assertEquals("Hello", fs.toString());
  }

  @Test
  void deleteEmptyRangeIsNoOp() {
    FlixelString fs = new FlixelString("abc");
    fs.delete(1, 1);
    assertEquals("abc", fs.toString());
  }

  @Test
  void deleteFullRangeEmptiesBuffer() {
    FlixelString fs = new FlixelString("abc");
    fs.delete(0, 3);
    assertEquals("", fs.toString());
    assertEquals(0, fs.length());
  }

  @Test
  void deletePastLengthThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.delete(1, 4));
  }

  @Test
  void deleteWithStartAfterEndThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.delete(2, 1));
  }

  @Test
  void setLengthTruncatesBuffer() {
    FlixelString fs = new FlixelString("Hello world");
    fs.setLength(5);
    assertEquals("Hello", fs.toString());
    assertEquals(5, fs.length());
  }

  @Test
  void setLengthToZeroEmptiesBuffer() {
    FlixelString fs = new FlixelString("abc");
    fs.setLength(0);
    assertEquals("", fs.toString());
    assertEquals(0, fs.length());
  }

  @Test
  void setLengthToCurrentLengthIsNoOp() {
    FlixelString fs = new FlixelString("abc");
    fs.setLength(3);
    assertEquals("abc", fs.toString());
  }

  @Test
  void setLengthGrowingThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.setLength(4));
  }

  @Test
  void setLengthNegativeThrows() {
    FlixelString fs = new FlixelString("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> fs.setLength(-1));
  }

  @Test
  void insertAndDeleteChainReturnsSameInstance() {
    FlixelString fs = new FlixelString("Helo");
    FlixelString result = fs.insert(2, 'l').delete(4, 5).insert(4, "!");
    assertEquals(fs, result);
    assertEquals("Hell!", fs.toString());
  }
}
