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
 * A 2D affine transform: a 2x3 matrix that combines translation, rotation,
 * scale, and shear.
 *
 * <p>An affine transform maps a point {@code (x, y)} to
 * {@code (m00*x + m01*y + m02, m10*x + m11*y + m12)}. That single matrix can
 * represent any chain of moves, rotations, and scales, which is why sprite and
 * rig rendering compose one per drawn piece and hand it straight to the batch.
 * The six components are public so the render path can read them without method
 * calls.
 *
 * <p>Each builder method ({@link #translate}, {@link #rotate}, {@link #scale},
 * {@link #mul}) multiplies the given step onto the current transform and returns
 * {@code this} for chaining, so a full transform reads top to bottom:
 *
 * <pre>{@code
 * FlixelAffine a = new FlixelAffine().idt()
 *     .translate(worldX, worldY)
 *     .rotate(angle)
 *     .scale(scaleX, scaleY);
 * }</pre>
 */
public final class FlixelAffine {

  /** Row 0, column 0 (x scale / cosine term). */
  public float m00 = 1f;

  /** Row 0, column 1 (x shear / negative sine term). */
  public float m01 = 0f;

  /** Row 0, column 2 (x translation). */
  public float m02 = 0f;

  /** Row 1, column 0 (y shear / sine term). */
  public float m10 = 0f;

  /** Row 1, column 1 (y scale / cosine term). */
  public float m11 = 1f;

  /** Row 1, column 2 (y translation). */
  public float m12 = 0f;

  /**
   * Creates an identity transform (no translation, rotation, or scale).
   */
  public FlixelAffine() {}

  /**
   * Creates a transform that copies another.
   *
   * @param other The transform to copy.
   */
  public FlixelAffine(@NotNull FlixelAffine other) {
    set(other);
  }

  /**
   * Resets this transform to the identity.
   *
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine idt() {
    m00 = 1f;
    m01 = 0f;
    m02 = 0f;
    m10 = 0f;
    m11 = 1f;
    m12 = 0f;
    return this;
  }

  /**
   * Copies another transform's components into this one.
   *
   * @param other The transform to copy from.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine set(@NotNull FlixelAffine other) {
    m00 = other.m00;
    m01 = other.m01;
    m02 = other.m02;
    m10 = other.m10;
    m11 = other.m11;
    m12 = other.m12;
    return this;
  }

  /**
   * Sets the six components directly.
   *
   * @param m00 Row 0, column 0.
   * @param m01 Row 0, column 1.
   * @param m02 Row 0, column 2.
   * @param m10 Row 1, column 0.
   * @param m11 Row 1, column 1.
   * @param m12 Row 1, column 2.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine set(float m00, float m01, float m02, float m10, float m11, float m12) {
    this.m00 = m00;
    this.m01 = m01;
    this.m02 = m02;
    this.m10 = m10;
    this.m11 = m11;
    this.m12 = m12;
    return this;
  }

  /**
   * Post-multiplies a translation onto this transform.
   *
   * @param x The horizontal translation.
   * @param y The vertical translation.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine translate(float x, float y) {
    m02 += m00 * x + m01 * y;
    m12 += m10 * x + m11 * y;
    return this;
  }

  /**
   * Post-multiplies a rotation onto this transform.
   *
   * @param degrees The rotation angle, in degrees.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine rotate(float degrees) {
    if (degrees == 0f) {
      return this;
    }
    float cos = FlixelMath.cosDeg(degrees);
    float sin = FlixelMath.sinDeg(degrees);
    float tmp00 = m00 * cos + m01 * sin;
    float tmp01 = m00 * -sin + m01 * cos;
    float tmp10 = m10 * cos + m11 * sin;
    float tmp11 = m10 * -sin + m11 * cos;
    m00 = tmp00;
    m01 = tmp01;
    m10 = tmp10;
    m11 = tmp11;
    return this;
  }

  /**
   * Post-multiplies a scale onto this transform.
   *
   * @param scaleX The horizontal scale.
   * @param scaleY The vertical scale.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine scale(float scaleX, float scaleY) {
    m00 *= scaleX;
    m01 *= scaleY;
    m10 *= scaleX;
    m11 *= scaleY;
    return this;
  }

  /**
   * Post-multiplies another transform onto this one ({@code this = this * other}).
   *
   * @param other The transform to apply after this one.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine mul(@NotNull FlixelAffine other) {
    float tmp00 = m00 * other.m00 + m01 * other.m10;
    float tmp01 = m00 * other.m01 + m01 * other.m11;
    float tmp02 = m00 * other.m02 + m01 * other.m12 + m02;
    float tmp10 = m10 * other.m00 + m11 * other.m10;
    float tmp11 = m10 * other.m01 + m11 * other.m11;
    float tmp12 = m10 * other.m02 + m11 * other.m12 + m12;
    m00 = tmp00;
    m01 = tmp01;
    m02 = tmp02;
    m10 = tmp10;
    m11 = tmp11;
    m12 = tmp12;
    return this;
  }

  /**
   * Sets this transform to the product of two others ({@code this = left * right}),
   * without allocating.
   *
   * @param left The left-hand transform.
   * @param right The right-hand transform.
   * @return This transform, for chaining.
   */
  public @NotNull FlixelAffine setToProduct(@NotNull FlixelAffine left, @NotNull FlixelAffine right) {
    m00 = left.m00 * right.m00 + left.m01 * right.m10;
    m01 = left.m00 * right.m01 + left.m01 * right.m11;
    m02 = left.m00 * right.m02 + left.m01 * right.m12 + left.m02;
    m10 = left.m10 * right.m00 + left.m11 * right.m10;
    m11 = left.m10 * right.m01 + left.m11 * right.m11;
    m12 = left.m10 * right.m02 + left.m11 * right.m12 + left.m12;
    return this;
  }

  /**
   * Applies this transform to a point, writing the result into {@code out}.
   *
   * @param x The point's horizontal component.
   * @param y The point's vertical component.
   * @param out The vector to receive the transformed point.
   * @return {@code out}, for chaining.
   */
  public @NotNull FlixelVector applyTo(float x, float y, @NotNull FlixelVector out) {
    out.x = m00 * x + m01 * y + m02;
    out.y = m10 * x + m11 * y + m12;
    return out;
  }
}
