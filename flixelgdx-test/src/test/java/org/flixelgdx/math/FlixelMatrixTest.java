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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Validates {@link FlixelMatrix} against the standard column-major projection math it must produce.
 *
 * <p>The expected values are built here from the textbook orthographic and multiply formulas, so
 * the test pins the matrix to a known-correct reference rather than to any one graphics library.
 * The layout is column-major: {@code val[column * 4 + row]}.
 */
class FlixelMatrixTest {

  private static final float DELTA = 1e-5f;

  @Test
  void identityMatchesReference() {
    assertArrayEquals(identity(), new FlixelMatrix().val, DELTA);
  }

  @Test
  void ortho2DMatchesReference() {
    FlixelMatrix ours = new FlixelMatrix().setToOrtho2D(0f, 0f, 1280f, 720f);
    assertArrayEquals(ortho2D(0f, 0f, 1280f, 720f), ours.val, DELTA);
  }

  @Test
  void ortho2DOffsetMatchesReference() {
    FlixelMatrix ours = new FlixelMatrix().setToOrtho2D(-40f, 15f, 800f, 600f);
    assertArrayEquals(ortho2D(-40f, 15f, 800f, 600f), ours.val, DELTA);
  }

  @Test
  void mulMatchesReference() {
    float[] refA = ortho2D(0f, 0f, 1280f, 720f);
    float[] refB = ortho2D(10f, 20f, 640f, 480f);

    FlixelMatrix a = new FlixelMatrix().setToOrtho2D(0f, 0f, 1280f, 720f);
    FlixelMatrix b = new FlixelMatrix().setToOrtho2D(10f, 20f, 640f, 480f);
    FlixelMatrix ours = new FlixelMatrix(a).mul(b);

    assertArrayEquals(mul(refA, refB), ours.val, DELTA);
  }

  @Test
  void bridgeRoundTripsThroughFloatArray() {
    // The float[] bridge is how the matrix crosses to and from a GPU library.
    float[] ref = ortho2D(5f, 6f, 300f, 200f);
    FlixelMatrix ours = new FlixelMatrix().set(ref);
    assertArrayEquals(ref, ours.val, DELTA);
  }

  /** The 4x4 identity in column-major order. */
  private static float[] identity() {
    float[] m = new float[16];
    m[0] = 1f;
    m[5] = 1f;
    m[10] = 1f;
    m[15] = 1f;
    return m;
  }

  /** The standard 2D orthographic projection (near 0, far 1) in column-major order. */
  private static float[] ortho2D(float x, float y, float width, float height) {
    float left = x;
    float right = x + width;
    float bottom = y;
    float top = y + height;
    float[] m = new float[16];
    m[0] = 2f / (right - left);
    m[5] = 2f / (top - bottom);
    m[10] = -2f;
    m[12] = -(right + left) / (right - left);
    m[13] = -(top + bottom) / (top - bottom);
    m[14] = -1f;
    m[15] = 1f;
    return m;
  }

  /** Standard column-major matrix product {@code a * b}. */
  private static float[] mul(float[] a, float[] b) {
    float[] r = new float[16];
    for (int col = 0; col < 4; col++) {
      for (int row = 0; row < 4; row++) {
        float sum = 0f;
        for (int k = 0; k < 4; k++) {
          sum += a[k * 4 + row] * b[col * 4 + k];
        }
        r[col * 4 + row] = sum;
      }
    }
    return r;
  }
}
