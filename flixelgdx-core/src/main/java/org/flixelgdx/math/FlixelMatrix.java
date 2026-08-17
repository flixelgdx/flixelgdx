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
public class FlixelMatrix {

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
   * Sets this matrix to a 2D orthographic projection using the OpenGL NDC depth convention
   * ({@code [-1, 1]}).
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
    return setToOrtho(x, x + width, y, y + height, 0f, 1f, false);
  }

  /**
   * Sets this matrix to a 2D orthographic projection, choosing the correct depth formula for the
   * active backend.
   *
   * <p>Pass {@code true} when the backend's NDC depth range is {@code [0, 1]} (Vulkan, Metal,
   * Direct3D) and {@code false} for the {@code [-1, 1]} convention (OpenGL, WebGL). Passing the
   * wrong value clips all geometry at the default depth, causing a black screen.
   *
   * @param x The left edge of the projected region.
   * @param y The bottom edge of the projected region.
   * @param width The width of the projected region.
   * @param height The height of the projected region.
   * @param zeroToOne {@code true} for {@code [0, 1]} depth range, {@code false} for {@code [-1, 1]}.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix setToOrtho2D(float x, float y, float width, float height, boolean zeroToOne) {
    return setToOrtho(x, x + width, y, y + height, 0f, 1f, zeroToOne);
  }

  /**
   * Sets this matrix to a general orthographic projection using the OpenGL NDC depth convention
   * ({@code [-1, 1]}).
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
    return setToOrtho(left, right, bottom, top, near, far, false);
  }

  /**
   * Sets this matrix to a general orthographic projection.
   *
   * <p>Pass {@code true} for {@code zeroToOne} when the rendering backend maps NDC depth to
   * {@code [0, 1]} (Vulkan, Metal, Direct3D). Pass {@code false} for the {@code [-1, 1]}
   * convention used by OpenGL and WebGL. Using the wrong convention depth-clips all geometry at
   * the standard near plane, producing a black screen.
   *
   * @param left The left clipping plane.
   * @param right The right clipping plane.
   * @param bottom The bottom clipping plane.
   * @param top The top clipping plane.
   * @param near The near clipping plane.
   * @param far The far clipping plane.
   * @param zeroToOne {@code true} for {@code [0, 1]} depth range, {@code false} for {@code [-1, 1]}.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix setToOrtho(
      float left, float right, float bottom, float top, float near, float far, boolean zeroToOne) {
    float xOrth = 2f / (right - left);
    float yOrth = 2f / (top - bottom);
    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float zOrth;
    float tz;
    if (zeroToOne) {
      // [0, 1] depth range: z = near maps to 0, z = far maps to 1 (Vulkan, Metal, D3D).
      zOrth = 1f / (far - near);
      tz = -near / (far - near);
    } else {
      // [-1, 1] depth range: z = near maps to -1, z = far maps to 1 (OpenGL, WebGL).
      zOrth = -2f / (far - near);
      tz = -(far + near) / (far - near);
    }
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

  /**
   * Post-multiplies this matrix with a translation, so the translation applies first when the
   * matrix transforms a point.
   *
   * @param x Translation along the x axis.
   * @param y Translation along the y axis.
   * @param z Translation along the z axis.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix translate(float x, float y, float z) {
    float[] v = val;
    v[M03] = v[M00] * x + v[M01] * y + v[M02] * z + v[M03];
    v[M13] = v[M10] * x + v[M11] * y + v[M12] * z + v[M13];
    v[M23] = v[M20] * x + v[M21] * y + v[M22] * z + v[M23];
    v[M33] = v[M30] * x + v[M31] * y + v[M32] * z + v[M33];
    return this;
  }

  /**
   * Post-multiplies this matrix with a counter-clockwise rotation around the z axis, the only
   * rotation a 2D renderer needs.
   *
   * @param degrees Rotation angle in degrees.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix rotateZ(float degrees) {
    float radians = degrees * FlixelMath.DEG_TO_RAD;
    float cos = (float) Math.cos(radians);
    float sin = (float) Math.sin(radians);
    float[] v = val;
    float t00 = v[M00] * cos + v[M01] * sin;
    float t01 = v[M00] * -sin + v[M01] * cos;
    float t10 = v[M10] * cos + v[M11] * sin;
    float t11 = v[M10] * -sin + v[M11] * cos;
    float t20 = v[M20] * cos + v[M21] * sin;
    float t21 = v[M20] * -sin + v[M21] * cos;
    float t30 = v[M30] * cos + v[M31] * sin;
    float t31 = v[M30] * -sin + v[M31] * cos;
    v[M00] = t00;
    v[M01] = t01;
    v[M10] = t10;
    v[M11] = t11;
    v[M20] = t20;
    v[M21] = t21;
    v[M30] = t30;
    v[M31] = t31;
    return this;
  }

  /**
   * Post-multiplies this matrix with a scale.
   *
   * @param x Scale factor along the x axis.
   * @param y Scale factor along the y axis.
   * @param z Scale factor along the z axis.
   * @return This matrix, for chaining.
   */
  public @NotNull FlixelMatrix scale(float x, float y, float z) {
    float[] v = val;
    v[M00] *= x;
    v[M10] *= x;
    v[M20] *= x;
    v[M30] *= x;
    v[M01] *= y;
    v[M11] *= y;
    v[M21] *= y;
    v[M31] *= y;
    v[M02] *= z;
    v[M12] *= z;
    v[M22] *= z;
    v[M32] *= z;
    return this;
  }
}
