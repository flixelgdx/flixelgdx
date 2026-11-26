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
package org.flixelgdx.input.action;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.input.gamepad.FlixelGamepadButton;
import org.flixelgdx.input.gamepad.FlixelGamepadInputManager;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.flixelgdx.input.touch.FlixelTouchManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDigitalBindingTest {

  @Test
  void bindingsAreFalseWithoutTheirInputManager() {
    Flixel.keys = null;
    Flixel.mouse = null;
    Flixel.gamepads = null;
    Flixel.touches = null;
    assertFalse(FlixelDigitalBinding.key(FlixelKey.A).evaluate());
    assertFalse(FlixelDigitalBinding.mouseButton(FlixelMouseButton.LEFT).evaluate());
    assertFalse(FlixelDigitalBinding.gamepadButton(0, FlixelGamepadButton.A).evaluate());
    assertFalse(FlixelDigitalBinding.touch(0).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegion(0f, 0f, 1f, 1f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegionWorld(0f, 0f, 1f, 1f).evaluate());
  }

  @Test
  void disabledManagersAreFalse() {
    Flixel.keys = new FlixelKeyInputManager();
    Flixel.keys.keyDown(FlixelKey.A);
    Flixel.keys.enabled = false;
    Flixel.mouse = new FlixelMouseInputManager();
    Flixel.mouse.enabled = false;
    Flixel.gamepads = new FlixelGamepadInputManager();
    Flixel.gamepads.enabled = false;
    Flixel.touches = new FlixelTouchManager();
    assertFalse(FlixelDigitalBinding.key(FlixelKey.A).evaluate());
    assertFalse(FlixelDigitalBinding.mouseButton(FlixelMouseButton.LEFT).evaluate());
    assertFalse(FlixelDigitalBinding.gamepadButton(FlixelDigitalBinding.GAMEPAD_SLOT_ANY, FlixelGamepadButton.A)
        .evaluate());
    assertFalse(FlixelDigitalBinding.touch(0).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegion(0f, 0f, 1f, 1f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegionWorld(0f, 0f, 1f, 1f).evaluate());
  }

  @Test
  void keyMouseAndGamepadBindingsReadTheirManagers() {
    Flixel.keys = new FlixelKeyInputManager();
    Flixel.keys.keyDown(FlixelKey.A);
    assertTrue(FlixelDigitalBinding.key(FlixelKey.A).evaluate());
    assertFalse(FlixelDigitalBinding.key(FlixelKey.B).evaluate());

    Flixel.mouse = new FlixelMouseInputManager();
    Flixel.mouse.injectButton(FlixelMouseButton.LEFT, true);
    Flixel.mouse.update();
    assertTrue(FlixelDigitalBinding.mouseButton(FlixelMouseButton.LEFT).evaluate());

    Flixel.gamepads = new FlixelGamepadInputManager();
    assertFalse(FlixelDigitalBinding.gamepadButton(FlixelDigitalBinding.GAMEPAD_SLOT_ANY, FlixelGamepadButton.A)
        .evaluate());
    assertFalse(FlixelDigitalBinding.gamepadButton(0, FlixelGamepadButton.A).evaluate());
  }

  @Test
  void touchBindingsCheckPointersAndRegions() {
    Flixel.touches = new FlixelTouchManager(3);
    Flixel.touches.enabled = true;
    Flixel.graphics = new Screen(0, 0);
    Flixel.touches.injectTouched(1, 30, 40);
    Flixel.touches.update();
    assertTrue(FlixelDigitalBinding.touch(1).evaluate());
    assertFalse(FlixelDigitalBinding.touch(0).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegion(0f, 0f, 1f, 1f).evaluate(), "no back buffer means no region");

    Flixel.graphics = new Screen(100, 100);
    assertTrue(FlixelDigitalBinding.touchRegion(0.2f, 0.3f, 0.2f, 0.2f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegion(0.5f, 0.3f, 0.2f, 0.2f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegion(0.2f, 0.5f, 0.2f, 0.2f).evaluate());
    assertTrue(FlixelDigitalBinding.touchRegionWorld(25f, 35f, 10f, 10f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegionWorld(35f, 35f, 10f, 10f).evaluate());
    assertFalse(FlixelDigitalBinding.touchRegionWorld(25f, 45f, 10f, 10f).evaluate());
  }

  private record Screen(int w, int h) implements FlixelGraphicsManager {

    @Override
    public FlixelList<FlixelDisplayMode> getDisplayModes() {
      return null;
    }

    @Override
    public int getBackBufferWidth() {
      return w;
    }

    @Override
    public int getBackBufferHeight() {
      return h;
    }
  }
}
