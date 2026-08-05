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

import org.jetbrains.annotations.Nullable;

/**
 * A map from a {@code boolean} key to an object value.
 *
 * <p>Because a boolean has only two possible keys, this holds exactly two
 * optional slots rather than a hash table, so every operation is a constant-time
 * field access with no allocation. It is handy for keeping a value per on/off
 * state (for example a "pressed" and a "released" sprite) behind a map-shaped
 * API. Values may be {@code null}.
 *
 * <p>This class is not thread safe.
 *
 * @param <V> The value type.
 */
public class FlixelBoolMap<V> {

  private V trueValue;
  private V falseValue;
  private boolean hasTrue;
  private boolean hasFalse;

  /**
   * Associates a value with a key, replacing any previous value.
   *
   * @param key The key.
   * @param value The value to store (may be {@code null}).
   * @return The value previously stored under {@code key}, or {@code null} if
   *     there was none.
   */
  public @Nullable V put(boolean key, @Nullable V value) {
    if (key) {
      V old = trueValue;
      trueValue = value;
      hasTrue = true;
      return old;
    }
    V old = falseValue;
    falseValue = value;
    hasFalse = true;
    return old;
  }

  /**
   * Returns the value stored under a key.
   *
   * @param key The key to look up.
   * @return The associated value, or {@code null} if the key is absent.
   */
  public @Nullable V get(boolean key) {
    return key ? trueValue : falseValue;
  }

  /**
   * Reports whether a key is present.
   *
   * @param key The key to check.
   * @return {@code true} if the key has an entry.
   */
  public boolean containsKey(boolean key) {
    return key ? hasTrue : hasFalse;
  }

  /**
   * Removes the entry for a key, if present.
   *
   * @param key The key to remove.
   * @return The value that was removed, or {@code null} if the key was absent.
   */
  public @Nullable V remove(boolean key) {
    if (key) {
      V old = trueValue;
      trueValue = null;
      hasTrue = false;
      return old;
    }
    V old = falseValue;
    falseValue = null;
    hasFalse = false;
    return old;
  }

  /**
   * Removes both entries, leaving the map empty.
   */
  public void clear() {
    trueValue = null;
    falseValue = null;
    hasTrue = false;
    hasFalse = false;
  }

  /**
   * Returns the number of entries (0, 1, or 2).
   *
   * @return The entry count.
   */
  public int size() {
    return (hasTrue ? 1 : 0) + (hasFalse ? 1 : 0);
  }

  /**
   * Reports whether the map has no entries.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return !hasTrue && !hasFalse;
  }
}
