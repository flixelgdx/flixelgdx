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

import org.flixelgdx.collections.FlixelCharArray;
import org.flixelgdx.locale.FlixelLocalization;
import org.flixelgdx.text.FlixelText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Reusable mutable text buffer backed by a {@link FlixelCharArray}, designed to display changing
 * values (health, FPS, velocity) every frame without allocating garbage.
 *
 * <h2>Why not StringBuilder?</h2>
 *
 * <p>{@link StringBuilder#append(float)} internally calls {@link Float#toString(float)}, which
 * allocates a new {@link String} on every invocation. At 60 frames per second, even a single HUD
 * counter can produce hundreds of short-lived strings per second that pressure the garbage
 * collector. {@link FlixelCharArray} writes digits directly into its backing {@code char[]} with no
 * intermediate {@link String}, so {@link #set(float)}, {@link #concat(float)}, and all other
 * primitive overloads on this class are allocation-free on the hot path.
 *
 * <p>The same applies to {@code int}, {@code long}, {@code double}, {@code boolean}, {@code char},
 * {@code byte}, and {@code short}: every primitive {@link #set} and {@link #concat} overload goes
 * through {@link FlixelCharArray} appenders rather than {@link Object#toString()}.
 *
 * <h2>Allocation-free float formatting</h2>
 *
 * <p>{@link #setFloatRounded(float, int)} and {@link #concatFloatRounded(float, int)} delegate to
 * {@link FlixelStringUtil#appendFloatRounded(FlixelCharArray, float, int)}, which formats a float to any
 * number of decimal places using only integer arithmetic, meaning no {@link String} is created at
 * any point. {@link #setFloatRoundedOneDecimal(float)} and
 * {@link #concatFloatRoundedOneDecimal(float)} are convenience variants for exactly one decimal
 * place.
 *
 * <h2>Passing to drawing APIs</h2>
 *
 * <p>This class implements {@link CharSequence}, so instances can be passed directly to APIs such
 * as text rendering without building a temporary {@link String}. Avoid calling {@link #toString()}
 * or using string concatenation on this type in per-frame code: both allocate. Pass {@code this} as a
 * {@link CharSequence} instead.
 *
 * <h2>set() vs concat()</h2>
 *
 * <p>{@link #set} clears the buffer and writes new content in one call, which is the typical
 * pattern for a HUD label that shows a single changing value. {@link #concat} appends without
 * clearing, which is useful when building a line from multiple parts. {@link #charBuffer()} exposes
 * the raw {@link FlixelCharArray} for advanced interop with APIs that require it directly.
 *
 * <h2>In-place editing</h2>
 *
 * <p>{@link #insert(int, char)}, {@link #insert(int, CharSequence)}, {@link #delete(int, int)}, and
 * {@link #setLength(int)} edit the buffer's content without clearing it.
 *
 * <pre>{@code
 * // A text box backing a single line of editable input.
 * FlixelString line = new FlixelString("Helo");
 *
 * // The player moved the caret between the two 'l's and typed 'l'.
 * line.insert(2, 'l');
 * // line is now "Hello".
 *
 * // The player selected the last two characters and pressed delete.
 * line.delete(3, 5);
 * // line is now "Hel".
 * }</pre>
 *
 * <h2>Numbered placeholders</h2>
 *
 * <p>{@link #setFormat(CharSequence, CharSequence)} and {@link #concatFormat(CharSequence, CharSequence)} fill
 * {@code {0}}-style placeholders, which is how translated text from
 * {@link FlixelLocalization} gets its values:
 *
 * <pre>{@code
 * scoreText.set(score);
 * label.setFormat("Score: {0}", scoreText);
 * }</pre>
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
 * @Override
 * public void update(float elapsed) {
 *   fs.set("Score: ");
 *   fs.concat(score);
 *   ft.setText(fs);
 * }
 * }</pre>
 */
public class FlixelString implements CharSequence {

  private final FlixelCharArray buffer;

  /** Creates an empty buffer with a default initial capacity. */
  public FlixelString() {
    this(48);
  }

  /**
   * Creates an empty buffer with the given initial capacity hint.
   *
   * @param initialCapacity Non-negative initial capacity for the backing {@link FlixelCharArray}.
   */
  public FlixelString(int initialCapacity) {
    buffer = new FlixelCharArray(Math.max(8, initialCapacity));
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
   * Returns the mutable backing {@link FlixelCharArray}. Callers must not retain references across frames if the
   * owning {@link FlixelString} is reused or pooled, because the buffer contents change in place.
   *
   * @return The internal {@link FlixelCharArray} (never {@code null}).
   */
  @NotNull
  public FlixelCharArray charBuffer() {
    return buffer;
  }

  /**
   * Clears all characters without shrinking the allocated buffer.
   *
   * @see FlixelCharArray#clear()
   */
  public void clear() {
    buffer.clear();
  }

  /**
   * Shrinks the allocated buffer to the current length.
   *
   * @see FlixelCharArray#shrink()
   */
  public void shrinkToFit() {
    buffer.shrink();
  }

  /**
   * Trims the internal storage to the current length. Suitable for teardown paths (for example
   * {@link FlixelText#destroy()}) but not for per-frame use.
   */
  public void trimToSize() {
    buffer.trimToSize();
  }

  /**
   * Returns {@code true} when the buffer contains no characters.
   *
   * @return {@code true} if the buffer is empty, {@code false} otherwise.
   */
  public boolean isEmpty() {
    return buffer.isEmpty();
  }

  /**
   * Returns whether the buffer contains no characters.
   *
   * @return {@code true} if the buffer is empty, {@code false} otherwise.
   */
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
    buffer.append(Objects.requireNonNullElse(text, "null"));
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
    buffer.append(other.buffer, 0, other.buffer.getSize());
    return this;
  }

  /**
   * Replaces the buffer content with {@code value} and returns {@code this} for chaining.
   *
   * @param value The boolean value to write into the buffer.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(boolean value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with {@code value} and returns {@code this} for chaining.
   *
   * @param value The character to write into the buffer.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(char value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The byte value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(byte value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The short value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(short value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The int value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(int value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The long value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(long value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The float value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString set(float value) {
    buffer.clear();
    buffer.append(value);
    return this;
  }

  /**
   * Replaces the buffer content with the decimal rendering of {@code value} and returns {@code this} for chaining.
   *
   * @param value The double value to write into the buffer as a decimal string.
   * @return {@code this} for chaining.
   */
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
  public FlixelString concat(@Nullable CharSequence other) {
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
    buffer.append(other.buffer, 0, other.buffer.getSize());
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
   * Appends {@code value} rounded to {@code decimals} decimal places using the same rules as
   * {@link FlixelStringUtil#appendFloatRounded(FlixelCharArray, float, int)}. Does not clear the buffer
   * first.
   *
   * <p>When {@code decimals} is zero or negative, the value is rounded to the nearest integer and
   * no decimal point is written. Fractional digits are zero-padded on the left so the output
   * always contains exactly {@code decimals} digits after the decimal point.
   *
   * @param value    Value to append (non-finite values use {@link FlixelCharArray#append(float)}).
   * @param decimals Number of digits after the decimal point; values of zero or less produce an
   *                 integer with no decimal point.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concatFloatRounded(float value, int decimals) {
    FlixelStringUtil.appendFloatRounded(buffer, value, decimals);
    return this;
  }

  /**
   * Appends {@code value} rounded to one decimal place (tenths). Convenience wrapper for
   * {@link #concatFloatRounded(float, int)} with {@code decimals = 1}.
   *
   * @param value Value to append (non-finite values use {@link FlixelCharArray#append(float)}).
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concatFloatRoundedOneDecimal(float value) {
    return concatFloatRounded(value, 1);
  }

  /**
   * Appends an object the same way {@link FlixelCharArray#append(Object)} does: {@code null} becomes the literal
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
   * Inserts a single character at the given position, shifting later characters right.
   *
   * @param index The position to insert at, from 0 to {@link #length()}.
   * @param c The character to insert.
   * @return {@code this} for chaining.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  @NotNull
  public FlixelString insert(int index, char c) {
    buffer.insert(index, c);
    return this;
  }

  /**
   * Inserts every character of {@code s} at the given position, shifting later characters right.
   *
   * @param index The position to insert at, from 0 to {@link #length()}.
   * @param s The characters to insert; {@code null} is treated like the literal {@code "null"}.
   * @return {@code this} for chaining.
   * @throws IndexOutOfBoundsException If {@code index} is out of range.
   */
  @NotNull
  public FlixelString insert(int index, @Nullable CharSequence s) {
    buffer.insertRange(index, s != null ? s : "null");
    return this;
  }

  /**
   * Removes the characters from {@code start} (inclusive) to {@code end} (exclusive), shifting
   * later characters left to close the gap.
   *
   * @param start The index of the first character to remove.
   * @param end The index just past the last character to remove.
   * @return {@code this} for chaining.
   * @throws IndexOutOfBoundsException If {@code end} is greater than {@link #length()}, or if
   *     {@code start} is greater than {@code end}.
   */
  @NotNull
  public FlixelString delete(int start, int end) {
    buffer.removeRange(start, end);
    return this;
  }

  /**
   * Sets the number of live characters, truncating the buffer.
   *
   * <p>Growing is not supported here: {@link FlixelCharArray#setSize(int)} leaves grown positions
   * with unspecified contents, which would corrupt the text this buffer represents, so this method
   * throws instead of padding like {@link StringBuilder#setLength(int)} does.
   *
   * @param length The new length; must be from 0 to the current {@link #length()}.
   * @return {@code this} for chaining.
   * @throws IndexOutOfBoundsException If {@code length} is negative or greater than the current
   *     {@link #length()}.
   */
  @NotNull
  public FlixelString setLength(int length) {
    int current = buffer.getSize();
    if (length < 0 || length > current) {
      throw new IndexOutOfBoundsException("length " + length + " out of range [0, " + current + "]");
    }
    buffer.setSize(length);
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
   * Clears the buffer and writes {@code value} rounded to {@code decimals} decimal places using
   * only {@link FlixelCharArray} primitive appenders, avoiding {@link Float#toString(float)} and similar
   * helpers that allocate {@link String} instances.
   *
   * <p>When {@code decimals} is zero or negative, the value is rounded to the nearest integer and
   * no decimal point is written. Fractional digits are zero-padded on the left so the output
   * always contains exactly {@code decimals} digits after the decimal point.
   *
   * @param value Value to format; non-finite values fall back to {@link FlixelCharArray#append(float)}.
   * @param decimals Number of digits after the decimal point; values of zero or less produce an
   *                 integer with no decimal point.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString setFloatRounded(float value, int decimals) {
    buffer.clear();
    FlixelStringUtil.appendFloatRounded(buffer, value, decimals);
    return this;
  }

  /**
   * Clears the buffer and writes {@code value} rounded to one decimal place (tenths). Convenience
   * wrapper for {@link #setFloatRounded(float, int)} with {@code decimals = 1}.
   *
   * @param value Value to format. Non-finite values fall back to {@link FlixelCharArray#append(float)}.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString setFloatRoundedOneDecimal(float value) {
    return setFloatRounded(value, 1);
  }

  /**
   * Clears the buffer and writes {@code pattern} with its {@code {0}} placeholder replaced by {@code a0}.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format, for example {@code "Score: {0}"}.
   * @param a0 The value for {@code {0}}; {@code null} writes the literal {@code "null"}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or {@code a0} is this buffer.
   */
  @NotNull
  public FlixelString setFormat(@NotNull CharSequence pattern, @Nullable CharSequence a0) {
    checkFormatSources(pattern, a0, null, null, null);
    buffer.clear();
    appendFormat(pattern, 1, a0, null, null, null);
    return this;
  }

  /**
   * Clears the buffer and writes {@code pattern} with {@code {0}} and {@code {1}} replaced.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format, for example {@code "{0} hit {1}!"}.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString setFormat(@NotNull CharSequence pattern, @Nullable CharSequence a0, @Nullable CharSequence a1) {
    checkFormatSources(pattern, a0, a1, null, null);
    buffer.clear();
    appendFormat(pattern, 2, a0, a1, null, null);
    return this;
  }

  /**
   * Clears the buffer and writes {@code pattern} with {@code {0}}, {@code {1}}, and {@code {2}} replaced.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @param a2 The value for {@code {2}}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString setFormat(
      @NotNull CharSequence pattern,
      @Nullable CharSequence a0,
      @Nullable CharSequence a1,
      @Nullable CharSequence a2) {
    checkFormatSources(pattern, a0, a1, a2, null);
    buffer.clear();
    appendFormat(pattern, 3, a0, a1, a2, null);
    return this;
  }

  /**
   * Clears the buffer and writes {@code pattern} with every numbered placeholder replaced by the matching argument.
   *
   * <p>A placeholder is a zero-based index in curly braces: {@code {0}} is the first argument, {@code {1}} the
   * second, and so on. The same placeholder may appear more than once, and placeholders may appear in any order,
   * which is what lets a translation move words around:
   *
   * <pre>{@code
   * // English: "{0} defeated {1}!"  Japanese might put the words in a different order.
   * label.setFormat("{0} defeated {1}!", playerName, bossName);
   * }</pre>
   *
   * <p>The rules are deliberately small so they behave the same on every platform:
   *
   * <ul>
   *   <li><code>{{</code> writes a single <code>{</code>, and <code>}}</code> writes a single <code>}</code>.</li>
   *   <li>A placeholder whose index has no matching argument (or that is not a plain number, such as
   *     {@code {name}}) is copied through unchanged, so a typo stays visible instead of vanishing.</li>
   *   <li>A {@code null} argument writes the literal {@code "null"}, matching {@link #set(CharSequence)}.</li>
   * </ul>
   *
   * <p>Arguments are {@link CharSequence}s so numbers can be passed without creating a {@link String}: keep a
   * second {@link FlixelString} around, {@link #set(int) set} the number into it, and pass that.
   *
   * <p>The one, two, and three argument overloads are allocation-free. This varargs overload allocates an
   * array on every call, so prefer the fixed overloads in per-frame code.
   *
   * @param pattern The text to format.
   * @param args The values for {@code {0}}, {@code {1}}, and so on.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString setFormat(@NotNull CharSequence pattern, @Nullable CharSequence... args) {
    checkFormatSources(pattern, null, null, null, args);
    buffer.clear();
    appendFormat(pattern, args != null ? args.length : 0, null, null, null, args);
    return this;
  }

  /**
   * Appends {@code pattern} with its {@code {0}} placeholder replaced by {@code a0}, without clearing.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format.
   * @param a0 The value for {@code {0}}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or {@code a0} is this buffer.
   */
  @NotNull
  public FlixelString concatFormat(@NotNull CharSequence pattern, @Nullable CharSequence a0) {
    checkFormatSources(pattern, a0, null, null, null);
    appendFormat(pattern, 1, a0, null, null, null);
    return this;
  }

  /**
   * Appends {@code pattern} with {@code {0}} and {@code {1}} replaced, without clearing.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString concatFormat(@NotNull CharSequence pattern, @Nullable CharSequence a0,
      @Nullable CharSequence a1) {
    checkFormatSources(pattern, a0, a1, null, null);
    appendFormat(pattern, 2, a0, a1, null, null);
    return this;
  }

  /**
   * Appends {@code pattern} with {@code {0}}, {@code {1}}, and {@code {2}} replaced, without clearing.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules.
   *
   * @param pattern The text to format.
   * @param a0 The value for {@code {0}}.
   * @param a1 The value for {@code {1}}.
   * @param a2 The value for {@code {2}}.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString concatFormat(
      @NotNull CharSequence pattern,
      @Nullable CharSequence a0,
      @Nullable CharSequence a1,
      @Nullable CharSequence a2) {
    checkFormatSources(pattern, a0, a1, a2, null);
    appendFormat(pattern, 3, a0, a1, a2, null);
    return this;
  }

  /**
   * Appends {@code pattern} with every numbered placeholder replaced, without clearing.
   *
   * <p>See {@link #setFormat(CharSequence, CharSequence...)} for the full placeholder rules. This overload
   * allocates an array on every call.
   *
   * @param pattern The text to format.
   * @param args The values for {@code {0}}, {@code {1}}, and so on.
   * @return {@code this} for chaining.
   * @throws IllegalArgumentException If {@code pattern} or any argument is this buffer.
   */
  @NotNull
  public FlixelString concatFormat(@NotNull CharSequence pattern, @Nullable CharSequence... args) {
    checkFormatSources(pattern, null, null, null, args);
    appendFormat(pattern, args != null ? args.length : 0, null, null, null, args);
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
    int n = buffer.getSize();
    return n == 0 ? "" : new String(buffer.getItems(), 0, n);
  }

  /**
   * Rejects formatting sources that are this buffer, because the buffer would be read while it is
   * being cleared or written.
   */
  private void checkFormatSources(
      @NotNull CharSequence pattern,
      @Nullable CharSequence a0,
      @Nullable CharSequence a1,
      @Nullable CharSequence a2,
      @Nullable CharSequence[] rest) {
    Objects.requireNonNull(pattern, "pattern");
    boolean self = pattern == this || a0 == this || a1 == this || a2 == this;
    if (!self && rest != null) {
      for (int i = 0; i < rest.length; i++) {
        if (rest[i] == this) {
          self = true;
          break;
        }
      }
    }
    if (self) {
      throw new IllegalArgumentException("A FlixelString cannot format itself into itself.");
    }
  }

  /**
   * Writes {@code pattern} into the buffer, replacing placeholders with arguments.
   *
   * <p>Arguments come from {@code a0} to {@code a2} when {@code rest} is {@code null}, or from
   * {@code rest} otherwise. This keeps the fixed-count overloads free of array allocation.
   */
  private void appendFormat(
      @NotNull CharSequence pattern,
      int argc,
      @Nullable CharSequence a0,
      @Nullable CharSequence a1,
      @Nullable CharSequence a2,
      @Nullable CharSequence[] rest) {
    int len = pattern.length();
    int i = 0;
    while (i < len) {
      char c = pattern.charAt(i);
      if (c == '}') {
        buffer.append('}');
        // A doubled closing brace is an escape for a single one.
        i += (i + 1 < len && pattern.charAt(i + 1) == '}') ? 2 : 1;
        continue;
      }
      if (c != '{') {
        buffer.append(c);
        i++;
        continue;
      }
      if (i + 1 < len && pattern.charAt(i + 1) == '{') {
        buffer.append('{');
        i += 2;
        continue;
      }

      // Read the digits after the opening brace and check that a closing brace follows them.
      int j = i + 1;
      int index = 0;
      while (j < len && j - i <= 9) {
        char d = pattern.charAt(j);
        if (d < '0' || d > '9') {
          break;
        }
        index = index * 10 + (d - '0');
        j++;
      }
      boolean valid = j > i + 1 && j < len && pattern.charAt(j) == '}' && index < argc;
      if (!valid) {
        // Not a usable placeholder, so copy the opening brace through and keep scanning after it.
        buffer.append('{');
        i++;
        continue;
      }

      CharSequence arg;
      if (rest != null) {
        arg = rest[index];
      } else {
        arg = switch (index) {
          case 0 -> a0;
          case 1 -> a1;
          default -> a2;
        };
      }
      buffer.append(Objects.requireNonNullElse(arg, "null"));
      i = j + 1;
    }
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
   * instance as a {@link CharSequence} instead where possible.
   */
  @Override
  @NotNull
  public String toString() {
    int n = buffer.getSize();
    return n == 0 ? "" : new String(buffer.getItems(), 0, n);
  }
}
