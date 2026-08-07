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

import com.badlogic.gdx.math.Affine2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelAffineTest {

  private static final float DELTA = 1e-5f;

  // Rotation goes through a lookup table, so allow a slightly looser tolerance.
  private static final float ROT_DELTA = 1e-2f;

  private static void assertMatches(Affine2 gdx, FlixelAffine ours, float delta) {
    assertEquals(gdx.m00, ours.m00, delta);
    assertEquals(gdx.m01, ours.m01, delta);
    assertEquals(gdx.m02, ours.m02, delta);
    assertEquals(gdx.m10, ours.m10, delta);
    assertEquals(gdx.m11, ours.m11, delta);
    assertEquals(gdx.m12, ours.m12, delta);
  }

  @Test
  void identityStart() {
    assertMatches(new Affine2(), new FlixelAffine(), DELTA);
  }

  @Test
  void translateScaleMatchGdxExactly() {
    Affine2 gdx = new Affine2().idt().translate(12f, -7f).scale(2f, 3f);
    FlixelAffine ours = new FlixelAffine().idt().translate(12f, -7f).scale(2f, 3f);
    assertMatches(gdx, ours, DELTA);
  }

  @Test
  void mulMatchesGdxExactly() {
    Affine2 a = new Affine2().idt().translate(3f, 4f).scale(2f, 2f);
    Affine2 b = new Affine2().idt().translate(-1f, 5f).scale(0.5f, 1.5f);
    Affine2 gdx = new Affine2(a).mul(b);

    FlixelAffine ao = new FlixelAffine().idt().translate(3f, 4f).scale(2f, 2f);
    FlixelAffine bo = new FlixelAffine().idt().translate(-1f, 5f).scale(0.5f, 1.5f);
    FlixelAffine ours = new FlixelAffine(ao).mul(bo);

    assertMatches(gdx, ours, DELTA);
  }

  @Test
  void setToProductMatchesGdxExactly() {
    Affine2 l = new Affine2().idt().translate(3f, 4f).scale(2f, 2f);
    Affine2 r = new Affine2().idt().translate(-1f, 5f);
    Affine2 gdx = new Affine2().setToProduct(l, r);

    FlixelAffine lo = new FlixelAffine().idt().translate(3f, 4f).scale(2f, 2f);
    FlixelAffine ro = new FlixelAffine().idt().translate(-1f, 5f);
    FlixelAffine ours = new FlixelAffine().setToProduct(lo, ro);

    assertMatches(gdx, ours, DELTA);
  }

  @Test
  void fullTransformChainCloseToGdx() {
    Affine2 gdx = new Affine2().idt().translate(100f, 50f).rotate(30f).scale(1.5f, 2f);
    FlixelAffine ours = new FlixelAffine().idt().translate(100f, 50f).rotate(30f).scale(1.5f, 2f);
    assertMatches(gdx, ours, ROT_DELTA);
  }

  @Test
  void applyToTransformsPoint() {
    FlixelAffine a = new FlixelAffine().idt().translate(10f, 20f).scale(2f, 2f);
    FlixelVector out = a.applyTo(3f, 4f, new FlixelVector());
    // (3,4) scaled by 2 -> (6,8), then translated by (10,20) -> (16,28).
    assertEquals(16f, out.x, DELTA);
    assertEquals(28f, out.y, DELTA);
  }
}
