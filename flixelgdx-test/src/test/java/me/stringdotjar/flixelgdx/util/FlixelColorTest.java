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
    a.lerp(b, 0.5f);
    assertEquals(0.5f, a.red(), 2e-2f);
    assertEquals(0f, a.green(), 2e-2f);
    assertEquals(0.5f, a.blue(), 2e-2f);
  }

  @Test
  void packRoundTrip() {
    FlixelColor c = new FlixelColor(1f, 0.5f, 0.25f, 1f);
    int packed = c.pack();
    FlixelColor fromPacked = new FlixelColor(packed);
    assertEquals(c.red(), fromPacked.red(), 2e-2f);
    assertEquals(c.green(), fromPacked.green(), 2e-2f);
    assertEquals(c.blue(), fromPacked.blue(), 2e-2f);
    assertEquals(c.alpha(), fromPacked.alpha(), 2e-2f);
  }
}
