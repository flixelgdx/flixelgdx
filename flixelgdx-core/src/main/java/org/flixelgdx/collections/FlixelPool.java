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
package org.flixelgdx.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * A reusable pool of objects that recycles instances instead of allocating new
 * ones.
 *
 * <p>Object pools are the framework's main defense against garbage-collection
 * hitches: rather than {@code new}-ing short-lived objects every frame (bullets,
 * particles, tweens) and letting the collector clean them up, you borrow one
 * with {@link #obtain()} and hand it back with {@link #free(Object)} when done.
 * The pool keeps freed objects on a stack and reuses them, so a busy game
 * settles into zero allocations for pooled types.
 *
 * <p>Subclasses supply {@link #newObject()}, which the pool calls only when it
 * has no free object to hand out. If the pooled type implements
 * {@link FlixelPoolable}, its {@link FlixelPoolable#reset()} runs automatically
 * on {@link #free(Object)} so no stale state carries over.
 *
 * <p>In development builds the pool tracks live-borrow statistics
 * ({@link #getOutstandingCount()}, {@link #peak}) to help find leaks - objects
 * obtained but never freed. These counters are cheap and always on.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelPool<Bullet> bullets = new FlixelPool<>() {
 *   protected Bullet newObject() {
 *     return new Bullet();
 *   }
 * };
 *
 * Bullet b = bullets.obtain();
 * // ...use the bullet...
 * bullets.free(b); // reset() runs here if Bullet is FlixelPoolable
 * }</pre>
 *
 * <p>This class is not thread safe. Confine each pool to a single thread (the
 * game loop), which is the common case.
 *
 * @param <T> The type of object stored in this pool.
 */
public abstract class FlixelPool<T> {

  private static final int DEFAULT_INITIAL_CAPACITY = 16;

  /**
   * The largest number of free objects this pool will retain. Objects freed
   * beyond this cap are discarded (left for the garbage collector) rather than
   * stored.
   */
  public final int max;

  /**
   * The highest number of free objects this pool has ever held at once. Useful
   * as a rough sizing hint when tuning pools.
   */
  public int peak;

  private Object[] freeObjects;
  private int free;
  private int outstanding;

  /**
   * Creates a pool with a default initial capacity and no upper bound on
   * retained objects.
   */
  public FlixelPool() {
    this(DEFAULT_INITIAL_CAPACITY, Integer.MAX_VALUE);
  }

  /**
   * Creates a pool with the given initial capacity and no upper bound on
   * retained objects.
   *
   * @param initialCapacity The starting size of the internal free-object
   *     storage.
   */
  public FlixelPool(int initialCapacity) {
    this(initialCapacity, Integer.MAX_VALUE);
  }

  /**
   * Creates a pool with the given initial capacity and retention cap.
   *
   * @param initialCapacity The starting size of the internal free-object
   *     storage.
   * @param max The largest number of free objects to retain; objects freed
   *     beyond this are discarded.
   */
  public FlixelPool(int initialCapacity, int max) {
    this.freeObjects = new Object[Math.max(1, initialCapacity)];
    this.max = max;
  }

  /**
   * Creates a fresh object for the pool.
   *
   * <p>The pool calls this only when {@link #obtain()} is asked for an object
   * and none are free. Implementations must return a ready-to-use instance.
   *
   * @return A newly created object.
   */
  protected abstract @NotNull T newObject();

  /**
   * Borrows an object from the pool, creating one only if none are free.
   *
   * @return A recycled object if one is available, otherwise a fresh one from
   *     {@link #newObject()}.
   */
  @SuppressWarnings("unchecked")
  public @NotNull T obtain() {
    outstanding++;
    if (free == 0) {
      return newObject();
    }
    free--;
    T object = (T) freeObjects[free];
    freeObjects[free] = null;
    return object;
  }

  /**
   * Returns an object to the pool for reuse.
   *
   * <p>If the object implements {@link FlixelPoolable}, its
   * {@link FlixelPoolable#reset()} runs before it is stored. Passing
   * {@code null} is ignored. Do not keep using an object after freeing it, and
   * do not free the same object twice.
   *
   * @param object The object to recycle, or {@code null} to ignore.
   */
  public void free(@Nullable T object) {
    if (object == null) {
      return;
    }
    outstanding--;
    reset(object);
    if (free >= max) {
      // At capacity: drop the object so the pool does not grow without bound.
      return;
    }
    if (free == freeObjects.length) {
      freeObjects = Arrays.copyOf(freeObjects, free << 1);
    }
    freeObjects[free] = object;
    free++;
    if (free > peak) {
      peak = free;
    }
  }

  /**
   * Discards every free object the pool is holding.
   *
   * <p>This does not touch objects currently on loan; it only empties the pool's
   * reserve so those instances can be garbage collected.
   */
  public void clear() {
    Arrays.fill(freeObjects, 0, free, null);
    free = 0;
  }

  /**
   * Resets a freed object before it is stored.
   *
   * <p>The default implementation calls {@link FlixelPoolable#reset()} when the
   * object is a {@link FlixelPoolable}. Subclasses may override to reset types
   * that do not implement the interface.
   *
   * @param object The object being returned to the pool.
   */
  protected void reset(@NotNull T object) {
    if (object instanceof FlixelPoolable poolable) {
      poolable.reset();
    }
  }

  /**
   * Returns the number of objects currently available to hand out.
   *
   * @return The count of free objects held by the pool.
   */
  public int getFree() {
    return free;
  }

  /**
   * Returns how many objects are currently on loan (obtained but not yet freed).
   *
   * <p>A steadily rising value across frames is the classic sign of a pool leak.
   * The counter can go negative if an object is freed more times than it was
   * obtained, which is itself a bug worth catching.
   *
   * @return The number of outstanding borrows.
   */
  public int getOutstandingCount() {
    return outstanding;
  }
}
