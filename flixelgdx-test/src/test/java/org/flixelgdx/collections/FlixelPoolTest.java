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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelPoolTest {

  private static final class Counter {
    int created;

    FlixelPool<Item> pool() {
      return new FlixelPool<>() {
        @Override
        protected Item newObject() {
          created++;
          return new Item();
        }
      };
    }
  }

  private static final class Item implements FlixelPoolable {
    int value = 42;
    boolean wasReset;

    @Override
    public void reset() {
      value = 0;
      wasReset = true;
    }
  }

  @Test
  void obtainCreatesWhenEmpty() {
    Counter counter = new Counter();
    FlixelPool<Item> pool = counter.pool();

    Item first = pool.obtain();
    Item second = pool.obtain();

    assertNotSame(first, second);
    assertEquals(2, counter.created);
  }

  @Test
  void freeRecyclesTheSameInstance() {
    Counter counter = new Counter();
    FlixelPool<Item> pool = counter.pool();

    Item obtained = pool.obtain();
    pool.free(obtained);
    Item reused = pool.obtain();

    assertSame(obtained, reused);
    // Only one object should ever have been created.
    assertEquals(1, counter.created);
  }

  @Test
  void freeCallsResetOnPoolable() {
    FlixelPool<Item> pool = new Counter().pool();

    Item item = pool.obtain();
    item.value = 99;
    pool.free(item);

    assertTrue(item.wasReset);
    assertEquals(0, item.value);
  }

  @Test
  void freeIgnoresNull() {
    FlixelPool<Item> pool = new Counter().pool();
    pool.free(null);
    assertEquals(0, pool.getFree());
  }

  @Test
  void retainedObjectsAreCappedByMax() {
    FlixelPool<Item> pool =
        new FlixelPool<>(1, 2) {
          @Override
          protected Item newObject() {
            return new Item();
          }
        };

    // Free three objects into a pool capped at two.
    pool.free(new Item());
    pool.free(new Item());
    pool.free(new Item());

    assertEquals(2, pool.getFree());
  }

  @Test
  void outstandingCountTracksBorrows() {
    FlixelPool<Item> pool = new Counter().pool();

    Item a = pool.obtain();
    Item b = pool.obtain();
    assertEquals(2, pool.getOutstandingCount());

    pool.free(a);
    assertEquals(1, pool.getOutstandingCount());
    pool.free(b);
    assertEquals(0, pool.getOutstandingCount());
  }

  @Test
  void peakTracksHighWaterMark() {
    FlixelPool<Item> pool = new Counter().pool();

    pool.free(new Item());
    pool.free(new Item());
    pool.free(new Item());
    assertEquals(3, pool.peak);

    pool.obtain();
    pool.obtain();
    // Peak stays at the high-water mark even after objects are handed back out.
    assertEquals(3, pool.peak);
  }

  @Test
  void clearEmptiesTheReserve() {
    FlixelPool<Item> pool = new Counter().pool();
    pool.free(new Item());
    pool.free(new Item());

    pool.clear();

    assertEquals(0, pool.getFree());
  }

  @Test
  void poolGrowsPastInitialCapacity() {
    Counter counter = new Counter();
    FlixelPool<Item> pool =
        new FlixelPool<>(2) {
          @Override
          protected Item newObject() {
            counter.created++;
            return new Item();
          }
        };

    // Free more than the initial capacity of 2 to force a backing-array grow.
    for (int i = 0; i < 10; i++) {
      pool.free(new Item());
    }

    assertEquals(10, pool.getFree());
    assertFalse(pool.getFree() < 10);
  }
}
