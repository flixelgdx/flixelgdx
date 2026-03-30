/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlixelMathUtilTest {

  @Test
  void roundTwoDecimals() {
    assertEquals(3.15f, FlixelMathUtil.round(3.145f, 2), 1e-5f);
  }

  @Test
  void roundZeroPlaces() {
    assertEquals(4f, FlixelMathUtil.round(3.7f, 0), 1e-5f);
  }
}
