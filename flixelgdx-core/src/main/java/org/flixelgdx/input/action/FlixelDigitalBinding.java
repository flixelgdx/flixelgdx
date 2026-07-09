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

import com.badlogic.gdx.Gdx;

import org.flixelgdx.Flixel;

/**
 * Boolean input contributor for a {@link FlixelActionDigital}.
 *
 * <p>Each binding answers one question per frame: is this input active right now? Multiple
 * bindings added to the same action are OR'd together, so the action fires if any one of them
 * returns {@code true}.
 *
 * <p>Create bindings only during setup, not each frame. The static factory methods cover the
 * common cases (keyboard, mouse, gamepad, touch region). For anything else, pass a plain lambda:
 *
 * <pre>{@code
 * jump.addBinding(FlixelDigitalBinding.key(FlixelKey.SPACE));
 * jump.addBinding(FlixelDigitalBinding.gamepadButton(0, FlixelGamepadInput.A));
 * jump.addBinding(() -> myCustomSensor.isActive()); // custom source
 * }</pre>
 *
 * @see FlixelActionDigital
 */
@FunctionalInterface
public interface FlixelDigitalBinding {

  /** Pass to {@link #gamepadButton(int, int)} so any active gamepad slot counts. */
  int GAMEPAD_SLOT_ANY = -1;

  /**
   * Returns {@code true} if this binding's input is active this frame.
   *
   * @return {@code true} when the bound input is currently triggered.
   */
  boolean evaluate();

  /**
   * Keyboard key binding using {@link org.flixelgdx.Flixel#keys Flixel.keys}.
   *
   * @param keycode libGDX keycode (for example {@link com.badlogic.gdx.Input.Keys#SPACE}).
   * @return Binding that fires while the key is held.
   */
  static FlixelDigitalBinding key(int keycode) {
    return () -> Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(keycode);
  }

  /**
   * Mouse button binding using {@link org.flixelgdx.Flixel#mouse Flixel.mouse}.
   *
   * @param button libGDX button index (for example {@link com.badlogic.gdx.Input.Buttons#LEFT}).
   * @return Binding that fires while the button is held.
   */
  static FlixelDigitalBinding mouseButton(int button) {
    return () -> Flixel.mouse != null && Flixel.mouse.enabled && Flixel.mouse.pressed(button);
  }

  /**
   * Gamepad button binding using {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}.
   *
   * @param slot Gamepad slot (0 and up), or {@link #GAMEPAD_SLOT_ANY} to match any connected slot.
   * @param logicalButton Logical button value from
   *   {@link org.flixelgdx.input.gamepad.FlixelGamepadInput FlixelGamepadInput}.
   * @return Binding that fires while the button is held on the given slot.
   */
  static FlixelDigitalBinding gamepadButton(int slot, int logicalButton) {
    return () -> {
      if (Flixel.gamepads == null || !Flixel.gamepads.enabled) {
        return false;
      }
      if (slot == GAMEPAD_SLOT_ANY) {
        return Flixel.gamepads.anyPressed(logicalButton);
      }
      return Flixel.gamepads.pressed(slot, logicalButton);
    };
  }

  /**
   * Normalized screen region binding. Fires when any active touch pointer falls inside the
   * rectangle. Coordinates are fractions of the back buffer dimensions, with the origin at the
   * top-left corner (matching libGDX screen-space Y).
   *
   * <pre>{@code
   * // Bottom-left quarter of the screen as a virtual jump button.
   * jump.addBinding(FlixelDigitalBinding.touchRegion(0f, 0.5f, 0.5f, 0.5f));
   * }</pre>
   *
   * @param normX Left edge as a fraction of the back buffer width (0..1).
   * @param normY Top edge as a fraction of the back buffer height (0..1).
   * @param normW Width as a fraction of the back buffer width (0..1).
   * @param normH Height as a fraction of the back buffer height (0..1).
   * @return Binding that fires while any touch pointer is inside the region.
   */
  static FlixelDigitalBinding touchRegion(float normX, float normY, float normW, float normH) {
    return () -> {
      int bw = Gdx.graphics.getBackBufferWidth();
      int bh = Gdx.graphics.getBackBufferHeight();
      if (bw <= 0 || bh <= 0) {
        return false;
      }
      float fx = 1f / bw;
      float fy = 1f / bh;
      for (int p = 0; p <= 20; p++) {
        if (!Gdx.input.isTouched(p)) {
          continue;
        }
        float px = Gdx.input.getX(p) * fx;
        float py = Gdx.input.getY(p) * fy;
        if (px >= normX && px <= normX + normW && py >= normY && py <= normY + normH) {
          return true;
        }
      }
      return false;
    };
  }
}
