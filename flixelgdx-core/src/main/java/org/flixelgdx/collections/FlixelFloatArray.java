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

import java.util.Arrays;

/**
 * A growable list of primitive {@code float} values.
 *
 * <p>Like {@link FlixelIntArray}, it stores primitives directly to avoid boxing,
 * which matters for the vertex, timing, and geometry buffers rendering code
 * fills every frame. The backing array {@link #items} and count {@link #size}
 * are public for zero-allocation indexed iteration.
 *
 * <p>This class is not thread safe.
 */
public class FlixelFloatArray {

  private static final int DEFAULT_CAPACITY = 16;

  /**
   * The backing array. Only the first {@link #size} entries are live. Safe to
   * read by index in hot loops.
   */
  public float[] items;

  /** The number of live values. */
  public int size;

  /** Whether removals preserve order (see {@link FlixelArray#ordered}). */
  public boolean ordered;

  /**
   * Creates an ordered list with the default initial capacity.
   */
  public FlixelFloatArray() {
    this(true, DEFAULT_CAPACITY);
  }

  /**
   * Creates an ordered list with the given initial capacity.
   *
   * @param capacity The initial backing-array size.
   */
  public FlixelFloatArray(int capacity) {
    this(true, capacity);
  }

  /**
   * Creates a list with the given ordering and initial capacity.
   *
   * @param ordered Whether removals preserve order.
   * @param capacity The initial backing-array size.
   */
  public FlixelFloatArray(boolean ordered, int capacity) {
    this.ordered = ordered;
    this.items = new float[Math.max(1, capacity)];
  }

  /**
   * Appends a value to the end of the list.
   *
   * @param value The value to add.
   */
  public void add(float value) {
    if (size == items.length) {
      grow(size + 1);
    }
    items[size++] = value;
  }

  /**
   * Returns the value at the given index.
   *
   * @param index The position to read, from 0 to {@code size - 1}.
   * @return The value at {@code index}.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public float get(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    return items[index];
  }

  /**
   * Replaces the value at the given index.
   *
   * @param index The position to write, from 0 to {@code size - 1}.
   * @param value The new value.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public void set(int index, float value) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    items[index] = value;
  }

  /**
   * Removes and returns the value at the given index.
   *
   * @param index The position to remove, from 0 to {@code size - 1}.
   * @return The value that was removed.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public float removeIndex(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    float removed = items[index];
    size--;
    if (ordered) {
      System.arraycopy(items, index + 1, items, index, size - index);
    } else {
      items[index] = items[size];
    }
    return removed;
  }

  /**
   * Removes and returns the last value (a stack pop).
   *
   * @return The former last value.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public float pop() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[--size];
  }

  /**
   * Returns the first value.
   *
   * @return The value at index 0.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public float first() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[0];
  }

  /**
   * Returns the last value without removing it.
   *
   * @return The value at index {@code size - 1}.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public float peek() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[size - 1];
  }

  /**
   * Reports whether the list has no values.
   *
   * @return {@code true} if {@link #size} is 0.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Removes every value.
   */
  public void clear() {
    size = 0;
  }

  /**
   * Returns a trimmed copy of the live values.
   *
   * @return A new array of length {@link #size}.
   */
  public float[] toArray() {
    return Arrays.copyOf(items, size);
  }

  private void grow(int minCapacity) {
    int newCapacity = Math.max(minCapacity, items.length + (items.length >> 1) + 1);
    items = Arrays.copyOf(items, newCapacity);
  }
}
