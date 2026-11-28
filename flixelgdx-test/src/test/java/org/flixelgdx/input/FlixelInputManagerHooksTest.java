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
package org.flixelgdx.input;

import org.flixelgdx.Flixel;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelInputManagerHooksTest {

  @Test
  void theUpdateHookReadsTheHardware() {
    int[] reads = new int[1];
    FlixelInputManager manager = new FlixelInputManager() {
      @Override
      public void update() {
        reads[0]++;
      }

      @Override
      public void endFrame() {}
    };
    manager.update(0.016f);
    assertEquals(1, reads[0]);
    assertFalse(manager.isPausable());
  }

  @Test
  void creatingTheInputManagersStartsListeningToTheDevice() {
    RecordingDevice device = new RecordingDevice();
    Flixel.input = device;
    FlixelKeyInputManager keys = new FlixelKeyInputManager();
    FlixelMouseInputManager mouse = new FlixelMouseInputManager();
    FlixelTouchManager touches = new FlixelTouchManager();
    keys.create();
    mouse.create();
    touches.create();
    assertSame(keys, device.keyboard);
    assertSame(mouse, device.mouse);
    assertSame(touches, device.touch);
  }

  @Test
  void aCreatedKeyboardSeesKeyPresses() {
    FlixelBaseInputDevice device = new FlixelBaseInputDevice() {
    };
    Flixel.input = device;
    FlixelKeyInputManager keys = new FlixelKeyInputManager();
    keys.create();
    device.injectKeyDown(FlixelKey.A);
    assertTrue(keys.pressed(FlixelKey.A));
  }

  @Test
  void destroyingTheGamepadsForgetsEveryGamepad() {
    FlixelGamepadInputManager gamepads = new FlixelGamepadInputManager();
    gamepads.numActiveGamepads = 2;
    gamepads.destroy();
    assertEquals(0, gamepads.numActiveGamepads);
  }

  private static final class RecordingDevice implements FlixelInputDevice {

    FlixelKeyboardListener keyboard;
    FlixelMouseListener mouse;
    FlixelTouchListener touch;

    @Override
    public void addKeyboardListener(FlixelKeyboardListener listener) {
      keyboard = listener;
    }

    @Override
    public void addMouseListener(FlixelMouseListener listener) {
      mouse = listener;
    }

    @Override
    public void addTouchListener(FlixelTouchListener listener) {
      touch = listener;
    }
  }
}
