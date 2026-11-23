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

import org.junit.jupiter.api.Test;

import static org.flixelgdx.locale.FlixelPluralCategory.FEW;
import static org.flixelgdx.locale.FlixelPluralCategory.MANY;
import static org.flixelgdx.locale.FlixelPluralCategory.ONE;
import static org.flixelgdx.locale.FlixelPluralCategory.OTHER;
import static org.flixelgdx.locale.FlixelPluralCategory.TWO;
import static org.flixelgdx.locale.FlixelPluralCategory.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelPluralRuleTest {

  private static void check(FlixelPluralRule rule, FlixelPluralCategory expected, long... counts) {
    for (long count : counts) {
      assertEquals(expected, rule.select(count), "count " + count);
    }
  }

  @Test
  void oneOtherUsesAbsoluteValue() {
    check(FlixelPluralRule.ONE_OTHER, ONE, 1, -1);
    check(FlixelPluralRule.ONE_OTHER, OTHER, 0, 2, 11, 21, Long.MIN_VALUE);
  }

  @Test
  void otherOnly() {
    check(FlixelPluralRule.OTHER_ONLY, OTHER, 0, 1, 2, 100);
  }

  @Test
  void hindi() {
    check(FlixelPluralRule.HINDI, ONE, 0, 1);
    check(FlixelPluralRule.HINDI, OTHER, 2, 1_000_000);
  }

  @Test
  void frenchAndSpanish() {
    check(FlixelPluralRule.FRENCH, ONE, 0, 1);
    check(FlixelPluralRule.FRENCH, MANY, 1_000_000, 3_000_000);
    check(FlixelPluralRule.FRENCH, OTHER, 2, 1_000_001);
    check(FlixelPluralRule.SPANISH, ONE, 1);
    check(FlixelPluralRule.SPANISH, MANY, 1_000_000);
    check(FlixelPluralRule.SPANISH, OTHER, 0, 2, 999_999);
  }

  @Test
  void slavic() {
    check(FlixelPluralRule.EAST_SLAVIC, ONE, 1, 21, 101);
    check(FlixelPluralRule.EAST_SLAVIC, FEW, 2, 4, 22, 104);
    check(FlixelPluralRule.EAST_SLAVIC, MANY, 0, 5, 11, 12, 14, 111);
    check(FlixelPluralRule.SOUTH_SLAVIC, ONE, 1, 21);
    check(FlixelPluralRule.SOUTH_SLAVIC, FEW, 3, 23);
    check(FlixelPluralRule.SOUTH_SLAVIC, OTHER, 0, 5, 11, 13);
    check(FlixelPluralRule.POLISH, ONE, 1);
    check(FlixelPluralRule.POLISH, FEW, 2, 22);
    check(FlixelPluralRule.POLISH, MANY, 0, 5, 12, 21);
    check(FlixelPluralRule.CZECH, ONE, 1);
    check(FlixelPluralRule.CZECH, FEW, 2, 4);
    check(FlixelPluralRule.CZECH, OTHER, 0, 5, 22);
    check(FlixelPluralRule.SLOVENIAN, ONE, 1, 101);
    check(FlixelPluralRule.SLOVENIAN, TWO, 2, 102);
    check(FlixelPluralRule.SLOVENIAN, FEW, 3, 4, 103);
    check(FlixelPluralRule.SLOVENIAN, OTHER, 0, 5, 11, 21);
  }

  @Test
  void baltic() {
    check(FlixelPluralRule.LITHUANIAN, ONE, 1, 21);
    check(FlixelPluralRule.LITHUANIAN, FEW, 2, 9, 22);
    check(FlixelPluralRule.LITHUANIAN, OTHER, 0, 10, 11, 19, 20);
    check(FlixelPluralRule.LATVIAN, ZERO, 0, 10, 11, 19, 20);
    check(FlixelPluralRule.LATVIAN, ONE, 1, 21);
    check(FlixelPluralRule.LATVIAN, OTHER, 2, 9, 22);
  }

  @Test
  void otherEuropean() {
    check(FlixelPluralRule.ICELANDIC, ONE, 1, 21);
    check(FlixelPluralRule.ICELANDIC, OTHER, 0, 2, 11);
    check(FlixelPluralRule.ROMANIAN, ONE, 1);
    check(FlixelPluralRule.ROMANIAN, FEW, 0, 2, 19, 101, 119);
    check(FlixelPluralRule.ROMANIAN, OTHER, 20, 100, 120);
    check(FlixelPluralRule.MALTESE, ONE, 1);
    check(FlixelPluralRule.MALTESE, TWO, 2);
    check(FlixelPluralRule.MALTESE, FEW, 0, 3, 10, 103);
    check(FlixelPluralRule.MALTESE, MANY, 11, 19, 111);
    check(FlixelPluralRule.MALTESE, OTHER, 20, 100, 102);
  }

  @Test
  void celtic() {
    check(FlixelPluralRule.IRISH, ONE, 1);
    check(FlixelPluralRule.IRISH, TWO, 2);
    check(FlixelPluralRule.IRISH, FEW, 3, 6);
    check(FlixelPluralRule.IRISH, MANY, 7, 10);
    check(FlixelPluralRule.IRISH, OTHER, 0, 11);
    check(FlixelPluralRule.SCOTTISH_GAELIC, ONE, 1, 11);
    check(FlixelPluralRule.SCOTTISH_GAELIC, TWO, 2, 12);
    check(FlixelPluralRule.SCOTTISH_GAELIC, FEW, 3, 10, 13, 19);
    check(FlixelPluralRule.SCOTTISH_GAELIC, OTHER, 0, 20, 21);
    check(FlixelPluralRule.WELSH, ZERO, 0);
    check(FlixelPluralRule.WELSH, ONE, 1);
    check(FlixelPluralRule.WELSH, TWO, 2);
    check(FlixelPluralRule.WELSH, FEW, 3);
    check(FlixelPluralRule.WELSH, MANY, 6);
    check(FlixelPluralRule.WELSH, OTHER, 4, 5, 7, 100);
  }

  @Test
  void semitic() {
    check(FlixelPluralRule.HEBREW, ONE, 1);
    check(FlixelPluralRule.HEBREW, TWO, 2);
    check(FlixelPluralRule.HEBREW, OTHER, 0, 3, 20);
    check(FlixelPluralRule.ARABIC, ZERO, 0);
    check(FlixelPluralRule.ARABIC, ONE, 1);
    check(FlixelPluralRule.ARABIC, TWO, 2);
    check(FlixelPluralRule.ARABIC, FEW, 3, 10, 103);
    check(FlixelPluralRule.ARABIC, MANY, 11, 99, 111);
    check(FlixelPluralRule.ARABIC, OTHER, 100, 101, 102, 200);
  }

  @Test
  void filipino() {
    check(FlixelPluralRule.FILIPINO, ONE, 0, 1, 2, 3, 5, 7, 8, 10, 11);
    check(FlixelPluralRule.FILIPINO, OTHER, 4, 6, 9, 14, 26);
  }
}
