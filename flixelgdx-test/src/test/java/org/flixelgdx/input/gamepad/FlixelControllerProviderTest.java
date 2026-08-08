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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link FlixelGamepadInputManager} discovers and polls controllers end to end through
 * a {@link FlixelControllerProvider}, without any real controller hardware.
 */
class FlixelControllerProviderTest {

  /** A fake controller whose button state the test drives directly. */
  private static final class FakeController implements FlixelController {

    final boolean[] buttons = new boolean[16];

    private final FlixelControllerMapping mapping = new FlixelControllerMapping();

    FakeController() {
      mapping.buttonA = 0;
      mapping.buttonB = 1;
      mapping.axisLeftX = 0;
      mapping.axisLeftY = 1;
    }

    @Override
    public String getName() {
      return "FakeController";
    }

    @Override
    public FlixelControllerMapping getMapping() {
      return mapping;
    }

    @Override
    public int getMinButtonIndex() {
      return 0;
    }

    @Override
    public int getMaxButtonIndex() {
      return buttons.length - 1;
    }

    @Override
    public boolean getButton(int buttonIndex) {
      return buttonIndex >= 0 && buttonIndex < buttons.length && buttons[buttonIndex];
    }

    @Override
    public int getAxisCount() {
      return 4;
    }

    @Override
    public float getAxis(int axisIndex) {
      return 0f;
    }

    @Override
    public boolean canVibrate() {
      return false;
    }

    @Override
    public void startVibration(int durationMs, float strength) {}

    @Override
    public void cancelVibration() {}
  }

  /** A provider that exposes a single, swappable fake controller. */
  private static final class FakeProvider implements FlixelControllerProvider {
    FlixelController controller;

    @Override
    public int getControllerCount() {
      return controller != null ? 1 : 0;
    }

    @Override
    public FlixelController getControllerAt(int index) {
      return index == 0 ? controller : null;
    }
  }

  @Test
  void discoversAndPollsControllerThroughProvider() {
    FakeController pad = new FakeController();
    FakeProvider provider = new FakeProvider();
    provider.controller = pad;

    FlixelGamepadInputManager manager = new FlixelGamepadInputManager();
    manager.setControllerProvider(provider);

    manager.update();
    assertEquals(1, manager.numActiveGamepads, "The provider's controller should occupy one slot.");
    assertFalse(manager.pressed(0, FlixelGamepadInput.A));

    pad.buttons[pad.getMapping().buttonA] = true;
    manager.update();
    assertTrue(manager.pressed(0, FlixelGamepadInput.A));
    assertTrue(manager.justPressed(0, FlixelGamepadInput.A));

    manager.endFrame();
    manager.update();
    assertTrue(manager.pressed(0, FlixelGamepadInput.A), "Still held, so still pressed.");
    assertFalse(manager.justPressed(0, FlixelGamepadInput.A), "Held since last frame, so no longer 'just'.");
  }

  @Test
  void reportsNoGamepadsWhenProviderIsEmpty() {
    FlixelGamepadInputManager manager = new FlixelGamepadInputManager();
    manager.setControllerProvider(new FakeProvider());
    manager.update();
    assertEquals(0, manager.numActiveGamepads);
    assertFalse(manager.anyPressed(FlixelGamepadInput.A));
  }
}
