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
import org.flixelgdx.input.gamepad.FlixelGamepadAxis;
import org.flixelgdx.math.FlixelVector;

/**
 * Two-axis input contributor for a {@link FlixelActionAnalog}.
 *
 * <p>Each binding adds its contribution to a shared {@link FlixelVector} accumulator each frame.
 * Multiple bindings on the same action are summed, then normalized to a maximum length of 1 so
 * diagonals do not exceed unit speed when mixing keys and sticks.
 *
 * <p>Create bindings only during setup, not each frame. The static factory methods cover the
 * common cases (keyboard halves, gamepad axes). For anything else, pass a plain lambda:
 *
 * <pre>{@code
 * move.addBinding("negX",   FlixelAnalogBinding.negXKey(FlixelKey.A));
 * move.addBinding("posX",   FlixelAnalogBinding.posXKey(FlixelKey.D));
 * move.addBinding("stickX", FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadAxis.LEFT_X));
 * move.addBinding("stickY", FlixelAnalogBinding.gamepadAxisY(0, FlixelGamepadAxis.LEFT_Y));
 * move.addBinding("custom", out -> out.x += myJoystick.getX());
 * }</pre>
 *
 * <h2>Axis conventions</h2>
 *
 * <p>Analog actions use the same y-down coordinate system as the rest of the framework: negative X
 * is left, positive X is right, negative Y is up, and positive Y is down. That means an action's
 * vector can be fed straight into world movement, such as {@code player.setVelocityY(move.getY() * 200f)},
 * without flipping any signs.
 *
 * <p>Key bindings contribute exactly -1 or +1 per axis. Gamepad axes contribute a smooth value in
 * the range -1..1, passed through exactly as the device reports it. Every supported backend already
 * reports stick Y as down-positive, so no correction is needed to match key bindings.
 *
 * @see FlixelActionAnalog
 */
@FunctionalInterface
public interface FlixelAnalogBinding {

  /**
   * Adds this binding's contribution to {@code out}.
   *
   * @param out Accumulator to modify in place.
   */
  void accumulate(FlixelVector out);

  /**
   * Subtracts 1 from {@code out.x} while the key is held (left / negative X).
   *
   * @param keycode Keycode.
   * @return Binding that contributes -1 on X when the key is pressed.
   */
  static FlixelAnalogBinding negXKey(int keycode) {
    return out -> {
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(keycode)) {
        out.x -= 1f;
      }
    };
  }

  /**
   * Adds 1 to {@code out.x} while the key is held (right / positive X).
   *
   * @param keycode Keycode.
   * @return Binding that contributes +1 on X when the key is pressed.
   */
  static FlixelAnalogBinding posXKey(int keycode) {
    return out -> {
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(keycode)) {
        out.x += 1f;
      }
    };
  }

  /**
   * Subtracts 1 from {@code out.y} while the key is held (up / negative Y).
   *
   * @param keycode Keycode.
   * @return Binding that contributes -1 on Y when the key is pressed.
   */
  static FlixelAnalogBinding negYKey(int keycode) {
    return out -> {
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(keycode)) {
        out.y -= 1f;
      }
    };
  }

  /**
   * Adds 1 to {@code out.y} while the key is held (down / positive Y).
   *
   * @param keycode Keycode.
   * @return Binding that contributes +1 on Y when the key is pressed.
   */
  static FlixelAnalogBinding posYKey(int keycode) {
    return out -> {
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(keycode)) {
        out.y += 1f;
      }
    };
  }

  /**
   * Adds the X component of a gamepad stick, passed through as the device reports it (left is
   * negative, right is positive).
   *
   * @param slot Gamepad slot (0 and up).
   * @param axis {@link FlixelGamepadAxis#LEFT_X} or similar.
   * @return Binding that contributes the stick X value.
   */
  static FlixelAnalogBinding gamepadAxisX(int slot, FlixelGamepadAxis axis) {
    return out -> {
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.x += Flixel.gamepads.getAxis(slot, axis);
      }
    };
  }

  /**
   * Adds the Y component of a gamepad stick, passed through as the device reports it (up is
   * negative, down is positive).
   *
   * <p>This already matches the framework's y-down coordinate system and the Y key bindings, so the
   * stick and keys can share one action without any sign flips.
   *
   * @param slot Gamepad slot (0 and up).
   * @param axis {@link FlixelGamepadAxis#LEFT_Y} or similar.
   * @return Binding that contributes the stick Y value (down = positive).
   */
  static FlixelAnalogBinding gamepadAxisY(int slot, FlixelGamepadAxis axis) {
    return out -> {
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.y += Flixel.gamepads.getAxis(slot, axis);
      }
    };
  }
}
