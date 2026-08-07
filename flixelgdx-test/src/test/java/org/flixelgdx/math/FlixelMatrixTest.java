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

import com.badlogic.gdx.math.Matrix4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FlixelMatrixTest {

  private static final float DELTA = 1e-5f;

  @Test
  void identityMatchesGdx() {
    assertArrayEquals(new Matrix4().val, new FlixelMatrix().val, DELTA);
  }

  @Test
  void ortho2DMatchesGdxExactly() {
    Matrix4 gdx = new Matrix4().setToOrtho2D(0f, 0f, 1280f, 720f);
    FlixelMatrix ours = new FlixelMatrix().setToOrtho2D(0f, 0f, 1280f, 720f);
    assertArrayEquals(gdx.val, ours.val, DELTA);
  }

  @Test
  void ortho2DOffsetMatchesGdxExactly() {
    Matrix4 gdx = new Matrix4().setToOrtho2D(-40f, 15f, 800f, 600f);
    FlixelMatrix ours = new FlixelMatrix().setToOrtho2D(-40f, 15f, 800f, 600f);
    assertArrayEquals(gdx.val, ours.val, DELTA);
  }

  @Test
  void mulMatchesGdxExactly() {
    Matrix4 gdxA = new Matrix4().setToOrtho2D(0f, 0f, 1280f, 720f);
    Matrix4 gdxB = new Matrix4().setToOrtho2D(10f, 20f, 640f, 480f);
    Matrix4 gdx = new Matrix4(gdxA).mul(gdxB);

    FlixelMatrix a = new FlixelMatrix().setToOrtho2D(0f, 0f, 1280f, 720f);
    FlixelMatrix b = new FlixelMatrix().setToOrtho2D(10f, 20f, 640f, 480f);
    FlixelMatrix ours = new FlixelMatrix(a).mul(b);

    assertArrayEquals(gdx.val, ours.val, DELTA);
  }

  @Test
  void bridgeRoundTripsThroughFloatArray() {
    // The float[] bridge is how the matrix crosses to and from a GPU library.
    Matrix4 gdx = new Matrix4().setToOrtho2D(5f, 6f, 300f, 200f);
    FlixelMatrix ours = new FlixelMatrix().set(gdx.val);
    assertArrayEquals(gdx.val, ours.val, DELTA);

    Matrix4 back = new Matrix4().set(ours.val);
    assertArrayEquals(gdx.val, back.val, DELTA);
  }
}
