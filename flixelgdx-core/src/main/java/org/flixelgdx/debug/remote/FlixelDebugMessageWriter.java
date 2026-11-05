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
package org.flixelgdx.debug.remote;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Writes remote debug protocol messages into one reusable little-endian buffer without producing
 * garbage.
 *
 * <p>Think of this class as a single sheet of paper on a clipboard. You start a message (a fresh
 * page), fill in the fields in order, then hand the page to the transport. For the next message you
 * wipe the page and reuse it. The page only gets replaced by a bigger one when a message does not fit,
 * and the new page is twice as big, so after a short warm-up no write allocates anything.
 *
 * <p>Arrays are written with {@link #beginArray()} and {@link #endArray(int)}: the writer reserves the
 * two count bytes, you write the elements, then {@code endArray(count)} goes back and fills the
 * count in. Arrays may be nested up to {@link #MAX_DEPTH} levels.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(); // Keep one for the whole session.
 *
 * w.beginMessage(FlixelDebugProtocol.MSG_PANEL_DATA);
 * w.writeU16(panelIndex);
 * w.beginArray();
 * for (int i = 0; i < rows; i++) {
 *   w.writeString(names[i]);
 *   w.writeValue(values[i]);
 * }
 * w.endArray(rows);
 * ByteBuffer message = w.finish(); // Valid until the next beginMessage(...).
 * transport.send(message);
 * }</pre>
 *
 * <p>This class is not thread safe. Use one writer per sending thread.
 *
 * @see FlixelDebugMessageReader
 * @see FlixelDebugProtocol
 */
public class FlixelDebugMessageWriter {

  /** Deepest allowed nesting of {@link #beginArray()} calls. */
  public static final int MAX_DEPTH = 16;

  private static final int DEFAULT_CAPACITY = 4096;

  private ByteBuffer buffer;
  private final int[] arrayStarts = new int[MAX_DEPTH];
  private int depth;

  /** Creates a writer with a 4 KB starting buffer. */
  public FlixelDebugMessageWriter() {
    this(DEFAULT_CAPACITY);
  }

  /**
   * Creates a writer with the given starting buffer size.
   *
   * @param capacity Starting size in bytes. Values below 16 are raised to 16.
   */
  public FlixelDebugMessageWriter(int capacity) {
    buffer = ByteBuffer.allocate(Math.max(16, capacity)).order(ByteOrder.LITTLE_ENDIAN);
  }

  /**
   * Discards anything written so far and starts a new message.
   *
   * @param type The message type, one of the {@code MSG_*} constants.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter beginMessage(int type) {
    buffer.clear();
    depth = 0;
    return writeU8(type);
  }

  /**
   * Ends the message and returns the buffer ready to send.
   *
   * <p>The returned buffer is the writer's own buffer, positioned at 0 with its limit at the message
   * end. It stays valid until the next call to {@link #beginMessage(int)}.
   *
   * @return The finished message.
   * @throws FlixelDebugProtocolException If an array was started and never ended.
   */
  @NotNull
  public ByteBuffer finish() {
    if (depth != 0) {
      throw new FlixelDebugProtocolException("Unfinished array: endArray() was not called");
    }
    buffer.flip();
    return buffer;
  }

  /**
   * Starts a counted array by reserving its {@code u16} count.
   *
   * @return {@code this} for chaining.
   * @throws FlixelDebugProtocolException If arrays are nested deeper than {@link #MAX_DEPTH}.
   */
  @NotNull
  public FlixelDebugMessageWriter beginArray() {
    if (depth >= MAX_DEPTH) {
      throw new FlixelDebugProtocolException("Arrays nested deeper than " + MAX_DEPTH);
    }
    ensure(2);
    arrayStarts[depth++] = buffer.position();
    buffer.putShort((short) 0);
    return this;
  }

  /**
   * Ends the innermost array and fills in its element count.
   *
   * @param count How many elements were written since the matching {@link #beginArray()}.
   * @return {@code this} for chaining.
   * @throws FlixelDebugProtocolException If there is no open array or {@code count} is not 0 to 65535.
   */
  @NotNull
  public FlixelDebugMessageWriter endArray(int count) {
    if (depth == 0) {
      throw new FlixelDebugProtocolException("endArray() without beginArray()");
    }
    if (count < 0 || count > FlixelDebugProtocol.MAX_ARRAY_COUNT) {
      throw new FlixelDebugProtocolException("Array count out of range: " + count);
    }
    buffer.putShort(arrayStarts[--depth], (short) count);
    return this;
  }

  /**
   * Writes an unsigned 8-bit integer.
   *
   * @param value The value; only the low 8 bits are kept.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeU8(int value) {
    ensure(1);
    buffer.put((byte) value);
    return this;
  }

  /**
   * Writes an unsigned 16-bit integer.
   *
   * @param value The value; only the low 16 bits are kept.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeU16(int value) {
    ensure(2);
    buffer.putShort((short) value);
    return this;
  }

  /**
   * Writes a signed 32-bit integer.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeI32(int value) {
    ensure(4);
    buffer.putInt(value);
    return this;
  }

  /**
   * Writes a signed 64-bit integer.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeI64(long value) {
    ensure(8);
    buffer.putLong(value);
    return this;
  }

  /**
   * Writes a 32-bit float.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeF32(float value) {
    ensure(4);
    buffer.putFloat(value);
    return this;
  }

  /**
   * Writes a 64-bit float.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeF64(double value) {
    ensure(8);
    buffer.putDouble(value);
    return this;
  }

  /**
   * Writes a boolean as one byte, 0 or 1.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeBool(boolean value) {
    return writeU8(value ? 1 : 0);
  }

  /**
   * Writes a string as a {@code u16} byte length followed by UTF-8 bytes.
   *
   * <p>The UTF-8 encoding is done by hand, so no temporary array is created. Strings that need more
   * than 65535 bytes are cut at the last complete character that fits, so the output is always valid
   * UTF-8. Unpaired surrogates are written as {@code '?'}.
   *
   * @param text The text to write; {@code null} is written as an empty string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeString(@Nullable CharSequence text) {
    if (text == null) {
      return writeU16(0);
    }
    int length = text.length();
    int bytes = 0;
    int end = 0;
    while (end < length) {
      char c = text.charAt(end);
      int width;
      int chars = 1;
      if (c < 0x80) {
        width = 1;
      } else if (c < 0x800) {
        width = 2;
      } else if (Character.isHighSurrogate(c) && end + 1 < length && Character.isLowSurrogate(text.charAt(end + 1))) {
        width = 4;
        chars = 2;
      } else if (Character.isSurrogate(c)) {
        width = 1;
      } else {
        width = 3;
      }
      if (bytes + width > FlixelDebugProtocol.MAX_STRING_BYTES) {
        break;
      }
      bytes += width;
      end += chars;
    }
    ensure(2 + bytes);
    buffer.putShort((short) bytes);
    for (int i = 0; i < end; i++) {
      char c = text.charAt(i);
      if (c < 0x80) {
        buffer.put((byte) c);
      } else if (c < 0x800) {
        buffer.put((byte) (0xC0 | (c >> 6)));
        buffer.put((byte) (0x80 | (c & 0x3F)));
      } else if (Character.isHighSurrogate(c) && i + 1 < end && Character.isLowSurrogate(text.charAt(i + 1))) {
        int cp = Character.toCodePoint(c, text.charAt(++i));
        buffer.put((byte) (0xF0 | (cp >> 18)));
        buffer.put((byte) (0x80 | ((cp >> 12) & 0x3F)));
        buffer.put((byte) (0x80 | ((cp >> 6) & 0x3F)));
        buffer.put((byte) (0x80 | (cp & 0x3F)));
      } else if (Character.isSurrogate(c)) {
        buffer.put((byte) '?');
      } else {
        buffer.put((byte) (0xE0 | (c >> 12)));
        buffer.put((byte) (0x80 | ((c >> 6) & 0x3F)));
        buffer.put((byte) (0x80 | (c & 0x3F)));
      }
    }
    return this;
  }

  /**
   * Writes a byte block as an {@code i32} length followed by the raw bytes.
   *
   * @param data The source array.
   * @param offset Index of the first byte to write.
   * @param length How many bytes to write.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeBytes(@NotNull byte[] data, int offset, int length) {
    ensure(4 + length);
    buffer.putInt(length);
    buffer.put(data, offset, length);
    return this;
  }

  /**
   * Writes a byte block as an {@code i32} length followed by the raw bytes.
   *
   * @param data The whole array is written.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeBytes(@NotNull byte[] data) {
    return writeBytes(data, 0, data.length);
  }

  /**
   * Writes a byte block taken from the remaining bytes of {@code data}. The source position advances.
   *
   * @param data The source buffer.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeBytes(@NotNull ByteBuffer data) {
    int length = data.remaining();
    ensure(4 + length);
    buffer.putInt(length);
    buffer.put(data);
    return this;
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_BOOL}.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(boolean value) {
    return writeU8(FlixelDebugProtocol.VALUE_BOOL).writeBool(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_INT}.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(int value) {
    return writeU8(FlixelDebugProtocol.VALUE_INT).writeI32(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_LONG}.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(long value) {
    return writeU8(FlixelDebugProtocol.VALUE_LONG).writeI64(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_FLOAT}.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(float value) {
    return writeU8(FlixelDebugProtocol.VALUE_FLOAT).writeF32(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_DOUBLE}.
   *
   * @param value The value.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(double value) {
    return writeU8(FlixelDebugProtocol.VALUE_DOUBLE).writeF64(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_STRING}.
   *
   * @param value The text; {@code null} is written as an empty string.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeValue(@Nullable CharSequence value) {
    return writeU8(FlixelDebugProtocol.VALUE_STRING).writeString(value);
  }

  /**
   * Writes a property value of kind {@link FlixelDebugProtocol#VALUE_ERROR}, used when reading a
   * value threw an exception.
   *
   * @param message A short description of the failure.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageWriter writeErrorValue(@Nullable CharSequence message) {
    return writeU8(FlixelDebugProtocol.VALUE_ERROR).writeString(message);
  }

  private void ensure(int extra) {
    if (buffer.remaining() >= extra) {
      return;
    }
    long needed = (long) buffer.position() + extra;
    long capacity = buffer.capacity();
    while (capacity < needed) {
      capacity <<= 1;
    }
    if (capacity > Integer.MAX_VALUE - 8) {
      throw new FlixelDebugProtocolException("Message too large");
    }
    ByteBuffer bigger = ByteBuffer.allocate((int) capacity).order(ByteOrder.LITTLE_ENDIAN);
    buffer.flip();
    bigger.put(buffer);
    buffer = bigger;
  }

  /**
   * Returns how many bytes the current message holds so far.
   *
   * @return The write position.
   */
  public int getSize() {
    return buffer.position();
  }

  /**
   * Returns the current buffer size in bytes.
   *
   * @return The capacity, which only ever grows.
   */
  public int getCapacity() {
    return buffer.capacity();
  }
}
