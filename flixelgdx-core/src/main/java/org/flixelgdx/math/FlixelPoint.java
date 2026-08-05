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
 * A 2D point (or vector) with {@code x} and {@code y} components.
 *
 * <p>This is the framework's own replacement for a libGDX {@code Vector2},
 * mirroring HaxeFlixel's {@code FlxPoint}. Beyond storing a coordinate it
 * carries the vector helpers gameplay code reaches for constantly:
 * {@link #distanceTo}, {@link #angleTo}, {@link #rotate}, and the usual
 * arithmetic.
 *
 * <p><b>Pooling.</b> Points are the classic per-frame allocation trap - a game
 * that news up a vector for every distance check quickly drowns the garbage
 * collector. To avoid that, borrow points from the shared pool with
 * {@link #get()} and return them with {@link #put()}:
 *
 * <pre>{@code
 * FlixelPoint p = FlixelPoint.get(playerX, playerY);
 * float dist = p.distanceTo(target);
 * p.put(); // back to the pool, no garbage created
 * }</pre>
 *
 * <p><b>Weak points.</b> For the common "make a throwaway point, pass it to one
 * method, forget it" pattern, {@link #weak()} hands out a point flagged for
 * auto-recycling. A method that receives one calls {@link #putWeak()} when it is
 * finished, and the point returns itself to the pool - so callers never have to
 * remember to free it.
 *
 * <p>You can still {@code new} a point directly when you want a plain, unpooled
 * instance (for example a long-lived field).
 *
 * <p>This class is not thread safe; the shared pool assumes single-threaded
 * (game-loop) use.
 */
public final class FlixelPoint implements FlixelPoolable {

  private static final FlixelPool<FlixelPoint> POOL =
      new FlixelPool<>() {
        @Override
        protected @NotNull FlixelPoint newObject() {
          return new FlixelPoint();
        }
      };

  /** The horizontal component. */
  public float x;

  /** The vertical component. */
  public float y;

  private boolean weak;

  /**
   * Creates a point at the origin {@code (0, 0)}.
   */
  public FlixelPoint() {
    this(0f, 0f);
  }

  /**
   * Creates a point at the given coordinates.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   */
  public FlixelPoint(float x, float y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Borrows a point from the shared pool, set to the origin.
   *
   * @return A recycled (or freshly created) point at {@code (0, 0)}.
   */
  public static @NotNull FlixelPoint get() {
    return get(0f, 0f);
  }

  /**
   * Borrows a point from the shared pool, set to the given coordinates.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   * @return A recycled (or freshly created) point.
   */
  public static @NotNull FlixelPoint get(float x, float y) {
    FlixelPoint point = POOL.obtain();
    point.x = x;
    point.y = y;
    point.weak = false;
    return point;
  }

  /**
   * Borrows a self-recycling point set to the origin.
   *
   * <p>Pass the result straight into a method that accepts a weak point; that
   * method returns it to the pool via {@link #putWeak()} when done.
   *
   * @return A weak point at {@code (0, 0)}.
   */
  public static @NotNull FlixelPoint weak() {
    return weak(0f, 0f);
  }

  /**
   * Borrows a self-recycling point set to the given coordinates.
   *
   * @param x The horizontal component.
   * @param y The vertical component.
   * @return A weak point.
   */
  public static @NotNull FlixelPoint weak(float x, float y) {
    FlixelPoint point = get(x, y);
    point.weak = true;
    return point;
  }

  /**
   * Sets both components at once.
   *
   * @param x The new horizontal component.
   * @param y The new vertical component.
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  /**
   * Copies the components of another point into this one.
   *
   * @param other The point to copy from.
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint copyFrom(@NotNull FlixelPoint other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  /**
   * Adds the given offsets to this point.
   *
   * @param dx The amount to add to {@code x}.
   * @param dy The amount to add to {@code y}.
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint add(float dx, float dy) {
    this.x += dx;
    this.y += dy;
    return this;
  }

  /**
   * Subtracts the given offsets from this point.
   *
   * @param dx The amount to subtract from {@code x}.
   * @param dy The amount to subtract from {@code y}.
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint subtract(float dx, float dy) {
    this.x -= dx;
    this.y -= dy;
    return this;
  }

  /**
   * Scales both components by a factor.
   *
   * @param factor The multiplier applied to {@code x} and {@code y}.
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint scale(float factor) {
    this.x *= factor;
    this.y *= factor;
    return this;
  }

  /**
   * Returns the straight-line distance from this point to another.
   *
   * @param other The other point.
   * @return The distance between the two points.
   */
  public float distanceTo(@NotNull FlixelPoint other) {
    float dx = other.x - x;
    float dy = other.y - y;
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Returns the angle from this point to another, in degrees.
   *
   * <p>The angle is measured with 0 degrees pointing along positive {@code x}
   * and increasing toward positive {@code y}, in the range -180 to 180.
   *
   * @param other The point to aim at.
   * @return The angle to {@code other}, in degrees.
   */
  public float angleTo(@NotNull FlixelPoint other) {
    return (float) Math.atan2(other.y - y, other.x - x) * FlixelMathUtil.RAD_TO_DEG;
  }

  /**
   * Rotates this point around a pivot by the given angle.
   *
   * @param pivotX The pivot's horizontal component.
   * @param pivotY The pivot's vertical component.
   * @param degrees The rotation angle, in degrees (positive turns toward
   *     positive {@code y}).
   * @return This point, for chaining.
   */
  public @NotNull FlixelPoint rotate(float pivotX, float pivotY, float degrees) {
    float radians = degrees * FlixelMathUtil.DEG_TO_RAD;
    float cos = FlixelMathUtil.cos(radians);
    float sin = FlixelMathUtil.sin(radians);
    float dx = x - pivotX;
    float dy = y - pivotY;
    this.x = pivotX + dx * cos - dy * sin;
    this.y = pivotY + dx * sin + dy * cos;
    return this;
  }

  /**
   * Returns this point to the shared pool for reuse.
   *
   * <p>Do not use the point after calling this. Only call it on points obtained
   * from {@link #get()} or {@link #weak()}, not on plain {@code new} instances.
   */
  public void put() {
    POOL.free(this);
  }

  /**
   * Returns this point to the pool only if it is a weak (auto-recycling) point.
   *
   * <p>Methods that accept a caller-supplied point call this when finished: a
   * weak point is recycled, while a normal point the caller still owns is left
   * untouched.
   */
  public void putWeak() {
    if (weak) {
      POOL.free(this);
    }
  }

  /**
   * Reports whether this point is a weak (auto-recycling) point.
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
