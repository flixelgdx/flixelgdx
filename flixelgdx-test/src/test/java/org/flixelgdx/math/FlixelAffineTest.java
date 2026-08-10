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

/**
 * Validates {@link FlixelAffine} against a plain reference implementation of the standard 2D affine
 * operations.
 *
 * <p>The reference ({@link RefAffine}) applies the textbook translate, scale, rotate, and compose
 * formulas with exact trigonometry, so the test pins {@link FlixelAffine} to known-correct math
 * rather than to any one graphics library. Because {@code FlixelAffine} rotates through a lookup
 * table, the rotation case is compared with a looser tolerance.
 */
class FlixelAffineTest {

  private static final float DELTA = 1e-5f;

  // Rotation goes through a lookup table, so allow a slightly looser tolerance.
  private static final float ROT_DELTA = 1e-2f;

  private static void assertMatches(RefAffine ref, FlixelAffine ours, float delta) {
    assertEquals(ref.m00, ours.m00, delta);
    assertEquals(ref.m01, ours.m01, delta);
    assertEquals(ref.m02, ours.m02, delta);
    assertEquals(ref.m10, ours.m10, delta);
    assertEquals(ref.m11, ours.m11, delta);
    assertEquals(ref.m12, ours.m12, delta);
  }

  @Test
  void identityStart() {
    assertMatches(new RefAffine(), new FlixelAffine(), DELTA);
  }

  @Test
  void translateScaleMatchReference() {
    RefAffine ref = new RefAffine().idt().translate(12f, -7f).scale(2f, 3f);
    FlixelAffine ours = new FlixelAffine().idt().translate(12f, -7f).scale(2f, 3f);
    assertMatches(ref, ours, DELTA);
  }

  @Test
  void mulMatchesReference() {
    RefAffine a = new RefAffine().idt().translate(3f, 4f).scale(2f, 2f);
    RefAffine b = new RefAffine().idt().translate(-1f, 5f).scale(0.5f, 1.5f);
    RefAffine ref = new RefAffine(a).mul(b);

    FlixelAffine ao = new FlixelAffine().idt().translate(3f, 4f).scale(2f, 2f);
    FlixelAffine bo = new FlixelAffine().idt().translate(-1f, 5f).scale(0.5f, 1.5f);
    FlixelAffine ours = new FlixelAffine(ao).mul(bo);

    assertMatches(ref, ours, DELTA);
  }

  @Test
  void setToProductMatchesReference() {
    RefAffine l = new RefAffine().idt().translate(3f, 4f).scale(2f, 2f);
    RefAffine r = new RefAffine().idt().translate(-1f, 5f);
    RefAffine ref = new RefAffine().setToProduct(l, r);

    FlixelAffine lo = new FlixelAffine().idt().translate(3f, 4f).scale(2f, 2f);
    FlixelAffine ro = new FlixelAffine().idt().translate(-1f, 5f);
    FlixelAffine ours = new FlixelAffine().setToProduct(lo, ro);

    assertMatches(ref, ours, DELTA);
  }

  @Test
  void fullTransformChainCloseToReference() {
    RefAffine ref = new RefAffine().idt().translate(100f, 50f).rotate(30f).scale(1.5f, 2f);
    FlixelAffine ours = new FlixelAffine().idt().translate(100f, 50f).rotate(30f).scale(1.5f, 2f);
    assertMatches(ref, ours, ROT_DELTA);
  }

  @Test
  void applyToTransformsPoint() {
    FlixelAffine a = new FlixelAffine().idt().translate(10f, 20f).scale(2f, 2f);
    FlixelVector out = a.applyTo(3f, 4f, new FlixelVector());
    // (3,4) scaled by 2 -> (6,8), then translated by (10,20) -> (16,28).
    assertEquals(16f, out.x, DELTA);
    assertEquals(28f, out.y, DELTA);
  }

  /**
   * A minimal reference 2D affine (2x3, row-major) implementing the same operations as
   * {@link FlixelAffine} with exact trigonometry, used only to check {@code FlixelAffine}.
   */
  private static final class RefAffine {

    float m00 = 1f;
    float m01 = 0f;
    float m02 = 0f;
    float m10 = 0f;
    float m11 = 1f;
    float m12 = 0f;

    RefAffine() {}

    RefAffine(RefAffine other) {
      m00 = other.m00;
      m01 = other.m01;
      m02 = other.m02;
      m10 = other.m10;
      m11 = other.m11;
      m12 = other.m12;
    }

    RefAffine idt() {
      m00 = 1f;
      m01 = 0f;
      m02 = 0f;
      m10 = 0f;
      m11 = 1f;
      m12 = 0f;
      return this;
    }

    RefAffine translate(float x, float y) {
      m02 += m00 * x + m01 * y;
      m12 += m10 * x + m11 * y;
      return this;
    }

    RefAffine scale(float sx, float sy) {
      m00 *= sx;
      m01 *= sy;
      m10 *= sx;
      m11 *= sy;
      return this;
    }

    RefAffine rotate(float degrees) {
      double rad = Math.toRadians(degrees);
      float cos = (float) Math.cos(rad);
      float sin = (float) Math.sin(rad);
      float t00 = m00 * cos + m01 * sin;
      float t01 = m00 * -sin + m01 * cos;
      float t10 = m10 * cos + m11 * sin;
      float t11 = m10 * -sin + m11 * cos;
      m00 = t00;
      m01 = t01;
      m10 = t10;
      m11 = t11;
      return this;
    }

    RefAffine mul(RefAffine r) {
      float t00 = m00 * r.m00 + m01 * r.m10;
      float t01 = m00 * r.m01 + m01 * r.m11;
      float t02 = m00 * r.m02 + m01 * r.m12 + m02;
      float t10 = m10 * r.m00 + m11 * r.m10;
      float t11 = m10 * r.m01 + m11 * r.m11;
      float t12 = m10 * r.m02 + m11 * r.m12 + m12;
      m00 = t00;
      m01 = t01;
      m02 = t02;
      m10 = t10;
      m11 = t11;
      m12 = t12;
      return this;
    }

    RefAffine setToProduct(RefAffine l, RefAffine r) {
      m00 = l.m00 * r.m00 + l.m01 * r.m10;
      m01 = l.m00 * r.m01 + l.m01 * r.m11;
      m02 = l.m00 * r.m02 + l.m01 * r.m12 + l.m02;
      m10 = l.m10 * r.m00 + l.m11 * r.m10;
      m11 = l.m10 * r.m01 + l.m11 * r.m11;
      m12 = l.m10 * r.m02 + l.m11 * r.m12 + l.m12;
      return this;
    }
  }
}
