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

/**
 * FlixelGDX's own JSON parser: turns JSON text into a {@link FlixelJsonValue} tree.
 *
 * <p>The parser is a small recursive-descent implementation with no reflection and no
 * platform dependencies, so it behaves identically on desktop, Android, and web. It accepts
 * standard JSON plus two common relaxations found in tool exports: trailing commas and
 * single-quoted strings.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelJsonValue root = FlixelJson.parse(Flixel.files.internal("data/level.json").readString());
 * int gravity = root.getInt("gravity", 600);
 * }</pre>
 */
public final class FlixelJson {

  private FlixelJson() {}

  /**
   * Parses JSON text into a value tree.
   *
   * @param text The JSON document.
   * @return The root value; never {@code null}.
   * @throws IllegalArgumentException If the text is not valid JSON.
   */
  @NotNull
  public static FlixelJsonValue parse(@NotNull String text) {
    Parser parser = new Parser(text);
    FlixelJsonValue root = parser.parseValue();
    parser.skipWhitespace();
    if (!parser.atEnd()) {
      throw parser.error("Unexpected trailing content");
    }
    return root;
  }

  /** Hand-rolled recursive-descent JSON reader over one input string. */
  private static final class Parser {

    @NotNull
    private final String text;

    private int pos;

    Parser(@NotNull String text) {
      this.text = text;
    }

    boolean atEnd() {
      return pos >= text.length();
    }

    void skipWhitespace() {
      while (pos < text.length()) {
        char c = text.charAt(pos);
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
          pos++;
        } else {
          break;
        }
      }
    }

    @NotNull
    FlixelJsonValue parseValue() {
      skipWhitespace();
      if (atEnd()) {
        throw error("Unexpected end of input");
      }
      char c = text.charAt(pos);
      return switch (c) {
        case '{' -> parseObject();
        case '[' -> parseArray();
        case '"', '\'' -> FlixelJsonValue.ofString(parseString());
        case 't', 'f' -> parseBool();
        case 'n' -> parseNull();
        default -> parseNumber();
      };
    }

    @NotNull
    private FlixelJsonValue parseObject() {
      FlixelJsonValue object = new FlixelJsonValue(FlixelJsonValue.Kind.OBJECT);
      pos++;
      skipWhitespace();
      if (peek() == '}') {
        pos++;
        return object;
      }
      while (true) {
        skipWhitespace();
        String name = parseString();
        skipWhitespace();
        expect(':');
        FlixelJsonValue child = parseValue();
        child.setName(name);
        object.addChild(child);
        skipWhitespace();
        char c = peek();
        if (c == ',') {
          pos++;
          skipWhitespace();
          // Tolerate a trailing comma before the closing brace.
          if (peek() == '}') {
            pos++;
            return object;
          }
        } else if (c == '}') {
          pos++;
          return object;
        } else {
          throw error("Expected ',' or '}' in object");
        }
      }
    }

    @NotNull
    private FlixelJsonValue parseArray() {
      FlixelJsonValue array = new FlixelJsonValue(FlixelJsonValue.Kind.ARRAY);
      pos++;
      skipWhitespace();
      if (peek() == ']') {
        pos++;
        return array;
      }
      while (true) {
        array.addChild(parseValue());
        skipWhitespace();
        char c = peek();
        if (c == ',') {
          pos++;
          skipWhitespace();
          if (peek() == ']') {
            pos++;
            return array;
          }
        } else if (c == ']') {
          pos++;
          return array;
        } else {
          throw error("Expected ',' or ']' in array");
        }
      }
    }

    @NotNull
    private String parseString() {
      char quote = peek();
      if (quote != '"' && quote != '\'') {
        throw error("Expected a string");
      }
      pos++;
      StringBuilder out = new StringBuilder(16);
      while (true) {
        if (atEnd()) {
          throw error("Unterminated string");
        }
        char c = text.charAt(pos++);
        if (c == quote) {
          return out.toString();
        }
        if (c != '\\') {
          out.append(c);
          continue;
        }
        if (atEnd()) {
          throw error("Unterminated escape");
        }
        char esc = text.charAt(pos++);
        switch (esc) {
          case '"' -> out.append('"');
          case '\'' -> out.append('\'');
          case '\\' -> out.append('\\');
          case '/' -> out.append('/');
          case 'b' -> out.append('\b');
          case 'f' -> out.append('\f');
          case 'n' -> out.append('\n');
          case 'r' -> out.append('\r');
          case 't' -> out.append('\t');
          case 'u' -> {
            if (pos + 4 > text.length()) {
              throw error("Bad unicode escape");
            }
            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
            pos += 4;
          }
          default -> throw error("Bad escape '\\" + esc + "'");
        }
      }
    }

    @NotNull
    private FlixelJsonValue parseBool() {
      if (text.startsWith("true", pos)) {
        pos += 4;
        return FlixelJsonValue.ofBool(true);
      }
      if (text.startsWith("false", pos)) {
        pos += 5;
        return FlixelJsonValue.ofBool(false);
      }
      throw error("Invalid literal");
    }

    @NotNull
    private FlixelJsonValue parseNull() {
      if (text.startsWith("null", pos)) {
        pos += 4;
        return new FlixelJsonValue(FlixelJsonValue.Kind.NULL);
      }
      throw error("Invalid literal");
    }

    @NotNull
    private FlixelJsonValue parseNumber() {
      int start = pos;
      while (pos < text.length()) {
        char c = text.charAt(pos);
        if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
          pos++;
        } else {
          break;
        }
      }
      if (pos == start) {
        throw error("Expected a value");
      }
      try {
        return FlixelJsonValue.ofNumber(Double.parseDouble(text.substring(start, pos)));
      } catch (NumberFormatException e) {
        throw error("Invalid number");
      }
    }

    private char peek() {
      return atEnd() ? '\0' : text.charAt(pos);
    }

    private void expect(char c) {
      if (peek() != c) {
        throw error("Expected '" + c + "'");
      }
      pos++;
    }

    @NotNull
    IllegalArgumentException error(@NotNull String message) {
      int line = 1;
      for (int i = 0; i < Math.min(pos, text.length()); i++) {
        if (text.charAt(i) == '\n') {
          line++;
        }
      }
      return new IllegalArgumentException(message + " at line " + line + " (offset " + pos + ").");
    }
  }
}
