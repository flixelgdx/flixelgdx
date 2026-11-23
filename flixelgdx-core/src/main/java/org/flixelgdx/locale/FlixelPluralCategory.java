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
package org.flixelgdx.locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The plural forms a translated string can have, named after the Unicode CLDR plural categories.
 *
 * <p>Languages split numbers into different groups. English only has two ("1 coin", "2 coins"),
 * Japanese has one, and some languages have up to six. Each group gets one of these names, and a
 * {@link FlixelPluralRule} decides which group a count belongs to.
 *
 * <p>In a JSON string file, a plural entry is an object whose keys are these names written in
 * lowercase, and it must include {@code "other"}:
 *
 * <pre>{@code
 * "coins": { "one": "{0} coin", "other": "{0} coins" }
 * }</pre>
 *
 * <p>The names are labels, not exact numbers. {@link #ONE} means "the form this language uses for
 * counts like one", which in some languages also covers 21, 31, and so on.
 */
public enum FlixelPluralCategory {

  /** The form for zero, in languages that give zero its own wording. */
  ZERO("zero"),

  /** The form for one, and for any other count the language groups with it. */
  ONE("one"),

  /** The form for two, in languages that have a dual form. */
  TWO("two"),

  /** The form for a small group of counts, such as 2 to 4 in several Slavic languages. */
  FEW("few"),

  /** The form for a large group of counts, in languages that separate it from {@link #OTHER}. */
  MANY("many"),

  /** The general form. Every plural entry must have it, and it is used when another form is missing. */
  OTHER("other");

  /** Every category, cached so lookups do not copy the array that {@code values()} returns. */
  static final FlixelPluralCategory[] ALL = values();

  @NotNull
  private final String key;

  FlixelPluralCategory(@NotNull String key) {
    this.key = key;
  }

  /**
   * Returns the category whose JSON name is {@code key}.
   *
   * @param key The lowercase name, such as {@code "few"}.
   * @return The matching category, or {@code null} when {@code key} is not a category name.
   */
  @Nullable
  public static FlixelPluralCategory fromKey(@NotNull String key) {
    for (int i = 0; i < ALL.length; i++) {
      if (ALL[i].key.equals(key)) {
        return ALL[i];
      }
    }
    return null;
  }

  /**
   * Returns the lowercase name used for this category in JSON string files.
   *
   * @return The name, such as {@code "one"}.
   */
  @NotNull
  public String getKey() {
    return key;
  }
}
