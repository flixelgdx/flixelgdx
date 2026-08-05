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

import org.flixelgdx.math.FlixelRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A growable, ordered list backed by a plain Java array.
 *
 * <p>A single growable list that also folds in a built-in snapshot mode (see the
 * snapshot section below), so there is one array type to learn for both plain
 * use and safe mutation during iteration.
 *
 * <p><b>Zero-allocation iteration.</b> The backing array {@link #items} and the
 * live element count {@link #size} are public on purpose. Hot loops should walk
 * them by index rather than using an iterator, which allocates nothing:
 *
 * <pre>{@code
 * for (int i = 0; i < list.size; i++) {
 *   Enemy e = list.items[i];
 *   e.update(elapsed);
 * }
 * }</pre>
 *
 * <p>Because Java cannot create a generic array ({@code new T[n]}), a
 * {@link FlixelArraySupplier} tells the list how to build its typed backing
 * store. Pass an array-constructor reference:
 *
 * <pre>{@code
 * FlixelArray<Enemy> enemies = new FlixelArray<>(Enemy[]::new);
 * }</pre>
 *
 * <p><b>Ordered vs. unordered.</b> An {@link #ordered} list keeps insertion
 * order and shifts elements down on removal. An unordered list may fill a
 * removed slot with the last element instead, making removal O(1) and ideal for
 * bags where order does not matter.
 *
 * <p><b>Snapshot mode.</b> Modifying a list while a {@code for-each} loop walks
 * it normally corrupts the traversal. Wrap the loop in {@link #begin()} /
 * {@link #end()} to iterate a stable snapshot: any add or remove during the loop
 * transparently swaps in a fresh backing array, so the elements you are looping
 * over never shift under you. This is exactly what update loops need when
 * objects can spawn or destroy their neighbors mid-frame.
 *
 * <p>This class is not thread safe.
 *
 * @param <T> The element type.
 */
public class FlixelArray<T> implements Iterable<T> {

  private static final int DEFAULT_CAPACITY = 16;

  /**
   * The backing array. Only the first {@link #size} entries are live; the rest
   * are unspecified. Safe to read by index in hot loops; prefer the list's
   * methods for mutation.
   */
  public T[] items;

  /** The number of live elements. */
  public int size;

  /**
   * Whether removals preserve order. When {@code true}, removing shifts later
   * elements down; when {@code false}, a removed slot may be filled by the last
   * element for O(1) removal.
   */
  public boolean ordered;

  private T[] snapshot;
  private int snapshotDepth;
  private FlixelArrayIterator<T> iterator1;
  private FlixelArrayIterator<T> iterator2;

  /**
   * Creates an ordered list with the default initial capacity, backed by a
   * plain {@code Object[]}.
   *
   * <p>Use this when you do not need {@link #items} to be a genuinely typed
   * {@code T[]}, which is the common case and the only option for a generic
   * container that cannot name {@code T} at runtime. Reading elements by index
   * still returns {@code T}. When you do want a real typed backing array (for
   * example to hand {@code items} to an API expecting {@code String[]}), use the
   * {@link FlixelArraySupplier}-based constructors instead.
   */
  public FlixelArray() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates an ordered {@code Object[]}-backed list with the given initial
   * capacity.
   *
   * @param capacity The initial backing-array size.
   */
  public FlixelArray(int capacity) {
    this(true, capacity);
  }

  /**
   * Creates an {@code Object[]}-backed list with the given ordering and initial
   * capacity.
   *
   * @param ordered Whether removals preserve order (see {@link #ordered}).
   * @param capacity The initial backing-array size.
   */
  @SuppressWarnings("unchecked")
  public FlixelArray(boolean ordered, int capacity) {
    this.ordered = ordered;
    this.items = (T[]) new Object[Math.max(1, capacity)];
  }

  /**
   * Creates an ordered list with the default initial capacity and a typed
   * backing array.
   *
   * @param supplier Builds the typed backing array, for example
   *     {@code Enemy[]::new}.
   */
  public FlixelArray(@NotNull FlixelArraySupplier<T[]> supplier) {
    this(supplier, true, DEFAULT_CAPACITY);
  }

  /**
   * Creates an ordered list with the given initial capacity and a typed backing
   * array.
   *
   * @param supplier Builds the typed backing array.
   * @param capacity The initial backing-array size.
   */
  public FlixelArray(@NotNull FlixelArraySupplier<T[]> supplier, int capacity) {
    this(supplier, true, capacity);
  }

  /**
   * Creates a list with the given ordering and initial capacity and a typed
   * backing array.
   *
   * @param supplier Builds the typed backing array.
   * @param ordered Whether removals preserve order (see {@link #ordered}).
   * @param capacity The initial backing-array size.
   */
  public FlixelArray(@NotNull FlixelArraySupplier<T[]> supplier, boolean ordered, int capacity) {
    this.ordered = ordered;
    this.items = supplier.get(Math.max(1, capacity));
  }

  /**
   * Appends an element to the end of the list.
   *
   * @param value The element to add (may be {@code null}).
   */
  public void add(@Nullable T value) {
    T[] target = beforeModify();
    if (size == target.length) {
      target = grow(size + 1);
    }
    target[size++] = value;
  }

  /**
   * Appends every element of another list, in order.
   *
   * @param other The list whose elements to append.
   */
  public void addAll(@NotNull FlixelArray<? extends T> other) {
    beforeModify();
    int needed = size + other.size;
    if (needed > items.length) {
      grow(needed);
    }
    System.arraycopy(other.items, 0, items, size, other.size);
    size += other.size;
  }

  /**
   * Returns the element at the given index.
   *
   * @param index The position to read, from 0 to {@code size - 1}.
   * @return The element at {@code index}.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public @Nullable T get(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    return items[index];
  }

  /**
   * Replaces the element at the given index.
   *
   * @param index The position to write, from 0 to {@code size - 1}.
   * @param value The new element.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public void set(int index, @Nullable T value) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    items[index] = value;
  }

  /**
   * Inserts an element at the given index, shifting later elements right.
   *
   * @param index The position to insert at, from 0 to {@code size}.
   * @param value The element to insert.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public void insert(int index, @Nullable T value) {
    if (index > size) {
      throw new IndexOutOfBoundsException("index " + index + " > size " + size);
    }
    T[] target = beforeModify();
    if (size == target.length) {
      target = grow(size + 1);
    }
    if (ordered) {
      System.arraycopy(target, index, target, index + 1, size - index);
    } else if (size > index) {
      // Unordered: push the displaced element to the end instead of shifting.
      target[size] = target[index];
    }
    target[index] = value;
    size++;
  }

  /**
   * Removes and returns the element at the given index.
   *
   * <p>In an ordered list, later elements shift down. In an unordered list, the
   * gap is filled by the last element for O(1) removal.
   *
   * @param index The position to remove, from 0 to {@code size - 1}.
   * @return The element that was removed.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  public @Nullable T removeIndex(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    T[] target = beforeModify();
    T removed = target[index];
    size--;
    if (ordered) {
      System.arraycopy(target, index + 1, target, index, size - index);
    } else {
      target[index] = target[size];
    }
    target[size] = null;
    return removed;
  }

  /**
   * Removes the first element equal to the given value.
   *
   * @param value The value to remove.
   * @param identity If {@code true}, compare with {@code ==}; if {@code false},
   *     compare with {@link Object#equals(Object)}.
   * @return {@code true} if an element was removed.
   */
  public boolean removeValue(@Nullable T value, boolean identity) {
    int index = indexOf(value, identity);
    if (index == -1) {
      return false;
    }
    removeIndex(index);
    return true;
  }

  /**
   * Removes and returns the last element (a stack pop).
   *
   * @return The former last element.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public @Nullable T pop() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    T[] target = beforeModify();
    size--;
    T value = target[size];
    target[size] = null;
    return value;
  }

  /**
   * Returns the index of the first element equal to the given value.
   *
   * @param value The value to find.
   * @param identity If {@code true}, compare with {@code ==}; otherwise use
   *     {@link Object#equals(Object)}.
   * @return The index, or -1 if not found.
   */
  public int indexOf(@Nullable T value, boolean identity) {
    T[] target = items;
    if (identity || value == null) {
      for (int i = 0; i < size; i++) {
        if (target[i] == value) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < size; i++) {
        if (value.equals(target[i])) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Reports whether the list contains an element equal to the given value.
   *
   * @param value The value to look for.
   * @param identity If {@code true}, compare with {@code ==}; otherwise use
   *     {@link Object#equals(Object)}.
   * @return {@code true} if a matching element exists.
   */
  public boolean contains(@Nullable T value, boolean identity) {
    return indexOf(value, identity) != -1;
  }

  /**
   * Returns the first element.
   *
   * @return The element at index 0.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public @Nullable T first() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[0];
  }

  /**
   * Returns the last element without removing it.
   *
   * @return The element at index {@code size - 1}.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public @Nullable T last() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[size - 1];
  }

  /**
   * Returns the last element without removing it.
   *
   * <p>This is an alias for {@link #last()}, named to match the stack-style
   * pairing with {@link #pop()}.
   *
   * @return The element at index {@code size - 1}.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public @Nullable T peek() {
    return last();
  }

  /**
   * Sorts the live elements in place using their natural ordering.
   *
   * <p>Every element must implement {@link Comparable}; otherwise a
   * {@link ClassCastException} is thrown at runtime.
   */
  public void sort() {
    Arrays.sort(items, 0, size);
  }

  /**
   * Sorts the live elements in place using the given comparator.
   *
   * @param comparator The comparator that defines the order.
   */
  public void sort(@NotNull Comparator<? super T> comparator) {
    Arrays.sort(items, 0, size, comparator);
  }

  /**
   * Returns a random element using the given generator.
   *
   * @param rng The random source to draw from.
   * @return A random element, or {@code null} if the list is empty.
   */
  public @Nullable T getRandom(@NotNull FlixelRandom rng) {
    if (size == 0) {
      return null;
    }
    return items[rng.nextInt(size)];
  }

  /**
   * Reports whether the list has no elements.
   *
   * @return {@code true} if {@link #size} is 0.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Removes every element, nulling the slots so they can be garbage collected.
   */
  public void clear() {
    T[] target = beforeModify();
    Arrays.fill(target, 0, size, null);
    size = 0;
  }

  /**
   * Ensures the backing array can hold at least the given number of elements
   * without another grow.
   *
   * @param capacity The minimum capacity to guarantee.
   */
  public void ensureCapacity(int capacity) {
    if (capacity > items.length) {
      grow(capacity);
    }
  }

  /**
   * Sets the live element count, growing the backing array if needed.
   *
   * <p>When shrinking, the trimmed slots are nulled so their elements can be
   * garbage collected. When growing, the new slots are {@code null}.
   *
   * @param newSize The new value of {@link #size}.
   * @return The backing array.
   */
  public @NotNull T[] setSize(int newSize) {
    if (newSize > items.length) {
      grow(newSize);
    } else if (newSize < size) {
      Arrays.fill(items, newSize, size, null);
    }
    size = newSize;
    return items;
  }

  /**
   * Swaps the elements at two indices.
   *
   * @param first The first index.
   * @param second The second index.
   * @throws IndexOutOfBoundsException If either index is out of range.
   */
  public void swap(int first, int second) {
    if (first >= size || second >= size) {
      throw new IndexOutOfBoundsException("swap(" + first + ", " + second + ") size " + size);
    }
    T tmp = items[first];
    items[first] = items[second];
    items[second] = tmp;
  }

  /**
   * Begins a stable iteration snapshot.
   *
   * <p>Between this call and {@link #end()}, any modification transparently
   * copies the backing array, so an in-progress loop over {@link #items} is not
   * disturbed. Calls may nest; each {@code begin()} needs a matching
   * {@code end()}.
   *
   * @return The backing array to iterate (its first {@link #size} entries).
   */
  public @NotNull T[] begin() {
    snapshotDepth++;
    snapshot = items;
    return items;
  }

  /**
   * Ends a stable iteration snapshot started by {@link #begin()}.
   */
  public void end() {
    snapshotDepth = Math.max(0, snapshotDepth - 1);
    if (snapshotDepth == 0) {
      // Once the outermost loop finishes, drop the snapshot reference. If a copy
      // was made mid-iteration, the live list is already the copy and the
      // original snapshot array is now free to be garbage collected.
      snapshot = null;
    }
  }

  /**
   * Returns an iterator over the live elements.
   *
   * <p>The iterator is reused between loops to avoid allocation, so do not run
   * two independent {@code for-each} loops over the same list at the same time.
   * In hot paths prefer indexing {@link #items} directly.
   *
   * @return A reusable iterator.
   */
  @Override
  public @NotNull Iterator<T> iterator() {
    if (iterator1 == null) {
      iterator1 = new FlixelArrayIterator<>(this);
      iterator2 = new FlixelArrayIterator<>(this);
    }
    if (!iterator1.valid) {
      iterator1.index = 0;
      iterator1.valid = true;
      iterator2.valid = false;
      return iterator1;
    }
    iterator2.index = 0;
    iterator2.valid = true;
    iterator1.valid = false;
    return iterator2;
  }

  /**
   * If a snapshot is active, replaces the backing array with a fresh copy so the
   * snapshot the caller is iterating stays stable, then returns the array to
   * mutate.
   *
   * @return The (possibly newly copied) backing array to modify.
   */
  private T[] beforeModify() {
    if (snapshotDepth > 0 && snapshot == items) {
      items = Arrays.copyOf(items, items.length);
    }
    return items;
  }

  private T[] grow(int minCapacity) {
    int newCapacity = Math.max(minCapacity, items.length + (items.length >> 1) + 1);
    // Arrays.copyOf preserves the backing array's real component type, so a
    // typed (supplier-built) array stays typed and an Object[]-backed one stays
    // Object[]. Growing also allocates a fresh array, so any active snapshot
    // (which still points at the old array) is preserved without extra work.
    items = Arrays.copyOf(items, newCapacity);
    return items;
  }

  private static final class FlixelArrayIterator<T> implements Iterator<T> {
    private final FlixelArray<T> array;
    private int index;
    private boolean valid = true;

    FlixelArrayIterator(FlixelArray<T> array) {
      this.array = array;
    }

    @Override
    public boolean hasNext() {
      return index < array.size;
    }

    @Override
    public T next() {
      if (index >= array.size) {
        throw new NoSuchElementException();
      }
      return array.items[index++];
    }

    @Override
    public void remove() {
      index--;
      array.removeIndex(index);
    }
  }
}
