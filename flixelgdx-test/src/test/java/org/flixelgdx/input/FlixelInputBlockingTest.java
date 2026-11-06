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
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.input.keyboard.FlixelKeyInputManager;
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.mouse.FlixelMouseInputManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the ways a tool can hide keys, buttons, and the wheel from the game while still reading them itself. */
class FlixelInputBlockingTest {

  private FlixelInputDevice previous;
  private ButtonDevice device;

  @BeforeEach
  void setUp() {
    previous = Flixel.input;
    device = new ButtonDevice();
    Flixel.input = device;
  }

  @AfterEach
  void tearDown() {
    Flixel.input = previous;
  }

  @Test
  void aBlockedKeyReadsAsReleasedToTheGameButNotToRawReads() {
    FlixelKeyInputManager keys = new FlixelKeyInputManager();
    keys.block(FlixelKey.W);
    keys.keyDown(FlixelKey.W);

    assertFalse(keys.pressed(FlixelKey.W));
    assertFalse(keys.justPressed(FlixelKey.W));
    assertFalse(keys.anyPressed(FlixelKey.W, FlixelKey.A));
    assertFalse(keys.anyJustPressed(FlixelKey.W));
    assertFalse(keys.pressed(FlixelKey.ANY), "the only held key is blocked");
    assertFalse(keys.justPressed(FlixelKey.ANY));
    assertEquals(FlixelKey.NONE, keys.firstPressed());
    assertEquals(FlixelKey.NONE, keys.firstJustPressed());
    assertTrue(keys.rawPressed(FlixelKey.W));
    assertTrue(keys.rawJustPressed(FlixelKey.W));

    keys.endFrame();
    keys.keyUp(FlixelKey.W);
    assertFalse(keys.justReleased(FlixelKey.W));
    assertTrue(keys.rawJustReleased(FlixelKey.W));
    assertEquals(FlixelKey.NONE, keys.firstJustReleased());
  }

  @Test
  void otherKeysStillWorkWhileOneIsBlockedAndUnblockingRestoresTheBlockedOne() {
    FlixelKeyInputManager keys = new FlixelKeyInputManager();
    keys.block(FlixelKey.W);
    keys.keyDown(FlixelKey.W);
    keys.keyDown(FlixelKey.SPACE);

    assertTrue(keys.pressed(FlixelKey.SPACE));
    assertTrue(keys.pressed(FlixelKey.ANY));
    assertEquals(FlixelKey.SPACE, keys.firstPressed());
    assertEquals(FlixelKey.SPACE, keys.firstJustPressed());

    assertTrue(keys.isBlocked(FlixelKey.W));
    keys.unblock(FlixelKey.W);
    assertFalse(keys.isBlocked(FlixelKey.W));
    assertTrue(keys.pressed(FlixelKey.W));

    keys.block(FlixelKey.W);
    keys.unblockAll();
    assertTrue(keys.pressed(FlixelKey.W));
  }

  @Test
  void aConsumedClickIsHiddenFromTheGameUntilTheButtonIsReleased() {
    FlixelMouseInputManager mouse = new FlixelMouseInputManager();
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));

    mouse.consume(FlixelMouseButton.LEFT);

    assertFalse(mouse.justPressed(FlixelMouseButton.LEFT));
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.rawJustPressed(FlixelMouseButton.LEFT));
    assertTrue(mouse.rawPressed(FlixelMouseButton.LEFT));

    mouse.endFrame();
    mouse.update();
    assertFalse(mouse.pressed(FlixelMouseButton.LEFT), "still held, still hidden");
    assertTrue(mouse.rawPressed(FlixelMouseButton.LEFT));

    mouse.endFrame();
    device.buttons[FlixelMouseButton.LEFT] = false;
    mouse.update();
    assertFalse(mouse.justReleased(FlixelMouseButton.LEFT), "the release belongs to the consumed press");
    assertFalse(mouse.rawJustReleased(FlixelMouseButton.LEFT));

    mouse.endFrame();
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT), "the next click is the game's again");
  }

  @Test
  void consumingAButtonThatIsNotHeldDoesNothing() {
    FlixelMouseInputManager mouse = new FlixelMouseInputManager();
    mouse.update();
    mouse.consume(FlixelMouseButton.LEFT);
    mouse.endFrame();
    device.buttons[FlixelMouseButton.LEFT] = true;
    mouse.update();
    assertTrue(mouse.justPressed(FlixelMouseButton.LEFT));
  }

  @Test
  void aBlockedButtonAndABlockedWheelStayHiddenUntilUnblocked() {
    FlixelMouseInputManager mouse = new FlixelMouseInputManager();
    mouse.setBlocked(FlixelMouseButton.RIGHT, true);
    mouse.setScrollBlocked(true);
    device.buttons[FlixelMouseButton.RIGHT] = true;
    mouse.scrolled(0f, -2f);
    mouse.update();

    assertTrue(mouse.isBlocked(FlixelMouseButton.RIGHT));
    assertFalse(mouse.pressed(FlixelMouseButton.RIGHT));
    assertFalse(mouse.justPressed(FlixelMouseButton.RIGHT));
    assertTrue(mouse.rawPressed(FlixelMouseButton.RIGHT));
    assertEquals(0f, mouse.getScrollDeltaY(), 0f);
    assertEquals(-2f, mouse.getRawScrollDeltaY(), 0f);

    mouse.setBlocked(FlixelMouseButton.RIGHT, false);
    mouse.setScrollBlocked(false);
    assertTrue(mouse.pressed(FlixelMouseButton.RIGHT));
    assertEquals(-2f, mouse.getScrollDeltaY(), 0f);
  }

  private static final class ButtonDevice implements FlixelInputDevice {

    final boolean[] buttons = new boolean[8];

    @Override
    public boolean isButtonPressed(int button) {
      return button >= 0 && button < buttons.length && buttons[button];
    }
  }
}
