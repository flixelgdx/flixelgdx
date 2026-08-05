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

/**
 * Creates a typed array of a requested size.
 *
 * <p>Java erases generic type information at runtime, so code that stores its
 * elements in a real {@code T[]} (rather than an {@code Object[]}) cannot write
 * {@code new T[size]} directly. This functional interface bridges that gap: a
 * caller passes an array constructor reference, and the collection uses it to
 * build a properly typed backing array on demand.
 *
 * <p>The idiomatic way to supply one is an array-constructor method reference:
 *
 * <pre>{@code
 * FlixelArraySupplier<FlixelCamera[]> factory = FlixelCamera[]::new;
 * FlixelCamera[] cameras = factory.get(8); // a new FlixelCamera[8]
 * }</pre>
 *
 * @param <T> The array type produced, for example {@code String[]}.
 */
@FunctionalInterface
public interface FlixelArraySupplier<T> {

  /**
   * Creates a new array of the given size.
   *
   * @param size The length of the array to create.
   * @return A newly allocated array of length {@code size}.
   */
  T get(int size);
}
