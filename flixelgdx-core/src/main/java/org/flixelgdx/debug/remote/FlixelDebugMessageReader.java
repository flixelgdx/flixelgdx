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

import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Reads remote debug protocol messages from a received buffer without producing garbage.
 *
 * <p>Think of this class as a finger running down a printed form. You place it at the top of the
 * message, and every {@code read...} call returns the next field and moves the finger past it. Text
 * is copied into a {@link FlixelString} you own and reuse, so reading a string never creates a new
 * object.
 *
 * <p>Every read is bounds checked. A message that ends early throws a
 * {@link FlixelDebugProtocolException}, which the agent should catch and treat as "drop this
 * message". Bytes left over after the last field you read are simply ignored, because newer protocol
 * minor versions may append fields.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugMessageReader r = new FlixelDebugMessageReader(); // Keep one for the whole session.
 * FlixelString text = new FlixelString();                      // Reused scratch space for strings.
 *
 * try {
 *   r.wrap(received);
 *   if (r.readU8() == FlixelDebugProtocol.MSG_PANEL_ACTION) {
 *     int panel = r.readU16();
 *     int action = r.readU8();
 *     if (action == FlixelDebugProtocol.CONSOLE_RUN) {
 *       console.run(r.readString(text));
 *     }
 *   }
 * } catch (FlixelDebugProtocolException e) {
 *   // Corrupt or truncated message: ignore it.
 * }
 * }</pre>
 *
 * <p>This class is not thread safe.
 *
 * @see FlixelDebugMessageWriter
 * @see FlixelDebugProtocol
 */
public class FlixelDebugMessageReader {

  private static final char REPLACEMENT = (char) 0xFFFD;

  private ByteBuffer buffer;

  /** Creates a reader with nothing to read yet. Call {@link #wrap(ByteBuffer)} before reading. */
  public FlixelDebugMessageReader() {}

  /**
   * Creates a reader positioned at the start of a message.
   *
   * @param message The received message; see {@link #wrap(ByteBuffer)}.
   */
  public FlixelDebugMessageReader(@NotNull ByteBuffer message) {
    wrap(message);
  }

  /**
   * Points this reader at a received message so one reader can be reused for every message.
   *
   * <p>Reading starts at the buffer's current position and stops at its limit. The buffer's byte
   * order is switched to little-endian, and reading advances its position.
   *
   * @param message The received message.
   * @return {@code this} for chaining.
   */
  @NotNull
  public FlixelDebugMessageReader wrap(@NotNull ByteBuffer message) {
    buffer = message;
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return this;
  }

  /**
   * Reads an unsigned 8-bit integer.
   *
   * @return A value from 0 to 255.
   */
  public int readU8() {
    need(1);
    return buffer.get() & 0xFF;
  }

  /**
   * Reads an unsigned 16-bit integer.
   *
   * @return A value from 0 to 65535.
   */
  public int readU16() {
    need(2);
    return buffer.getShort() & 0xFFFF;
  }

  /**
   * Reads a signed 32-bit integer.
   *
   * @return The value.
   */
  public int readI32() {
    need(4);
    return buffer.getInt();
  }

  /**
   * Reads a signed 64-bit integer.
   *
   * @return The value.
   */
  public long readI64() {
    need(8);
    return buffer.getLong();
  }

  /**
   * Reads a 32-bit float.
   *
   * @return The value.
   */
  public float readF32() {
    need(4);
    return buffer.getFloat();
  }

  /**
   * Reads a 64-bit float.
   *
   * @return The value.
   */
  public double readF64() {
    need(8);
    return buffer.getDouble();
  }

  /**
   * Reads a boolean stored as one byte.
   *
   * @return {@code true} for any non-zero byte.
   */
  public boolean readBool() {
    return readU8() != 0;
  }

  /**
   * Reads the {@code u16} element count at the start of an array.
   *
   * @return The number of elements that follow.
   */
  public int readCount() {
    return readU16();
  }

  /**
   * Reads a string into a reusable buffer.
   *
   * <p>The buffer is cleared first. Malformed UTF-8 sequences become U+FFFD.
   *
   * @param out Receives the decoded text.
   * @return {@code out} for chaining.
   */
  @NotNull
  public FlixelString readString(@NotNull FlixelString out) {
    int length = readU16();
    need(length);
    out.clear();
    int end = buffer.position() + length;
    while (buffer.position() < end) {
      int b = buffer.get() & 0xFF;
      if (b < 0x80) {
        out.concat((char) b);
        continue;
      }
      int extra = b >= 0xF0 ? 3 : b >= 0xE0 ? 2 : b >= 0xC0 ? 1 : -1;
      if (extra < 0 || b > 0xF4 || buffer.position() + extra > end) {
        out.concat(REPLACEMENT);
        if (extra > 0) {
          buffer.position(end);
        }
        continue;
      }
      int cp = b & (0x3F >> extra);
      boolean ok = true;
      for (int i = 0; i < extra; i++) {
        int next = buffer.get() & 0xFF;
        if ((next & 0xC0) != 0x80) {
          ok = false;
        }
        cp = (cp << 6) | (next & 0x3F);
      }
      if (!ok || !Character.isValidCodePoint(cp)) {
        out.concat(REPLACEMENT);
      } else if (cp >= 0x10000) {
        out.concat(Character.highSurrogate(cp));
        out.concat(Character.lowSurrogate(cp));
      } else {
        out.concat((char) cp);
      }
    }
    return out;
  }

  /**
   * Reads the {@code i32} length at the start of a byte block and checks the bytes are present.
   *
   * <p>Follow it with {@link #readBytes(byte[], int, int)} using the returned length.
   *
   * @return The number of bytes in the block.
   */
  public int readBytesLength() {
    int length = readI32();
    if (length < 0) {
      throw new FlixelDebugProtocolException("Negative byte block length: " + length);
    }
    need(length);
    return length;
  }

  /**
   * Copies bytes out of the message.
   *
   * @param dst Destination array.
   * @param offset Index of the first byte to fill.
   * @param length How many bytes to copy, usually the value from {@link #readBytesLength()}.
   */
  public void readBytes(@NotNull byte[] dst, int offset, int length) {
    need(length);
    buffer.get(dst, offset, length);
  }

  /**
   * Skips bytes without reading them.
   *
   * @param count How many bytes to skip.
   */
  public void skip(int count) {
    need(count);
    buffer.position(buffer.position() + count);
  }

  /**
   * Skips one value (its kind byte and payload), including a whole list with everything inside it.
   *
   * @param scratch Kept for callers written against older versions; skipping no longer needs it.
   */
  public void skipValue(@NotNull FlixelString scratch) {
    skipValue();
  }

  /**
   * Skips one value (its kind byte and payload), including a whole list with everything inside it.
   *
   * <p>Lists are skipped item by item without recursion depth limits beyond the message itself, and nothing is
   * allocated.
   *
   * @throws FlixelDebugProtocolException If the message is too short or a kind is unknown.
   */
  public void skipValue() {
    int pending = 1;
    while (pending > 0) {
      pending--;
      int kind = readU8();
      switch (kind) {
        case FlixelDebugProtocol.VALUE_BOOL -> skip(1);
        case FlixelDebugProtocol.VALUE_INT, FlixelDebugProtocol.VALUE_FLOAT -> skip(4);
        case FlixelDebugProtocol.VALUE_LONG, FlixelDebugProtocol.VALUE_DOUBLE -> skip(8);
        case FlixelDebugProtocol.VALUE_STRING, FlixelDebugProtocol.VALUE_ERROR -> skip(readU16());
        case FlixelDebugProtocol.VALUE_NULL -> {
        }
        case FlixelDebugProtocol.VALUE_BYTES -> skip(readBytesLength());
        case FlixelDebugProtocol.VALUE_LIST -> pending += readCount();
        default -> throw new FlixelDebugProtocolException("Unknown value kind: " + kind);
      }
    }
  }

  private void need(int count) {
    if (count < 0 || buffer.remaining() < count) {
      throw new FlixelDebugProtocolException(
          "Message too short: needed " + count + " bytes but only " + buffer.remaining() + " remain");
    }
  }

  /**
   * Returns how many bytes are left unread.
   *
   * @return The remaining byte count.
   */
  public int getRemaining() {
    return buffer.remaining();
  }
}
