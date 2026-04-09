/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import com.badlogic.gdx.utils.CharArray;

import me.stringdotjar.flixelgdx.functional.supplier.ByteSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.CharSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.FloatSupplier;
import me.stringdotjar.flixelgdx.functional.supplier.ShortSupplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Reusable mutable text buffer backed by libGDX {@link CharArray}, implementing {@link CharSequence}
 * so it can be passed directly to libGDX text APIs (for example {@link com.badlogic.gdx.graphics.g2d.BitmapFont#draw})
 * without building a temporary {@link String} on each frame.
 *
 * <p>libGDX 1.14 replaced the legacy {@code com.badlogic.gdx.utils.StringBuilder} type with {@link CharArray},
 * which provides appenders for primitives that avoid boxing and implements {@link CharSequence}. This class
 * wraps a single {@link CharArray} instance and exposes {@link #set} overloads that clear the buffer then append
 * new content, which matches common HUD and debug-overlay usage patterns.
 *
 * <p>Prefer passing {@code this} (as a {@link CharSequence}) into drawing and layout
 * calls. Avoid {@link #toString()} in per-frame code: it allocates a new {@link String} from the backing buffer.
 * The same applies to string concatenation that implicitly calls {@code toString()} on this type.
 *
 * <p>Primitive {@link #set} overloads and supplier-based overloads use {@link CharArray} appenders or supplier
 * {@code getAs*} methods so primitive watches and counters do not box values before formatting.
 *
 * <p>Java 9 and later give {@link CharSequence} a default {@code chars()} method that returns an {@code IntStream}
 * of UTF-16 code units; this class does not replace that API. Prefer {@link #clear()} plus {@link #concat} overloads
 * to build text without touching {@link CharArray} at call sites. {@link #charBuffer()} remains available for
 * advanced interop with libGDX APIs that require a raw {@link CharArray}.
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
   * {@link me.stringdotjar.flixelgdx.text.FlixelText#destroy()}) but not for per-frame use.
   */
  public void trimToSize() {
    buffer.trimToSize();
  }

  /** @return {@code true} when the buffer contains no characters. */
  public boolean isEmpty() {
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
    buffer.append((int) value);
    return this;
  }

  /** @return {@code this} after replacing content with the decimal rendering of {@code value}. */
  @NotNull
  public FlixelString set(short value) {
    buffer.clear();
    buffer.append((int) value);
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

  /** @return {@code this} after replacing content with the supplied {@code boolean}. */
  @NotNull
  public FlixelString set(@NotNull BooleanSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsBoolean());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code char}. */
  @NotNull
  public FlixelString set(@NotNull CharSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsChar());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code byte}. */
  @NotNull
  public FlixelString set(@NotNull ByteSupplier supplier) {
    buffer.clear();
    buffer.append((int) supplier.getAsByte());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code short}. */
  @NotNull
  public FlixelString set(@NotNull ShortSupplier supplier) {
    buffer.clear();
    buffer.append((int) supplier.getAsShort());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code int}. */
  @NotNull
  public FlixelString set(@NotNull IntSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsInt());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code long}. */
  @NotNull
  public FlixelString set(@NotNull LongSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsLong());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code float}. */
  @NotNull
  public FlixelString set(@NotNull FloatSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsFloat());
    return this;
  }

  /** @return {@code this} after replacing content with the supplied {@code double}. */
  @NotNull
  public FlixelString set(@NotNull DoubleSupplier supplier) {
    buffer.clear();
    buffer.append(supplier.getAsDouble());
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
  public FlixelString concat(@NotNull boolean value) {
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
  public FlixelString concat(@NotNull char value) {
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
  public FlixelString concat(@NotNull byte value) {
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
  public FlixelString concat(@NotNull short value) {
    buffer.append((int) value);
    return this;
  }

  /**
   * Appends the content of {@code value} to the buffer.
   *
   * @param value The int value to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull int value) {
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
  public FlixelString concat(@NotNull long value) {
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
  public FlixelString concat(@NotNull float value) {
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
  public FlixelString concat(@NotNull double value) {
    buffer.append(value);
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link BooleanSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull BooleanSupplier supplier) {
    buffer.append(supplier.getAsBoolean());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link CharSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull CharSupplier supplier) {
    buffer.append(supplier.getAsChar());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link ByteSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull ByteSupplier supplier) {
    buffer.append((int) supplier.getAsByte());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link ShortSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull ShortSupplier supplier) {
    buffer.append((int) supplier.getAsShort());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link IntSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull IntSupplier supplier) {
    buffer.append(supplier.getAsInt());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link LongSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull LongSupplier supplier) {
    buffer.append(supplier.getAsLong());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link FloatSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull FloatSupplier supplier) {
    buffer.append(supplier.getAsFloat());
    return this;
  }

  /**
   * Appends the content of {@code supplier} to the buffer.
   *
   * @param supplier The {@link DoubleSupplier} to append.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelString concat(@NotNull DoubleSupplier supplier) {
    buffer.append(supplier.getAsDouble());
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
