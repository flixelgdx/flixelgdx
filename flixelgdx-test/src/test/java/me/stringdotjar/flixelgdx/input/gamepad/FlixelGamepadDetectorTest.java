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
