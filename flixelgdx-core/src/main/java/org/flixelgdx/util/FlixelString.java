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
package org.flixelgdx.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.CharArray;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Reusable mutable text buffer backed by libGDX {@link CharArray}, designed to display changing
 * values (health, FPS, velocity) every frame without allocating garbage.
 *
 * <h2>Why not StringBuilder?</h2>
 *
 * <p>{@link StringBuilder#append(float)} internally calls {@link Float#toString(float)}, which
 * allocates a new {@link String} on every invocation. At 60 frames per second, even a single HUD
 * counter can produce hundreds of short-lived strings per second that pressure the garbage
 * collector. {@link CharArray} writes digits directly into its backing {@code char[]} with no
 * intermediate {@link String}, so {@link #set(float)}, {@link #concat(float)}, and all other
 * primitive overloads on this class are allocation-free on the hot path.
 *
 * <p>The same applies to {@code int}, {@code long}, {@code double}, {@code boolean}, {@code char},
 * {@code byte}, and {@code short}: every primitive {@link #set} and {@link #concat} overload goes
 * through {@link CharArray} appenders rather than {@link Object#toString()}.
 *
 * <h2>Allocation-free float formatting</h2>
 *
 * <p>{@link #setFloatRoundedOneDecimal(float)} and {@link #concatFloatRoundedOneDecimal(float)}
 * delegate to {@link FlixelStringUtil#appendFloatRoundedOneDecimal(CharArray, float)}, which
 * formats a float to one decimal place using only integer arithmetic, meaning no {@link String} is
 * created at any point.
 *
 * <h2>Passing to libGDX drawing APIs</h2>
 *
 * <p>This class implements {@link CharSequence}, so instances can be passed directly to APIs such
 * as {@link BitmapFont#draw} without building a temporary {@link String}. Avoid calling {@link #toString()}
 * or using string concatenation on this type in per-frame code: both allocate. Pass {@code this} as a
 * {@link CharSequence} instead.
 *
 * <h2>set() vs concat()</h2>
 *
 * <p>{@link #set} clears the buffer and writes new content in one call, which is the typical
 * pattern for a HUD label that shows a single changing value. {@link #concat} appends without
 * clearing, which is useful when building a line from multiple parts. {@link #charBuffer()} exposes
 * the raw {@link CharArray} for advanced interop with libGDX APIs that require it directly.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // Create a new FlixelString with a capacity of 32 characters.
 * // Because it implements CharSequence, it can be used as a parameter
 * // for methods that expect a CharSequence, like FlixelText.
 * FlixelString fs = new FlixelString(32);
 * FlixelText ft = new FlixelText();
 *
 * // In your update loop...
 * @Override
 * public void update(float elapsed) {
 *   // Below would be the same equivalent of doing ft.setText("Score: " + score),
 *   // except it doesn't allocate new strings every frame and keeps your
 *   // framerate silky smooth!
 *   fs.set("Score: ");
 *   fs.concat(score);
 *   ft.setText(fs);
 * }
 * }</pre>
 */
public class FlixelString implements CharSequence {

  private final CharArray buffer;

  /** Creates an empty buffer with a default initial capacity. */
  public FlixelString() {
    this(48);
  }

  /**
   * Creates an empty buffer with the given initial capacity hint.
   *
   * @param initialCapacity Non-negative initial capacity for the backing {@link CharArray}.
   */
  public FlixelString(int initialCapacity) {
    buffer = new CharArray(Math.max(8, initialCapacity));
  }

  /**
   * Creates a buffer whose content is a copy of {@code text}.
   *
   * @param text Source characters; {@code null} is treated like the literal {@code "null"}.
   */
  public FlixelString(@Nullable CharSequence text) {
    this(16);
    set(text);
  }

  /**
   * Returns the mutable backing {@link CharArray}. Callers must not retain references across frames if the
   * owning {@link FlixelString} is reused or pooled, because the buffer contents change in place.
   *
   * @return The internal {@link CharArray} (never {@code null}).
   */
  @NotNull
  public CharArray charBuffer() {
    return buffer;
  }

  /**
   * Clears all characters without shrinking the allocated buffer.
   *
   * @see CharArray#clear()
   */
  public void clear() {
    buffer.clear();
  }

  /**
   * Shrinks the allocated buffer to the current length.
   *
   * @see CharArray#shrink()
   */
  public void shrinkToFit() {
    buffer.shrink();
  }

  /**
   * Trims the internal storage to the current length. Suitable for teardown paths (for example
   * {@link org.flixelgdx.text.FlixelText#destroy() FlixelText.destroy()}) but not for per-frame use.
   */
  public void trimToSize() {
    buffer.trimToSize();
  }

  /** @return {@code true} when the buffer contains no characters. */
  public boolean isEmpty() {
    return buffer.isEmpty();
  }

  /** Returns whether the buffer contains no characters. */
  public boolean getEmpty() {
    return buffer.isEmpty();
  }

  /**
   * Replaces the entire buffer with a copy of {@code text}.
   *
   * @param text New content; {@code null} is treated like the literal {@code "null"}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(@Nullable CharSequence text) {
    buffer.clear();
    if (text == null) {
      buffer.append("null");
    } else {
      buffer.append(text);
    }
    return this;
  }

  /**
   * Replaces the buffer with a copy of another {@link FlixelString}.
   *
   * @param other Source buffer; {@code null} is treated like the literal {@code "null"} via {@link #set(CharSequence)}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(@Nullable FlixelString other) {
    if (other == null) {
      return set((CharSequence) null);
    }
    buffer.clear();
    buffer.append(other.buffer, 0, other.buffer.size);
    return this;
  }

  /** @return {@code this} after replacing content with {@code value}. */
  @NotNull
  public FlixelString set(boolean value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with {@code value}. */
  @NotNull
  public FlixelString set(char value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(byte value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(short value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(int value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(long value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(float value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(double value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code other} to the buffer.
   *
   * <p>Note: If the {@code other} is a {@link String}, it will be converted to a {@link CharSequence} using
   * {@link String#subSequence(int, int)}.
   *
   * @param other The {@link CharSequence} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull CharSequence other) {
    buffer.append(other != null ? other : "null");
    return this;
  }

  /**
   * Appends the content of {@code other} to the buffer.
   *
   * @param other The {@link FlixelString} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull FlixelString other) {
    buffer.append(other.buffer, 0, other.buffer.size);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The boolean value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(boolean value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The char value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(char value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The byte value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(byte value) {
    buffer.append((int) value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The short value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(short value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The int value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(int value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The long value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(long value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The float value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(float value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The double value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(double value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends {@code value} rounded to one decimal place (tenths) using the same rules as
   * {@link FlixelStringUtil#appendFloatRoundedOneDecimal(CharArray, float)}. Does not clear the buffer first.
   *
   * @param value Value to append (non-finite values use {@link CharArray#append(float)}).
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concatFloatRoundedOneDecimal(float value) {
    FlixelStringUtil.appendFloatRoundedOneDecimal(buffer, value);
    return this;
  }

  /**
   * Appends an object the same way {@link CharArray#append(Object)} does: {@code null} becomes the literal
   * {@code "null"}, {@link CharSequence} is appended without {@link Object#toString()}, and other types use
   * {@link Object#toString()}.
   *
   * @param obj Value to append (may be {@code null}).
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@Nullable Object obj) {
    buffer.append(obj);
    return this;
  }

  /**
   * Replaces the buffer with the {@link CharSequence} returned by {@code supplier}. If the supplier returns
   * {@code null}, the literal {@code "null"} is appended.
   *
   * <p>Note: If the supplier constructs a new {@link String} on each call, allocations move to the supplier.
   * Prefer {@link CharSequence} sources that are stable or reused when possible.
   *
   * @param supplier Source of the new content; must not be {@code null}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString setCharSequence(@NotNull Supplier<? extends CharSequence> supplier) {
    buffer.clear();
    CharSequence seq = supplier.get();
    if (seq == null) {
      buffer.append("null");
    } else {
      buffer.append(seq);
    }
    return this;
  }

  /**
   * Appends {@code value} rounded to one decimal place (tenths), using only {@link CharArray} integer appenders.
   * This avoids {@link Float#toString(float)} and similar helpers that allocate {@link String} instances.
   *
   * <p>The buffer is cleared before formatting.
   *
   * @param value Finite input; non-finite values fall back to {@link CharArray#append(float)}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString setFloatRoundedOneDecimal(float value) {
    buffer.clear();
    FlixelStringUtil.appendFloatRoundedOneDecimal(buffer, value);
    return this;
  }

  /**
   * Copies the current buffer into a new {@link String}. Intended for cold paths (for example a debug
   * {@code forEach} callback), not per-frame drawing.
   *
   * @return A new string holding the current characters.
   */
  @NotNull
  public String copyContentToNewString() {
    int n = buffer.size;
    return n == 0 ? "" : new String(buffer.items, 0, n);
  }

  @Override
  public int length() {
    return buffer.length();
  }

  @Override
  public char charAt(int index) {
    return buffer.charAt(index);
  }

  @Override
  @NotNull
  public CharSequence subSequence(int start, int end) {
    return buffer.subSequence(start, end);
  }

  /**
   * {@inheritDoc}
   *
   * <p><strong>Allocation warning:</strong> Builds a new {@link String}. Do not use on hot paths; pass this
   * instance as a {@link CharSequence} instead.
   */
  @Override
  @NotNull
  public String toString() {
    int n = buffer.size;
    return n == 0 ? "" : new String(buffer.items, 0, n);
  }
}
