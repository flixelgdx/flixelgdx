/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelAnimationTypesTest {

  @Test
  void frameDataRejectsNullAnimationName() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelAnimationFrameSignalData(null, 0, null));
  }

  @Test
  void stateMachineTracksStateWithoutControllerSideEffects() {
    FlixelAnimationStateMachine sm = new FlixelAnimationStateMachine();
    assertEquals("", sm.getState());
    sm.clear();
    assertEquals("", sm.getState());
  }
}
