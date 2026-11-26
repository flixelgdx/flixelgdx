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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelLocalizationTest {

  private FlixelLocalization l10n;

  @BeforeEach
  void setUp() {
    l10n = new FlixelLocalization();
    l10n.setWarnMissing(false);
    l10n.loadJson("en", """
        { "menu": { "play": "Play", "quit": "Quit" }, "hud": { "score": "Score: {0}" }, "only": "English only" }
        """);
    l10n.loadJson("pt", """
        { "menu": { "play": "Jogar", "quit": "Sair" } }
        """);
    l10n.loadJson("pt-BR", """
        { "menu": { "quit": "Fechar" } }
        """);
  }

  @Test
  void defaultsToEnglish() {
    assertEquals("en", l10n.getLocale());
    assertEquals("Play", l10n.get("menu.play"));
  }

  @Test
  void fallbackChainGoesFromRegionToLanguageToFallback() {
    l10n.setLocale("pt-BR");
    assertEquals("Fechar", l10n.get("menu.quit"));
    assertEquals("Jogar", l10n.get("menu.play"));
    assertEquals("English only", l10n.get("only"));
  }

  @Test
  void underscoreTagsMatchHyphenTags() {
    l10n.setLocale("pt_BR");
    assertEquals("pt-BR", l10n.getLocale());
    assertEquals("Fechar", l10n.get("menu.quit"));
  }

  @Test
  void missingKeyReturnsKeyOrDefault() {
    assertEquals("nope.key", l10n.get("nope.key"));
    assertEquals("dflt", l10n.get("nope.key", "dflt"));
    assertFalse(l10n.has("nope.key"));
    assertTrue(l10n.has("menu.play"));
  }

  @Test
  void unloadedLocaleFallsBackAndJoinsChainWhenLoadedLater() {
    l10n.setLocale("fr");
    assertEquals("Play", l10n.get("menu.play"));
    l10n.loadJson("fr", "{ \"menu\": { \"play\": \"Jouer\" } }");
    assertEquals("Jouer", l10n.get("menu.play"));
  }

  @Test
  void loadingSameTagMerges() {
    l10n.loadJson("en", "{ \"menu\": { \"play\": \"Start\" }, \"extra\": \"Yes\" }");
    assertEquals("Start", l10n.get("menu.play"));
    assertEquals("Quit", l10n.get("menu.quit"));
    assertEquals("Yes", l10n.get("extra"));
  }

  @Test
  void customFallback() {
    l10n.setFallback("pt");
    l10n.setLocale("de");
    assertEquals("Jogar", l10n.get("menu.play"));
    assertEquals("only", l10n.get("only"));
  }

  @Test
  void formatFillsPlaceholders() {
    FlixelString out = new FlixelString();
    FlixelString value = new FlixelString().set(120);
    assertSame(out, l10n.format(out, "hud.score", value));
    assertEquals("Score: 120", out.toString());
  }

  @Test
  void localeChangeDispatchesOnlyOnRealChange() {
    String[] seen = new String[1];
    int[] count = new int[1];
    l10n.onLocaleChanged.add(tag -> {
      seen[0] = tag;
      count[0]++;
    });
    l10n.setLocale("pt");
    l10n.setLocale("pt");
    assertEquals("pt", seen[0]);
    assertEquals(1, count[0]);
  }

  @Test
  void removeDropsTableFromChain() {
    l10n.setLocale("pt");
    assertEquals("Jogar", l10n.get("menu.play"));
    l10n.remove("pt");
    assertNull(l10n.getTable("pt"));
    assertEquals("Play", l10n.get("menu.play"));
  }

  @Test
  void addReplacesTable() {
    l10n.add(new FlixelLocalizationTable("en").put("menu.play", "Go"));
    assertEquals("Go", l10n.get("menu.play"));
    assertEquals("menu.quit", l10n.get("menu.quit"));
  }

  @Test
  void nonObjectRootThrows() {
    assertThrows(IllegalArgumentException.class, () -> l10n.loadJson("en", "[1, 2]"));
  }
}
