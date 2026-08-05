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

class FlixelIntMapTest {

  @Test
  void putGetReplace() {
    FlixelIntMap<String> map = new FlixelIntMap<>();
    assertNull(map.put(1, "a"));
    assertEquals("a", map.get(1));
    assertEquals("a", map.put(1, "b"));
    assertEquals("b", map.get(1));
    assertEquals(1, map.size());
  }

  @Test
  void zeroKeyIsHandledSpecially() {
    FlixelIntMap<String> map = new FlixelIntMap<>();
    assertFalse(map.containsKey(0));
    map.put(0, "zero");
    assertTrue(map.containsKey(0));
    assertEquals("zero", map.get(0));
    assertEquals(1, map.size());
    assertEquals("zero", map.remove(0));
    assertFalse(map.containsKey(0));
    assertEquals(0, map.size());
  }

  @Test
  void missingKeyDefaults() {
    FlixelIntMap<String> map = new FlixelIntMap<>();
    assertNull(map.get(42));
    assertEquals("x", map.getOrDefault(42, "x"));
  }

  @Test
  void entriesIterationIncludesZeroKey() {
    FlixelIntMap<Integer> map = new FlixelIntMap<>();
    map.put(0, 100);
    map.put(1, 1);
    map.put(2, 2);

    int keySum = 0;
    int valueSum = 0;
    for (FlixelIntMap.Entry<Integer> e : map.entries()) {
      keySum += e.key;
      valueSum += e.value;
    }
    assertEquals(3, keySum);
    assertEquals(103, valueSum);
  }

  @Test
  void behavesLikeHashMapUnderRandomOps() {
    FlixelIntMap<Integer> ours = new FlixelIntMap<>(4);
    Map<Integer, Integer> ref = new HashMap<>();
    Random random = new Random(11);

    for (int i = 0; i < 20000; i++) {
      int key = random.nextInt(400) - 50;
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
}
