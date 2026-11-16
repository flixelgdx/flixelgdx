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
package org.flixelgdx.gradle.assets;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.lang.model.SourceVersion;

/**
 * Turns folder and file names into Java identifiers for the generated class.
 *
 * <p>A name is first split into words. Anything that is not an ASCII letter or digit separates words (so
 * {@code player-idle.png} is {@code player}, {@code idle}, {@code png}), and so does a change of case inside a word
 * ({@code playerIdle} is {@code player}, {@code Idle}, and {@code HUDIcon} is {@code HUD}, {@code Icon}). The words are
 * then joined in the style the identifier needs:
 *
 * <ul>
 *   <li>Folders become nested classes in PascalCase: {@code shared} is {@code Shared}, {@code ui_icons} is
 *       {@code UiIcons}.</li>
 *   <li>Files become constants in UPPER_SNAKE_CASE, extension included: {@code player.png} is
 *       {@code PLAYER_PNG}.</li>
 * </ul>
 *
 * <p>A name that would start with a digit gets a leading underscore ({@code 8bit.ogg} is {@code _8BIT_OGG}), because a
 * Java identifier cannot start with one.
 */
public final class AssetNames {

  private AssetNames() {}

  /**
   * Turns a folder name into a PascalCase class name.
   *
   * @param name The folder name, such as {@code ui_icons}.
   * @return The class name, such as {@code UiIcons}, or {@code null} if the name has no ASCII letter or digit.
   */
  @Nullable
  public static String toClassName(String name) {
    List<String> words = words(name);
    if (words.isEmpty()) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    for (String word : words) {
      out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
    }
    return guardDigit(out.toString());
  }

  /**
   * Turns a file name into an UPPER_SNAKE_CASE constant name.
   *
   * @param name The file name, such as {@code player.png}.
   * @return The constant name, such as {@code PLAYER_PNG}, or {@code null} if the name has no ASCII letter or digit.
   */
  @Nullable
  public static String toConstantName(String name) {
    List<String> words = words(name);
    if (words.isEmpty()) {
      return null;
    }
    return guardDigit(String.join("_", words).toUpperCase(Locale.ROOT));
  }

  /**
   * Checks whether a string is a valid Java identifier that is not a keyword or a literal.
   *
   * @param name The string to check.
   * @return {@code true} if a class, field or package segment may be called this.
   */
  public static boolean isIdentifier(String name) {
    if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    for (int i = 1; i < name.length(); i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        return false;
      }
    }
    return !SourceVersion.isKeyword(name) && !name.equals("_");
  }

  /**
   * Splits a name into words at every character that is not an ASCII letter or digit, and at every change of case.
   *
   * @param name The name to split.
   * @return The words in order, keeping their original case. Empty if the name has no ASCII letter or digit.
   */
  static List<String> words(String name) {
    List<String> words = new ArrayList<>();
    StringBuilder word = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!isAsciiLetterOrDigit(c)) {
        flush(word, words);
        continue;
      }
      if (!word.isEmpty()) {
        char prev = name.charAt(i - 1);
        boolean nextIsLower = i + 1 < name.length() && isAsciiLower(name.charAt(i + 1));
        // "playerIdle" splits before the I, and "HUDIcon" splits before the I that starts "Icon".
        if ((isAsciiLower(prev) || isAsciiDigit(prev)) && isAsciiUpper(c)
            || isAsciiUpper(prev) && isAsciiUpper(c) && nextIsLower) {
          flush(word, words);
        }
      }
      word.append(c);
    }
    flush(word, words);
    return words;
  }

  private static void flush(StringBuilder word, List<String> words) {
    if (!word.isEmpty()) {
      words.add(word.toString());
      word.setLength(0);
    }
  }

  private static String guardDigit(String name) {
    return isAsciiDigit(name.charAt(0)) ? "_" + name : name;
  }

  private static boolean isAsciiLetterOrDigit(char c) {
    return isAsciiLower(c) || isAsciiUpper(c) || isAsciiDigit(c);
  }

  private static boolean isAsciiLower(char c) {
    return c >= 'a' && c <= 'z';
  }

  private static boolean isAsciiUpper(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
