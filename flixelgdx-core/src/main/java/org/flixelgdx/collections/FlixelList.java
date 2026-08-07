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

/**
 * A read-only view over an ordered list.
 *
 * <p>The name mirrors Java's {@link java.util.List} on purpose: both represent an ordered,
 * indexed sequence you can query and iterate. The key difference is cost. Java's {@code List}
 * hierarchy was designed for general correctness; unmodifiable wrappers ({@code
 * Collections.unmodifiableList}) allocate a wrapper object and throw at runtime on mutation
 * attempts. {@code FlixelList} enforces read-only access at compile time through the type system,
 * with no wrapper object, no allocation on read, and no garbage produced during iteration.
 *
 * <p>Mutation methods ({@code add}, {@code remove}, {@code set}, and so on) live only on
 * {@link FlixelArray}, which implements this interface. Returning {@code FlixelList} from an API
 * signals that the caller should not mutate the collection; the owner retains a {@link FlixelArray}
 * reference internally and modifies it freely.
 *
 * <p>For the tightest loops, bypass the iterator entirely: read from {@link #getItems()} by index.
 * This is entirely allocation-free and is the same pattern the framework uses internally.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelList<FlixelDisplayMode> modes = Flixel.graphics.getDisplayModes();
 * FlixelDisplayMode[] items = modes.getItems();
 * for (int i = 0; i < modes.getSize(); i++) {
 *   FlixelDisplayMode mode = items[i];
 *   // ...
 * }
 * }</pre>
 *
 * @param <T> The element type.
 * @see FlixelArray
 */
public interface FlixelList<T> extends Iterable<T> {

  /**
   * Returns the element at the given index.
   *
   * @param index The position to read, from 0 to {@code getSize() - 1}.
   * @return The element at {@code index}.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  @Nullable
  T get(int index);

  /**
   * Returns the first element.
   *
   * @return The element at index 0.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  @Nullable
  T first();

  /**
   * Returns the last element.
   *
   * @return The element at index {@code getSize() - 1}.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  @Nullable
  T last();

  /**
   * Returns a random element using the given generator.
   *
   * @param rng The random source to draw from.
   * @return A random element, or {@code null} if the list is empty.
   */
  @Nullable
  T getRandom(@NotNull FlixelRandom rng);

  /**
   * Returns the index of the first element equal to the given value.
   *
   * @param value The value to find.
   * @param identity If {@code true}, compare with {@code ==}; otherwise use
   *     {@link Object#equals(Object)}.
   * @return The index, or -1 if not found.
   */
  int indexOf(@Nullable T value, boolean identity);

  /**
   * Reports whether the list contains an element equal to the given value.
   *
   * @param value The value to look for.
   * @param identity If {@code true}, compare with {@code ==}; otherwise use
   *     {@link Object#equals(Object)}.
   * @return {@code true} if a matching element exists.
   */
  boolean contains(@Nullable T value, boolean identity);

  /**
   * Reports whether the list has no elements.
   *
   * @return {@code true} if {@link #getSize()} is 0.
   */
  boolean isEmpty();

  /**
   * Returns the backing array. Only the first {@link #getSize()} entries are live.
   *
   * <p>Safe to read in hot loops by index. Do not write to the returned array; use
   * {@link FlixelArray} if mutation is needed.
   *
   * @return The backing array.
   */
  @NotNull
  T[] getItems();

  /** Returns the number of live elements. */
  int getSize();

  /** Returns whether removals preserve insertion order. */
  boolean isOrdered();
}
