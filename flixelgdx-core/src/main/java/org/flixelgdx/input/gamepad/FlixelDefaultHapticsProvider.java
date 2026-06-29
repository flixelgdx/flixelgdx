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

import com.badlogic.gdx.controllers.Controller;

/**
 * Default {@link FlixelHapticsProvider} backed by gdx-controllers'
 * {@link Controller#startVibration}.
 *
 * <p>Works on desktop (SDL via {@code gdx-controllers-desktop}) and web (W3C Gamepad API via
 * {@code gdx-controllers-teavm}) without any extra setup. Both platforms expose vibration through
 * the same {@link Controller} interface, so no backend-specific code is needed here.
 *
 * <p>Because the underlying API accepts a single unified strength rather than separate motor
 * channels, {@code leftIntensity} and {@code rightIntensity} are collapsed to
 * {@code Math.max(leftIntensity, rightIntensity)} before the call. Both values are clamped to
 * {@code [0, 1]} first. For true independent dual-motor control, supply a custom
 * {@link FlixelHapticsProvider} via {@link FlixelGamepadManager#setHapticsProvider}.
 */
final class FlixelDefaultHapticsProvider implements FlixelHapticsProvider {

  private final FlixelGamepadManager manager;

  FlixelDefaultHapticsProvider(FlixelGamepadManager manager) {
    this.manager = manager;
  }

  @Override
  public void vibrate(int slot, float leftIntensity, float rightIntensity, float durationSecs) {
    Controller c = manager.controllerAt(slot);
    if (c == null || !c.canVibrate()) {
      return;
    }
    float left = Math.max(0f, Math.min(1f, leftIntensity));
    float right = Math.max(0f, Math.min(1f, rightIntensity));
    c.startVibration((int) (durationSecs * 1000f), Math.max(left, right));
  }

  @Override
  public void stopVibration(int slot) {
    Controller c = manager.controllerAt(slot);
    if (c != null) {
      c.cancelVibration();
    }
  }

  @Override
  public boolean canVibrate(int slot) {
    Controller c = manager.controllerAt(slot);
    return c != null && c.canVibrate();
  }
}
