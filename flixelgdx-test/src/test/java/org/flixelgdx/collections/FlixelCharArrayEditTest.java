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
package org.flixelgdx.collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelCharArrayEditTest {

  @Test
  void insertAtStartShiftsExistingCharsRight() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("ello");
    array.insert(0, 'H');
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void insertInMiddleShiftsLaterCharsRight() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Helo");
    array.insert(2, 'l');
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void insertAtEndBehavesLikeAdd() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hell");
    array.insert(4, 'o');
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void insertGrowsCapacityWhenFull() {
    FlixelCharArray array = new FlixelCharArray(1);
    array.append("ac");
    array.insert(1, 'b');
    assertEquals("abc", array.toStringValue());
    assertEquals(3, array.getSize());
  }

  @Test
  void insertPastSizeThrows() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> array.insert(4, 'z'));
  }

  @Test
  void insertRangeAtStart() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("world");
    array.insertRange(0, "hello ");
    assertEquals("hello world", array.toStringValue());
  }

  @Test
  void insertRangeInMiddle() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hlo");
    array.insertRange(1, "el");
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void insertRangeAtEndAppends() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hell");
    array.insertRange(4, "o!");
    assertEquals("Hello!", array.toStringValue());
  }

  @Test
  void insertRangeCausingGrowth() {
    FlixelCharArray array = new FlixelCharArray(2);
    array.append("ad");
    array.insertRange(1, "bc");
    assertEquals("abcd", array.toStringValue());
  }

  @Test
  void insertRangeOfEmptySequenceIsNoOp() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    array.insertRange(1, "");
    assertEquals("abc", array.toStringValue());
  }

  @Test
  void insertRangeOfNullIsNoOp() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    array.insertRange(1, null);
    assertEquals("abc", array.toStringValue());
  }

  @Test
  void insertRangePastSizeThrows() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> array.insertRange(4, "z"));
  }

  @Test
  void removeRangeFromStart() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hello world");
    array.removeRange(0, 6);
    assertEquals("world", array.toStringValue());
  }

  @Test
  void removeRangeFromMiddle() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hexxxllo");
    array.removeRange(2, 5);
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void removeRangeFromEnd() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("Hello!!!");
    array.removeRange(5, 8);
    assertEquals("Hello", array.toStringValue());
  }

  @Test
  void removeEmptyRangeIsNoOp() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    array.removeRange(1, 1);
    assertEquals("abc", array.toStringValue());
    assertEquals(3, array.getSize());
  }

  @Test
  void removeFullRangeEmptiesBuffer() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    array.removeRange(0, 3);
    assertEquals("", array.toStringValue());
    assertEquals(0, array.getSize());
  }

  @Test
  void removeRangePastSizeThrows() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> array.removeRange(1, 4));
  }

  @Test
  void removeRangeWithStartAfterEndThrows() {
    FlixelCharArray array = new FlixelCharArray();
    array.append("abc");
    assertThrows(IndexOutOfBoundsException.class, () -> array.removeRange(2, 1));
  }
}
