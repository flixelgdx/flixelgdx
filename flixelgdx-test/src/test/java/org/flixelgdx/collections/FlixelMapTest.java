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
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelMapTest {

  @Test
  void putGetAndReplace() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    assertNull(map.put("a", 1));
    assertEquals(1, map.get("a"));
    assertEquals(1, map.put("a", 2));
    assertEquals(2, map.get("a"));
    assertEquals(1, map.getSize());
  }

  @Test
  void getMissingReturnsNullAndDefault() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    assertNull(map.get("missing"));
    assertEquals(99, map.getOrDefault("missing", 99));
    map.put("here", 5);
    assertEquals(5, map.getOrDefault("here", 99));
  }

  @Test
  void containsAndRemove() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    map.put("a", 1);
    assertTrue(map.containsKey("a"));
    assertEquals(1, map.remove("a"));
    assertFalse(map.containsKey("a"));
    assertNull(map.remove("a"));
    assertEquals(0, map.getSize());
  }

  @Test
  void nullKeyRejected() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    assertThrows(IllegalArgumentException.class, () -> map.put(null, 1));
  }

  @Test
  void nullValuesAllowed() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    map.put("a", null);
    assertTrue(map.containsKey("a"));
    assertNull(map.get("a"));
  }

  @Test
  void clearEmptiesMap() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.clear();
    assertEquals(0, map.getSize());
    assertTrue(map.isEmpty());
    assertFalse(map.containsKey("a"));
  }

  @Test
  void entriesIterationVisitsEverything() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    int sum = 0;
    Set<String> keysSeen = new HashSet<>();
    for (FlixelMap.Entry<String, Integer> e : map.entries()) {
      keysSeen.add(e.key);
      sum += e.value;
    }
    assertEquals(6, sum);
    assertEquals(Set.of("a", "b", "c"), keysSeen);
  }

  @Test
  void keysAndValuesIteration() {
    FlixelMap<String, Integer> map = new FlixelMap<>();
    map.put("a", 1);
    map.put("b", 2);

    Set<String> keys = new HashSet<>();
    for (String k : map.keys()) {
      keys.add(k);
    }
    assertEquals(Set.of("a", "b"), keys);

    int sum = 0;
    for (int v : map.values()) {
      sum += v;
    }
    assertEquals(3, sum);
  }

  @Test
  void behavesLikeHashMapUnderRandomOps() {
    // Differential test: mirror a stream of operations against java.util.HashMap
    // and assert the two agree throughout. This validates probing and resizing.
    FlixelMap<Integer, Integer> ours = new FlixelMap<>(4);
    Map<Integer, Integer> ref = new HashMap<>();
    Random random = new Random(42);

    for (int i = 0; i < 20000; i++) {
      int key = random.nextInt(500);
      int op = random.nextInt(3);
      if (op == 0) {
        int value = random.nextInt();
        assertEquals(ref.put(key, value), ours.put(key, value));
      } else if (op == 1) {
        assertEquals(ref.remove(key), ours.remove(key));
      } else {
        assertEquals(ref.get(key), ours.get(key));
      }
      assertEquals(ref.size(), ours.getSize());
    }

    for (int key = 0; key < 500; key++) {
      assertEquals(ref.containsKey(key), ours.containsKey(key));
      assertEquals(ref.get(key), ours.get(key));
    }
  }
}
