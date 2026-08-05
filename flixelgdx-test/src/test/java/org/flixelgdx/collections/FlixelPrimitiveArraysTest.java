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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelPrimitiveArraysTest {

  @Test
  void longArrayCoreOps() {
    FlixelLongArray array = new FlixelLongArray(2);
    for (long i = 0; i < 40; i++) {
      array.add(i * 1_000_000_000L);
    }
    assertEquals(40, array.size);
    assertEquals(5_000_000_000L, array.get(5));
    array.set(5, 7L);
    assertEquals(7L, array.get(5));
    assertEquals(3, array.indexOf(3_000_000_000L));
    assertTrue(array.contains(0L));
    assertTrue(array.removeValue(0L));
    assertEquals(39, array.size);
    assertEquals(array.peek(), array.pop());
    array.setSize(0);
    assertTrue(array.isEmpty());
    assertThrows(IndexOutOfBoundsException.class, array::pop);
  }

  @Test
  void shortArrayCoreOps() {
    FlixelShortArray array = new FlixelShortArray();
    array.add((short) 1);
    array.add((short) 2);
    array.add((short) 3);
    assertEquals((short) 2, array.removeIndex(1));
    assertEquals((short) 3, array.get(1));
    assertEquals(2, array.size);
    assertArrayEqualsShort(array);
  }

  private static void assertArrayEqualsShort(FlixelShortArray array) {
    short[] copy = array.toArray();
    assertEquals(array.size, copy.length);
  }

  @Test
  void byteArrayCoreOps() {
    FlixelByteArray array = new FlixelByteArray();
    for (int i = 0; i < 10; i++) {
      array.add((byte) i);
    }
    assertEquals((byte) 0, array.first());
    assertEquals((byte) 9, array.peek());
    assertTrue(array.removeValue((byte) 5));
    assertFalse(array.contains((byte) 5));
    assertEquals(9, array.size);
  }

  @Test
  void booleanArrayCoreOps() {
    FlixelBooleanArray array = new FlixelBooleanArray();
    array.add(true);
    array.add(false);
    array.add(true);
    assertEquals(3, array.size);
    assertTrue(array.get(0));
    assertFalse(array.get(1));
    assertEquals(0, array.indexOf(true));
    assertTrue(array.contains(false));
    assertTrue(array.removeValue(false));
    assertFalse(array.contains(false));
    assertEquals(2, array.size);
  }

  @Test
  void unorderedRemoveSwapsLast() {
    FlixelLongArray array = new FlixelLongArray(false, 8);
    array.add(1L);
    array.add(2L);
    array.add(3L);
    assertEquals(1L, array.removeIndex(0));
    assertEquals(3L, array.get(0));
  }
}
