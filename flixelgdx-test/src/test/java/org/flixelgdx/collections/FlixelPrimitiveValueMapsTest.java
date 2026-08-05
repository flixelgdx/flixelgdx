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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelPrimitiveValueMapsTest {

  @Test
  void intIntMapBasicsAndDefaults() {
    FlixelIntIntMap map = new FlixelIntIntMap();
    assertEquals(-1, map.get(7, -1));
    map.put(7, 42);
    assertEquals(42, map.get(7, -1));
    map.put(0, 5);
    assertTrue(map.containsKey(0));
    assertEquals(5, map.get(0, -1));
    assertEquals(2, map.size());
    assertEquals(45, map.increment(0, 40));
    assertEquals(45, map.get(0, -1));
    assertEquals(42, map.remove(7, -1));
    assertFalse(map.containsKey(7));
  }

  @Test
  void intIntMapBehavesLikeHashMap() {
    FlixelIntIntMap ours = new FlixelIntIntMap(4);
    Map<Integer, Integer> ref = new HashMap<>();
    Random random = new Random(21);
    for (int i = 0; i < 20000; i++) {
      int key = random.nextInt(500) - 100;
      int op = random.nextInt(3);
      if (op == 0) {
        int value = random.nextInt();
        ref.put(key, value);
        ours.put(key, value);
      } else if (op == 1) {
        Integer removed = ref.remove(key);
        assertEquals(removed == null ? -999 : removed, ours.remove(key, -999));
      } else {
        Integer v = ref.get(key);
        assertEquals(v == null ? -999 : v, ours.get(key, -999));
      }
      assertEquals(ref.size(), ours.size());
    }
  }

  @Test
  void intFloatMap() {
    FlixelIntFloatMap map = new FlixelIntFloatMap();
    map.put(3, 1.5f);
    assertEquals(1.5f, map.get(3, 0f), 1e-6f);
    assertEquals(0f, map.get(99, 0f), 1e-6f);
    assertEquals(4f, map.increment(3, 2.5f), 1e-6f);
  }

  @Test
  void objectIntMapBasics() {
    FlixelObjectIntMap<String> map = new FlixelObjectIntMap<>();
    assertEquals(0, map.get("x", 0));
    map.put("x", 10);
    assertEquals(10, map.get("x", 0));
    assertEquals(11, map.increment("x", 1));
    assertTrue(map.containsKey("x"));
    assertThrows(IllegalArgumentException.class, () -> map.put(null, 1));
    assertEquals(11, map.remove("x", -1));
    assertFalse(map.containsKey("x"));
  }

  @Test
  void objectIntMapBehavesLikeHashMap() {
    FlixelObjectIntMap<Integer> ours = new FlixelObjectIntMap<>(4);
    Map<Integer, Integer> ref = new HashMap<>();
    Random random = new Random(33);
    for (int i = 0; i < 20000; i++) {
      Integer key = random.nextInt(400);
      int op = random.nextInt(3);
      if (op == 0) {
        int value = random.nextInt();
        ref.put(key, value);
        ours.put(key, value);
      } else if (op == 1) {
        Integer removed = ref.remove(key);
        assertEquals(removed == null ? -999 : removed, ours.remove(key, -999));
      } else {
        Integer v = ref.get(key);
        assertEquals(v == null ? -999 : v, ours.get(key, -999));
      }
      assertEquals(ref.size(), ours.size());
    }
  }

  @Test
  void objectFloatMap() {
    FlixelObjectFloatMap<String> map = new FlixelObjectFloatMap<>();
    map.put("a", 2.5f);
    assertEquals(2.5f, map.get("a", 0f), 1e-6f);
    assertEquals(0f, map.get("missing", 0f), 1e-6f);
    assertEquals(5f, map.increment("a", 2.5f), 1e-6f);
  }
}
