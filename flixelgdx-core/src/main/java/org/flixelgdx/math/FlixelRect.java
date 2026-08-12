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
 * An axis-aligned rectangle defined by a top-left corner and a size.
 *
 * <p>It is the workhorse for hit testing, camera dead zones, and culling, so it
 * ships with {@link #contains(float, float)}, {@link #overlaps(FlixelRect)},
 * {@link #union(FlixelRect)}, and {@link #intersection(FlixelRect)}.
 *
 * <p>Like {@link FlixelVector}, rectangles are poolable to avoid per-frame
 * allocations: borrow one with {@link #get()} and return it with {@link #put()},
 * or use {@link #weak()} for the throwaway "pass it once and forget it" pattern
 * (the receiving method calls {@link #putWeak()}). You may also {@code new} a
 * rectangle directly for a plain, unpooled instance.
 *
 * <p>This class is not thread safe; the shared pool assumes single-threaded
 * (game-loop) use.
 */
public final class FlixelRect implements FlixelPoolable {

  private static final FlixelPool<FlixelRect> POOL =
      new FlixelPool<>() {
        @Override
        protected @NotNull FlixelRect newObject() {
          return new FlixelRect();
        }
      };

  /** The left edge (x coordinate of the top-left corner). */
  public float x;

  /** The top edge (y coordinate of the top-left corner). */
  public float y;

  /** The width; expected to be non-negative. */
  public float width;

  /** The height; expected to be non-negative. */
  public float height;

  private boolean weak;

  /**
   * Creates an empty rectangle at the origin with zero size.
   */
  public FlixelRect() {
    this(0f, 0f, 0f, 0f);
  }

  /**
   * Creates a rectangle with the given position and size.
   *
   * @param x The left edge.
   * @param y The top edge.
   * @param width The width.
   * @param height The height.
   */
  public FlixelRect(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  /**
   * Creates a rectangle that copies another's position and size.
   *
   * @param other The rectangle to copy.
   */
  public FlixelRect(@NotNull FlixelRect other) {
    this(other.x, other.y, other.width, other.height);
    weak = other.weak;
  }

  /**
   * Borrows a rectangle from the shared pool, set to empty at the origin.
   *
   * @return A recycled (or freshly created) rectangle.
   */
  public static @NotNull FlixelRect get() {
    return get(0f, 0f, 0f, 0f);
  }

  /**
   * Borrows a rectangle from the shared pool with the given position and size.
   *
   * @param x The left edge.
   * @param y The top edge.
   * @param width The width.
   * @param height The height.
   * @return A recycled (or freshly created) rectangle.
   */
  public static @NotNull FlixelRect get(float x, float y, float width, float height) {
    FlixelRect rect = POOL.obtain();
    rect.x = x;
    rect.y = y;
    rect.width = width;
    rect.height = height;
    rect.weak = false;
    return rect;
  }

  /**
   * Borrows a self-recycling rectangle set to empty at the origin.
   *
   * @return A weak rectangle.
   */
  public static @NotNull FlixelRect weak() {
    return weak(0f, 0f, 0f, 0f);
  }

  /**
   * Borrows a self-recycling rectangle with the given position and size.
   *
   * @param x The left edge.
   * @param y The top edge.
   * @param width The width.
   * @param height The height.
   * @return A weak rectangle.
   */
  public static @NotNull FlixelRect weak(float x, float y, float width, float height) {
    FlixelRect rect = get(x, y, width, height);
    rect.weak = true;
    return rect;
  }

  /**
   * Sets the position and size at once.
   *
   * @param x The new left edge.
   * @param y The new top edge.
   * @param width The new width.
   * @param height The new height.
   * @return This rectangle, for chaining.
   */
  public @NotNull FlixelRect set(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    return this;
  }

  /**
   * Copies another rectangle's position and size into this one.
   *
   * @param other The rectangle to copy from.
   * @return This rectangle, for chaining.
   */
  public @NotNull FlixelRect copyFrom(@NotNull FlixelRect other) {
    this.x = other.x;
    this.y = other.y;
    this.width = other.width;
    this.height = other.height;
    return this;
  }

  /**
   * Reports whether a point lies inside this rectangle.
   *
   * <p>The left and top edges are inclusive; the right and bottom edges are
   * exclusive, so adjacent rectangles do not both claim a shared border pixel.
   *
   * @param px The point's horizontal component.
   * @param py The point's vertical component.
   * @return {@code true} if the point is inside.
   */
  public boolean contains(float px, float py) {
    return px >= x && px < x + width && py >= y && py < y + height;
  }

  /**
   * Reports whether a point lies inside this rectangle.
   *
   * @param point The point to test.
   * @return {@code true} if the point is inside.
   */
  public boolean contains(@NotNull FlixelVector point) {
    return contains(point.x, point.y);
  }

  /**
   * Reports whether this rectangle overlaps another.
   *
   * @param other The rectangle to test against.
   * @return {@code true} if the two rectangles share any area.
   */
  public boolean overlaps(@NotNull FlixelRect other) {
    return x < other.x + other.width
        && x + width > other.x
        && y < other.y + other.height
        && y + height > other.y;
  }

  /**
   * Expands this rectangle in place so it also encloses another.
   *
   * @param other The rectangle to include.
   * @return This rectangle, now covering both, for chaining.
   */
  public @NotNull FlixelRect union(@NotNull FlixelRect other) {
    float minX = Math.min(x, other.x);
    float minY = Math.min(y, other.y);
    float maxX = Math.max(x + width, other.x + other.width);
    float maxY = Math.max(y + height, other.y + other.height);
    this.x = minX;
    this.y = minY;
    this.width = maxX - minX;
    this.height = maxY - minY;
    return this;
  }

  /**
   * Shrinks this rectangle in place to the area it shares with another.
   *
   * <p>If the two rectangles do not overlap, this rectangle is set to empty
   * (zero width and height) and the method reports {@code false}.
   *
   * @param other The rectangle to intersect with.
   * @return {@code true} if the rectangles overlapped, {@code false} otherwise.
   */
  public boolean intersection(@NotNull FlixelRect other) {
    float minX = Math.max(x, other.x);
    float minY = Math.max(y, other.y);
    float maxX = Math.min(x + width, other.x + other.width);
    float maxY = Math.min(y + height, other.y + other.height);
    if (minX >= maxX || minY >= maxY) {
      set(0f, 0f, 0f, 0f);
      return false;
    }
    set(minX, minY, maxX - minX, maxY - minY);
    return true;
  }

  /**
   * Reports whether this rectangle has no area.
   *
   * @return {@code true} if either the width or height is zero or negative.
   */
  public boolean isEmpty() {
    return width <= 0f || height <= 0f;
  }

  /**
   * Returns this rectangle to the shared pool for reuse.
   *
   * <p>Do not use the rectangle after calling this, and only call it on
   * rectangles obtained from {@link #get()} or {@link #weak()}.
   */
  public void put() {
    POOL.free(this);
  }

  /**
   * Returns this rectangle to the pool only if it is a weak (auto-recycling)
   * rectangle.
   */
  public void putWeak() {
    if (weak) {
      POOL.free(this);
    }
  }

  /**
   * Returns the x coordinate of the right edge ({@code x + width}).
   *
   * @return The right edge.
   */
  public float getRight() {
    return x + width;
  }

  /**
   * Returns the y coordinate of the bottom edge ({@code y + height}).
   *
   * @return The bottom edge.
   */
  public float getBottom() {
    return y + height;
  }

  /**
   * Reports whether this rectangle is a weak (auto-recycling) rectangle.
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
    width = 0f;
    height = 0f;
    weak = false;
  }
}
