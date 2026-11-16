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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetNamesTest {

  @Test
  void foldersBecomePascalCase() {
    assertEquals("Shared", AssetNames.toClassName("shared"));
    assertEquals("Sprites", AssetNames.toClassName("Sprites"));
    assertEquals("UiIcons", AssetNames.toClassName("ui_icons"));
    assertEquals("MySprites", AssetNames.toClassName("my-sprites"));
    assertEquals("HudIcon", AssetNames.toClassName("HUDIcon"));
    assertEquals("Hud", AssetNames.toClassName("HUD"));
    assertEquals("LevelOne", AssetNames.toClassName("level one"));
    assertEquals("Week2", AssetNames.toClassName("week2"));
  }

  @Test
  void filesBecomeUpperSnakeCaseWithTheExtension() {
    assertEquals("PLAYER_PNG", AssetNames.toConstantName("player.png"));
    assertEquals("PLAYER_IDLE_PNG", AssetNames.toConstantName("playerIdle.png"));
    assertEquals("BOSS_THEME_FINAL_OGG", AssetNames.toConstantName("boss theme (final).ogg"));
    assertEquals("TILES_ATLAS_PNG", AssetNames.toConstantName("tiles.atlas.png"));
    assertEquals("HUD_ICON_PNG", AssetNames.toConstantName("HUDIcon.png"));
    assertEquals("LICENSE", AssetNames.toConstantName("LICENSE"));
    assertEquals("LEVEL2_JSON", AssetNames.toConstantName("level2.json"));
  }

  @Test
  void leadingDigitGetsAnUnderscore() {
    assertEquals("_8BIT_OGG", AssetNames.toConstantName("8bit.ogg"));
    assertEquals("_2d", AssetNames.toClassName("2d"));
    assertEquals("_2dArt", AssetNames.toClassName("2d-art"));
  }

  @Test
  void namesWithoutAsciiLettersOrDigitsHaveNoIdentifier() {
    assertNull(AssetNames.toClassName("---"));
    assertNull(AssetNames.toConstantName("音楽"));
    assertNull(AssetNames.toConstantName(""));
  }

  @Test
  void nonAsciiLettersAreDroppedAsSeparators() {
    assertEquals("CAF_THEME_OGG", AssetNames.toConstantName("café theme.ogg"));
  }

  @Test
  void wordsSplitOnSymbolsAndCaseChanges() {
    assertEquals(List.of("player", "Idle", "png"), AssetNames.words("player_Idle.png"));
    assertEquals(List.of("XML", "Parser"), AssetNames.words("XMLParser"));
    assertEquals(List.of("a1", "B"), AssetNames.words("a1B"));
    assertTrue(AssetNames.words("...").isEmpty());
  }

  @Test
  void identifiersRejectKeywordsAndBadStarts() {
    assertTrue(AssetNames.isIdentifier("com"));
    assertTrue(AssetNames.isIdentifier("Assets"));
    assertTrue(AssetNames.isIdentifier("_8BIT"));
    assertFalse(AssetNames.isIdentifier("class"));
    assertFalse(AssetNames.isIdentifier("true"));
    assertFalse(AssetNames.isIdentifier("_"));
    assertFalse(AssetNames.isIdentifier("1st"));
    assertFalse(AssetNames.isIdentifier("my-game"));
    assertFalse(AssetNames.isIdentifier(""));
  }
}
