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
package org.flixelgdx.backend.teavm;

import com.badlogic.gdx.controllers.Controllers;

import org.flixelgdx.input.gamepad.FlixelHapticsProvider;
import org.teavm.jso.JSBody;

/**
 * Web-backend {@link FlixelHapticsProvider} that calls the W3C Gamepad Haptics API directly via
 * {@link JSBody} inline JavaScript, bypassing the xpenatan gdx-teavm controller layer entirely.
 *
 * <p>This provider is installed automatically by {@link FlixelTeaVMLauncher} and replaces the
 * default {@code org.flixelgdx.input.gamepad.FlixelDefaultHapticsProvider}. You do not need to
 * install it manually.
 *
 * <h2>Motor mapping</h2>
 *
 * <p>The W3C spec names the two rumble channels {@code weakMagnitude} (high-frequency, right motor)
 * and {@code strongMagnitude} (low-frequency, left motor). These map to FlixelGDX's parameters as
 * follows:
 *
 * <ul>
 *   <li>{@code leftIntensity} drives {@code strongMagnitude} (left, low-frequency motor)
 *   <li>{@code rightIntensity} drives {@code weakMagnitude} (right, high-frequency motor)
 * </ul>
 *
 * <h2>Browser support</h2>
 *
 * <p>The Gamepad Haptics API ({@code vibrationActuator.playEffect}) is supported in Chromium-based
 * browsers. Firefox does not expose {@code vibrationActuator}, so {@link #canVibrate} returns
 * {@code false} there, and vibration calls are silently skipped.
 */
final class FlixelTeaVMHapticsProvider implements FlixelHapticsProvider {

  @Override
  public void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs) {
    int index = browserIndex(slot);
    if (index < 0) {
      return;
    }
    float left = Math.max(0f, Math.min(1f, leftIntensity));
    float right = Math.max(0f, Math.min(1f, rightIntensity));
    doVibrateJS(index, (int) (durationSecs * 1000f), right, left);
  }

  @Override
  public void stopVibration(int slot) {
    int index = browserIndex(slot);
    if (index >= 0) {
      doVibrateJS(index, 0, 0f, 0f);
    }
  }

  @Override
  public boolean canVibrate(int slot) {
    int index = browserIndex(slot);
    return index >= 0 && canVibrateJS(index);
  }

  private static int browserIndex(int slot) {
    if (slot < 0 || slot >= Controllers.getControllers().size) {
      return -1;
    }
    String uid = Controllers.getControllers().get(slot).getUniqueId();
    try {
      return Integer.parseInt(uid);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }

  /**
   * Fetches the live gamepad reference from {@code navigator.getGamepads()} and calls
   * {@code vibrationActuator.playEffect('dual-rumble', ...)} with independent motor channels.
   *
   * <p>A fresh reference is obtained on every call to avoid stale cached objects, which can
   * lose their {@code vibrationActuator} property in some browser versions.
   */
  @JSBody(params = { "index", "duration", "weakMagnitude", "strongMagnitude" },
      script = "var gp = navigator.getGamepads ? navigator.getGamepads()[index] : null;"
          + "if (!gp || !gp.vibrationActuator || !gp.vibrationActuator.playEffect) return;"
          + "gp.vibrationActuator.playEffect('dual-rumble', {"
          + "startDelay: 0, duration: duration,"
          + "weakMagnitude: weakMagnitude, strongMagnitude: strongMagnitude});")
  private static native void doVibrateJS(int index, int duration,
      float weakMagnitude, float strongMagnitude);

  @JSBody(params = "index", script = "var gp = navigator.getGamepads ? navigator.getGamepads()[index] : null;"
      + "return !!(gp && gp.vibrationActuator && gp.vibrationActuator.playEffect);")
  private static native boolean canVibrateJS(int index);
}
