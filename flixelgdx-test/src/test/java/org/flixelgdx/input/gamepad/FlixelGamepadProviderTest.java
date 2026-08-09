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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link FlixelGamepadInputManager} discovers and polls gamepads end to end through
 * a {@link FlixelGamepadProvider} and a {@link FlixelGamepadMappingResolver}, without any real
 * controller hardware.
 */
class FlixelGamepadProviderTest {

  /** A fake gamepad whose button state the test drives directly. */
  private static final class FakeGamepad implements FlixelGamepad {

    final boolean[] buttons = new boolean[16];

    @Override
    public @NotNull String getName() {
      return "FakeGamepad";
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

  /** A provider that exposes a single, swappable fake gamepad. */
  private static final class FakeProvider implements FlixelGamepadProvider {
    FlixelGamepad gamepad;

    @Override
    public int getGamepadCount() {
      return gamepad != null ? 1 : 0;
    }

    @Override
    public FlixelGamepad getGamepadAt(int index) {
      return index == 0 ? gamepad : null;
    }

    @Override
    public void addListener(@NotNull FlixelGamepadListener listener) {}

    @Override
    public void removeListener(@NotNull FlixelGamepadListener listener) {}
  }

  private static FlixelGamepadMapping buildTestMapping() {
    FlixelGamepadMapping m = new FlixelGamepadMapping();
    m.registerButton(FlixelGamepadButton.A, 0);
    m.registerButton(FlixelGamepadButton.B, 1);
    m.registerAxis(FlixelGamepadAxis.LEFT_X, 0);
    m.registerAxis(FlixelGamepadAxis.LEFT_Y, 1);
    return m;
  }

  @Test
  void discoversAndPollsGamepadThroughProvider() {
    FakeGamepad pad = new FakeGamepad();
    FakeProvider provider = new FakeProvider();
    provider.gamepad = pad;
    FlixelGamepadMapping mapping = buildTestMapping();

    FlixelGamepadInputManager manager = new FlixelGamepadInputManager();
    manager.setGamepadProvider(provider);
    manager.addMappingResolver(g -> mapping);

    manager.update();
    assertEquals(1, manager.numActiveGamepads, "The provider's gamepad should occupy one slot.");
    assertFalse(manager.pressed(0, FlixelGamepadButton.A));

    pad.buttons[0] = true; // FlixelGamepadButton.A mapped to native index 0
    manager.update();
    assertTrue(manager.pressed(0, FlixelGamepadButton.A));
    assertTrue(manager.justPressed(0, FlixelGamepadButton.A));

    manager.endFrame();
    manager.update();
    assertTrue(manager.pressed(0, FlixelGamepadButton.A), "Still held, so still pressed.");
    assertFalse(manager.justPressed(0, FlixelGamepadButton.A), "Held since last frame, so no longer 'just'.");
  }

  @Test
  void reportsNoGamepadsWhenProviderIsEmpty() {
    FlixelGamepadInputManager manager = new FlixelGamepadInputManager();
    manager.setGamepadProvider(new FakeProvider());
    manager.update();
    assertEquals(0, manager.numActiveGamepads);
    assertFalse(manager.anyPressed(FlixelGamepadButton.A));
  }
}
