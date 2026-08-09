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
package org.flixelgdx.xml;

import org.jetbrains.annotations.NotNull;

/**
 * FlixelGDX's own minimal XML parser, sized for data files such as Sparrow texture atlases.
 *
 * <p>It reads elements, attributes, and text, and skips declarations, comments, DOCTYPEs, and
 * CDATA framing. It is not a validating parser and does not process namespaces; game data
 * formats do not need either, and keeping it small keeps it identical on every platform.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelXmlElement atlas = FlixelXml.parse(Flixel.files.internal("images/hero.xml").readString());
 * }</pre>
 */
public final class FlixelXml {

  private FlixelXml() {}

  /**
   * Parses an XML document and returns its root element.
   *
   * @param text The XML document.
   * @return The root element; never {@code null}.
   * @throws IllegalArgumentException If no root element can be parsed.
   */
  @NotNull
  public static FlixelXmlElement parse(@NotNull String text) {
    Parser parser = new Parser(text);
    FlixelXmlElement root = parser.parseElement();
    if (root == null) {
      throw new IllegalArgumentException("No root element found in XML document.");
    }
    return root;
  }

  /** Hand-rolled XML reader over one input string. */
  private static final class Parser {

    @NotNull
    private final String text;

    private int pos;

    Parser(@NotNull String text) {
      this.text = text;
    }

    /** Parses the next element start at or after the cursor, or returns {@code null} at the end. */
    FlixelXmlElement parseElement() {
      while (pos < text.length()) {
        int lt = text.indexOf('<', pos);
        if (lt < 0) {
          return null;
        }
        pos = lt + 1;
        char c = charAt(pos);
        if (c == '?') {
          skipPast("?>");
        } else if (c == '!') {
          if (text.startsWith("!--", pos)) {
            skipPast("-->");
          } else if (text.startsWith("![CDATA[", pos)) {
            skipPast("]]>");
          } else {
            skipPast(">");
          }
        } else if (c == '/') {
          // A closing tag at this level belongs to the caller.
          pos = lt;
          return null;
        } else {
          return parseElementBody();
        }
      }
      return null;
    }

    /** Parses one element starting at its tag name (cursor is just past {@code '<'}). */
    @NotNull
    private FlixelXmlElement parseElementBody() {
      int nameStart = pos;
      while (pos < text.length() && !isNameEnd(text.charAt(pos))) {
        pos++;
      }
      FlixelXmlElement element = new FlixelXmlElement(text.substring(nameStart, pos));

      // Attributes.
      while (pos < text.length()) {
        skipWhitespace();
        char c = charAt(pos);
        if (c == '>' || c == '/') {
          break;
        }
        int keyStart = pos;
        while (pos < text.length() && text.charAt(pos) != '=' && !Character.isWhitespace(text.charAt(pos))) {
          pos++;
        }
        String key = text.substring(keyStart, pos);
        skipWhitespace();
        if (charAt(pos) != '=') {
          continue;
        }
        pos++;
        skipWhitespace();
        char quote = charAt(pos);
        if (quote != '"' && quote != '\'') {
          continue;
        }
        pos++;
        int valueStart = pos;
        while (pos < text.length() && text.charAt(pos) != quote) {
          pos++;
        }
        element.setAttribute(key, unescape(text.substring(valueStart, pos)));
        pos++;
      }

      if (charAt(pos) == '/') {
        // Self-closing tag.
        skipPast(">");
        return element;
      }
      pos++;

      // Children and text until the matching closing tag.
      int textStart = pos;
      while (pos < text.length()) {
        FlixelXmlElement child = parseElement();
        if (child != null) {
          element.addChild(child);
          textStart = pos;
          continue;
        }
        // parseElement stopped at a closing tag or the end.
        int lt = text.indexOf('<', pos);
        if (lt < 0) {
          break;
        }
        String body = text.substring(textStart, lt).trim();
        if (!body.isEmpty()) {
          element.setText(unescape(body));
        }
        pos = lt + 1;
        skipPast(">");
        break;
      }
      return element;
    }

    private char charAt(int at) {
      return at < text.length() ? text.charAt(at) : '\0';
    }

    private void skipWhitespace() {
      while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
        pos++;
      }
    }

    private void skipPast(@NotNull String token) {
      int at = text.indexOf(token, pos);
      pos = at < 0 ? text.length() : at + token.length();
    }

    private static boolean isNameEnd(char c) {
      return c == '>' || c == '/' || Character.isWhitespace(c);
    }

    @NotNull
    private static String unescape(@NotNull String value) {
      if (value.indexOf('&') < 0) {
        return value;
      }
      return value.replace("&lt;", "<")
          .replace("&gt;", ">")
          .replace("&quot;", "\"")
          .replace("&apos;", "'")
          .replace("&amp;", "&");
    }
  }
}
