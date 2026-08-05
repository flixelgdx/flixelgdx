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

import org.flixelgdx.math.FlixelRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelArrayTest {

  private static FlixelArray<String> of(String... values) {
    FlixelArray<String> array = new FlixelArray<>(String[]::new);
    for (String v : values) {
      array.add(v);
    }
    return array;
  }

  @Test
  void addAndGet() {
    FlixelArray<String> array = of("a", "b", "c");
    assertEquals(3, array.size);
    assertEquals("a", array.get(0));
    assertEquals("c", array.get(2));
  }

  @Test
  void setReplacesInPlace() {
    FlixelArray<String> array = of("a", "b");
    array.set(1, "z");
    assertEquals("z", array.get(1));
    assertEquals(2, array.size);
  }

  @Test
  void insertShiftsWhenOrdered() {
    FlixelArray<String> array = of("a", "c");
    array.insert(1, "b");
    assertEquals("a", array.get(0));
    assertEquals("b", array.get(1));
    assertEquals("c", array.get(2));
  }

  @Test
  void orderedRemoveShiftsDown() {
    FlixelArray<String> array = of("a", "b", "c", "d");
    assertEquals("b", array.removeIndex(1));
    assertEquals(3, array.size);
    assertEquals("a", array.get(0));
    assertEquals("c", array.get(1));
    assertEquals("d", array.get(2));
  }

  @Test
  void unorderedRemoveSwapsWithLast() {
    FlixelArray<String> array = new FlixelArray<>(String[]::new, false, 8);
    array.add("a");
    array.add("b");
    array.add("c");
    array.add("d");
    // Removing index 1 in an unordered list fills the gap with the last element.
    assertEquals("b", array.removeIndex(1));
    assertEquals(3, array.size);
    assertEquals("d", array.get(1));
  }

  @Test
  void removeValueByEqualsAndIdentity() {
    FlixelArray<String> array = of("a", "b", "c");
    assertTrue(array.removeValue("b", false));
    assertFalse(array.contains("b", false));

    String needle = new String("a");
    // Identity comparison should not match a distinct but equal instance.
    assertFalse(array.removeValue(needle, true));
    assertTrue(array.removeValue("a", false));
  }

  @Test
  void popReturnsAndRemovesLast() {
    FlixelArray<String> array = of("a", "b");
    assertEquals("b", array.pop());
    assertEquals(1, array.size);
    assertEquals("a", array.last());
  }

  @Test
  void firstLastAndEmpty() {
    FlixelArray<String> array = of("a", "b", "c");
    assertEquals("a", array.first());
    assertEquals("c", array.last());
    assertFalse(array.isEmpty());
  }

  @Test
  void indexOfAndContains() {
    FlixelArray<String> array = of("a", "b", "c");
    assertEquals(1, array.indexOf("b", false));
    assertEquals(-1, array.indexOf("z", false));
    assertTrue(array.contains("c", false));
  }

  @Test
  void clearNullsSlots() {
    FlixelArray<String> array = of("a", "b");
    array.clear();
    assertEquals(0, array.size);
    assertTrue(array.isEmpty());
    // The backing slots must be nulled so elements can be collected.
    assertNull(array.items[0]);
    assertNull(array.items[1]);
  }

  @Test
  void growsPastInitialCapacity() {
    FlixelArray<Integer> array = new FlixelArray<>(Integer[]::new, 2);
    for (int i = 0; i < 100; i++) {
      array.add(i);
    }
    assertEquals(100, array.size);
    assertEquals(0, array.get(0));
    assertEquals(99, array.get(99));
  }

  @Test
  void addAllAppendsInOrder() {
    FlixelArray<String> a = of("a", "b");
    FlixelArray<String> b = of("c", "d");
    a.addAll(b);
    assertEquals(4, a.size);
    assertEquals("c", a.get(2));
    assertEquals("d", a.get(3));
  }

  @Test
  void getRandomStaysInBounds() {
    FlixelArray<String> array = of("a", "b", "c");
    FlixelRandom rng = new FlixelRandom(1L);
    for (int i = 0; i < 100; i++) {
      assertTrue(array.contains(array.getRandom(rng), true));
    }
    assertNull(of().getRandom(rng));
  }

  @Test
  void iteratorWalksAllElements() {
    FlixelArray<String> array = of("a", "b", "c");
    StringBuilder joined = new StringBuilder();
    for (String s : array) {
      joined.append(s);
    }
    assertEquals("abc", joined.toString());
  }

  @Test
  void outOfBoundsAccessThrows() {
    FlixelArray<String> array = of("a");
    assertThrows(IndexOutOfBoundsException.class, () -> array.get(5));
    assertThrows(IndexOutOfBoundsException.class, () -> array.removeIndex(5));
  }

  @Test
  void snapshotStaysStableWhileMutating() {
    FlixelArray<String> array = of("x", "y", "z");
    String[] snap = array.begin();
    int snapSize = array.size;

    // Mutate the list mid-iteration: the snapshot must not shift under us.
    array.add("w");
    array.removeValue("y", false);

    StringBuilder seen = new StringBuilder();
    for (int i = 0; i < snapSize; i++) {
      seen.append(snap[i]);
    }
    assertEquals("xyz", seen.toString());
    array.end();

    // After the snapshot ends, the live list reflects the changes.
    assertEquals(3, array.size);
    assertEquals("x", array.get(0));
    assertEquals("z", array.get(1));
    assertEquals("w", array.get(2));
  }
}
