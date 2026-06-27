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
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Two-axis vector built from {@link FlixelAnalogAxisBinding} contributors plus optional Steam analog for {@link #getName()}.
 *
 * <h2>How values combine</h2>
 *
 * <p>Each frame, key halves add {@code -1}, {@code 0}, or {@code +1} per axis; {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads}
 * axis bindings add smooth stick values. Steam {@link FlixelSteamActionReader#getAnalogX} / {@code getAnalogY} are added
 * on top. The result is clamped to a maximum length of {@code 1} so diagonals do not exceed unit speed when mixing keys
 * and sticks.
 *
 * <h2>Typical setup</h2>
 *
 * <pre>{@code
 * move = new FlixelActionAnalog("move");
 * move.addAxisBinding(FlixelAnalogAxisBinding.negXKey(FlixelKey.LEFT));
 * move.addAxisBinding(FlixelAnalogAxisBinding.posXKey(FlixelKey.RIGHT));
 * move.addAxisBinding(FlixelAnalogAxisBinding.negYKey(FlixelKey.DOWN));
 * move.addAxisBinding(FlixelAnalogAxisBinding.posYKey(FlixelKey.UP));
 * move.addAxisBinding(FlixelAnalogAxisBinding.gamepadAxisX(0, FlixelGamepadInput.AXIS_LEFT_X));
 * move.addAxisBinding(FlixelAnalogAxisBinding.gamepadAxisY(0, FlixelGamepadInput.AXIS_LEFT_Y));
 * }</pre>
 *
 * <h2>Reading</h2>
 *
 * <p>Use {@link #getX()} and {@link #getY()} after {@code super.update(elapsed)} in your state. {@link #getPrevX()} / {@link #getPrevY()}
 * mirror the previous frame after {@link FlixelActionSet#endFrame()}. {@link #moved()} is a small helper for non-zero length.
 *
 * <h2>Flick detection</h2>
 *
 * <p>{@link #flicked()} returns {@code true} for exactly one frame when the stick first crosses {@link #flickThreshold}.
 * It resets to {@code false} as long as the stick stays past the threshold, and fires again only after the stick
 * returns below the threshold and crosses it again. This mirrors the single-frame contract of
 * {@link FlixelActionDigital#justPressed() FlixelActionDigital.justPressed()} and is useful for menu navigation
 * where each stick deflection should trigger exactly one action.
 *
 * <pre>{@code
 * // Navigate a menu with a single stick deflection.
 * if (navigate.flicked()) {
 *   if (navigate.getY() < 0) selectNextItem();
 *   else if (navigate.getY() > 0) selectPreviousItem();
 * }
 * }</pre>
 *
 * <p>Key bindings contribute {@code +-1.0} per axis, so pressing any bound key immediately exceeds the default threshold
 * and fires {@link #flicked()} on that frame. Adjust {@link #flickThreshold} before the game loop if your game needs
 * a different sensitivity.
 */
public final class FlixelActionAnalog extends FlixelAction {

  /**
   * Minimum stick magnitude (0 to 1) required for {@link #flicked()} to fire. The comparison is
   * made against the normalized vector length after all bindings are accumulated, so a value of
   * {@code 0.3} means roughly 30% deflection. Defaults to {@code 0.3f}; adjust before the game
   * loop if your game needs a different sensitivity.
   */
  public float flickThreshold = 0.3f;

  private final Array<FlixelAnalogAxisBinding> bindings = new Array<>(12);

  private final Vector2 scratch = new Vector2();

  private float x;
  private float y;
  private float prevX;
  private float prevY;

  private boolean flickState;
  private boolean prevFlickState;

  public FlixelActionAnalog(@Nullable String name) {
    super(name);
  }

  public void addAxisBinding(@NotNull FlixelAnalogAxisBinding binding) {
    bindings.add(binding);
  }

  @Override
  void updateAction(float elapsed) {
    if (!active) {
      x = 0f;
      y = 0f;
      flickState = false;
      return;
    }
    scratch.set(0f, 0f);
    for (int i = 0, n = bindings.size; i < n; i++) {
      accumulate(bindings.get(i), scratch);
    }
    FlixelSteamActionReader steam = owner != null ? owner.steamReader : null;
    if (steam != null) {
      scratch.x += steam.getAnalogX(getName());
      scratch.y += steam.getAnalogY(getName());
    }
    float sx = scratch.x;
    float sy = scratch.y;
    float len = (float) Math.sqrt(sx * sx + sy * sy);
    if (len > 1f && len > 1e-6f) {
      sx /= len;
      sy /= len;
    }
    x = sx;
    y = sy;
    float ft = flickThreshold;
    flickState = (x * x + y * y) >= ft * ft;
  }

  private static void accumulate(@NotNull FlixelAnalogAxisBinding b, @NotNull Vector2 out) {
    switch (b.kind) {
      case KEY_NEG_X -> {
        if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
          out.x -= 1f;
        }
      }
      case KEY_POS_X -> {
        if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
          out.x += 1f;
        }
      }
      case KEY_NEG_Y -> {
        if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
          out.y -= 1f;
        }
      }
      case KEY_POS_Y -> {
        if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
          out.y += 1f;
        }
      }
      case GAMEPAD_AXIS_X -> {
        if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
          out.x += Flixel.gamepads.getAxis(b.gamepadSlot, b.keyOrAxis);
        }
      }
      case GAMEPAD_AXIS_Y -> {
        if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
          out.y += Flixel.gamepads.getAxis(b.gamepadSlot, b.keyOrAxis);
        }
      }
      default -> {
      }
    }
  }

  @Override
  void endFrameAction() {
    prevX = x;
    prevY = y;
    prevFlickState = flickState;
  }

  @Override
  void resetAction() {
    x = 0f;
    y = 0f;
    prevX = 0f;
    prevY = 0f;
    flickState = false;
    prevFlickState = false;
  }

  public float getX() {
    return active ? x : 0f;
  }

  public float getY() {
    return active ? y : 0f;
  }

  public float getPrevX() {
    return active ? prevX : 0f;
  }

  public float getPrevY() {
    return active ? prevY : 0f;
  }

  public boolean moved() {
    if (!active) {
      return false;
    }
    return Math.abs(x) > 1e-4f || Math.abs(y) > 1e-4f;
  }

  /**
   * Returns {@code true} for the single frame when the stick first crosses {@link #flickThreshold}.
   *
   * <p>Stays {@code false} while the stick remains past the threshold, and fires again only after
   * the stick drops below it and crosses it once more. This mirrors the single-frame contract of
   * {@link FlixelActionDigital#justPressed() FlixelActionDigital.justPressed()}, making it safe
   * to use for menu navigation where one deflection should trigger exactly one action.
   *
   * <p>Key and button bindings contribute {@code +-1.0} per axis, so any bound key press that
   * brings the magnitude past {@link #flickThreshold} fires {@code flicked()} on that frame.
   *
   * @return {@code true} for one frame when the stick crosses the threshold from below.
   */
  public boolean flicked() {
    return active && flickState && !prevFlickState;
  }
}
