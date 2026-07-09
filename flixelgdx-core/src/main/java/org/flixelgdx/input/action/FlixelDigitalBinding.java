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
import org.flixelgdx.input.mouse.FlixelMouseButton;
import org.flixelgdx.input.touch.FlixelTouch;

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
 * jump.addBinding(FlixelDigitalBinding.touch(0));          // first finger
 * jump.addBinding(() -> myCustomSensor.isActive());        // custom source
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
   * @param button Mouse button index (for example {@link FlixelMouseButton#LEFT}).
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
   * Touch pointer binding using {@link org.flixelgdx.Flixel#touches Flixel.touches}.
   *
   * <pre>{@code
   * // Fire while the first finger is down.
   * shoot.addBinding(FlixelDigitalBinding.touch(0));
   * }</pre>
   *
   * @param pointer Zero-based pointer index (0 = first finger).
   * @return Binding that fires while the given pointer is in contact with the screen.
   */
  static FlixelDigitalBinding touch(int pointer) {
    return () -> Flixel.touches != null && Flixel.touches.enabled && Flixel.touches.pressed(pointer);
  }

  /**
   * Normalized screen region binding. Fires when any active touch pointer falls inside the
   * rectangle. Coordinates are fractions of the back buffer dimensions, with the origin at the
   * top-left corner (matching libGDX screen-space Y, where Y increases downward).
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
      if (Flixel.touches == null || !Flixel.touches.enabled) {
        return false;
      }
      int bw = Gdx.graphics.getBackBufferWidth();
      int bh = Gdx.graphics.getBackBufferHeight();
      if (bw <= 0 || bh <= 0) {
        return false;
      }
      float fx = 1f / bw;
      float fy = 1f / bh;
      int max = Flixel.touches.getMaxPointers();
      for (int p = 0; p < max; p++) {
        FlixelTouch t = Flixel.touches.list[p];
        if (!t.isPressed()) {
          continue;
        }
        float px = t.screenX * fx;
        float py = t.screenY * fy;
        if (px >= normX && px <= normX + normW && py >= normY && py <= normY + normH) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * World-space region binding. Fires when any active touch pointer's unprojected world position
   * falls inside the rectangle. Coordinates use the bottom-left origin (Y increases upward),
   * matching the standard game-world convention used by cameras and game objects.
   *
   * <p>World coordinates are taken from {@link FlixelTouch#worldX} and {@link FlixelTouch#worldY},
   * which are unprojected each frame via the touch manager's active camera (configurable with
   * {@link org.flixelgdx.input.touch.FlixelTouchManager#setWorldCamera FlixelTouchManager.setWorldCamera(...)}).
   *
   * <pre>{@code
   * // Virtual jump button occupying the left half of a 480x270 world.
   * jump.addBinding(FlixelDigitalBinding.touchRegionWorld(0f, 0f, 240f, 270f));
   * }</pre>
   *
   * @param x Left edge in world units.
   * @param y Bottom edge in world units.
   * @param w Width in world units.
   * @param h Height in world units.
   * @return Binding that fires while any touch pointer's world position is inside the region.
   */
  static FlixelDigitalBinding touchRegionWorld(float x, float y, float w, float h) {
    return () -> {
      if (Flixel.touches == null || !Flixel.touches.enabled) {
        return false;
      }
      int max = Flixel.touches.getMaxPointers();
      for (int p = 0; p < max; p++) {
        FlixelTouch t = Flixel.touches.list[p];
        if (!t.isPressed()) {
          continue;
        }
        if (t.worldX >= x && t.worldX <= x + w && t.worldY >= y && t.worldY <= y + h) {
          return true;
        }
      }
      return false;
    };
  }
}
