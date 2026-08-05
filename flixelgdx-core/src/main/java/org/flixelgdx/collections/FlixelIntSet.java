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
 * An unordered set of primitive {@code int} values.
 *
 * <p>This is the framework's own replacement for libGDX's {@code IntSet}. It
 * stores {@code int}s directly (no {@code Integer} boxing), making it ideal for
 * tracking sets of ids or handles - "have I already processed this id?" - with
 * no garbage.
 *
 * <p>As in {@link FlixelIntMap}, the value {@code 0} is tracked with a dedicated
 * flag because {@code 0} marks empty slots in the hash table. Iteration order is
 * undefined.
 *
 * <p>This class is not thread safe.
 */
public class FlixelIntSet {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int HASH_MULTIPLIER = 0x9E3779B1;

  private int[] keyTable;
  private int size;
  private int mask;
  private int threshold;
  private int shift;
  private boolean hasZeroKey;

  /**
   * Creates an empty set with the default initial capacity.
   */
  public FlixelIntSet() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an empty set sized to hold at least the given number of values
   * before it needs to grow.
   *
   * @param initialCapacity The expected value count.
   */
  public FlixelIntSet(int initialCapacity) {
    int cap = tableSizeFor(Math.max(1, initialCapacity));
    keyTable = new int[cap];
    mask = cap - 1;
    threshold = (int) (cap * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
  }

  /**
   * Adds a value if it is not already present.
   *
   * @param value The value to add.
   * @return {@code true} if the value was added, {@code false} if it was already
   *     in the set.
   */
  public boolean add(int value) {
    if (value == 0) {
      if (hasZeroKey) {
        return false;
      }
      hasZeroKey = true;
      size++;
      return true;
    }
    int i = locate(value);
    if (keyTable[i] != 0) {
      return false;
    }
    keyTable[i] = value;
    if (++size >= threshold) {
      resize(keyTable.length << 1);
    }
    return true;
  }

  /**
   * Reports whether a value is in the set.
   *
   * @param value The value to check.
   * @return {@code true} if present.
   */
  public boolean contains(int value) {
    if (value == 0) {
      return hasZeroKey;
    }
    return keyTable[locate(value)] != 0;
  }

  /**
   * Removes a value, if present.
   *
   * @param value The value to remove.
   * @return {@code true} if the value was in the set.
   */
  public boolean remove(int value) {
    if (value == 0) {
      if (!hasZeroKey) {
        return false;
      }
      hasZeroKey = false;
      size--;
      return true;
    }
    int i = locate(value);
    if (keyTable[i] == 0) {
      return false;
    }
    int next = (i + 1) & mask;
    while (keyTable[next] != 0) {
      int ideal = place(keyTable[next]);
      if (((next - ideal) & mask) >= ((next - i) & mask)) {
        keyTable[i] = keyTable[next];
        i = next;
      }
      next = (next + 1) & mask;
    }
    keyTable[i] = 0;
    size--;
    return true;
  }

  /**
   * Removes every value, leaving the set empty.
   */
  public void clear() {
    Arrays.fill(keyTable, 0);
    hasZeroKey = false;
    size = 0;
  }

  /**
   * Returns the number of values.
   *
   * @return The value count.
   */
  public int size() {
    return size;
  }

  /**
   * Reports whether the set has no values.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  private int place(int value) {
    return (value * HASH_MULTIPLIER) >>> shift;
  }

  private int locate(int value) {
    int i = place(value);
    while (true) {
      int existing = keyTable[i];
      if (existing == 0 || existing == value) {
        return i;
      }
      i = (i + 1) & mask;
    }
  }

  private void resize(int newSize) {
    int[] oldKeys = keyTable;
    keyTable = new int[newSize];
    mask = newSize - 1;
    threshold = (int) (newSize * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
    for (int key : oldKeys) {
      if (key != 0) {
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
}
