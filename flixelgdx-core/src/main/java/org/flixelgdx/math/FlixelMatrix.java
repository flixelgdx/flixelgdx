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

import org.jetbrains.annotations.NotNull;

/**
 * A 4x4 matrix used for camera projection and world transforms.
 *
 * <p>The sixteen components live in {@link #val}, stored in column-major order
 * (the same layout OpenGL shaders expect), so element {@code (row, col)} is at
 * {@code val[col * 4 + row]}. Named index constants ({@link #M00} and friends)
 * make direct access readable.
 *
 * <p>For 2D rendering the common job is a screen-space projection built with
 * {@link #setToOrtho2D}, optionally combined with a world transform via
 * {@link #mul}. All builder methods mutate this matrix in place and return
 * {@code this} for chaining.
 */
public final class FlixelMatrix {

  /** Index of row 0, column 0 in {@link #val}. */
  public static final int M00 = 0;

  /** Index of row 0, column 1 in {@link #val}. */
  public static final int M01 = 4;

  /** Index of row 0, column 2 in {@link #val}. */
  public static final int M02 = 8;

  /** Index of row 0, column 3 in {@link #val}. */
  public static final int M03 = 12;

  /** Index of row 1, column 0 in {@link #val}. */
  public static final int M10 = 1;

  /** Index of row 1, column 1 in {@link #val}. */
  public static final int M11 = 5;

  /** Index of row 1, column 2 in {@link #val}. */
  public static final int M12 = 9;

  /** Index of row 1, column 3 in {@link #val}. */
  public static final int M13 = 13;

  /** Index of row 2, column 0 in {@link #val}. */
  public static final int M20 = 2;

  /** Index of row 2, column 1 in {@link #val}. */
  public static final int M21 = 6;

  /** Index of row 2, column 2 in {@link #val}. */
  public static final int M22 = 10;

  /** Index of row 2, column 3 in {@link #val}. */
  public static final int M23 = 14;

  /** Index of row 3, column 0 in {@link #val}. */
  public static final int M30 = 3;

  /** Index of row 3, column 1 in {@link #val}. */
  public static final int M31 = 7;

  /** Index of row 3, column 2 in {@link #val}. */
  public static final int M32 = 11;

  /** Index of row 3, column 3 in {@link #val}. */
  public static final int M33 = 15;

  /** The sixteen matrix components, column-major. */
  public final float[] val = new float[16];

  /**
   * Creates an identity matrix.
   */
  public FlixelMatrix() {
    idt();
  }

  /**
   * Creates a matrix that copies another.
   *
   * @param other The matrix to copy.
   */
  public FlixelMatrix(@NotNull FlixelMatrix other) {
    set(other);
  }

  /**
   * Resets this matrix to the identity.
   *
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix idt() {
    float[] v = val;
    v[M00] = 1f;
    v[M01] = 0f;
    v[M02] = 0f;
    v[M03] = 0f;
    v[M10] = 0f;
    v[M11] = 1f;
    v[M12] = 0f;
    v[M13] = 0f;
    v[M20] = 0f;
    v[M21] = 0f;
    v[M22] = 1f;
    v[M23] = 0f;
    v[M30] = 0f;
    v[M31] = 0f;
    v[M32] = 0f;
    v[M33] = 1f;
    return this;
  }

  /**
   * Copies another matrix's components into this one.
   *
   * @param other The matrix to copy from.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix set(@NotNull FlixelMatrix other) {
    System.arraycopy(other.val, 0, val, 0, 16);
    return this;
  }

  /**
   * Copies sixteen column-major components into this matrix.
   *
   * <p>This is the bridge used to accept a matrix from an external
   * column-major source (for example a GPU library's matrix array).
   *
   * @param values The sixteen components, column-major; must have length 16.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix set(@NotNull float[] values) {
    System.arraycopy(values, 0, val, 0, 16);
    return this;
  }

  /**
   * Sets this matrix to a 2D orthographic projection.
   *
   * <p>The result maps the rectangle {@code [x, x + width] x [y, y + height]}
   * onto the normalized device cube, which is exactly what a 2D camera needs to
   * project world coordinates onto the screen.
   *
   * @param x The left edge of the projected region.
   * @param y The bottom edge of the projected region.
   * @param width The width of the projected region.
   * @param height The height of the projected region.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix setToOrtho2D(float x, float y, float width, float height) {
    return setToOrtho(x, x + width, y, y + height, 0f, 1f);
  }

  /**
   * Sets this matrix to a general orthographic projection.
   *
   * @param left The left clipping plane.
   * @param right The right clipping plane.
   * @param bottom The bottom clipping plane.
   * @param top The top clipping plane.
   * @param near The near clipping plane.
   * @param far The far clipping plane.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix setToOrtho(
      float left, float right, float bottom, float top, float near, float far) {
    float xOrth = 2f / (right - left);
    float yOrth = 2f / (top - bottom);
    float zOrth = -2f / (far - near);
    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);
    float[] v = val;
    v[M00] = xOrth;
    v[M10] = 0f;
    v[M20] = 0f;
    v[M30] = 0f;
    v[M01] = 0f;
    v[M11] = yOrth;
    v[M21] = 0f;
    v[M31] = 0f;
    v[M02] = 0f;
    v[M12] = 0f;
    v[M22] = zOrth;
    v[M32] = 0f;
    v[M03] = tx;
    v[M13] = ty;
    v[M23] = tz;
    v[M33] = 1f;
    return this;
  }

  /**
   * Post-multiplies another matrix onto this one ({@code this = this * other}).
   *
   * @param other The matrix to apply after this one.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix mul(@NotNull FlixelMatrix other) {
    float[] a = val;
    float[] b = other.val;
    float t00 = a[M00] * b[M00] + a[M01] * b[M10] + a[M02] * b[M20] + a[M03] * b[M30];
    float t01 = a[M00] * b[M01] + a[M01] * b[M11] + a[M02] * b[M21] + a[M03] * b[M31];
    float t02 = a[M00] * b[M02] + a[M01] * b[M12] + a[M02] * b[M22] + a[M03] * b[M32];
    float t03 = a[M00] * b[M03] + a[M01] * b[M13] + a[M02] * b[M23] + a[M03] * b[M33];
    float t10 = a[M10] * b[M00] + a[M11] * b[M10] + a[M12] * b[M20] + a[M13] * b[M30];
    float t11 = a[M10] * b[M01] + a[M11] * b[M11] + a[M12] * b[M21] + a[M13] * b[M31];
    float t12 = a[M10] * b[M02] + a[M11] * b[M12] + a[M12] * b[M22] + a[M13] * b[M32];
    float t13 = a[M10] * b[M03] + a[M11] * b[M13] + a[M12] * b[M23] + a[M13] * b[M33];
    float t20 = a[M20] * b[M00] + a[M21] * b[M10] + a[M22] * b[M20] + a[M23] * b[M30];
    float t21 = a[M20] * b[M01] + a[M21] * b[M11] + a[M22] * b[M21] + a[M23] * b[M31];
    float t22 = a[M20] * b[M02] + a[M21] * b[M12] + a[M22] * b[M22] + a[M23] * b[M32];
    float t23 = a[M20] * b[M03] + a[M21] * b[M13] + a[M22] * b[M23] + a[M23] * b[M33];
    float t30 = a[M30] * b[M00] + a[M31] * b[M10] + a[M32] * b[M20] + a[M33] * b[M30];
    float t31 = a[M30] * b[M01] + a[M31] * b[M11] + a[M32] * b[M21] + a[M33] * b[M31];
    float t32 = a[M30] * b[M02] + a[M31] * b[M12] + a[M32] * b[M22] + a[M33] * b[M32];
    float t33 = a[M30] * b[M03] + a[M31] * b[M13] + a[M32] * b[M23] + a[M33] * b[M33];
    a[M00] = t00;
    a[M01] = t01;
    a[M02] = t02;
    a[M03] = t03;
    a[M10] = t10;
    a[M11] = t11;
    a[M12] = t12;
    a[M13] = t13;
    a[M20] = t20;
    a[M21] = t21;
    a[M22] = t22;
    a[M23] = t23;
    a[M30] = t30;
    a[M31] = t31;
    a[M32] = t32;
    a[M33] = t33;
    return this;
  }
}
