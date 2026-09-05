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
package org.flixelgdx.json;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A tiny streaming JSON text builder.
 *
 * <p>This is the write side of the JSON layer: {@link FlixelJson} parses text into a
 * {@link FlixelJsonValue} tree, and this builds a JSON string back up. It is what the
 * {@link JsonSerializable} annotation processor emits calls to, so game code rarely creates one
 * directly. Commas between fields are inserted automatically as values are written.
 *
 * <p>It is a one-shot serialization helper (for save files, settings, network payloads), not a
 * per-frame path, so it uses a {@link StringBuilder} internally for clarity. Build one object, read
 * {@link #toString()}, and discard it.
 *
 * <p>Example:
 * <pre>{@code
 * String json = new FlixelJsonWriter()
 *     .beginObject()
 *     .name("score").value(1200)
 *     .name("name").value("Ada")
 *     .endObject()
 *     .toString();
 * // -> {"score":1200,"name":"Ada"}
 * }</pre>
 */
public final class FlixelJsonWriter {

  @NotNull
  private final StringBuilder out = new StringBuilder(64);

  /** Whether the next {@link #name(String)} must be preceded by a comma. */
  private boolean needComma;

  /**
   * Opens a JSON object.
   *
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter beginObject() {
    separateValue();
    out.append('{');
    needComma = false;
    return this;
  }

  /**
   * Closes the current JSON object.
   *
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter endObject() {
    out.append('}');
    needComma = true;
    return this;
  }

  /**
   * Opens a JSON array.
   *
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter beginArray() {
    separateValue();
    out.append('[');
    needComma = false;
    return this;
  }

  /**
   * Closes the current JSON array.
   *
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter endArray() {
    out.append(']');
    needComma = true;
    return this;
  }

  /**
   * Writes a field name inside the current object. The next {@code value}/{@code raw}/{@code begin*}
   * call supplies its value.
   *
   * @param name The field name.
   * @return This writer.
   */
  @NotNull
  public FlixelJsonWriter name(@NotNull String name) {
    if (needComma) {
      out.append(',');
    }
    out.append('"');
    escape(name);
    out.append("\":");
    needComma = false;
    return this;
  }

  /**
   * Writes a string value, or {@code null} when {@code value} is {@code null}.
   *
   * @param value The string to write, or {@code null} to emit a JSON {@code null}.
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter value(@Nullable String value) {
    separateValue();
    if (value == null) {
      out.append("null");
    } else {
      out.append('"');
      escape(value);
      out.append('"');
    }
    needComma = true;
    return this;
  }

  /**
   * Writes an integer value.
   *
   * @param value The long integer to write.
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter value(long value) {
    separateValue();
    out.append(value);
    needComma = true;
    return this;
  }

  /**
   * Writes a floating-point value.
   *
   * @param value The double to write.
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter value(double value) {
    separateValue();
    out.append(value);
    needComma = true;
    return this;
  }

  /**
   * Writes a boolean value.
   *
   * @param value The boolean to write.
   * @return This writer, for method chaining.
   */
  @NotNull
  public FlixelJsonWriter value(boolean value) {
    separateValue();
    out.append(value);
    needComma = true;
    return this;
  }

  /**
   * Writes an already-serialized JSON fragment verbatim (for nested objects). Pass {@code "null"}
   * for an absent value.
   *
   * @param json A valid JSON fragment.
   * @return This writer.
   */
  @NotNull
  public FlixelJsonWriter raw(@NotNull String json) {
    separateValue();
    out.append(json);
    needComma = true;
    return this;
  }

  @NotNull
  @Override
  public String toString() {
    return out.toString();
  }

  /** Inserts a separating comma before an array element (a value written while inside an array). */
  private void separateValue() {
    int len = out.length();
    if (needComma && len > 0 && out.charAt(len - 1) != ':') {
      out.append(',');
      needComma = false;
    }
  }

  /** Appends {@code s} with the JSON string escapes applied. */
  private void escape(@NotNull String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
  }
}
