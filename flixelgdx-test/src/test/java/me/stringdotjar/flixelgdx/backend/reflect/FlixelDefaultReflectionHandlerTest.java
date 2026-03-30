/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.reflect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDefaultReflectionHandlerTest {

  static final class Sample {
    @SuppressWarnings("unused")
    float health = 10f;
  }

  @Test
  void hasFieldAndReadWrite() {
    FlixelDefaultReflectionHandler handler = new FlixelDefaultReflectionHandler();
    Sample s = new Sample();
    assertTrue(handler.hasField(s, "health"));
    assertEquals(10f, (Float) handler.field(s, "health"));
    handler.setField(s, "health", 42f);
    assertEquals(42f, (Float) handler.field(s, "health"));
  }
}
