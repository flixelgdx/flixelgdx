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
package org.flixelgdx.gradle.shader.worker;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Small text helpers for turning compiler positions into positions in the user's file.
 *
 * <p>These work on plain strings and do not touch the GPU, so they can be tested without Dawn.
 */
public final class SourceText {

  private static final Pattern COMMENTS = Pattern.compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

  private SourceText() {}

  /**
   * Counts the line breaks in a text.
   *
   * @param text The text to scan.
   * @return The number of {@code \n} characters.
   */
  public static int countNewlines(String text) {
    int count = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns one line of a text.
   *
   * @param text The text.
   * @param line The 1-based line number.
   * @return The line without its line break, or an empty string when the text has no such line.
   */
  public static String lineOf(String text, int line) {
    int start = 0;
    for (int i = 1; i < line; i++) {
      int next = text.indexOf('\n', start);
      if (next < 0) {
        return "";
      }
      start = next + 1;
    }
    int end = text.indexOf('\n', start);
    String found = end < 0 ? text.substring(start) : text.substring(start, end);
    return found.endsWith("\r") ? found.substring(0, found.length() - 1) : found;
  }

  /**
   * Converts a column counted in UTF-8 bytes into a column counted in characters.
   *
   * <p>Dawn reports columns in bytes. For plain ASCII text the two are the same, but a comment with an accented
   * letter would otherwise push the column too far right.
   *
   * @param line The text of the line.
   * @param bytePos The 1-based byte column reported by the compiler.
   * @return The 1-based character column.
   */
  public static int charColumn(String line, long bytePos) {
    long bytes = 0;
    int i = 0;
    while (i < line.length() && bytes < bytePos - 1) {
      int cp = line.codePointAt(i);
      bytes += new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
      i += Character.charCount(cp);
    }
    return i + 1;
  }

  /**
   * Returns whether WGSL source defines a function with the given name, ignoring comments.
   *
   * @param source The WGSL text.
   * @param name The function name.
   * @return {@code true} when {@code fn <name>} appears in code.
   */
  public static boolean definesFunction(String source, String name) {
    String code = COMMENTS.matcher(source).replaceAll(" ");
    return Pattern.compile("\\bfn\\s+" + Pattern.quote(name) + "\\b").matcher(code).find();
  }

  /**
   * Formats one finding.
   *
   * <p>The first line is {@code path:line:column: severity: message}. When the line is known, the offending line
   * of the user's file follows, with a caret under the column.
   *
   * @param path The path of the user's file.
   * @param line The 1-based line in the user's file, or 0 when the finding has no position.
   * @param column The 1-based column, used only when {@code line} is positive.
   * @param severity A word such as {@code error} or {@code warning}.
   * @param message The message from the compiler.
   * @param userSource The text of the user's file.
   * @return The formatted text.
   */
  public static String format(String path, int line, int column, String severity, String message,
      String userSource) {
    if (line <= 0) {
      return path + ": " + severity + ": " + message;
    }
    StringBuilder out = new StringBuilder().append(path).append(':').append(line).append(':').append(column)
        .append(": ").append(severity).append(": ").append(message);
    String text = lineOf(userSource, line);
    if (!text.isEmpty()) {
      out.append('\n').append(text).append('\n');
      for (int i = 1; i < column; i++) {
        out.append(i - 1 < text.length() && text.charAt(i - 1) == '\t' ? '\t' : ' ');
      }
      out.append('^');
    }
    return out.toString();
  }
}
