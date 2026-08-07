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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelIdentityMapTest {

  @Test
  void distinctButEqualKeysAreSeparate() {
    FlixelIdentityMap<String, Integer> map = new FlixelIdentityMap<>();
    String a = new String("key");
    String b = new String("key");
    // a.equals(b) is true, but they are different objects.
    map.put(a, 1);
    map.put(b, 2);
    assertEquals(2, map.getSize());
    assertEquals(1, map.get(a));
    assertEquals(2, map.get(b));
  }

  @Test
  void sameObjectReplaces() {
    FlixelIdentityMap<Object, Integer> map = new FlixelIdentityMap<>();
    Object key = new Object();
    assertNull(map.put(key, 1));
    assertEquals(1, map.put(key, 2));
    assertEquals(2, map.get(key));
    assertEquals(1, map.getSize());
  }

  @Test
  void containsRemoveAndDefault() {
    FlixelIdentityMap<Object, Integer> map = new FlixelIdentityMap<>();
    Object key = new Object();
    map.put(key, 7);
    assertTrue(map.containsKey(key));
    assertEquals(7, map.getOrDefault(key, 99));
    assertEquals(99, map.getOrDefault(new Object(), 99));
    assertEquals(7, map.remove(key));
    assertFalse(map.containsKey(key));
  }

  @Test
  void entriesIterationVisitsEveryObject() {
    FlixelIdentityMap<Object, Integer> map = new FlixelIdentityMap<>();
    for (int i = 0; i < 100; i++) {
      map.put(new Object(), i);
    }
    int count = 0;
    int sum = 0;
    for (FlixelIdentityMap.Entry<Object, Integer> e : map.entries()) {
      count++;
      sum += e.value;
    }
    assertEquals(100, count);
    assertEquals(4950, sum);
  }

  @Test
  void growthRehashesEverything() {
    FlixelIdentityMap<Object, Integer> map = new FlixelIdentityMap<>(4);
    Object[] keys = new Object[200];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = new Object();
      map.put(keys[i], i);
    }
    for (int i = 0; i < keys.length; i++) {
      assertEquals(i, map.get(keys[i]));
    }
    assertEquals(200, map.getSize());
  }
}
