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

/**
 * Marks an object that can be reused by a {@link FlixelPool}.
 *
 * <p>When a pooled object is handed back with {@link FlixelPool#free(Object)},
 * the pool calls {@link #reset()} so the object can clear its state and be ready
 * for the next caller. Implement this to avoid stale data leaking from one use
 * of an object into the next.
 *
 * <p>Example:
 *
 * <pre>{@code
 * public class Bullet implements FlixelPoolable {
 *
 *   public float x, y, speed;
 *   public boolean alive;
 *
 *   // Called automatically when the bullet is returned to its pool.
 *   @Override
 *   public void reset() {
 *     x = 0f;
 *     y = 0f;
 *     speed = 0f;
 *   }
 * }
 * }</pre>
 */
public interface FlixelPoolable {

  /**
   * Clears this object's state so it is safe to reuse.
   *
   * <p>Called by {@link FlixelPool#free(Object)} the moment the object returns
   * to the pool. Reset every field the object owns; do not assume the object
   * will be garbage collected.
   */
  void reset();
}
