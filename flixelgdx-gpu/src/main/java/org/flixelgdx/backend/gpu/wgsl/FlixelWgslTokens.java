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
package org.flixelgdx.backend.gpu.wgsl;

import org.jetbrains.annotations.NotNull;

/**
 * A minimal WGSL tokenizer used by the layout parser: it drops comments and splits the text into identifiers,
 * numbers and single-character punctuation.
 *
 * <p>It exists only to find struct declarations, uniform bindings and the fragment entry point, so it does not
 * understand expressions. Each token remembers where it sits in the source, so a caller can rewrite exact characters.
 * Instances are built once when a shader is created, never per frame.
 */
final class FlixelWgslTokens {

  private final String[] text;
  private final int[] starts;
  private final int count;

  private FlixelWgslTokens(String[] text, int[] starts, int count) {
    this.text = text;
    this.starts = starts;
    this.count = count;
  }

  @NotNull
  static FlixelWgslTokens of(@NotNull String source) {
    String[] out = new String[Math.max(16, source.length() / 3)];
    int[] at = new int[out.length];
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
        at = ensure(at, n);
        at[n] = start;
        out = ensure(out, n);
        out[n++] = source.substring(start, i);
      } else if (c >= '0' && c <= '9') {
        int start = i;
        while (i < len && (isIdentPart(source.charAt(i)) || source.charAt(i) == '.')) {
          i++;
        }
        at = ensure(at, n);
        at[n] = start;
        out = ensure(out, n);
        out[n++] = source.substring(start, i);
      } else {
        at = ensure(at, n);
        at[n] = i;
        out = ensure(out, n);
        out[n++] = String.valueOf(c);
        i++;
      }
    }
    return new FlixelWgslTokens(out, at, n);
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

  /**
   * Returns where a token starts in the source.
   *
   * @param index The token index.
   * @return The offset of its first character.
   */
  int start(int index) {
    return starts[index];
  }

  /**
   * Returns where a token ends in the source.
   *
   * @param index The token index.
   * @return The offset just past its last character.
   */
  int end(int index) {
    return starts[index] + text[index].length();
  }

  private static int[] ensure(int[] array, int used) {
    if (used < array.length) {
      return array;
    }
    int[] bigger = new int[array.length * 2];
    System.arraycopy(array, 0, bigger, 0, used);
    return bigger;
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
