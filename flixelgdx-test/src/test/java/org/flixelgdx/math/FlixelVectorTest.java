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

class FlixelVectorTest {

  private static final float DELTA = 1e-3f;

  @Test
  void lengthAndLengthSquared() {
    FlixelVector v = new FlixelVector(3f, 4f);
    assertEquals(5f, v.length(), DELTA);
    assertEquals(25f, v.lengthSquared(), DELTA);
  }

  @Test
  void normalizeGivesUnitLength() {
    FlixelVector v = new FlixelVector(0f, 5f).normalize();
    assertEquals(1f, v.length(), DELTA);
    assertEquals(0f, v.x, DELTA);
    assertEquals(1f, v.y, DELTA);
    // Normalizing a zero vector leaves it unchanged.
    assertTrue(new FlixelVector().normalize().isZero());
  }

  @Test
  void dotProduct() {
    assertEquals(11f, new FlixelVector(1f, 2f).dot(new FlixelVector(3f, 4f)), DELTA);
    assertEquals(0f, new FlixelVector(1f, 0f).dot(new FlixelVector(0f, 1f)), DELTA);
  }

  @Test
  void perAxisSettersAndNegate() {
    FlixelVector v = new FlixelVector().setX(2f).setY(3f);
    assertEquals(2f, v.x, DELTA);
    assertEquals(3f, v.y, DELTA);
    v.negate();
    assertEquals(-2f, v.x, DELTA);
    assertEquals(-3f, v.y, DELTA);
    assertTrue(v.setZero().isZero());
  }

  @Test
  void vectorArithmeticOverloads() {
    FlixelVector v = new FlixelVector(1f, 1f);
    v.add(new FlixelVector(2f, 3f));
    assertEquals(3f, v.x, DELTA);
    assertEquals(4f, v.y, DELTA);
    v.subtract(new FlixelVector(1f, 1f));
    assertEquals(2f, v.x, DELTA);
    assertEquals(3f, v.y, DELTA);
    v.scale(2f, 3f);
    assertEquals(4f, v.x, DELTA);
    assertEquals(9f, v.y, DELTA);
  }

  @Test
  void constructorsSetComponents() {
    FlixelVector origin = new FlixelVector();
    assertEquals(0f, origin.x, DELTA);
    assertEquals(0f, origin.y, DELTA);

    FlixelVector p = new FlixelVector(3f, -4f);
    assertEquals(3f, p.x, DELTA);
    assertEquals(-4f, p.y, DELTA);
  }

  @Test
  void arithmeticHelpers() {
    FlixelVector p = new FlixelVector(2f, 3f);
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
    FlixelVector source = new FlixelVector(7f, 8f);
    FlixelVector dest = new FlixelVector();
    dest.copyFrom(source);
    assertEquals(7f, dest.x, DELTA);
    assertEquals(8f, dest.y, DELTA);
  }

  @Test
  void distanceToUsesPythagoras() {
    FlixelVector a = new FlixelVector(0f, 0f);
    FlixelVector b = new FlixelVector(3f, 4f);
    assertEquals(5f, a.distanceTo(b), DELTA);
  }

  @Test
  void angleToCardinalDirections() {
    FlixelVector origin = new FlixelVector(0f, 0f);
    assertEquals(0f, origin.angleTo(new FlixelVector(1f, 0f)), DELTA);
    assertEquals(90f, origin.angleTo(new FlixelVector(0f, 1f)), DELTA);
    assertEquals(180f, Math.abs(origin.angleTo(new FlixelVector(-1f, 0f))), DELTA);
  }

  @Test
  void rotateNinetyDegreesAroundOrigin() {
    FlixelVector p = new FlixelVector(1f, 0f);
    p.rotate(0f, 0f, 90f);
    assertEquals(0f, p.x, DELTA);
    assertEquals(1f, p.y, DELTA);
  }

  @Test
  void poolReusesFreedInstance() {
    FlixelVector borrowed = FlixelVector.get(5f, 5f);
    borrowed.put();
    FlixelVector next = FlixelVector.get();
    assertSame(borrowed, next);
    // A pooled point is reset to the origin before being handed out again.
    assertEquals(0f, next.x, DELTA);
    assertEquals(0f, next.y, DELTA);
    next.put();
  }

  @Test
  void weakPointRecyclesOnPutWeak() {
    FlixelVector w = FlixelVector.weak(1f, 2f);
    assertTrue(w.isWeak());
    w.putWeak();
    FlixelVector next = FlixelVector.get();
    assertSame(w, next);
    assertFalse(next.isWeak());
    next.put();
  }

  @Test
  void putWeakLeavesNormalPointsAlone() {
    FlixelVector normal = FlixelVector.get(3f, 3f);
    assertFalse(normal.isWeak());
    // putWeak on a non-weak point should not recycle it.
    normal.putWeak();
    FlixelVector other = FlixelVector.get();
    assertFalse(other == normal && other.x == 3f);
    normal.put();
    other.put();
  }

  @Test
  void resetClearsState() {
    FlixelVector p = FlixelVector.weak(9f, 9f);
    p.reset();
    assertEquals(0f, p.x, DELTA);
    assertEquals(0f, p.y, DELTA);
    assertFalse(p.isWeak());
  }
}
