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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unordered set of unique elements, backed by an open-addressing hash table.
 *
 * <p>This is the framework's own replacement for libGDX's {@code ObjectSet}. It
 * shares the compact, cache-friendly linear-probing design of {@link FlixelMap}
 * but stores only membership - use it to answer "have I already seen this?"
 * without the overhead of values.
 *
 * <p>Elements must not be {@code null}. Iteration order is undefined and may
 * change as the set grows. Like the map, {@link #iterator()} is reused between
 * loops to avoid allocation, so do not run two loops over the same set at once.
 *
 * <p>This class is not thread safe.
 *
 * @param <T> The element type.
 */
public class FlixelSet<T> implements Iterable<T> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int HASH_MULTIPLIER = 0x9E3779B1;

  private T[] keyTable;
  private int size;
  private int mask;
  private int threshold;
  private int shift;

  private SetIterator<T> iterator;

  /**
   * Creates an empty set with the default initial capacity.
   */
  public FlixelSet() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an empty set sized to hold at least the given number of elements
   * before it needs to grow.
   *
   * @param initialCapacity The expected element count.
   */
  @SuppressWarnings("unchecked")
  public FlixelSet(int initialCapacity) {
    int cap = tableSizeFor(Math.max(1, initialCapacity));
    keyTable = (T[]) new Object[cap];
    mask = cap - 1;
    threshold = (int) (cap * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
  }

  /**
   * Adds an element if it is not already present.
   *
   * @param value The element to add; must not be {@code null}.
   * @return {@code true} if the element was added, {@code false} if it was
   *     already in the set.
   * @throws IllegalArgumentException If {@code value} is {@code null}.
   */
  public boolean add(@NotNull T value) {
    if (value == null) {
      throw new IllegalArgumentException("value cannot be null");
    }
    int i = locate(value);
    if (keyTable[i] != null) {
      return false;
    }
    keyTable[i] = value;
    if (++size >= threshold) {
      resize(keyTable.length << 1);
    }
    return true;
  }

  /**
   * Reports whether an element is in the set.
   *
   * @param value The element to check.
   * @return {@code true} if present.
   */
  public boolean contains(@NotNull T value) {
    return keyTable[locate(value)] != null;
  }

  /**
   * Removes an element, if present.
   *
   * @param value The element to remove.
   * @return {@code true} if the element was in the set.
   */
  public boolean remove(@NotNull T value) {
    int i = locate(value);
    if (keyTable[i] == null) {
      return false;
    }
    // Backward-shift deletion keeps the probe chains intact (no tombstones).
    int next = (i + 1) & mask;
    while (keyTable[next] != null) {
      int ideal = place(keyTable[next]);
      if (((next - ideal) & mask) >= ((next - i) & mask)) {
        keyTable[i] = keyTable[next];
        i = next;
      }
      next = (next + 1) & mask;
    }
    keyTable[i] = null;
    size--;
    return true;
  }

  /**
   * Removes every element, leaving the set empty.
   */
  public void clear() {
    Arrays.fill(keyTable, null);
    size = 0;
  }

  /**
   * Returns the number of elements.
   *
   * @return The element count.
   */
  public int size() {
    return size;
  }

  /**
   * Reports whether the set has no elements.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns a reusable iterator over the elements.
   *
   * <p>The iterator is reused between loops, so do not run two loops over the
   * same set at the same time.
   *
   * @return An iterator over the elements.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    if (iterator == null) {
      iterator = new SetIterator<>(this);
    }
    iterator.reset();
    return iterator;
  }

  private int place(T value) {
    return (value.hashCode() * HASH_MULTIPLIER) >>> shift;
  }

  private int locate(T value) {
    int i = place(value);
    while (true) {
      T existing = keyTable[i];
      if (existing == null || existing.equals(value)) {
        return i;
      }
      i = (i + 1) & mask;
    }
  }

  @SuppressWarnings("unchecked")
  private void resize(int newSize) {
    T[] oldKeys = keyTable;
    keyTable = (T[]) new Object[newSize];
    mask = newSize - 1;
    threshold = (int) (newSize * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
    for (T key : oldKeys) {
      if (key != null) {
        keyTable[locate(key)] = key;
      }
    }
  }

  private static int tableSizeFor(int capacity) {
    int needed = (int) (capacity / LOAD_FACTOR) + 1;
    int size = 2;
    while (size < needed) {
      size <<= 1;
    }
    return size;
  }

  private static final class SetIterator<T> implements Iterator<T> {
    private final FlixelSet<T> set;
    private int nextIndex;
    private int currentIndex;
    private boolean hasNext;

    SetIterator(FlixelSet<T> set) {
      this.set = set;
    }

    void reset() {
      nextIndex = -1;
      currentIndex = -1;
      advance();
    }

    void advance() {
      hasNext = false;
      T[] keyTable = set.keyTable;
      for (nextIndex++; nextIndex < keyTable.length; nextIndex++) {
        if (keyTable[nextIndex] != null) {
          hasNext = true;
          break;
        }
      }
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public T next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      currentIndex = nextIndex;
      T value = set.keyTable[nextIndex];
      advance();
      return value;
    }

    @Override
    public void remove() {
      int i = currentIndex;
      if (i < 0) {
        throw new IllegalStateException("next() must be called before remove().");
      }
      T[] keyTable = set.keyTable;
      int mask = set.mask;
      int next = (i + 1) & mask;
      T key;
      while ((key = keyTable[next]) != null) {
        int ideal = set.place(key);
        if (((next - ideal) & mask) >= ((next - i) & mask)) {
          keyTable[i] = key;
          i = next;
        }
        next = (next + 1) & mask;
      }
      keyTable[i] = null;
      set.size--;
      // A later element may have been pulled into the vacated slot; rescan.
      nextIndex = currentIndex - 1;
      currentIndex = -1;
      advance();
    }
  }
}
