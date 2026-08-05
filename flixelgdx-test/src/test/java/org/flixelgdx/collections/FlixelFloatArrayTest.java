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

class FlixelFloatArrayTest {

  private static final float DELTA = 1e-6f;

  @Test
  void addGetSet() {
    FlixelFloatArray array = new FlixelFloatArray();
    array.add(1.5f);
    array.add(2.5f);
    assertEquals(2, array.size);
    assertEquals(1.5f, array.get(0), DELTA);
    array.set(1, 9.5f);
    assertEquals(9.5f, array.get(1), DELTA);
  }

  @Test
  void orderedRemoveShifts() {
    FlixelFloatArray array = new FlixelFloatArray();
    array.add(1f);
    array.add(2f);
    array.add(3f);
    assertEquals(2f, array.removeIndex(1), DELTA);
    assertEquals(3f, array.get(1), DELTA);
    assertEquals(2, array.size);
  }

  @Test
  void stackAndEnds() {
    FlixelFloatArray array = new FlixelFloatArray();
    array.add(1f);
    array.add(2f);
    assertEquals(1f, array.first(), DELTA);
    assertEquals(2f, array.peek(), DELTA);
    assertEquals(2f, array.pop(), DELTA);
    assertEquals(1, array.size);
  }

  @Test
  void growPreservesValues() {
    FlixelFloatArray array = new FlixelFloatArray(2);
    for (int i = 0; i < 40; i++) {
      array.add(i * 0.5f);
    }
    assertEquals(40, array.size);
    assertEquals(19.5f, array.get(39), DELTA);
  }

  @Test
  void emptyAccessThrows() {
    FlixelFloatArray array = new FlixelFloatArray();
    assertTrue(array.isEmpty());
    assertThrows(IndexOutOfBoundsException.class, array::pop);
  }
}
