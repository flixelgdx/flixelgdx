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

import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLocalizationPluralTest {

  private static final FlixelPluralRule SLAVIC = n -> {
    long ones = n % 10;
    long tens = n % 100;
    if (n == 1) {
      return FlixelPluralCategory.ONE;
    }
    if (ones >= 2 && ones <= 4 && (tens < 12 || tens > 14)) {
      return FlixelPluralCategory.FEW;
    }
    return FlixelPluralCategory.MANY;
  };

  private FlixelLocalization l10n;
  private FlixelString out;

  @BeforeEach
  void setUp() {
    l10n = new FlixelLocalization();
    l10n.setWarnMissing(false);
    out = new FlixelString();
    l10n.loadJson("en", """
        {
          "hud": {
            "coins": { "one": "{0} coin", "other": "{0} coins" },
            "found": { "one": "{0} key in {1}", "other": "{0} keys in {1}" },
            "only": { "other": "{0} things" },
            "plain": "{0} items"
          }
        }
        """);
    l10n.loadJson("pl", """
        { "hud": { "coins": { "one": "{0} moneta", "few": "{0} monety", "many": "{0} monet", "other": "{0} monety" } } }
        """);
  }

  @Test
  void defaultRuleIsOneOther() {
    assertEquals("1 coin", l10n.plural(out, "hud.coins", 1).toString());
    assertEquals("0 coins", l10n.plural(out, "hud.coins", 0).toString());
    assertEquals("12 coins", l10n.plural(out, "hud.coins", 12).toString());
  }

  @Test
  void registeredRulePicksForms() {
    l10n.setPluralRule("pl", SLAVIC);
    l10n.setLocale("pl");
    assertEquals("1 moneta", l10n.plural(out, "hud.coins", 1).toString());
    assertEquals("3 monety", l10n.plural(out, "hud.coins", 3).toString());
    assertEquals("12 monet", l10n.plural(out, "hud.coins", 12).toString());
    assertEquals("22 monety", l10n.plural(out, "hud.coins", 22).toString());
    assertEquals("5 monet", l10n.plural(out, "hud.coins", 5).toString());
  }

  @Test
  void ruleRegisteredLaterStillApplies() {
    l10n.setLocale("pl");
    assertEquals("5 monety", l10n.plural(out, "hud.coins", 5).toString());
    l10n.setPluralRule("pl", SLAVIC);
    assertEquals("5 monet", l10n.plural(out, "hud.coins", 5).toString());
  }

  @Test
  void fallbackTextUsesItsOwnLanguagesRule() {
    l10n.setPluralRule("pl", SLAVIC);
    l10n.setLocale("pl");
    assertEquals("3 keys in Cave", l10n.plural(out, "hud.found", 3, "Cave").toString());
  }

  @Test
  void ruleForShorterTagCoversLongerTag() {
    l10n.setPluralRule("pl", SLAVIC);
    assertSame(SLAVIC, l10n.getPluralRule("pl-PL"));
    assertSame(FlixelPluralRule.ONE_OTHER, l10n.getPluralRule("de"));
    l10n.setPluralRule("pl", null);
    assertSame(FlixelPluralRule.ONE_OTHER, l10n.getPluralRule("pl"));
  }

  @Test
  void missingFormFallsBackToOther() {
    assertEquals("1 things", l10n.plural(out, "hud.only", 1).toString());
  }

  @Test
  void plainStringIsUsedWhenNoPluralEntryExists() {
    assertEquals("4 items", l10n.plural(out, "hud.plain", 4).toString());
    assertEquals("nope", l10n.plural(out, "nope", 4).toString());
  }

  @Test
  void customDefaultRule() {
    l10n.setDefaultPluralRule(FlixelPluralRule.OTHER_ONLY);
    assertEquals("1 coins", l10n.plural(out, "hud.coins", 1).toString());
  }

  @Test
  void varargsOverloadShiftsValuesAfterCount() {
    l10n.loadJson("en", "{ \"multi\": { \"other\": \"{0}-{1}-{2}-{3}\" } }");
    assertEquals("9-a-b-c", l10n.plural(out, "multi", 9, "a", "b", "c").toString());
  }

  @Test
  void hasSeesPluralEntries() {
    assertTrue(l10n.has("hud.coins"));
    assertFalse(l10n.has("hud.coins.one"));
  }

  @Test
  void pluralTextReturnsUnformattedForm() {
    assertEquals("{0} coin", l10n.pluralText("hud.coins", 1));
  }

  @Test
  void objectsThatAreNotPluralStillFlatten() {
    FlixelLocalizationTable table = new FlixelLocalizationTable("en").putJson("""
        { "a": { "one": "x", "two": "y" }, "b": { "one": "x", "extra": "y", "other": "z" } }
        """);
    assertEquals("x", table.get("a.one"));
    assertEquals("z", table.get("b.other"));
    assertFalse(table.hasPlural("a"));
    assertFalse(table.hasPlural("b"));
  }

  @Test
  void putPluralBuildsEntryByHand() {
    FlixelLocalizationTable table = new FlixelLocalizationTable("en")
        .putPlural("lives", FlixelPluralCategory.ONE, "{0} life")
        .putPlural("lives", FlixelPluralCategory.OTHER, "{0} lives");
    assertEquals("{0} life", table.getPlural("lives", FlixelPluralCategory.ONE));
    assertEquals("{0} lives", table.getPlural("lives", FlixelPluralCategory.FEW));
    assertNull(table.getPlural("missing", FlixelPluralCategory.ONE));
    table.remove("lives");
    assertFalse(table.hasPlural("lives"));
  }

  @Test
  void categoryKeysRoundTrip() {
    for (FlixelPluralCategory category : FlixelPluralCategory.values()) {
      assertSame(category, FlixelPluralCategory.fromKey(category.getKey()));
    }
    assertNull(FlixelPluralCategory.fromKey("several"));
  }
}
