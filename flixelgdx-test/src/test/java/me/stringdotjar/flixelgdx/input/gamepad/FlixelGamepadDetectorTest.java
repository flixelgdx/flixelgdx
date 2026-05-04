/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.gamepad;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FlixelGamepadDetectorTest {

  @Test
  void detectDualSense() {
    Assertions.assertEquals(FlixelGamepadModel.PS5, FlixelGamepadDetector.detect("Sony DualSense Wireless Controller"));
  }

  @Test
  void detectDs4() {
    Assertions.assertEquals(FlixelGamepadModel.PS4, FlixelGamepadDetector.detect("Wireless Controller"));
  }

  @Test
  void detectSwitchDoesNotMatchPsWireless() {
    Assertions.assertEquals(
      FlixelGamepadModel.SWITCH_PRO,
      FlixelGamepadDetector.detect("Nintendo Switch Pro Controller"));
  }

  @Test
  void detectXbox360() {
    Assertions.assertEquals(FlixelGamepadModel.XBOX_360, FlixelGamepadDetector.detect("Xbox 360 Controller"));
  }

  @Test
  void detectXboxOne() {
    Assertions.assertEquals(FlixelGamepadModel.XBOX_ONE, FlixelGamepadDetector.detect("Xbox Wireless Controller"));
  }

  @Test
  void detectXInput() {
    Assertions.assertEquals(FlixelGamepadModel.XBOX_ONE, FlixelGamepadDetector.detect("XInput Controller"));
  }

  @Test
  void detectWii() {
    Assertions.assertEquals(FlixelGamepadModel.WII, FlixelGamepadDetector.detect("Nintendo RVL-CNT-01"));
  }

  @Test
  void detectOuya() {
    Assertions.assertEquals(FlixelGamepadModel.OUYA, FlixelGamepadDetector.detect("OUYA Game Controller"));
  }

  @Test
  void detectUnknown() {
    Assertions.assertEquals(FlixelGamepadModel.UNKNOWN, FlixelGamepadDetector.detect("Some Random HID"));
  }

  @Test
  void detectNull() {
    Assertions.assertEquals(FlixelGamepadModel.UNKNOWN, FlixelGamepadDetector.detect(null));
  }
}
