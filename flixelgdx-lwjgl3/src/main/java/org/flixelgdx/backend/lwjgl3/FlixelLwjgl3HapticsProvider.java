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
package org.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.desktop.support.JamepadController;
import com.studiohartman.jamepad.ControllerIndex;
import com.studiohartman.jamepad.ControllerUnpluggedException;

import org.flixelgdx.Flixel;
import org.flixelgdx.input.gamepad.FlixelHapticsProvider;

import java.lang.reflect.Field;

/**
 * Desktop {@link FlixelHapticsProvider} backed by SDL via Jamepad. Supports true independent
 * dual-motor vibration by calling {@code ControllerIndex.doVibration(left, right, duration)}
 * directly, which maps to {@code SDL_JoystickRumble} with separate low-frequency (left) and
 * high-frequency (right) motor channels.
 *
 * <p>This provider is installed automatically by {@link FlixelLwjgl3Launcher} and replaces the
 * default {@code org.flixelgdx.input.gamepad.FlixelDefaultHapticsProvider}. You do not need to
 * install it manually.
 *
 * <h2>Fallback behavior</h2>
 *
 * <p>Reflection is used to reach the private {@code controllerIndex} field on {@link JamepadController}.
 * If reflection is unavailable (for example, under certain security managers or after module-system
 * hardening), the provider falls back to {@link Controller#startVibration} with the stronger of
 * the two intensities driving both motors, matching the behavior of the default provider.
 */
final class FlixelLwjgl3HapticsProvider implements FlixelHapticsProvider {

  private static final Field CONTROLLER_INDEX_FIELD;

  static {
    Field f = null;
    try {
      f = JamepadController.class.getDeclaredField("controllerIndex");
      f.setAccessible(true);
    } catch (Throwable ignored) {
    }
    CONTROLLER_INDEX_FIELD = f;
  }

  @Override
  public void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs) {
    ControllerIndex ci = indexAt(slot);
    if (ci != null) {
      float left = Math.max(0f, Math.min(1f, leftIntensity));
      float right = Math.max(0f, Math.min(1f, rightIntensity));
      try {
        ci.doVibration(left, right, (int) (durationSecs * 1000f));
      } catch (ControllerUnpluggedException ignored) {
      }
      return;
    }
    Controller c = Flixel.gamepads.controllerAt(slot);
    if (c != null && c.canVibrate()) {
      float peak = Math.max(0f, Math.min(1f, Math.max(leftIntensity, rightIntensity)));
      c.startVibration((int) (durationSecs * 1000f), peak);
    }
  }

  @Override
  public void stopVibration(int slot) {
    Controller c = Flixel.gamepads.controllerAt(slot);
    if (c != null) {
      c.cancelVibration();
    }
  }

  @Override
  public boolean canVibrate(int slot) {
    Controller c = Flixel.gamepads.controllerAt(slot);
    return c != null && c.canVibrate();
  }

  private static ControllerIndex indexAt(int slot) {
    if (CONTROLLER_INDEX_FIELD == null) {
      return null;
    }
    Controller c = Flixel.gamepads.controllerAt(slot);
    if (c != null && !(c instanceof JamepadController)) {
      return null;
    }
    try {
      return (ControllerIndex) CONTROLLER_INDEX_FIELD.get(c);
    } catch (Throwable ignored) {
      return null;
    }
  }
}
