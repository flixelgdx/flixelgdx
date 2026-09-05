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
 * An unordered map from keys to values, backed by an open-addressing hash table.
 *
 * <p>It stores keys and values in flat arrays and resolves collisions by linear
 * probing, which keeps memory compact and lookups cache friendly, a good fit for
 * the per-frame lookups games do constantly.
 *
 * <p>Keys must not be {@code null}; values may be. Iteration order is not
 * defined and can change as the map grows.
 *
 * <h2>Zero-allocation iteration</h2>
 * {@link #keys()}, {@link #values()}, and
 * {@link #entries()} return iterators that are reused between loops, so ordinary
 * traversal allocates nothing. The trade-off is that you must not run two loops
 * of the same kind over one map at the same time. The shared {@link Entry} handed
 * out by {@link #entries()} is also reused, so copy its fields if you need to
 * keep them.
 *
 * <pre>{@code
 * FlixelMap<String, Integer> scores = new FlixelMap<>();
 * scores.put("alice", 10);
 * for (FlixelMap.Entry<String, Integer> e : scores.entries()) {
 *   Flixel.info(e.key + " = " + e.value);
 * }
 * }</pre>
 *
 * <p>This class is not thread safe.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FlixelMap<K, V> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;

  // Fibonacci hashing multiplier (2^32 / golden ratio) for good bit mixing.
  private static final int HASH_MULTIPLIER = 0x9E3779B1;

  private K[] keyTable;
  private V[] valueTable;
  private int size;
  private int mask;
  private int threshold;
  private int shift;

  private Entries<K, V> entries;
  private Keys<K> keys;
  private Values<V> values;

  /**
   * Creates an empty map with the default initial capacity.
   */
  public FlixelMap() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an empty map sized to hold at least the given number of entries
   * before it needs to grow.
   *
   * @param initialCapacity The expected entry count.
   */
  @SuppressWarnings("unchecked")
  public FlixelMap(int initialCapacity) {
    int cap = tableSizeFor(Math.max(1, initialCapacity));
    keyTable = (K[]) new Object[cap];
    valueTable = (V[]) new Object[cap];
    mask = cap - 1;
    threshold = (int) (cap * LOAD_FACTOR);
    shift = Integer.numberOfLeadingZeros(mask);
  }

  /**
   * Associates a value with a key, replacing any previous value.
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
   * @return The associated value, or {@code null} if the key is absent (or maps
   *     to {@code null}).
   */
  public @Nullable V get(@NotNull K key) {
    return get(key, null);
  }

  /**
   * Returns the value stored under a key, or a fallback if the key is absent.
   *
   * @param key The key to look up.
   * @param defaultValue The value to return when the key is not present.
   * @return The associated value, or {@code defaultValue} if the key is absent.
   */
  public @Nullable V get(@NotNull K key, @Nullable V defaultValue) {
    int i = locate(key);
    return keyTable[i] == null ? defaultValue : valueTable[i];
  }

  /**
   * Reports whether a key is present.
   *
   * @param key The key to check.
   * @return {@code true} if the key has an entry.
   */
  public boolean containsKey(@NotNull K key) {
    return keyTable[locate(key)] != null;
  }

  /**
   * Removes the entry for a key, if present.
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
    // Backward-shift deletion: walk the probe chain and pull back any entry that
    // would otherwise become unreachable, keeping the table free of tombstones.
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
  public int getSize() {
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

  /**
   * Returns a reusable iterable over the map's keys.
   *
   * @return An iterable over the keys.
   */
  public @NotNull Keys<K> keys() {
    if (keys == null) {
      keys = new Keys<>(this);
    }
    keys.reset();
    return keys;
  }

  /**
   * Returns a reusable iterable over the map's values.
   *
   * @return An iterable over the values.
   */
  public @NotNull Values<V> values() {
    if (values == null) {
      values = new Values<>(this);
    }
    values.reset();
    return values;
  }

  private int place(K key) {
    return (key.hashCode() * HASH_MULTIPLIER) >>> shift;
  }

  private int locate(K key) {
    int i = place(key);
    while (true) {
      K existing = keyTable[i];
      if (existing == null || existing.equals(key)) {
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
    // Round up to a power of two large enough that the load factor leaves room.
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
   * <p>The same instance is reused across iteration steps, so copy the fields if
   * you need to hold onto them past the current step.
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

  private abstract static class MapIterator<K, V> {
    final FlixelMap<K, V> map;
    int nextIndex;
    int currentIndex;
    boolean hasNext;

    MapIterator(FlixelMap<K, V> map) {
      this.map = map;
    }

    void reset() {
      nextIndex = -1;
      currentIndex = -1;
      advance();
    }

    void advance() {
      hasNext = false;
      K[] keyTable = map.keyTable;
      for (nextIndex++; nextIndex < keyTable.length; nextIndex++) {
        if (keyTable[nextIndex] != null) {
          hasNext = true;
          break;
        }
      }
    }

    /**
     * Removes the entry returned by the most recent {@link Iterator#next()} call.
     *
     * <p>Safe to call once per {@code next()} while iterating. It performs the
     * same backward-shift deletion the map uses, then rewinds the scan so an
     * entry pulled into the vacated slot is still visited.
     *
     * @throws IllegalStateException If {@code next()} has not been called, or was
     *     already followed by a {@code remove()}.
     */
    void doRemove() {
      int i = currentIndex;
      if (i < 0) {
        throw new IllegalStateException("next() must be called before remove().");
      }
      K[] keyTable = map.keyTable;
      V[] valueTable = map.valueTable;
      int mask = map.mask;
      int next = (i + 1) & mask;
      K key;
      while ((key = keyTable[next]) != null) {
        int ideal = map.place(key);
        if (((next - ideal) & mask) >= ((next - i) & mask)) {
          keyTable[i] = key;
          valueTable[i] = valueTable[next];
          i = next;
        }
        next = (next + 1) & mask;
      }
      keyTable[i] = null;
      valueTable[i] = null;
      map.size--;
      // A later entry may have been pulled back into the vacated slot; rescan
      // from there so it is not skipped.
      nextIndex = currentIndex - 1;
      currentIndex = -1;
      advance();
    }
  }

  /**
   * A reusable iterable over a map's entries.
   *
   * @param <K> The key type.
   * @param <V> The value type.
   */
  public static final class Entries<K, V> extends MapIterator<K, V>
      implements Iterable<Entry<K, V>>, Iterator<Entry<K, V>> {

    private final Entry<K, V> entry = new Entry<>();

    Entries(FlixelMap<K, V> map) {
      super(map);
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
      currentIndex = nextIndex;
      entry.key = map.keyTable[nextIndex];
      entry.value = map.valueTable[nextIndex];
      advance();
      return entry;
    }

    @Override
    public void remove() {
      doRemove();
    }
  }

  /**
   * A reusable iterable over a map's keys.
   *
   * @param <K> The key type.
   */
  public static final class Keys<K> extends MapIterator<K, Object> implements Iterable<K>, Iterator<K> {

    @SuppressWarnings("unchecked")
    Keys(FlixelMap<K, ?> map) {
      super((FlixelMap<K, Object>) map);
    }

    @Override
    public @NotNull Iterator<K> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public K next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      currentIndex = nextIndex;
      K key = map.keyTable[nextIndex];
      advance();
      return key;
    }

    @Override
    public void remove() {
      doRemove();
    }
  }

  /**
   * A reusable iterable over a map's values.
   *
   * @param <V> The value type.
   */
  public static final class Values<V> extends MapIterator<Object, V> implements Iterable<V>, Iterator<V> {

    @SuppressWarnings("unchecked")
    Values(FlixelMap<?, V> map) {
      super((FlixelMap<Object, V>) map);
    }

    @Override
    public @NotNull Iterator<V> iterator() {
      return this;
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public V next() {
      if (!hasNext) {
        throw new NoSuchElementException();
      }
      currentIndex = nextIndex;
      V value = map.valueTable[nextIndex];
      advance();
      return value;
    }

    @Override
    public void remove() {
      doRemove();
    }
  }
}
