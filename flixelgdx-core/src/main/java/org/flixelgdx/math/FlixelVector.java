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

import org.flixelgdx.collections.FlixelPool;
import org.flixelgdx.collections.FlixelPoolable;
import org.jetbrains.annotations.NotNull;

/**
 * A 2D vector with {@code x} and {@code y} components, used for positions,
 * offsets, velocities, and directions.
 *
 * <p>It carries the vector helpers gameplay code reaches for constantly:
 * {@link #distanceTo}, {@link #angleTo}, {@link #rotate}, {@link #length}, and
 * {@link #normalize}, plus per-axis and arithmetic convenience methods that all
 * return {@code this} for chaining.
 *
 * <h2>Pooling</h2>
 * Vectors are the classic per-frame allocation trap: a game
 * that creates a fresh vector for every distance check quickly floods the
 * garbage collector. To avoid that, borrow vectors from the shared pool with
 * {@link #get()} and return them with {@link #put()}:
 *
 * <pre>{@code
 * FlixelVector v = FlixelVector.get(playerX, playerY);
 * float dist = v.distanceTo(target);
 * v.put(); // Back to the pool, no garbage created.
 * }</pre>
 *
 * <h2>Weak vectors</h2>
 * For the common "make a throwaway vector, pass it to
 * one method, forget it" pattern, {@link #weak()} hands out a vector flagged for
 * auto-recycling. A method that receives one calls {@link #putWeak()} when it is
 * finished, and the vector returns itself to the pool, so callers never have to
 * remember to free it.
 *
 * <p>You can still {@code new} a vector directly when you want a plain, unpooled
 * instance (for example a long-lived field).
 *
 * <p>This class is not thread safe; the shared pool assumes single-threaded
 * (game-loop) use.
 */
public class FlixelVector implements FlixelPoolable {

  private static final FlixelPool<FlixelVector> POOL =
      new FlixelPool<>() {
        @Override
        protected @NotNull FlixelVector newObject() {
          return new FlixelVector();
        }
      };

  /** The horizontal component. */
  public float x;

  /** The vertical component. */
  public float y;

  private boolean weak;

  /**
   * Creates a vector at the origin {@code (0, 0)}.
   */
  public FlixelVector() {
    this(0f, 0f);
  }

  /**
   * Creates a vector with the given components.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   */
  public FlixelVector(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Creates a vector that copies another's components.
   *
   * @param other The vector to copy.
   */
  public FlixelVector(@NotNull FlixelVector other) {
    this.x = other.x;
    this.y = other.y;
    this.weak = other.weak;
  }

  /**
   * Borrows a vector from the shared pool, set to the origin.
   *
   * @return A recycled (or freshly created) vector at {@code (0, 0)}.
   */
  public static @NotNull FlixelVector get() {
    return get(0f, 0f);
  }

  /**
   * Borrows a vector from the shared pool, set to the given components.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   * @return A recycled (or freshly created) vector.
   */
  public static @NotNull FlixelVector get(float x, float y) {
    FlixelVector vector = POOL.obtain();
    vector.x = x;
    vector.y = y;
    vector.weak = false;
    return vector;
  }

  /**
   * Borrows a self-recycling vector set to the origin.
   *
   * <p>Pass the result straight into a method that accepts a weak vector; that
   * method returns it to the pool via {@link #putWeak()} when done.
   *
   * @return A weak vector at {@code (0, 0)}.
   */
  public static @NotNull FlixelVector weak() {
    return weak(0f, 0f);
  }

  /**
   * Borrows a self-recycling vector set to the given components.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   * @return A weak vector.
   */
  public static @NotNull FlixelVector weak(float x, float y) {
    FlixelVector vector = get(x, y);
    vector.weak = true;
    return vector;
  }

  /**
   * Sets both components at once.
   *
   * @param x The new horizontal component.
   * @param y The new vertical component.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  /**
   * Sets only the horizontal component.
   *
   * @param x The new horizontal component.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector setX(float x) {
    this.x = x;
    return this;
  }

  /**
   * Sets only the vertical component.
   *
   * @param y The new vertical component.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector setY(float y) {
    this.y = y;
    return this;
  }

  /**
   * Resets both components to zero.
   *
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector setZero() {
    this.x = 0f;
    this.y = 0f;
    return this;
  }

  /**
   * Copies the components of another vector into this one.
   *
   * @param other The vector to copy from.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector copyFrom(@NotNull FlixelVector other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  /**
   * Adds the given offsets to this vector.
   *
   * @param dx The amount to add to {@code x}.
   * @param dy The amount to add to {@code y}.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector add(float dx, float dy) {
    this.x += dx;
    this.y += dy;
    return this;
  }

  /**
   * Adds another vector to this one.
   *
   * @param other The vector to add.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector add(@NotNull FlixelVector other) {
    return add(other.x, other.y);
  }

  /**
   * Subtracts the given offsets from this vector.
   *
   * @param dx The amount to subtract from {@code x}.
   * @param dy The amount to subtract from {@code y}.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector subtract(float dx, float dy) {
    this.x -= dx;
    this.y -= dy;
    return this;
  }

  /**
   * Subtracts another vector from this one.
   *
   * @param other The vector to subtract.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector subtract(@NotNull FlixelVector other) {
    return subtract(other.x, other.y);
  }

  /**
   * Scales both components by a factor.
   *
   * @param factor The multiplier applied to {@code x} and {@code y}.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector scale(float factor) {
    this.x *= factor;
    this.y *= factor;
    return this;
  }

  /**
   * Scales each component by a separate factor.
   *
   * @param factorX The multiplier applied to {@code x}.
   * @param factorY The multiplier applied to {@code y}.
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector scale(float factorX, float factorY) {
    this.x *= factorX;
    this.y *= factorY;
    return this;
  }

  /**
   * Flips both components by multiplying each by -1.
   *
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector negate() {
    this.x = -x;
    this.y = -y;
    return this;
  }

  /**
   * Returns the length (magnitude) of this vector.
   *
   * @return The distance from the origin to this vector.
   */
  public float length() {
    return (float) Math.sqrt(x * x + y * y);
  }

  /**
   * Returns the squared length of this vector.
   *
   * <p>Prefer this over {@link #length()} when only comparing magnitudes, since
   * it skips the square root.
   *
   * @return The squared distance from the origin to this vector.
   */
  public float lengthSquared() {
    return x * x + y * y;
  }

  /**
   * Scales this vector to unit length, leaving a zero vector unchanged.
   *
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector normalize() {
    float len = length();
    if (len != 0f) {
      x /= len;
      y /= len;
    }
    return this;
  }

  /**
   * Returns the dot product of this vector and another.
   *
   * @param other The other vector.
   * @return The dot product {@code x*other.x + y*other.y}.
   */
  public float dot(@NotNull FlixelVector other) {
    return x * other.x + y * other.y;
  }

  /**
   * Returns the straight-line distance from this vector to another.
   *
   * @param other The other vector.
   * @return The distance between the two vectors.
   */
  public float distanceTo(@NotNull FlixelVector other) {
    float dx = other.x - x;
    float dy = other.y - y;
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Returns the angle from this vector to another, in degrees.
   *
   * <p>The angle is measured with 0 degrees pointing along positive {@code x}
   * and increasing toward positive {@code y}, in the range -180 to 180.
   *
   * @param other The vector to aim at.
   * @return The angle to {@code other}, in degrees.
   */
  public float angleTo(@NotNull FlixelVector other) {
    return (float) Math.atan2(other.y - y, other.x - x) * FlixelMath.RAD_TO_DEG;
  }

  /**
   * Rotates this vector around a pivot by the given angle.
   *
   * @param pivotX The pivot's horizontal component.
   * @param pivotY The pivot's vertical component.
   * @param degrees The rotation angle, in degrees (positive turns toward
   *     positive {@code y}).
   * @return This vector, for chaining.
   */
  public @NotNull FlixelVector rotate(float pivotX, float pivotY, float degrees) {
    float radians = degrees * FlixelMath.DEG_TO_RAD;
    float cos = FlixelMath.cos(radians);
    float sin = FlixelMath.sin(radians);
    float dx = x - pivotX;
    float dy = y - pivotY;
    this.x = pivotX + dx * cos - dy * sin;
    this.y = pivotY + dx * sin + dy * cos;
    return this;
  }

  /**
   * Reports whether both components are exactly zero.
   *
   * @return {@code true} if this is the zero vector.
   */
  public boolean isZero() {
    return x == 0f && y == 0f;
  }

  /**
   * Returns this vector to the shared pool for reuse.
   *
   * <p>Do not use the vector after calling this. Only call it on vectors
   * obtained from {@link #get()} or {@link #weak()}, not on plain {@code new}
   * instances.
   */
  public void put() {
    POOL.free(this);
  }

  /**
   * Returns this vector to the pool only if it is a weak (auto-recycling)
   * vector.
   *
   * <p>Methods that accept a caller-supplied vector call this when finished: a
   * weak vector is recycled, while a normal vector the caller still owns is left
   * untouched.
   */
  public void putWeak() {
    if (weak) {
      POOL.free(this);
    }
  }

  /**
   * Reports whether this vector is a weak (auto-recycling) vector.
   *
   * @return {@code true} if obtained via {@link #weak()}.
   */
  public boolean isWeak() {
    return weak;
  }

  @Override
  public void reset() {
    x = 0f;
    y = 0f;
    weak = false;
  }
}
