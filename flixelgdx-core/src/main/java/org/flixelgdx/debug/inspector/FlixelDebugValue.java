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
package org.flixelgdx.debug.inspector;

import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.remote.FlixelDebugProtocolException;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * A value that the debugger app sent for a property, decoded into a reusable holder.
 *
 * <p>Think of it as a small tray that one value at a time is placed on. The panel keeps a single tray and
 * {@linkplain #read(FlixelDebugMessageReader) reloads} it for every edit, so reading what the app sent never
 * creates garbage. A property then looks at the {@linkplain #getKind() kind} of what is on the tray and takes
 * what it needs.
 *
 * <p>Numbers are stored both as a {@code long} and as a {@code double}, so any numeric kind can be handed to any
 * numeric property: {@link #asInt()}, {@link #asLong()}, {@link #asFloat()}, and {@link #asDouble()} convert the
 * stored number the way a Java cast would.
 *
 * @see FlixelDebugProperty
 */
public final class FlixelDebugValue {

  private long longValue;
  private double doubleValue;

  private int kind;

  private final FlixelString text = new FlixelString(32);

  /**
   * Reads a value (a kind byte followed by its data) from a message, replacing what this holder had.
   *
   * @param reader The reader positioned at the value's kind byte.
   * @return This holder, for chaining.
   * @throws FlixelDebugProtocolException If the message is too short or the kind is
   *     unknown.
   */
  @NotNull
  public FlixelDebugValue read(@NotNull FlixelDebugMessageReader reader) {
    int k = reader.readU8();
    switch (k) {
      case FlixelDebugProtocol.VALUE_BOOL -> {
        longValue = reader.readBool() ? 1L : 0L;
        doubleValue = longValue;
      }
      case FlixelDebugProtocol.VALUE_INT -> {
        longValue = reader.readI32();
        doubleValue = longValue;
      }
      case FlixelDebugProtocol.VALUE_LONG -> {
        longValue = reader.readI64();
        doubleValue = longValue;
      }
      case FlixelDebugProtocol.VALUE_FLOAT -> {
        doubleValue = reader.readF32();
        longValue = (long) doubleValue;
      }
      case FlixelDebugProtocol.VALUE_DOUBLE -> {
        doubleValue = reader.readF64();
        longValue = (long) doubleValue;
      }
      case FlixelDebugProtocol.VALUE_STRING, FlixelDebugProtocol.VALUE_ERROR -> {
        reader.readString(text);
        longValue = 0L;
        doubleValue = 0.0;
      }
      case FlixelDebugProtocol.VALUE_NULL -> {
        longValue = 0L;
        doubleValue = 0.0;
      }
      case FlixelDebugProtocol.VALUE_BYTES, FlixelDebugProtocol.VALUE_LIST -> {
        // A holder only keeps simple values, so the content of bytes and lists is skipped. The kind still tells
        // the caller what arrived.
        skipContent(reader, k);
        longValue = 0L;
        doubleValue = 0.0;
      }
      default -> throw new FlixelDebugProtocolException("Unknown value kind: " + k);
    }
    kind = k;
    return this;
  }

  /**
   * Returns whether the holder is the {@code NULL} value, which events use for "nothing", such as a cleared
   * selection.
   *
   * @return {@code true} for {@code NULL}.
   */
  public boolean isNull() {
    return kind == FlixelDebugProtocol.VALUE_NULL;
  }

  /**
   * Sets this holder to a text value. Handy for tests and for code that builds a value by hand.
   *
   * @param value The text to hold.
   * @return This holder, for chaining.
   */
  @NotNull
  public FlixelDebugValue setString(@NotNull CharSequence value) {
    text.set(value);
    kind = FlixelDebugProtocol.VALUE_STRING;
    return this;
  }

  /**
   * Returns whether the held value is one of the numeric kinds (int, long, float, or double).
   *
   * @return {@code true} for a number.
   */
  public boolean isNumber() {
    return kind == FlixelDebugProtocol.VALUE_INT
        || kind == FlixelDebugProtocol.VALUE_LONG
        || kind == FlixelDebugProtocol.VALUE_FLOAT
        || kind == FlixelDebugProtocol.VALUE_DOUBLE;
  }

  /**
   * Returns the held number as an {@code int}, truncating like a Java cast.
   *
   * @return The number as an int.
   */
  public int asInt() {
    return (int) (kind == FlixelDebugProtocol.VALUE_FLOAT || kind == FlixelDebugProtocol.VALUE_DOUBLE
        ? (long) doubleValue : longValue);
  }

  /**
   * Returns the held number as a {@code long}, truncating like a Java cast.
   *
   * @return The number as a long.
   */
  public long asLong() {
    return longValue;
  }

  /**
   * Returns the held number as a {@code float}.
   *
   * @return The number as a float.
   */
  public float asFloat() {
    return (float) doubleValue;
  }

  /**
   * Returns the held number as a {@code double}.
   *
   * @return The number as a double.
   */
  public double asDouble() {
    return doubleValue;
  }

  /**
   * Returns the held flag. Only meaningful when {@link #getKind()} is {@link FlixelDebugProtocol#VALUE_BOOL}.
   *
   * @return The flag.
   */
  public boolean asBoolean() {
    return longValue != 0L;
  }

  private static void skipContent(FlixelDebugMessageReader reader, int kind) {
    if (kind == FlixelDebugProtocol.VALUE_BYTES) {
      reader.skip(reader.readBytesLength());
      return;
    }
    for (int i = 0, n = reader.readCount(); i < n; i++) {
      reader.skipValue();
    }
  }

  /**
   * Returns the held text. Only meaningful when {@link #getKind()} is {@link FlixelDebugProtocol#VALUE_STRING}.
   * The buffer is reused by the next {@link #read(FlixelDebugMessageReader)}, so call {@code toString()} to
   * keep the text.
   *
   * @return The reused text buffer.
   */
  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Returns the kind of the held value.
   *
   * @return One of the {@code VALUE_*} constants of {@link FlixelDebugProtocol}, or 0 before the first read.
   */
  public int getKind() {
    return kind;
  }
}
