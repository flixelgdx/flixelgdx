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

import com.badlogic.gdx.math.Vector2;

import org.flixelgdx.Flixel;
import org.flixelgdx.input.gamepad.FlixelGamepadInput;

/**
 * Two-axis input contributor for a {@link FlixelActionAnalog}.
 *
 * <p>Each binding adds its contribution to a shared {@link Vector2} accumulator each frame.
 * Multiple bindings on the same action are summed, then normalized to a maximum length of 1 so
 * diagonals do not exceed unit speed when mixing keys and sticks.
 *
 * <p>Create bindings only during setup, not each frame. The static factory methods cover the
 * common cases (keyboard halves, gamepad axes). For anything else, pass a plain lambda:
 *
 * <pre>{@code
 * move.addBinding(FlixelAnalogBinding.negXKey(FlixelKey.A));
 * move.addBinding(FlixelAnalogBinding.posXKey(FlixelKey.D));
 * move.addBinding(FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadInput.AXIS_LEFT_X));
 * move.addBinding(FlixelAnalogBinding.gamepadAxisY(0, FlixelGamepadInput.AXIS_LEFT_Y));
 * move.addBinding(out -> out.x += myJoystick.getX()); // Custom contributor.
 * }</pre>
 *
 * <h2>Axis conventions</h2>
 *
 * <p>Key bindings contribute exactly -1 or +1 per axis. Gamepad axes contribute a smooth value in
 * the range -1..1. {@link #gamepadAxisY(int, int)} corrects the hardware Y axis (where up is
 * negative in screen-space) so that up = positive Y, matching key bindings. Use
 * {@link #gamepadAxisY(int, int, boolean) gamepadAxisY(slot, axis, true)} when you need the raw
 * hardware value instead.
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
  void accumulate(Vector2 out);

  /**
   * Subtracts 1 from {@code out.x} while the key is held (left / negative X).
   *
   * @param keycode libGDX keycode.
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
   * @param keycode libGDX keycode.
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
   * Subtracts 1 from {@code out.y} while the key is held (down / negative Y).
   *
   * @param keycode libGDX keycode.
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
   * Adds 1 to {@code out.y} while the key is held (up / positive Y).
   *
   * @param keycode libGDX keycode.
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
   * Adds the X component of a gamepad stick. Most drivers report X in math-space already (left is
   * negative, right is positive), so no correction is applied.
   *
   * @param slot Gamepad slot (0 and up).
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_X} or similar.
   * @return Binding that contributes the stick X value.
   */
  static FlixelAnalogBinding gamepadAxisX(int slot, int logicalAxis) {
    return out -> {
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.x += Flixel.gamepads.getAxis(slot, logicalAxis);
      }
    };
  }

  /**
   * Adds the Y component of a gamepad stick with the screen-space inversion corrected so that
   * up = positive Y, matching key bindings. This is the right choice for almost every game.
   *
   * @param slot Gamepad slot (0 and up).
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_Y} or similar.
   * @return Binding that contributes the stick Y value (up = positive).
   */
  static FlixelAnalogBinding gamepadAxisY(int slot, int logicalAxis) {
    return gamepadAxisY(slot, logicalAxis, false);
  }

  /**
   * Adds the Y component of a gamepad stick.
   *
   * @param slot Gamepad slot (0 and up).
   * @param logicalAxis {@link FlixelGamepadInput#AXIS_LEFT_Y} or similar.
   * @param raw When {@code false} (the default), the raw hardware Y is negated so that up = positive
   *   Y, matching key bindings. Pass {@code true} to use the raw screen-space value unchanged.
   * @return Binding that contributes the stick Y value.
   */
  static FlixelAnalogBinding gamepadAxisY(int slot, int logicalAxis, boolean raw) {
    if (raw) {
      return out -> {
        if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
          out.y += Flixel.gamepads.getAxis(slot, logicalAxis);
        }
      };
    }
    return out -> {
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.y -= Flixel.gamepads.getAxis(slot, logicalAxis);
      }
    };
  }
}
