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

import org.flixelgdx.collections.FlixelByteArray;
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
 * <p>The writer tracks the exact shape of the document it is building (whether it is inside an
 * object, inside an array, or waiting for a value after {@link #name(String)}), so a call made out
 * of order throws immediately instead of silently producing broken JSON. For example, closing an
 * object twice, or writing two values in a row for one field name, both throw
 * {@link IllegalStateException} right away rather than letting bad output slip through.
 *
 * <p>It is a one-shot serialization helper (for save files, settings, network payloads), not a
 * per-frame path, so it uses a {@link StringBuilder} internally for clarity. Build one object, read
 * {@link #toString()}, and discard it.
 *
 * <p>Compact example (the default):
 * <pre>{@code
 * String json = new FlixelJsonWriter()
 *     .beginObject()
 *     .name("score").value(1200)
 *     .name("name").value("Ada")
 *     .endObject()
 *     .toString();
 * // -> {"score":1200,"name":"Ada"}
 * }</pre>
 *
 * <p>Pretty-print example (indent = 2 spaces):
 * <pre>{@code
 * String json = new FlixelJsonWriter()
 *     .setIndent(2)
 *     .beginObject()
 *     .name("score").value(1200)
 *     .name("name").value("Ada")
 *     .endObject()
 *     .toString();
 * // -> {
 * //      "score": 1200,
 * //      "name": "Ada"
 * //    }
 * }</pre>
 */
public final class FlixelJsonWriter {

  /** Nothing has been written yet; a single top-level value may still start the document. */
  private static final byte EMPTY_DOCUMENT = 0;

  /** A top-level value has already been written; no further top-level value is allowed. */
  private static final byte NONEMPTY_DOCUMENT = 1;

  /** Inside an array, no elements have been written yet. */
  private static final byte EMPTY_ARRAY = 2;

  /** Inside an array, at least one element has already been written. */
  private static final byte NONEMPTY_ARRAY = 3;

  /** Inside an object, no fields have been written yet. */
  private static final byte EMPTY_OBJECT = 4;

  /** Inside an object, {@link #name(String)} was called and its value is still pending. */
  private static final byte DANGLING_NAME = 5;

  /** Inside an object, at least one complete field has already been written. */
  private static final byte NONEMPTY_OBJECT = 6;

  @NotNull
  private final StringBuilder out = new StringBuilder(64);

  /**
   * A stack of the container states above, one entry per nesting level plus a bottom entry for the
   * top-level document. The top entry always reflects what is legal to write next.
   */
  @NotNull
  private final FlixelByteArray scopes = new FlixelByteArray(8);

  private int indent;

  /**
   * Creates a writer for a brand-new, empty document.
   */
  public FlixelJsonWriter() {
    scopes.add(EMPTY_DOCUMENT);
  }

  /**
   * Enables pretty-print indentation.
   *
   * <p>By default the writer produces compact JSON with no whitespace. Setting a positive indent
   * turns on pretty-printing: each nesting level is indented by {@code spaces} more spaces, keys
   * get a space after the colon, and opening/closing brackets are placed on their own lines. Set
   * to 0 (the default) to return to compact output.
   *
   * <p>This method must be called before writing any output, because whitespace emitted earlier
   * cannot be retroactively adjusted.
   *
   * @param spaces The number of spaces per indentation level. Must be 0 or greater.
   * @return This writer, for method chaining.
   * @throws IllegalArgumentException If {@code spaces} is negative.
   * @throws IllegalStateException If this writer has already written output (a container was
   *     opened, a value was written, or a name was started).
   */
  @NotNull
  public FlixelJsonWriter setIndent(int spaces) {
    if (spaces < 0) {
      throw new IllegalArgumentException("Indent must be >= 0, got " + spaces);
    }
    if (out.length() > 0) {
      throw new IllegalStateException(
          "setIndent(...) must be called before any output is written.");
    }
    indent = spaces;
    return this;
  }

  /**
   * Opens a JSON object.
   *
   * @return This writer, for method chaining.
   * @throws IllegalStateException If an object cannot legally start here, for example directly
   *     inside another object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter beginObject() {
    beforeValue();
    out.append('{');
    scopes.add(EMPTY_OBJECT);
    return this;
  }

  /**
   * Closes the current JSON object.
   *
   * @return This writer, for method chaining.
   * @throws IllegalStateException If the innermost open container is not an object, or a
   *     {@link #name(String)} call is still waiting for its value.
   */
  @NotNull
  public FlixelJsonWriter endObject() {
    byte context = scopes.peek();
    if (context == DANGLING_NAME) {
      throw new IllegalStateException(
          "endObject() was called while name(...) is still waiting for its value.");
    }
    if (context != EMPTY_OBJECT && context != NONEMPTY_OBJECT) {
      throw new IllegalStateException(
          "endObject() was called but the innermost open container is not an object.");
    }
    scopes.pop();
    if (indent > 0) {
      out.append('\n');
      appendIndent(currentDepth());
    }
    out.append('}');
    return this;
  }

  /**
   * Opens a JSON array.
   *
   * @return This writer, for method chaining.
   * @throws IllegalStateException If an array cannot legally start here, for example directly
   *     inside another object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter beginArray() {
    beforeValue();
    out.append('[');
    scopes.add(EMPTY_ARRAY);
    return this;
  }

  /**
   * Closes the current JSON array.
   *
   * @return This writer, for method chaining.
   * @throws IllegalStateException If the innermost open container is not an array.
   */
  @NotNull
  public FlixelJsonWriter endArray() {
    byte context = scopes.peek();
    if (context != EMPTY_ARRAY && context != NONEMPTY_ARRAY) {
      throw new IllegalStateException(
          "endArray() was called but the innermost open container is not an array.");
    }
    scopes.pop();
    if (indent > 0) {
      out.append('\n');
      appendIndent(currentDepth());
    }
    out.append(']');
    return this;
  }

  /**
   * Writes a field name inside the current object. The next {@code value}/{@code raw}/{@code begin*}
   * call supplies its value.
   *
   * @param name The field name.
   * @return This writer.
   * @throws IllegalStateException If the innermost open container is not an object, or
   *     {@code name(...)} was already called and is still waiting for its value.
   */
  @NotNull
  public FlixelJsonWriter name(@NotNull String name) {
    byte context = scopes.peek();
    if (context == DANGLING_NAME) {
      throw new IllegalStateException(
          "name(...) was already called for a field; write its value before naming another one.");
    }
    if (context != EMPTY_OBJECT && context != NONEMPTY_OBJECT) {
      throw new IllegalStateException(
          "name(...) can only be called directly inside an object opened with beginObject().");
    }
    if (context == NONEMPTY_OBJECT) {
      out.append(',');
    }
    if (indent > 0) {
      out.append('\n');
      appendIndent(currentDepth());
    }
    out.append('"');
    escape(name);
    out.append('"');
    out.append(':');
    if (indent > 0) {
      out.append(' ');
    }
    scopes.set(scopes.getSize() - 1, DANGLING_NAME);
    return this;
  }

  /**
   * Writes a string value, or {@code null} when {@code value} is {@code null}.
   *
   * @param value The string to write, or {@code null} to emit a JSON {@code null}.
   * @return This writer, for method chaining.
   * @throws IllegalStateException If a value cannot legally be written here, for example directly
   *     inside an object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter value(@Nullable String value) {
    beforeValue();
    if (value == null) {
      out.append("null");
    } else {
      out.append('"');
      escape(value);
      out.append('"');
    }
    return this;
  }

  /**
   * Writes an integer value.
   *
   * @param value The long integer to write.
   * @return This writer, for method chaining.
   * @throws IllegalStateException If a value cannot legally be written here, for example directly
   *     inside an object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter value(long value) {
    beforeValue();
    out.append(value);
    return this;
  }

  /**
   * Writes a floating-point value.
   *
   * @param value The double to write. Must be finite, since {@code NaN} and infinite values have
   *     no representation in JSON.
   * @return This writer, for method chaining.
   * @throws IllegalArgumentException If {@code value} is {@code NaN} or infinite.
   * @throws IllegalStateException If a value cannot legally be written here, for example directly
   *     inside an object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter value(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(
          "Cannot write a non-finite double as JSON (NaN and Infinity have no JSON representation): "
              + value);
    }
    beforeValue();
    out.append(value);
    return this;
  }

  /**
   * Writes a boolean value.
   *
   * @param value The boolean to write.
   * @return This writer, for method chaining.
   * @throws IllegalStateException If a value cannot legally be written here, for example directly
   *     inside an object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter value(boolean value) {
    beforeValue();
    out.append(value);
    return this;
  }

  /**
   * Writes an already-serialized JSON fragment verbatim (for nested objects). Pass {@code "null"}
   * for an absent value.
   *
   * @param json A valid JSON fragment.
   * @return This writer.
   * @throws IllegalStateException If a value cannot legally be written here, for example directly
   *     inside an object without a preceding {@link #name(String)}, or after the document is
   *     already complete.
   */
  @NotNull
  public FlixelJsonWriter raw(@NotNull String json) {
    beforeValue();
    out.append(json);
    return this;
  }

  /**
   * Reports whether this writer has produced one complete, self-contained JSON document.
   *
   * <p>This is {@code true} once the single top-level value (an object, an array, or a lone
   * scalar) has been fully closed, and {@code false} while a container is still open or nothing has
   * been written yet.
   *
   * @return {@code true} if {@link #toString()} currently holds a complete document.
   */
  public boolean isComplete() {
    return scopes.getSize() == 1 && scopes.peek() == NONEMPTY_DOCUMENT;
  }

  @NotNull
  @Override
  public String toString() {
    return out.toString();
  }

  /**
   * Validates that a value may legally be written next, inserts the separating comma or newline it
   * needs, and advances the current container's state to reflect that the value is now present.
   *
   * @throws IllegalStateException If the innermost scope cannot accept a value right now (it is an
   *     object waiting for a field name, or the document already holds its one top-level value).
   */
  private void beforeValue() {
    byte context = scopes.peek();
    switch (context) {
      case EMPTY_DOCUMENT -> scopes.set(scopes.getSize() - 1, NONEMPTY_DOCUMENT);
      case NONEMPTY_DOCUMENT -> throw new IllegalStateException(
          "Cannot write more than one top-level value; the document is already complete.");
      case EMPTY_ARRAY -> {
        scopes.set(scopes.getSize() - 1, NONEMPTY_ARRAY);
        if (indent > 0) {
          out.append('\n');
          appendIndent(currentDepth());
        }
      }
      case NONEMPTY_ARRAY -> {
        out.append(',');
        if (indent > 0) {
          out.append('\n');
          appendIndent(currentDepth());
        }
      }
      case DANGLING_NAME -> scopes.set(scopes.getSize() - 1, NONEMPTY_OBJECT);
      default -> throw new IllegalStateException(
          "Cannot write a value directly inside an object; call name(...) first to start a field.");
    }
  }

  /** Returns the number of currently open containers (objects and arrays). */
  private int currentDepth() {
    return scopes.getSize() - 1;
  }

  /** Appends {@code depth * indent} spaces to give the current nesting its proper visual offset. */
  private void appendIndent(int depth) {
    int spaces = depth * indent;
    for (int i = 0; i < spaces; i++) {
      out.append(' ');
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
