/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import me.stringdotjar.flixelgdx.GdxHeadlessExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelColorTest {

  @Test
  void lerpHalfwayBetweenRedAndBlue() {
    FlixelColor a = new FlixelColor(1f, 0f, 0f, 1f);
    FlixelColor b = new FlixelColor(0f, 0f, 1f, 1f);
    a.getGdxColor().lerp(b.getGdxColor(), 0.5f);
    assertEquals(0.5f, a.getGdxColor().r, 2e-2f);
    assertEquals(0f, a.getGdxColor().g, 2e-2f);
    assertEquals(0.5f, a.getGdxColor().b, 2e-2f);
  }

  @Test
  void packRoundTrip() {
    FlixelColor c = new FlixelColor(1f, 0.5f, 0.25f, 1f);
    int packed = c.getColor();
    FlixelColor fromPacked = new FlixelColor(packed);
    assertEquals(c.getGdxColor().r, fromPacked.getGdxColor().r, 2e-2f);
    assertEquals(c.getGdxColor().g, fromPacked.getGdxColor().g, 2e-2f);
    assertEquals(c.getGdxColor().b, fromPacked.getGdxColor().b, 2e-2f);
    assertEquals(c.getGdxColor().a, fromPacked.getGdxColor().a, 2e-2f);
  }
}
