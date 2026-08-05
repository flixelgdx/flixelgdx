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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unordered map that compares keys by identity ({@code ==}) rather than
 * {@link Object#equals(Object)}.
 *
 * <p>It behaves like {@link FlixelMap}, except two keys are considered the same
 * only when they are the exact same object. That is what you want when the key
 * is a live object instance (a sprite, a node) and you must not conflate two
 * distinct objects that happen to be {@code equals}. Keys are hashed with
 * {@link System#identityHashCode(Object)}.
 *
 * <p>Keys must not be {@code null}; values may be. Iteration order is undefined.
 *
 * <p>This class is not thread safe.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FlixelIdentityMap<K, V> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;
  private static final int HASH_MULTIPLIER = 0x9E3779B1;

  private K[] keyTable;
  private V[] valueTable;
  private int size;
  private int mask;
  private int threshold;
  private int shift;

  private Entries<K, V> entries;

  /**
   * Creates an empty map with the default initial capacity.
   */
  public FlixelIdentityMap() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an empty map sized to hold at least the given number of entries
   * before it needs to grow.
   *
   * @param initialCapacity The expected entry count.
   */
  @SuppressWarnings("unchecked")
  public FlixelIdentityMap(int initialCapacity) {
    int cap = tableSizeFor(Math.max(1, initialCapacity));
    keyTable = (K[]) new Object[cap];
    valueTable = (V[]) new Object[cap];
    mask = cap - 1;
    threshold = (int) (cap * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
  }

  /**
   * Associates a value with a key, replacing any previous value for the same
   * object.
   *
   * @param key The key; must not be {@code null}.
   * @param value The value to store (may be {@code null}).
   * @return The value previously stored under {@code key}, or {@code null} if
   *     there was none.
   * @throws IllegalArgumentException If {@code key} is {@code null}.
   */
  public @Nullable V put(@NotNull K key, @Nullable V value) {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }
    int i = locate(key);
    if (keyTable[i] != null) {
      V old = valueTable[i];
      valueTable[i] = value;
      return old;
    }
    keyTable[i] = key;
    valueTable[i] = value;
    if (++size >= threshold) {
      resize(keyTable.length << 1);
    }
    return null;
  }

  /**
   * Returns the value stored under a key.
   *
   * @param key The key to look up.
   * @return The associated value, or {@code null} if the exact key object is
   *     absent.
   */
  public @Nullable V get(@NotNull K key) {
    int i = locate(key);
    return keyTable[i] == null ? null : valueTable[i];
  }

  /**
   * Returns the value stored under a key, or a fallback if the key is absent.
   *
   * @param key The key to look up.
   * @param defaultValue The value to return when the key is not present.
   * @return The associated value, or {@code defaultValue} if the key is absent.
   */
  public @Nullable V getOrDefault(@NotNull K key, @Nullable V defaultValue) {
    int i = locate(key);
    return keyTable[i] == null ? defaultValue : valueTable[i];
  }

  /**
   * Reports whether the exact key object is present.
   *
   * @param key The key to check.
   * @return {@code true} if the key has an entry.
   */
  public boolean containsKey(@NotNull K key) {
    return keyTable[locate(key)] != null;
  }

  /**
   * Removes the entry for the exact key object, if present.
   *
   * @param key The key to remove.
   * @return The value that was removed, or {@code null} if the key was absent.
   */
  public @Nullable V remove(@NotNull K key) {
    int i = locate(key);
    if (keyTable[i] == null) {
      return null;
    }
    V oldValue = valueTable[i];
    int next = (i + 1) & mask;
    while (keyTable[next] != null) {
      int ideal = place(keyTable[next]);
      if (((next - ideal) & mask) >= ((next - i) & mask)) {
        keyTable[i] = keyTable[next];
        valueTable[i] = valueTable[next];
        i = next;
      }
      next = (next + 1) & mask;
    }
    keyTable[i] = null;
    valueTable[i] = null;
    size--;
    return oldValue;
  }

  /**
   * Removes every entry, leaving the map empty.
   */
  public void clear() {
    Arrays.fill(keyTable, null);
    Arrays.fill(valueTable, null);
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
  public @NotNull Entries<K, V> entries() {
    if (entries == null) {
      entries = new Entries<>(this);
    }
    entries.reset();
    return entries;
  }

  private int place(K key) {
    return (System.identityHashCode(key) * HASH_MULTIPLIER) >>> shift;
  }

  private int locate(K key) {
    int i = place(key);
    while (true) {
      K existing = keyTable[i];
      if (existing == null || existing == key) {
        return i;
      }
      i = (i + 1) & mask;
    }
  }

  @SuppressWarnings("unchecked")
  private void resize(int newSize) {
    K[] oldKeys = keyTable;
    V[] oldValues = valueTable;
    keyTable = (K[]) new Object[newSize];
    valueTable = (V[]) new Object[newSize];
    mask = newSize - 1;
    threshold = (int) (newSize * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
    for (int i = 0; i < oldKeys.length; i++) {
      K key = oldKeys[i];
      if (key != null) {
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
   *
   * @param <K> The key type.
   * @param <V> The value type.
   */
  public static final class Entry<K, V> {

    /** The entry's key. */
    public K key;

    /** The entry's value. */
    public V value;
  }

  /**
   * A reusable iterable over an identity map's entries.
   *
   * @param <K> The key type.
   * @param <V> The value type.
   */
  public static final class Entries<K, V> implements Iterable<Entry<K, V>>, Iterator<Entry<K, V>> {

    private final FlixelIdentityMap<K, V> map;
    private final Entry<K, V> entry = new Entry<>();
    private int index;
    private boolean hasNext;

    Entries(FlixelIdentityMap<K, V> map) {
      this.map = map;
    }

    void reset() {
      index = -1;
      advance();
    }

    void advance() {
      hasNext = false;
      K[] keyTable = map.keyTable;
      for (index++; index < keyTable.length; index++) {
        if (keyTable[index] != null) {
          hasNext = true;
          break;
        }
      }
    }

    @Override
    public @NotNull Iterator<Entry<K, V>> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public Entry<K, V> next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      entry.key = map.keyTable[index];
      entry.value = map.valueTable[index];
      advance();
      return entry;
    }
  }
}
