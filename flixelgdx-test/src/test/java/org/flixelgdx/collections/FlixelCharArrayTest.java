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
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelCharArrayTest {

  @Test
  void addGetSet() {
    FlixelCharArray array = new FlixelCharArray();
    array.add('a');
    array.add('b');
    assertEquals(2, array.size);
    assertEquals('a', array.get(0));
    array.set(1, 'z');
    assertEquals('z', array.get(1));
  }

  @Test
  void toStringValueBuildsString() {
    FlixelCharArray array = new FlixelCharArray();
    for (char c : "hello".toCharArray()) {
      array.add(c);
    }
    assertEquals("hello", array.toStringValue());
  }

  @Test
  void orderedRemoveShifts() {
    FlixelCharArray array = new FlixelCharArray();
    array.add('a');
    array.add('b');
    array.add('c');
    assertEquals('b', array.removeIndex(1));
    assertEquals("ac", array.toStringValue());
  }

  @Test
  void stackAndEnds() {
    FlixelCharArray array = new FlixelCharArray();
    array.add('x');
    array.add('y');
    assertEquals('y', array.peek());
    assertEquals('y', array.pop());
    assertEquals(1, array.size);
  }

  @Test
  void growPreservesValues() {
    FlixelCharArray array = new FlixelCharArray(2);
    for (int i = 0; i < 30; i++) {
      array.add((char) ('a' + (i % 26)));
    }
    assertEquals(30, array.size);
  }

  @Test
  void emptyAccessThrows() {
    FlixelCharArray array = new FlixelCharArray();
    assertTrue(array.isEmpty());
    assertThrows(IndexOutOfBoundsException.class, array::pop);
  }
}
