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
package org.flixelgdx.graphics.wgsl;

import org.jetbrains.annotations.NotNull;

/**
 * A minimal WGSL tokenizer used by the layout parser: it drops comments and splits the text into identifiers,
 * numbers and single-character punctuation.
 *
 * <p>It exists only to find struct declarations and uniform bindings, so it does not understand expressions.
 * Instances are built once when a shader is created, never per frame.
 */
final class FlixelWgslTokens {

  private final String[] text;
  private final int count;

  private FlixelWgslTokens(String[] text, int count) {
    this.text = text;
    this.count = count;
  }

  @NotNull
  static FlixelWgslTokens of(@NotNull String source) {
    String[] out = new String[Math.max(16, source.length() / 3)];
    int n = 0;
    int i = 0;
    int len = source.length();
    while (i < len) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        i++;
      } else if (c == '/' && i + 1 < len && source.charAt(i + 1) == '/') {
        while (i < len && source.charAt(i) != '\n') {
          i++;
        }
      } else if (c == '/' && i + 1 < len && source.charAt(i + 1) == '*') {
        int depth = 1;
        i += 2;
        while (i < len && depth > 0) {
          if (source.startsWith("/*", i)) {
            depth++;
            i += 2;
          } else if (source.startsWith("*/", i)) {
            depth--;
            i += 2;
          } else {
            i++;
          }
        }
      } else if (isIdentStart(c)) {
        int start = i;
        while (i < len && isIdentPart(source.charAt(i))) {
          i++;
        }
        out = ensure(out, n);
        out[n++] = source.substring(start, i);
      } else if (c >= '0' && c <= '9') {
        int start = i;
        while (i < len && (isIdentPart(source.charAt(i)) || source.charAt(i) == '.')) {
          i++;
        }
        out = ensure(out, n);
        out[n++] = source.substring(start, i);
      } else {
        out = ensure(out, n);
        out[n++] = String.valueOf(c);
        i++;
      }
    }
    return new FlixelWgslTokens(out, n);
  }

  int size() {
    return count;
  }

  @NotNull
  String get(int index) {
    return index >= 0 && index < count ? text[index] : "";
  }

  boolean is(int index, @NotNull String value) {
    return index >= 0 && index < count && text[index].equals(value);
  }

  private static String[] ensure(String[] array, int used) {
    if (used < array.length) {
      return array;
    }
    String[] bigger = new String[array.length * 2];
    System.arraycopy(array, 0, bigger, 0, used);
    return bigger;
  }

  private static boolean isIdentStart(char c) {
    return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean isIdentPart(char c) {
    return isIdentStart(c) || (c >= '0' && c <= '9');
  }
}
