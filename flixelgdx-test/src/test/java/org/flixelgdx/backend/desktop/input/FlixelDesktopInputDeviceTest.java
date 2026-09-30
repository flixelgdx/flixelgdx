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
package org.flixelgdx.backend.desktop.input;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.input.FlixelInputDevice;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the desktop input device keeps a click or key tap that SDL delivered within one
 * event pump across two frames, instead of losing it.
 */
@ExtendWith(FlixelHeadlessExtension.class)
class FlixelDesktopInputDeviceTest {

  private FlixelInputDevice previousInput;
  private FlixelDesktopInputDevice device;
  private FlixelMouseInputManager mouse;
  private FlixelKeyInputManager keys;

  @BeforeEach
  void setUp() {
    previousInput = Flixel.input;
    device = new FlixelDesktopInputDevice();
    Flixel.input = device;
    mouse = new FlixelMouseInputManager();
    keys = new FlixelKeyInputManager();
    device.addMouseListener(mouse);
    device.addKeyboardListener(keys);
  }

  @AfterEach
  void tearDown() {
    Flixel.input = previousInput;
  }

  @Test
  void queuedEventsWaitForDrain() {
    device.getEventQueue().postMouseDown(FlixelMouseButton.LEFT, 5, 7);
    assertFalse(device.isButtonPressed(FlixelMouseButton.LEFT));

    device.drain();
    assertTrue(device.isButtonPressed(FlixelMouseButton.LEFT));
    assertEquals(5, device.getX());
    assertEquals(7, device.getY());
  }

  @Test
  void clickWithinOnePumpReportsPressThenRelease() {
    device.getEventQueue().postMouseDown(FlixelMouseButton.LEFT, 0, 0);
    device.getEventQueue().postMouseUp(FlixelMouseButton.LEFT, 0, 0);

    beginFrame();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
    assertFalse(mouse.justReleased(FlixelMouseButton.LEFT));
    mouse.endFrame();

    beginFrame();
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.justReleased(FlixelMouseButton.LEFT));
  }

  @Test
  void keyTapWithinOnePumpReportsPressThenRelease() {
    device.getEventQueue().postKeyDown(FlixelKey.SPACE);
    device.getEventQueue().postKeyUp(FlixelKey.SPACE);

    beginFrame();
    assertTrue(keys.justPressed(FlixelKey.SPACE));
    keys.endFrame();

    beginFrame();
    assertTrue(keys.justReleased(FlixelKey.SPACE));
    assertFalse(device.isKeyPressed(FlixelKey.SPACE));
  }

  /** Mirrors one iteration of the desktop loop: drain after the pump, then update the managers. */
  private void beginFrame() {
    device.drain();
    keys.update();
    mouse.update();
  }
}
