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

/**
 * Decides which plural form a language uses for a count.
 *
 * <p>The framework does not decide which rule belongs to which language; you register one for each
 * language tag your game ships with {@link FlixelLocalization#setPluralRule(String, FlixelPluralRule)}.
 * That keeps you in full control of how your own tags pair with rules. A language without a rule
 * uses {@link FlixelLocalization#getDefaultPluralRule()}, which is {@link #ONE_OTHER} unless you
 * change it.
 *
 * <p>A rule is a small function, so most are one lambda. The plural rules for every language are
 * published by the Unicode CLDR project, which is the best place to look them up:
 *
 * <pre>{@code
 * // Only one form for every count.
 * Flixel.locale.setPluralRule("ja", FlixelPluralRule.OTHER_ONLY);
 *
 * // One form for 0 and 1, another for everything else.
 * Flixel.locale.setPluralRule("fr", n -> n == 0 || n == 1 ? FlixelPluralCategory.ONE : FlixelPluralCategory.OTHER);
 *
 * // Separate forms for 1, for 2 to 4 (but not 12 to 14), and for everything else.
 * Flixel.locale.setPluralRule("pl", n -> {
 *   long ones = n % 10;
 *   long tens = n % 100;
 *   if (n == 1) {
 *     return FlixelPluralCategory.ONE;
 *   }
 *   if (ones >= 2 && ones <= 4 && (tens < 12 || tens > 14)) {
 *     return FlixelPluralCategory.FEW;
 *   }
 *   return FlixelPluralCategory.MANY;
 * });
 * }</pre>
 *
 * <p>Rules run every time a plural string is looked up, which can be every frame, so they must not
 * allocate. Negative counts are passed through unchanged.
 */
@FunctionalInterface
public interface FlixelPluralRule {

  /** Uses {@link FlixelPluralCategory#ONE} for exactly 1 and {@link FlixelPluralCategory#OTHER} otherwise. */
  FlixelPluralRule ONE_OTHER = n -> n == 1 ? FlixelPluralCategory.ONE : FlixelPluralCategory.OTHER;

  /** Uses {@link FlixelPluralCategory#OTHER} for every count, for languages without plural forms. */
  FlixelPluralRule OTHER_ONLY = n -> FlixelPluralCategory.OTHER;

  /**
   * Returns the plural form for {@code count}.
   *
   * @param count The number being described, such as how many coins the player has.
   * @return The category to use. Returning a form the translation does not have falls back to
   *     {@link FlixelPluralCategory#OTHER}.
   */
  @NotNull
  FlixelPluralCategory select(long count);
}
