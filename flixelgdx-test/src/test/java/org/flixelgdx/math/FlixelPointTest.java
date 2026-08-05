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

class FlixelPointTest {

  private static final float DELTA = 1e-3f;

  @Test
  void constructorsSetComponents() {
    FlixelPoint origin = new FlixelPoint();
    assertEquals(0f, origin.x, DELTA);
    assertEquals(0f, origin.y, DELTA);

    FlixelPoint p = new FlixelPoint(3f, -4f);
    assertEquals(3f, p.x, DELTA);
    assertEquals(-4f, p.y, DELTA);
  }

  @Test
  void arithmeticHelpers() {
    FlixelPoint p = new FlixelPoint(2f, 3f);
    p.add(1f, 1f);
    assertEquals(3f, p.x, DELTA);
    assertEquals(4f, p.y, DELTA);

    p.subtract(2f, 1f);
    assertEquals(1f, p.x, DELTA);
    assertEquals(3f, p.y, DELTA);

    p.scale(2f);
    assertEquals(2f, p.x, DELTA);
    assertEquals(6f, p.y, DELTA);
  }

  @Test
  void copyFromMirrorsAnother() {
    FlixelPoint source = new FlixelPoint(7f, 8f);
    FlixelPoint dest = new FlixelPoint();
    dest.copyFrom(source);
    assertEquals(7f, dest.x, DELTA);
    assertEquals(8f, dest.y, DELTA);
  }

  @Test
  void distanceToUsesPythagoras() {
    FlixelPoint a = new FlixelPoint(0f, 0f);
    FlixelPoint b = new FlixelPoint(3f, 4f);
    assertEquals(5f, a.distanceTo(b), DELTA);
  }

  @Test
  void angleToCardinalDirections() {
    FlixelPoint origin = new FlixelPoint(0f, 0f);
    assertEquals(0f, origin.angleTo(new FlixelPoint(1f, 0f)), DELTA);
    assertEquals(90f, origin.angleTo(new FlixelPoint(0f, 1f)), DELTA);
    assertEquals(180f, Math.abs(origin.angleTo(new FlixelPoint(-1f, 0f))), DELTA);
  }

  @Test
  void rotateNinetyDegreesAroundOrigin() {
    FlixelPoint p = new FlixelPoint(1f, 0f);
    p.rotate(0f, 0f, 90f);
    assertEquals(0f, p.x, DELTA);
    assertEquals(1f, p.y, DELTA);
  }

  @Test
  void poolReusesFreedInstance() {
    FlixelPoint borrowed = FlixelPoint.get(5f, 5f);
    borrowed.put();
    FlixelPoint next = FlixelPoint.get();
    assertSame(borrowed, next);
    // A pooled point is reset to the origin before being handed out again.
    assertEquals(0f, next.x, DELTA);
    assertEquals(0f, next.y, DELTA);
    next.put();
  }

  @Test
  void weakPointRecyclesOnPutWeak() {
    FlixelPoint w = FlixelPoint.weak(1f, 2f);
    assertTrue(w.isWeak());
    w.putWeak();
    FlixelPoint next = FlixelPoint.get();
    assertSame(w, next);
    assertFalse(next.isWeak());
    next.put();
  }

  @Test
  void putWeakLeavesNormalPointsAlone() {
    FlixelPoint normal = FlixelPoint.get(3f, 3f);
    assertFalse(normal.isWeak());
    // putWeak on a non-weak point should not recycle it.
    normal.putWeak();
    FlixelPoint other = FlixelPoint.get();
    assertFalse(other == normal && other.x == 3f);
    normal.put();
    other.put();
  }

  @Test
  void resetClearsState() {
    FlixelPoint p = FlixelPoint.weak(9f, 9f);
    p.reset();
    assertEquals(0f, p.x, DELTA);
    assertEquals(0f, p.y, DELTA);
    assertFalse(p.isWeak());
  }
}
