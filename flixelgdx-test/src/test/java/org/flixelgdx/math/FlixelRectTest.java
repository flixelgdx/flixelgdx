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
package org.flixelgdx.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelRectTest {

  private static final float DELTA = 1e-5f;

  @Test
  void containsRespectsInclusiveTopLeftExclusiveBottomRight() {
    FlixelRect rect = new FlixelRect(0f, 0f, 10f, 10f);
    assertTrue(rect.contains(0f, 0f));
    assertTrue(rect.contains(5f, 5f));
    assertTrue(rect.contains(9.99f, 9.99f));
    assertFalse(rect.contains(10f, 10f));
    assertFalse(rect.contains(-1f, 5f));
  }

  @Test
  void containsPointOverload() {
    FlixelRect rect = new FlixelRect(0f, 0f, 4f, 4f);
    assertTrue(rect.contains(new FlixelPoint(2f, 2f)));
    assertFalse(rect.contains(new FlixelPoint(5f, 2f)));
  }

  @Test
  void overlapsDetectsSharedArea() {
    FlixelRect a = new FlixelRect(0f, 0f, 10f, 10f);
    FlixelRect b = new FlixelRect(5f, 5f, 10f, 10f);
    FlixelRect far = new FlixelRect(100f, 100f, 5f, 5f);
    assertTrue(a.overlaps(b));
    assertFalse(a.overlaps(far));
  }

  @Test
  void adjacentRectanglesDoNotOverlap() {
    FlixelRect a = new FlixelRect(0f, 0f, 10f, 10f);
    FlixelRect touching = new FlixelRect(10f, 0f, 10f, 10f);
    assertFalse(a.overlaps(touching));
  }

  @Test
  void unionExpandsToCoverBoth() {
    FlixelRect a = new FlixelRect(0f, 0f, 10f, 10f);
    FlixelRect b = new FlixelRect(20f, 20f, 10f, 10f);
    a.union(b);
    assertEquals(0f, a.x, DELTA);
    assertEquals(0f, a.y, DELTA);
    assertEquals(30f, a.width, DELTA);
    assertEquals(30f, a.height, DELTA);
  }

  @Test
  void intersectionOnOverlap() {
    FlixelRect a = new FlixelRect(0f, 0f, 10f, 10f);
    FlixelRect b = new FlixelRect(5f, 5f, 10f, 10f);
    assertTrue(a.intersection(b));
    assertEquals(5f, a.x, DELTA);
    assertEquals(5f, a.y, DELTA);
    assertEquals(5f, a.width, DELTA);
    assertEquals(5f, a.height, DELTA);
  }

  @Test
  void intersectionWithoutOverlapBecomesEmpty() {
    FlixelRect a = new FlixelRect(0f, 0f, 10f, 10f);
    FlixelRect b = new FlixelRect(50f, 50f, 10f, 10f);
    assertFalse(a.intersection(b));
    assertTrue(a.isEmpty());
    assertEquals(0f, a.width, DELTA);
    assertEquals(0f, a.height, DELTA);
  }

  @Test
  void edgeAccessors() {
    FlixelRect rect = new FlixelRect(2f, 3f, 10f, 20f);
    assertEquals(12f, rect.getRight(), DELTA);
    assertEquals(23f, rect.getBottom(), DELTA);
  }

  @Test
  void isEmptyForZeroSize() {
    assertTrue(new FlixelRect(0f, 0f, 0f, 5f).isEmpty());
    assertTrue(new FlixelRect(0f, 0f, 5f, 0f).isEmpty());
    assertFalse(new FlixelRect(0f, 0f, 5f, 5f).isEmpty());
  }

  @Test
  void poolReusesFreedInstance() {
    FlixelRect borrowed = FlixelRect.get(1f, 2f, 3f, 4f);
    borrowed.put();
    FlixelRect next = FlixelRect.get();
    assertSame(borrowed, next);
    assertEquals(0f, next.width, DELTA);
    next.put();
  }

  @Test
  void weakRectRecyclesOnPutWeak() {
    FlixelRect w = FlixelRect.weak(1f, 1f, 1f, 1f);
    assertTrue(w.isWeak());
    w.putWeak();
    FlixelRect next = FlixelRect.get();
    assertSame(w, next);
    next.put();
  }
}
