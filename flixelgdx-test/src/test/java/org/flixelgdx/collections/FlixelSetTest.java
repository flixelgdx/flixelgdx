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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelSetTest {

  @Test
  void addReportsNoveltyAndDeduplicates() {
    FlixelSet<String> set = new FlixelSet<>();
    assertTrue(set.add("a"));
    assertFalse(set.add("a"));
    assertEquals(1, set.size());
  }

  @Test
  void containsAndRemove() {
    FlixelSet<String> set = new FlixelSet<>();
    set.add("a");
    assertTrue(set.contains("a"));
    assertTrue(set.remove("a"));
    assertFalse(set.contains("a"));
    assertFalse(set.remove("a"));
    assertTrue(set.isEmpty());
  }

  @Test
  void nullRejected() {
    FlixelSet<String> set = new FlixelSet<>();
    assertThrows(IllegalArgumentException.class, () -> set.add(null));
  }

  @Test
  void clearEmptiesSet() {
    FlixelSet<String> set = new FlixelSet<>();
    set.add("a");
    set.add("b");
    set.clear();
    assertEquals(0, set.size());
    assertFalse(set.contains("a"));
  }

  @Test
  void iterationVisitsEveryElement() {
    FlixelSet<Integer> set = new FlixelSet<>();
    for (int i = 0; i < 50; i++) {
      set.add(i);
    }
    Set<Integer> seen = new HashSet<>();
    for (int value : set) {
      seen.add(value);
    }
    assertEquals(50, seen.size());
  }

  @Test
  void behavesLikeHashSetUnderRandomOps() {
    FlixelSet<Integer> ours = new FlixelSet<>(4);
    Set<Integer> ref = new HashSet<>();
    Random random = new Random(7);

    for (int i = 0; i < 20000; i++) {
      int value = random.nextInt(300);
      int op = random.nextInt(3);
      if (op == 0) {
        assertEquals(ref.add(value), ours.add(value));
      } else if (op == 1) {
        assertEquals(ref.remove(value), ours.remove(value));
      } else {
        assertEquals(ref.contains(value), ours.contains(value));
      }
      assertEquals(ref.size(), ours.size());
    }
  }
}
