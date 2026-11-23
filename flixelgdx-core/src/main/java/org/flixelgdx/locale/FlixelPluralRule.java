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

import static org.flixelgdx.locale.FlixelPluralCategory.FEW;
import static org.flixelgdx.locale.FlixelPluralCategory.MANY;
import static org.flixelgdx.locale.FlixelPluralCategory.ONE;
import static org.flixelgdx.locale.FlixelPluralCategory.OTHER;
import static org.flixelgdx.locale.FlixelPluralCategory.TWO;
import static org.flixelgdx.locale.FlixelPluralCategory.ZERO;

/**
 * Decides which plural form a language uses for a count.
 *
 * <p>The framework does not decide which rule belongs to which language; you register one for each
 * language tag your game ships with {@link FlixelLocalization#setPluralRule(String, FlixelPluralRule)}.
 * That keeps you in full control of how your own tags pair with rules. A language without a rule
 * uses {@link FlixelLocalization#getDefaultPluralRule()}, which is {@link #ONE_OTHER} unless you
 * change it.
 *
 * <p>Think of a rule like a sorting machine at a post office: every count goes in, and the machine
 * drops it into one of a few bins ({@link FlixelPluralCategory}). Each language wires its machine
 * differently, so this interface ships ready-made rules for the most common languages. Each one
 * lists the language tags it fits, following the integer rules published by the Unicode CLDR
 * project:
 *
 * <pre>{@code
 * Flixel.locale.setPluralRule("en", FlixelPluralRule.ONE_OTHER);
 * Flixel.locale.setPluralRule("ja", FlixelPluralRule.OTHER_ONLY);
 * Flixel.locale.setPluralRule("fr", FlixelPluralRule.FRENCH);
 * Flixel.locale.setPluralRule("ru", FlixelPluralRule.EAST_SLAVIC);
 * Flixel.locale.setPluralRule("pl", FlixelPluralRule.POLISH);
 * Flixel.locale.setPluralRule("ar", FlixelPluralRule.ARABIC);
 * }</pre>
 *
 * <p>If your language is not covered, a rule is a small function, so you can write your own as one
 * lambda. The CLDR plural rules chart is the best place to look up how a language works:
 *
 * <pre>{@code
 * // One form for 1, another for everything else, written by hand.
 * Flixel.locale.setPluralRule("xx", n -> n == 1 ? FlixelPluralCategory.ONE : FlixelPluralCategory.OTHER);
 * }</pre>
 *
 * <p>Rules run every time a plural string is looked up, which can be every frame, so they must not
 * allocate. Negative counts are passed through unchanged; the ready-made rules use the count's
 * absolute value, so {@code -1} picks the same form as {@code 1}. Counts are whole numbers, so the
 * ready-made rules only cover the forms CLDR uses for integers.
 */
@FunctionalInterface
public interface FlixelPluralRule {

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1 and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits English (en), German (de), Dutch (nl), Swedish (sv), Danish (da), Norwegian (nb, nn,
   * no), Finnish (fi), Estonian (et), Greek (el), Hungarian (hu), Turkish (tr), Bulgarian (bg),
   * Albanian (sq), Georgian (ka), Swahili (sw), Basque (eu), Galician (gl), Marathi (mr), and Kazakh
   * (kk).
   */
  FlixelPluralRule ONE_OTHER = n -> abs(n) == 1 ? ONE : OTHER;

  /**
   * Uses {@link FlixelPluralCategory#OTHER} for every count.
   *
   * <p>Fits languages without plural forms, such as Japanese (ja), Chinese (zh), Korean (ko), Thai
   * (th), Vietnamese (vi), Indonesian (id), Malay (ms), Burmese (my), Lao (lo), and Khmer (km).
   */
  FlixelPluralRule OTHER_ONLY = n -> OTHER;

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 0 and 1 and {@link FlixelPluralCategory#OTHER}
   * otherwise.
   *
   * <p>Fits Hindi (hi), Bengali (bn), Gujarati (gu), Kannada (kn), Persian (fa), Amharic (am), Zulu
   * (zu), and Armenian (hy).
   */
  FlixelPluralRule HINDI = n -> abs(n) <= 1 ? ONE : OTHER;

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 0 and 1, {@link FlixelPluralCategory#MANY} for exact
   * millions, and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits French (fr) and Brazilian Portuguese (pt). The millions form exists because these
   * languages write "1 000 000 de pieces" with an extra word, so it is safe to leave out of a
   * translation; a missing {@code many} falls back to {@code other}.
   */
  FlixelPluralRule FRENCH = n -> {
    long a = abs(n);
    if (a <= 1) {
      return ONE;
    }
    return isMillions(a) ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#MANY} for exact
   * millions, and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Spanish (es), Italian (it), Catalan (ca), and European Portuguese (pt-PT). Like
   * {@link #FRENCH}, the millions form is optional in a translation.
   */
  FlixelPluralRule SPANISH = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    return isMillions(a) ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for counts ending in 1 (but not 11),
   * {@link FlixelPluralCategory#FEW} for counts ending in 2 to 4 (but not 12 to 14), and
   * {@link FlixelPluralCategory#MANY} otherwise.
   *
   * <p>Fits Russian (ru), Ukrainian (uk), and Belarusian (be).
   */
  FlixelPluralRule EAST_SLAVIC = n -> {
    long a = abs(n);
    long ones = a % 10;
    long tens = a % 100;
    if (ones == 1 && tens != 11) {
      return ONE;
    }
    if (ones >= 2 && ones <= 4 && (tens < 12 || tens > 14)) {
      return FEW;
    }
    return MANY;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for counts ending in 1 (but not 11),
   * {@link FlixelPluralCategory#FEW} for counts ending in 2 to 4 (but not 12 to 14), and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Croatian (hr), Serbian (sr), Bosnian (bs), and Serbo-Croatian (sh). It matches
   * {@link #EAST_SLAVIC} except that the last group is {@code other} instead of {@code many}.
   */
  FlixelPluralRule SOUTH_SLAVIC = n -> {
    long a = abs(n);
    long ones = a % 10;
    long tens = a % 100;
    if (ones == 1 && tens != 11) {
      return ONE;
    }
    if (ones >= 2 && ones <= 4 && (tens < 12 || tens > 14)) {
      return FEW;
    }
    return OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#FEW} for counts ending
   * in 2 to 4 (but not 12 to 14), and {@link FlixelPluralCategory#MANY} otherwise.
   *
   * <p>Fits Polish (pl).
   */
  FlixelPluralRule POLISH = n -> {
    long a = abs(n);
    long ones = a % 10;
    long tens = a % 100;
    if (a == 1) {
      return ONE;
    }
    if (ones >= 2 && ones <= 4 && (tens < 12 || tens > 14)) {
      return FEW;
    }
    return MANY;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#FEW} for 2 to 4, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Czech (cs) and Slovak (sk).
   */
  FlixelPluralRule CZECH = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    return a >= 2 && a <= 4 ? FEW : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE}, {@link FlixelPluralCategory#TWO}, and
   * {@link FlixelPluralCategory#FEW} for counts whose last two digits are 1, 2, and 3 to 4, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Slovenian (sl).
   */
  FlixelPluralRule SLOVENIAN = n -> {
    long tens = abs(n) % 100;
    if (tens == 1) {
      return ONE;
    }
    if (tens == 2) {
      return TWO;
    }
    return tens == 3 || tens == 4 ? FEW : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for counts ending in 1 (but not 11) and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Icelandic (is) and Macedonian (mk).
   */
  FlixelPluralRule ICELANDIC = n -> {
    long a = abs(n);
    return a % 10 == 1 && a % 100 != 11 ? ONE : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for counts ending in 1, {@link FlixelPluralCategory#FEW}
   * for counts ending in 2 to 9, and {@link FlixelPluralCategory#OTHER} otherwise, where counts
   * ending in 11 to 19 always use {@code other}.
   *
   * <p>Fits Lithuanian (lt).
   */
  FlixelPluralRule LITHUANIAN = n -> {
    long a = abs(n);
    long ones = a % 10;
    long tens = a % 100;
    if (tens >= 11 && tens <= 19) {
      return OTHER;
    }
    if (ones == 1) {
      return ONE;
    }
    return ones >= 2 ? FEW : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ZERO} for counts ending in 0 or 11 to 19,
   * {@link FlixelPluralCategory#ONE} for counts ending in 1 (but not 11), and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Latvian (lv).
   */
  FlixelPluralRule LATVIAN = n -> {
    long a = abs(n);
    long ones = a % 10;
    long tens = a % 100;
    if (ones == 0 || (tens >= 11 && tens <= 19)) {
      return ZERO;
    }
    return ones == 1 ? ONE : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#FEW} for 0 and counts
   * whose last two digits are 1 to 19, and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Romanian (ro) and Moldovan (mo).
   */
  FlixelPluralRule ROMANIAN = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    long tens = a % 100;
    return a == 0 || (tens >= 1 && tens <= 19) ? FEW : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#TWO} for 2, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Hebrew (he, iw).
   */
  FlixelPluralRule HEBREW = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    return a == 2 ? TWO : OTHER;
  };

  /**
   * Uses all six forms: {@link FlixelPluralCategory#ZERO} for 0, {@link FlixelPluralCategory#ONE}
   * for 1, {@link FlixelPluralCategory#TWO} for 2, {@link FlixelPluralCategory#FEW} when the last
   * two digits are 3 to 10, {@link FlixelPluralCategory#MANY} when they are 11 to 99, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Arabic (ar) and its regional tags.
   */
  FlixelPluralRule ARABIC = n -> {
    long a = abs(n);
    if (a <= 2) {
      return a == 0 ? ZERO : a == 1 ? ONE : TWO;
    }
    long tens = a % 100;
    if (tens >= 3 && tens <= 10) {
      return FEW;
    }
    return tens >= 11 ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#TWO} for 2,
   * {@link FlixelPluralCategory#FEW} for 0 and counts whose last two digits are 3 to 10,
   * {@link FlixelPluralCategory#MANY} when they are 11 to 19, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Maltese (mt).
   */
  FlixelPluralRule MALTESE = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    if (a == 2) {
      return TWO;
    }
    long tens = a % 100;
    if (a == 0 || (tens >= 3 && tens <= 10)) {
      return FEW;
    }
    return tens >= 11 && tens <= 19 ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1, {@link FlixelPluralCategory#TWO} for 2,
   * {@link FlixelPluralCategory#FEW} for 3 to 6, {@link FlixelPluralCategory#MANY} for 7 to 10,
   * and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Irish (ga).
   */
  FlixelPluralRule IRISH = n -> {
    long a = abs(n);
    if (a == 1) {
      return ONE;
    }
    if (a == 2) {
      return TWO;
    }
    if (a >= 3 && a <= 6) {
      return FEW;
    }
    return a >= 7 && a <= 10 ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for 1 and 11, {@link FlixelPluralCategory#TWO} for 2 and
   * 12, {@link FlixelPluralCategory#FEW} for 3 to 10 and 13 to 19, and
   * {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Scottish Gaelic (gd).
   */
  FlixelPluralRule SCOTTISH_GAELIC = n -> {
    long a = abs(n);
    if (a == 1 || a == 11) {
      return ONE;
    }
    if (a == 2 || a == 12) {
      return TWO;
    }
    return a >= 3 && a <= 19 ? FEW : OTHER;
  };

  /**
   * Uses all six forms: {@link FlixelPluralCategory#ZERO} for 0, {@link FlixelPluralCategory#ONE}
   * for 1, {@link FlixelPluralCategory#TWO} for 2, {@link FlixelPluralCategory#FEW} for 3,
   * {@link FlixelPluralCategory#MANY} for 6, and {@link FlixelPluralCategory#OTHER} otherwise.
   *
   * <p>Fits Welsh (cy).
   */
  FlixelPluralRule WELSH = n -> {
    long a = abs(n);
    if (a == 0) {
      return ZERO;
    }
    if (a == 1) {
      return ONE;
    }
    if (a == 2) {
      return TWO;
    }
    if (a == 3) {
      return FEW;
    }
    return a == 6 ? MANY : OTHER;
  };

  /**
   * Uses {@link FlixelPluralCategory#ONE} for counts that do not end in 4, 6, or 9, and
   * {@link FlixelPluralCategory#OTHER} for counts that do.
   *
   * <p>Fits Filipino (fil) and Tagalog (tl).
   */
  FlixelPluralRule FILIPINO = n -> {
    long ones = abs(n) % 10;
    return ones == 4 || ones == 6 || ones == 9 ? OTHER : ONE;
  };

  /**
   * Returns the plural form for {@code count}.
   *
   * @param count The number being described, such as how many coins the player has.
   * @return The category to use. Returning a form the translation does not have falls back to
   *     {@link FlixelPluralCategory#OTHER}.
   */
  @NotNull
  FlixelPluralCategory select(long count);

  /**
   * Returns the absolute value of {@code n}, the way CLDR reads a count.
   *
   * <p>{@link Long#MIN_VALUE} has no positive counterpart, so it is clamped to {@link Long#MAX_VALUE}.
   */
  private static long abs(long n) {
    return n == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(n);
  }

  /** Returns whether {@code a} is a nonzero exact multiple of one million. */
  private static boolean isMillions(long a) {
    return a != 0 && a % 1_000_000 == 0;
  }
}
