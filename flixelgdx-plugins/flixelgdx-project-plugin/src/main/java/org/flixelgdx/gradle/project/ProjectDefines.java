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
import org.tomlj.TomlTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the {@code [defines]} table of a {@code project.toml} into checked {@link ProjectDefine}s.
 *
 * <p>Each key becomes an UPPER_SNAKE_CASE constant name with the same rules the assets plugin uses for file names:
 * words are split at anything that is not an ASCII letter or digit and at every change of case, so {@code max_saves},
 * {@code max-saves} and {@code maxSaves} all become {@code MAX_SAVES}, and a name that starts with a digit gets a
 * leading {@code _} ({@code 8bit} becomes {@code _8BIT}).
 *
 * <p>Values can be booleans, whole numbers ({@code int}, or {@code long} when they do not fit), decimal numbers
 * ({@code float}, the type the framework uses everywhere) and strings. Anything else fails the build with the key
 * that is wrong.
 */
final class ProjectDefines {

  static final String TABLE = "defines";

  private ProjectDefines() {}

  /**
   * Reads the defines of a parsed file.
   *
   * @param toml The parsed file.
   * @return The defines in the order they are written, or an empty list when there is no {@code [defines]} table.
   * @throws GradleException if {@code defines} is not a table, a value has an unsupported type, a key has no letter
   *     or digit, or two keys turn into the same constant name.
   */
  static List<ProjectDefine> parse(TomlTable toml) {
    Object table = toml.get(TABLE);
    if (table == null) {
      return List.of();
    }
    if (!(table instanceof TomlTable defines)) {
      throw new GradleException("Invalid defines in project.toml: it must be a table, such as [defines].");
    }
    List<ProjectDefine> out = new ArrayList<>();
    Map<String, String> keysByName = new HashMap<>();
    // keySet() keeps the order of the file, so the constants come out in the order they were written.
    for (String key : defines.keySet()) {
      String field = TABLE + "." + key;
      String name = toConstantName(key);
      if (name == null) {
        throw new GradleException("Invalid " + field + " in project.toml: the key needs at least one ASCII letter or "
            + "digit to become a constant name.");
      }
      String other = keysByName.put(name, key);
      if (other != null) {
        throw new GradleException("Invalid " + field + " in project.toml: it becomes the constant " + name
            + ", the same as defines." + other + ". Rename one of them.");
      }
      out.add(new ProjectDefine(key, name, value(field, defines.get(List.of(key)))));
    }
    return List.copyOf(out);
  }

  private static Object value(String field, Object value) {
    return switch (value) {
      case Boolean flag -> flag;
      case String text -> text;
      case Long number -> number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE ? (Object) number.intValue()
          : number;
      case Double decimal -> decimal(field, decimal);
      case TomlTable nested -> throw new GradleException("Invalid " + field + " in project.toml: nested tables are not "
          + "supported in [defines]. Use one key per value, such as " + field + "_speed = 2.0.");
      default -> throw new GradleException("Invalid " + field + " in project.toml: it must be true or false, a "
          + "number or a string in quotes.");
    };
  }

  private static Float decimal(String field, double decimal) {
    float single = (float) decimal;
    if (Double.isNaN(decimal) || Double.isInfinite(single)) {
      throw new GradleException("Invalid " + field + " in project.toml: " + decimal + " is not a number a float "
          + "can hold.");
    }
    return single;
  }

  /**
   * Turns a key into an UPPER_SNAKE_CASE constant name.
   *
   * @param key The key, such as {@code maxSaves}.
   * @return The constant name, such as {@code MAX_SAVES}, or {@code null} if the key has no ASCII letter or digit.
   */
  static String toConstantName(String key) {
    List<String> words = new ArrayList<>();
    StringBuilder word = new StringBuilder();
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      if (!isLower(c) && !isUpper(c) && !isDigit(c)) {
        flush(word, words);
        continue;
      }
      if (!word.isEmpty()) {
        char prev = key.charAt(i - 1);
        boolean nextIsLower = i + 1 < key.length() && isLower(key.charAt(i + 1));
        // "maxSaves" splits before the S, and "HUDScale" splits before the S that starts "Scale".
        if ((isLower(prev) || isDigit(prev)) && isUpper(c) || isUpper(prev) && isUpper(c) && nextIsLower) {
          flush(word, words);
        }
      }
      word.append(c);
    }
    flush(word, words);
    if (words.isEmpty()) {
      return null;
    }
    String name = String.join("_", words).toUpperCase(Locale.ROOT);
    return isDigit(name.charAt(0)) ? "_" + name : name;
  }

  private static void flush(StringBuilder word, List<String> words) {
    if (!word.isEmpty()) {
      words.add(word.toString());
      word.setLength(0);
    }
  }

  private static boolean isLower(char c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isUpper(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
