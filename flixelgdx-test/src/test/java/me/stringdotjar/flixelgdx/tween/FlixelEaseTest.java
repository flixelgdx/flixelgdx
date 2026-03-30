/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.tween;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelEaseTest {

  @Test
  void linearEndpoints() {
    assertEquals(0f, FlixelEase.linear(0f), 1e-6f);
    assertEquals(1f, FlixelEase.linear(1f), 1e-6f);
  }

  @Test
  void quadInIncreasesOnZeroToOne() {
    assertTrue(FlixelEase.quadIn(0.25f) < FlixelEase.quadIn(0.75f));
  }

  @Test
  void quadInEndpoints() {
    assertEquals(0f, FlixelEase.quadIn(0f), 1e-6f);
    assertEquals(1f, FlixelEase.quadIn(1f), 1e-6f);
  }
}
