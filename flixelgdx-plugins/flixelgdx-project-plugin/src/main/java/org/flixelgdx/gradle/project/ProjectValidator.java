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
package org.flixelgdx.gradle.project;

import org.gradle.api.GradleException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks the text values of a {@code project.toml}, so a bad name fails the build with a message that names the field
 * and the character that is wrong.
 *
 * <p>The title and company end up in folder names, executable names and save paths on every operating system, so they
 * are limited to what is safe in all of them: printable ASCII, none of the characters Windows forbids in file names,
 * no reserved Windows device names, and no leading or trailing space or dot.
 */
public final class ProjectValidator {

  private static final String FORBIDDEN = "<>:\"/\\|?*";

  private static final Pattern VERSION = Pattern.compile("[0-9][0-9A-Za-z.+-]*");

  private static final Pattern SEGMENT = Pattern.compile("[a-z_][a-z0-9_]*");

  private static final Set<String> RESERVED = Set.of(
      "CON", "PRN", "AUX", "NUL",
      "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
      "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

  private static final Set<String> KEYWORDS = Set.of(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
      "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
      "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
      "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null", "_");

  private ProjectValidator() {}

  /**
   * Checks a title or company name.
   *
   * @param field The field name used in messages, such as {@code app.title}.
   * @param value The value to check.
   * @throws GradleException if the value is not safe to use as a file name on every operating system.
   */
  public static void checkName(String field, String value) {
    if (value.isBlank()) {
      throw fail(field, "must not be blank");
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < 0x20 || c > 0x7E) {
        throw fail(field, "contains " + describe(c) + " at position " + (i + 1)
            + ", but only printable ASCII characters are allowed");
      }
      if (FORBIDDEN.indexOf(c) >= 0) {
        throw fail(field, "contains " + describe(c) + " at position " + (i + 1)
            + ", which is not allowed in a file name (none of " + FORBIDDEN + " may be used)");
      }
    }
    char first = value.charAt(0);
    char last = value.charAt(value.length() - 1);
    if (first == ' ' || first == '.') {
      throw fail(field, "must not start with " + describe(first));
    }
    if (last == ' ' || last == '.') {
      throw fail(field, "must not end with " + describe(last));
    }
    if (RESERVED.contains(value.toUpperCase(Locale.ROOT))) {
      throw fail(field, "must not be '" + value + "', which is a reserved name on Windows");
    }
  }

  /**
   * Checks a version.
   *
   * @param field The field name used in messages, such as {@code app.version}.
   * @param value The value to check.
   * @throws GradleException if the version does not start with a digit or has characters outside letters, digits,
   *     dots, plus signs and hyphens.
   */
  public static void checkVersion(String field, String value) {
    if (value.isEmpty()) {
      throw fail(field, "must not be empty");
    }
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      boolean ok = c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
          || c == '.' || c == '+' || c == '-';
      if (!ok) {
        throw fail(field, "contains " + describe(c) + " at position " + (i + 1)
            + ", but only letters, digits, '.', '+' and '-' are allowed");
      }
    }
    if (!VERSION.matcher(value).matches()) {
      throw fail(field, "must start with a digit, such as '0.1.0'");
    }
  }

  /**
   * Checks a Java package name.
   *
   * @param field The field name used in messages, such as {@code app.package}.
   * @param value The value to check.
   * @throws GradleException if a segment is empty, does not follow the lowercase naming style, or is a Java keyword.
   */
  public static void checkPackage(String field, String value) {
    int start = 0;
    while (start <= value.length()) {
      int dot = value.indexOf('.', start);
      String segment = value.substring(start, dot < 0 ? value.length() : dot);
      if (segment.isEmpty()) {
        throw fail(field, "must not have an empty part (check for a leading, trailing or doubled '.')");
      }
      if (!SEGMENT.matcher(segment).matches()) {
        for (int i = 0; i < segment.length(); i++) {
          char c = segment.charAt(i);
          boolean ok = c >= 'a' && c <= 'z' || c == '_' || i > 0 && c >= '0' && c <= '9';
          if (!ok) {
            throw fail(field, "contains " + describe(c) + " in '" + segment
                + "'; each part must start with a lowercase letter or '_' and only contain lowercase letters,"
                + " digits and '_'");
          }
        }
      }
      if (KEYWORDS.contains(segment)) {
        throw fail(field, "has the part '" + segment + "', which is a Java keyword");
      }
      if (dot < 0) {
        break;
      }
      start = dot + 1;
    }
  }

  /**
   * Checks the language.
   *
   * @param field The field name used in messages, such as {@code app.language}.
   * @param value The value to check.
   * @throws GradleException if the language is neither {@code java} nor {@code kotlin}.
   */
  public static void checkLanguage(String field, String value) {
    if (!value.equals("java") && !value.equals("kotlin")) {
      throw fail(field, "must be \"java\" or \"kotlin\", not \"" + value + "\"");
    }
  }

  private static String describe(char c) {
    if (c < 0x20 || c > 0x7E) {
      return String.format("the character U+%04X", (int) c);
    }
    return "the character '" + c + "'";
  }

  private static GradleException fail(String field, String problem) {
    return new GradleException("Invalid " + field + " in project.toml: it " + problem + ".");
  }
}
