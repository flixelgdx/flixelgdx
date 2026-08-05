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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLongCharMapTest {

  @Test
  void longMapZeroKeyAndBasics() {
    FlixelLongMap<String> map = new FlixelLongMap<>();
    assertNull(map.put(0L, "zero"));
    assertEquals("zero", map.get(0L));
    assertTrue(map.containsKey(0L));
    map.put(5_000_000_000L, "big");
    assertEquals("big", map.get(5_000_000_000L));
    assertEquals("zero", map.remove(0L));
    assertFalse(map.containsKey(0L));
    assertEquals(1, map.size());
  }

  @Test
  void longMapBehavesLikeHashMap() {
    FlixelLongMap<Integer> ours = new FlixelLongMap<>(4);
    Map<Long, Integer> ref = new HashMap<>();
    Random random = new Random(3);
    for (int i = 0; i < 20000; i++) {
      long key = random.nextInt(600) - 100L;
      int op = random.nextInt(3);
      if (op == 0) {
        int value = random.nextInt();
        assertEquals(ref.put(key, value), ours.put(key, value));
      } else if (op == 1) {
        assertEquals(ref.remove(key), ours.remove(key));
      } else {
        assertEquals(ref.get(key), ours.get(key));
      }
      assertEquals(ref.size(), ours.size());
    }
  }

  @Test
  void charMapZeroKeyAndBasics() {
    FlixelCharMap<String> map = new FlixelCharMap<>();
    assertNull(map.put('\0', "nul"));
    assertEquals("nul", map.get('\0'));
    map.put('a', "letter");
    assertEquals("letter", map.get('a'));
    assertEquals(2, map.size());
    assertEquals("nul", map.remove('\0'));
    assertFalse(map.containsKey('\0'));
  }

  @Test
  void charMapBehavesLikeHashMap() {
    FlixelCharMap<Integer> ours = new FlixelCharMap<>(4);
    Map<Character, Integer> ref = new HashMap<>();
    Random random = new Random(9);
    for (int i = 0; i < 20000; i++) {
      char key = (char) random.nextInt(200);
      int op = random.nextInt(3);
      if (op == 0) {
        int value = random.nextInt();
        assertEquals(ref.put(key, value), ours.put(key, value));
      } else if (op == 1) {
        assertEquals(ref.remove(key), ours.remove(key));
      } else {
        assertEquals(ref.get(key), ours.get(key));
      }
      assertEquals(ref.size(), ours.size());
    }
  }

  @Test
  void entriesIteration() {
    FlixelLongMap<Integer> map = new FlixelLongMap<>();
    map.put(0L, 100);
    map.put(1L, 1);
    map.put(2L, 2);
    long keySum = 0;
    int valueSum = 0;
    for (FlixelLongMap.Entry<Integer> e : map.entries()) {
      keySum += e.key;
      valueSum += e.value;
    }
    assertEquals(3L, keySum);
    assertEquals(103, valueSum);
  }
}
