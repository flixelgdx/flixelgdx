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

/**
 * A growable buffer of primitive {@code char} values that doubles as a mutable,
 * allocation-friendly text builder.
 *
 * <p>Like {@link FlixelIntArray} it stores primitives directly (no boxing), but
 * it also implements {@link CharSequence} and {@link Appendable} so it can stand
 * in for a {@link StringBuilder} in hot paths. That is exactly what on-screen
 * text needs:
 * numbers and labels that change every frame can be rebuilt in place with the
 * {@code append} family instead of allocating a fresh {@code String} each time,
 * which keeps the garbage collector quiet.
 *
 * <p>Integer appends ({@link #append(int)}, {@link #append(long)}) write digits
 * directly into the backing {@code char[]} with no intermediate allocation.
 *
 * <p>The backing array {@link #getItems()} and count {@link #getSize()} are
 * exposed for zero-allocation indexed iteration.
 *
 * <p>This class is not thread safe.
 */
public class FlixelCharArray implements CharSequence, Appendable {

  private static final int DEFAULT_CAPACITY = 16;

  /**
   * The backing array. Only the first {@link #getSize()} entries are live. Safe
   * to read by index in hot loops.
   */
  private char[] items;

  /** The number of live values. */
  private int size;

  /** Whether removals preserve order (see {@link FlixelArray#isOrdered()}). */
  private boolean ordered;

  /**
   * Creates an ordered buffer with the default initial capacity.
   */
  public FlixelCharArray() {
    this(true, DEFAULT_CAPACITY);
  }

  /**
   * Creates an ordered buffer with the given initial capacity.
   *
   * @param capacity The initial backing-array size.
   */
  public FlixelCharArray(int capacity) {
    this(true, capacity);
  }

  /**
   * Creates a buffer with the given ordering and initial capacity.
   *
   * @param ordered Whether removals preserve order.
   * @param capacity The initial backing-array size.
   */
  public FlixelCharArray(boolean ordered, int capacity) {
    this.ordered = ordered;
    this.items = new char[Math.max(1, capacity)];
  }

  /**
   * Adds a single character to the end of the buffer.
   *
   * @param value The character to add.
   */
  public void add(char value) {
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
  public char get(int index) {
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
  public void set(int index, char value) {
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
  public char removeIndex(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    char removed = items[index];
    size--;
    if (ordered) {
      System.arraycopy(items, index + 1, items, index, size - index);
    } else {
      items[index] = items[size];
    }
    return removed;
  }

  /**
   * Appends a single character.
   *
   * @param value The character to append.
   * @return This buffer, for chaining.
   */
  @Override
  public @NotNull FlixelCharArray append(char value) {
    add(value);
    return this;
  }

  /**
   * Appends the characters of a sequence (such as a {@link String}).
   *
   * @param sequence The characters to append; a {@code null} is written as the
   *     literal text {@code "null"}.
   * @return This buffer, for chaining.
   */
  @Override
  public @NotNull FlixelCharArray append(CharSequence sequence) {
    CharSequence value = sequence == null ? "null" : sequence;
    return append(value, 0, value.length());
  }

  /**
   * Appends part of a character sequence.
   *
   * @param sequence The source sequence; a {@code null} is written as
   *     {@code "null"}.
   * @param start The index of the first character to append.
   * @param end The index just past the last character to append.
   * @return This buffer, for chaining.
   */
  @Override
  public @NotNull FlixelCharArray append(CharSequence sequence, int start, int end) {
    CharSequence value = sequence == null ? "null" : sequence;
    ensureCapacity(size + (end - start));
    for (int i = start; i < end; i++) {
      items[size++] = value.charAt(i);
    }
    return this;
  }

  /**
   * Appends every character of an array.
   *
   * @param chars The characters to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(char[] chars) {
    ensureCapacity(size + chars.length);
    System.arraycopy(chars, 0, items, size, chars.length);
    size += chars.length;
    return this;
  }

  /**
   * Appends the base-10 text of an {@code int} with no intermediate allocation.
   *
   * @param value The value to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(int value) {
    return append((long) value);
  }

  /**
   * Appends the base-10 text of a {@code long} with no intermediate allocation.
   *
   * @param value The value to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(long value) {
    if (value == Long.MIN_VALUE) {
      return append("-9223372036854775808");
    }
    if (value < 0) {
      add('-');
      value = -value;
    }
    int digitsStart = size;
    // Write digits least-significant first, then reverse them in place.
    do {
      add((char) ('0' + (int) (value % 10)));
      value /= 10;
    } while (value != 0);
    for (int i = digitsStart, j = size - 1; i < j; i++, j--) {
      char tmp = items[i];
      items[i] = items[j];
      items[j] = tmp;
    }
    return this;
  }

  /**
   * Appends the text of a {@code float}.
   *
   * <p>This convenience path uses {@link Float#toString(float)} and therefore
   * allocates. For per-frame numeric display prefer the allocation-free
   * {@code FlixelStringUtil} float helpers.
   *
   * @param value The value to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(float value) {
    return append(Float.toString(value));
  }

  /**
   * Appends the text of a {@code double}.
   *
   * @param value The value to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(double value) {
    return append(Double.toString(value));
  }

  /**
   * Appends {@code "true"} or {@code "false"}.
   *
   * @param value The value to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(boolean value) {
    return append(value ? "true" : "false");
  }

  /**
   * Appends the text of an object via {@link String#valueOf(Object)} (so a
   * {@code null} is written as {@code "null"}).
   *
   * @param value The object to append.
   * @return This buffer, for chaining.
   */
  public @NotNull FlixelCharArray append(Object value) {
    return append(String.valueOf(value));
  }

  /**
   * Ensures the backing array can hold at least the given number of characters
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
   * Sets the live length, growing the backing array if needed. New positions are
   * left with unspecified contents.
   *
   * @param newSize The new value of {@link #getSize()}.
   * @return The backing array.
   */
  public char[] setSize(int newSize) {
    ensureCapacity(newSize);
    size = newSize;
    return items;
  }

  /**
   * Trims the backing array down to exactly {@link #getSize()}.
   *
   * @return The backing array after trimming.
   */
  public char[] shrink() {
    if (items.length != size) {
      items = Arrays.copyOf(items, size);
    }
    return items;
  }

  /**
   * Trims the backing array down to exactly {@link #getSize()} (an alias for
   * {@link #shrink()}).
   */
  public void trimToSize() {
    shrink();
  }

  /**
   * Removes and returns the last character (a stack pop).
   *
   * @return The former last character.
   * @throws IndexOutOfBoundsException If the buffer is empty.
   */
  public char pop() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[--size];
  }

  /**
   * Returns the last character without removing it.
   *
   * @return The character at index {@code size - 1}.
   * @throws IndexOutOfBoundsException If the buffer is empty.
   */
  public char peek() {
    if (size == 0) {
      throw new IndexOutOfBoundsException("array is empty");
    }
    return items[size - 1];
  }

  /**
   * Reports whether the buffer has no characters.
   *
   * @return {@code true} if {@link #getSize()} is 0.
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Removes every character.
   */
  public void clear() {
    size = 0;
  }

  @Override
  public int length() {
    return size;
  }

  @Override
  public char charAt(int index) {
    if (index >= size) {
      throw new IndexOutOfBoundsException("index " + index + " >= size " + size);
    }
    return items[index];
  }

  @Override
  public @NotNull CharSequence subSequence(int start, int end) {
    return new String(items, start, end - start);
  }

  /**
   * Returns the live characters as a {@link String} (an alias for
   * {@link #toString()}).
   *
   * @return A string built from the first {@link #getSize()} characters.
   */
  public String toStringValue() {
    return toString();
  }

  /**
   * Returns a trimmed copy of the live characters.
   *
   * @return A new array of length {@link #getSize()}.
   */
  public char[] toArray() {
    return Arrays.copyOf(items, size);
  }

  @Override
  public String toString() {
    return new String(items, 0, size);
  }

  /**
   * Returns the backing array. Only the first {@link #getSize()} entries are live.
   *
   * @return The raw backing array; entries past index {@code getSize() - 1} are undefined.
   */
  public char[] getItems() {
    return items;
  }

  /**
   * Returns the number of live elements.
   *
   * @return The count of elements currently stored in this array.
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns whether removals preserve insertion order.
   *
   * @return {@code true} if removals shift remaining elements to maintain order.
   */
  public boolean isOrdered() {
    return ordered;
  }

  /**
   * Sets whether removals should preserve insertion order. Changing this mid-use
   * only affects future removals.
   *
   * @param ordered Whether removals should preserve insertion order.
   */
  public void setOrdered(boolean ordered) {
    this.ordered = ordered;
  }

  private void grow(int minCapacity) {
    int newCapacity = Math.max(minCapacity, items.length + (items.length >> 1) + 1);
    items = Arrays.copyOf(items, newCapacity);
  }
}
