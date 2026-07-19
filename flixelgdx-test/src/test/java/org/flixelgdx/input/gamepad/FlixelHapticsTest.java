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
package org.flixelgdx.input.gamepad;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlixelHapticsTest {

  private FlixelGamepadInputManager manager;

  @BeforeEach
  void setUp() {
    manager = new FlixelGamepadInputManager();
  }

  @Test
  void vibrateIsNoopWhenDisabled() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = false;
    manager.numActiveGamepads = 1;

    manager.vibrate(0, 1f);

    Assertions.assertEquals(0, provider.vibrateCount);
  }

  @Test
  void vibrateIsNoopForNegativeSlot() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.vibrate(-1, 1f);

    Assertions.assertEquals(0, provider.vibrateCount);
  }

  @Test
  void vibrateIsNoopWhenSlotExceedsActiveCount() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.vibrate(1, 1f);

    Assertions.assertEquals(0, provider.vibrateCount);
  }

  @Test
  void vibrateSingleArgExpandsToFullIntensityBothMotors() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.vibrate(0, 0.5f);

    Assertions.assertEquals(1, provider.vibrateCount);
    Assertions.assertEquals(1f, provider.lastLeft, 0.001f);
    Assertions.assertEquals(1f, provider.lastRight, 0.001f);
    Assertions.assertEquals(0.5f, provider.lastDuration, 0.001f);
  }

  @Test
  void vibrateTwoArgExpandsToSameIntensityBothMotors() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.vibrate(0, 0.75f, 2f);

    Assertions.assertEquals(1, provider.vibrateCount);
    Assertions.assertEquals(0.75f, provider.lastLeft, 0.001f);
    Assertions.assertEquals(0.75f, provider.lastRight, 0.001f);
    Assertions.assertEquals(2f, provider.lastDuration, 0.001f);
  }

  @Test
  void vibrateDualMotorPropagatesIndependently() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.vibrate(0, 0.3f, 0.9f, 0.25f);

    Assertions.assertEquals(1, provider.vibrateCount);
    Assertions.assertEquals(0, provider.lastSlot);
    Assertions.assertEquals(0.3f, provider.lastLeft, 0.001f);
    Assertions.assertEquals(0.9f, provider.lastRight, 0.001f);
    Assertions.assertEquals(0.25f, provider.lastDuration, 0.001f);
  }

  @Test
  void stopVibrationIsNoopWhenDisabled() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = false;
    manager.numActiveGamepads = 1;

    manager.stopVibration(0);

    Assertions.assertEquals(0, provider.stopCount);
  }

  @Test
  void stopVibrationDelegatesToProvider() {
    RecordingProvider provider = new RecordingProvider();
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    manager.stopVibration(0);

    Assertions.assertEquals(1, provider.stopCount);
  }

  @Test
  void canVibrateReturnsFalseWhenDisabled() {
    RecordingProvider provider = new RecordingProvider();
    provider.canVibrateResult = true;
    manager.setHapticsProvider(provider);
    manager.enabled = false;
    manager.numActiveGamepads = 1;

    Assertions.assertFalse(manager.canVibrate(0));
  }

  @Test
  void canVibrateDelegatesToProvider() {
    RecordingProvider provider = new RecordingProvider();
    provider.canVibrateResult = true;
    manager.setHapticsProvider(provider);
    manager.enabled = true;
    manager.numActiveGamepads = 1;

    Assertions.assertTrue(manager.canVibrate(0));
  }

  @Test
  void setHapticsProviderRejectsNull() {
    Assertions.assertThrows(NullPointerException.class, () -> manager.setHapticsProvider(null));
  }

  private static final class RecordingProvider implements FlixelHapticsProvider {

    int vibrateCount;
    int stopCount;
    int lastSlot;
    float lastLeft;
    float lastRight;
    float lastDuration;
    boolean canVibrateResult;

    @Override
    public void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs) {
      vibrateCount++;
      lastSlot = slot;
      lastLeft = leftIntensity;
      lastRight = rightIntensity;
      lastDuration = durationSecs;
    }

    @Override
    public void stopVibration(int slot) {
      stopCount++;
    }

    @Override
    public boolean canVibrate(int slot) {
      return canVibrateResult;
    }
  }
}
