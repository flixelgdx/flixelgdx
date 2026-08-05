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

class FlixelBoolMapTest {

  @Test
  void putGetContainsRemove() {
    FlixelBoolMap<String> map = new FlixelBoolMap<>();
    assertTrue(map.isEmpty());
    assertNull(map.put(true, "on"));
    assertNull(map.put(false, "off"));
    assertEquals("on", map.get(true));
    assertEquals("off", map.get(false));
    assertEquals(2, map.size());
    assertTrue(map.containsKey(true));

    assertEquals("on", map.put(true, "ON"));
    assertEquals("ON", map.get(true));

    assertEquals("off", map.remove(false));
    assertFalse(map.containsKey(false));
    assertEquals(1, map.size());
  }

  @Test
  void nullValuesAndClear() {
    FlixelBoolMap<String> map = new FlixelBoolMap<>();
    map.put(true, null);
    assertTrue(map.containsKey(true));
    assertNull(map.get(true));
    map.clear();
    assertTrue(map.isEmpty());
    assertFalse(map.containsKey(true));
  }
}
