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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelIntArrayTest {

  @Test
  void addGetSet() {
    FlixelIntArray array = new FlixelIntArray();
    array.add(10);
    array.add(20);
    assertEquals(2, array.size);
    assertEquals(10, array.get(0));
    array.set(0, 99);
    assertEquals(99, array.get(0));
  }

  @Test
  void orderedRemoveShifts() {
    FlixelIntArray array = new FlixelIntArray();
    array.add(1);
    array.add(2);
    array.add(3);
    assertEquals(2, array.removeIndex(1));
    assertEquals(2, array.size);
    assertEquals(3, array.get(1));
  }

  @Test
  void unorderedRemoveSwapsLast() {
    FlixelIntArray array = new FlixelIntArray(false, 8);
    array.add(1);
    array.add(2);
    array.add(3);
    assertEquals(1, array.removeIndex(0));
    assertEquals(3, array.get(0));
    assertEquals(2, array.size);
  }

  @Test
  void indexOfAndContains() {
    FlixelIntArray array = new FlixelIntArray();
    array.add(5);
    array.add(6);
    assertEquals(1, array.indexOf(6));
    assertEquals(-1, array.indexOf(42));
    assertTrue(array.contains(5));
    assertFalse(array.contains(42));
  }

  @Test
  void stackAndEnds() {
    FlixelIntArray array = new FlixelIntArray();
    array.add(1);
    array.add(2);
    array.add(3);
    assertEquals(1, array.first());
    assertEquals(3, array.peek());
    assertEquals(3, array.pop());
    assertEquals(2, array.size);
  }

  @Test
  void growAndToArray() {
    FlixelIntArray array = new FlixelIntArray(2);
    for (int i = 0; i < 50; i++) {
      array.add(i);
    }
    assertEquals(50, array.size);
    int[] copy = array.toArray();
    assertEquals(50, copy.length);
    assertArrayEquals(new int[] { 0, 1, 2 }, new int[] { copy[0], copy[1], copy[2] });
  }

  @Test
  void emptyAccessThrows() {
    FlixelIntArray array = new FlixelIntArray();
    assertTrue(array.isEmpty());
    assertThrows(IndexOutOfBoundsException.class, array::pop);
    assertThrows(IndexOutOfBoundsException.class, () -> array.get(0));
  }
}
