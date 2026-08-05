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
 * An unordered map from primitive {@code int} keys to primitive
 * {@code float} values.
 *
 * <p>Both the keys and the values are stored without boxing, so lookups and
 * updates create no garbage. Because the values are primitive, reads take a
 * fallback to return when a key is absent (there is no {@code null}).
 *
 * <p>The key {@code 0} is stored in a dedicated slot rather than the hash
 * table, because it doubles as the "empty" marker. Iteration order is undefined.
 *
 * <p>This class is not thread safe.
 */
public class FlixelIntFloatMap {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int HASH_MULTIPLIER = 0x9E3779B1;

  private int[] keyTable;
  private float[] valueTable;
  private float zeroValue;
  private int size;
  private int mask;
  private int threshold;
  private int shift;
  private boolean hasZeroKey;

  private Entries entries;

  /**
   * Creates an empty map with the default initial capacity.
   */
  public FlixelIntFloatMap() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an empty map sized to hold at least the given number of entries
   * before it needs to grow.
   *
   * @param initialCapacity The expected entry count.
   */
  public FlixelIntFloatMap(int initialCapacity) {
    int cap = tableSizeFor(Math.max(1, initialCapacity));
    keyTable = new int[cap];
    valueTable = new float[cap];
    mask = cap - 1;
    threshold = (int) (cap * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
  }

  /**
   * Associates a value with a key, replacing any previous value.
   *
   * @param key The key.
   * @param value The value to store.
   */
  public void put(int key, float value) {
    if (key == 0) {
      if (!hasZeroKey) {
        hasZeroKey = true;
        size++;
      }
      zeroValue = value;
      return;
    }
    int i = locate(key);
    if (keyTable[i] != 0) {
      valueTable[i] = value;
      return;
    }
    keyTable[i] = key;
    valueTable[i] = value;
    if (++size >= threshold) {
      resize(keyTable.length << 1);
    }
  }

  /**
   * Returns the value stored under a key, or a fallback if the key is absent.
   *
   * @param key The key to look up.
   * @param defaultValue The value to return when the key is not present.
   * @return The associated value, or the fallback if the key is absent.
   */
  public float get(int key, float defaultValue) {
    if (key == 0) {
      return hasZeroKey ? zeroValue : defaultValue;
    }
    int i = locate(key);
    return keyTable[i] == 0 ? defaultValue : valueTable[i];
  }

  /**
   * Adds an amount to the value stored under a key, treating an absent key as a
   * starting value of zero.
   *
   * @param key The key.
   * @param amount The amount to add.
   * @return The new value stored under the key.
   */
  public float increment(int key, float amount) {
    float current = get(key, (float) 0);
    float updated = (float) (current + amount);
    put(key, updated);
    return updated;
  }

  /**
   * Reports whether a key is present.
   *
   * @param key The key to check.
   * @return {@code true} if the key has an entry.
   */
  public boolean containsKey(int key) {
    if (key == 0) {
      return hasZeroKey;
    }
    return keyTable[locate(key)] != 0;
  }

  /**
   * Removes the entry for a key, if present.
   *
   * @param key The key to remove.
   * @param defaultValue The value to return when the key is absent.
   * @return The value that was removed, or the fallback if the key was absent.
   */
  public float remove(int key, float defaultValue) {
    if (key == 0) {
      if (!hasZeroKey) {
        return defaultValue;
      }
      hasZeroKey = false;
      float old = zeroValue;
      size--;
      return old;
    }
    int i = locate(key);
    if (keyTable[i] == 0) {
      return defaultValue;
    }
    float oldValue = valueTable[i];
    int next = (i + 1) & mask;
    while (keyTable[next] != 0) {
      int ideal = place(keyTable[next]);
      if (((next - ideal) & mask) >= ((next - i) & mask)) {
        keyTable[i] = keyTable[next];
        valueTable[i] = valueTable[next];
        i = next;
      }
      next = (next + 1) & mask;
    }
    keyTable[i] = 0;
    size--;
    return oldValue;
  }

  /**
   * Removes every entry, leaving the map empty.
   */
  public void clear() {
    Arrays.fill(keyTable, (int) 0);
    hasZeroKey = false;
    size = 0;
  }

  /**
   * Returns the number of entries.
   *
   * @return The entry count.
   */
  public int size() {
    return size;
  }

  /**
   * Reports whether the map has no entries.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns a reusable iterable over the map's entries.
   *
   * @return An iterable whose {@link Entry} is reused each step.
   */
  public @NotNull Entries entries() {
    if (entries == null) {
      entries = new Entries(this);
    }
    entries.reset();
    return entries;
  }

  private int place(int key) {
    return (key * HASH_MULTIPLIER) >>> shift;
  }

  private int locate(int key) {
    int i = place(key);
    while (true) {
      int existing = keyTable[i];
      if (existing == 0 || existing == key) {
        return i;
      }
      i = (i + 1) & mask;
    }
  }

  private void resize(int newSize) {
    int[] oldKeys = keyTable;
    float[] oldValues = valueTable;
    keyTable = new int[newSize];
    valueTable = new float[newSize];
    mask = newSize - 1;
    threshold = (int) (newSize * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
    for (int i = 0; i < oldKeys.length; i++) {
      int key = oldKeys[i];
      if (key != 0) {
        int j = locate(key);
        keyTable[j] = key;
        valueTable[j] = oldValues[i];
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

  /**
   * A single key-value pair yielded by {@link #entries()}.
   *
   * <p>The same instance is reused across iteration steps.
   */
  public static final class Entry {

    /** The entry's key. */
    public int key;

    /** The entry's value. */
    public float value;
  }

  /**
   * A reusable iterable over a map's entries.
   */
  public static final class Entries implements Iterable<Entry>, Iterator<Entry> {

    private final FlixelIntFloatMap map;
    private final Entry entry = new Entry();
    private int index;
    private boolean zeroPending;
    private boolean hasNext;

    Entries(FlixelIntFloatMap map) {
      this.map = map;
    }

    void reset() {
      index = -1;
      zeroPending = map.hasZeroKey;
      advance();
    }

    void advance() {
      if (zeroPending) {
        hasNext = true;
        return;
      }
      hasNext = false;
      int[] keyTable = map.keyTable;
      for (index++; index < keyTable.length; index++) {
        if (keyTable[index] != 0) {
          hasNext = true;
          break;
        }
      }
    }

    @Override
    public @NotNull Iterator<Entry> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public Entry next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      if (zeroPending) {
        zeroPending = false;
        entry.key = 0;
        entry.value = map.zeroValue;
        advance();
        return entry;
      }
      entry.key = map.keyTable[index];
      entry.value = map.valueTable[index];
      advance();
      return entry;
    }
  }
}
