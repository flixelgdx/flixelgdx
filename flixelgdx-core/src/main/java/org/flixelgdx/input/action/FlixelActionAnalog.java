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
 * <p>Each frame, key halves add {@code -1}, {@code 0}, or {@code +1} per axis; {@link org.flixelgdx.Flixel#gamepads}
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
 */
public final class FlixelActionAnalog extends FlixelAction {

  private final Array<FlixelAnalogAxisBinding> bindings = new Array<>(12);

  private final Vector2 scratch = new Vector2();

  private float x;
  private float y;
  private float prevX;
  private float prevY;

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
  }

  private static void accumulate(@NotNull FlixelAnalogAxisBinding b, @NotNull Vector2 out) {
    switch (b.kind) {
    case KEY_NEG_X:
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
        out.x -= 1f;
      }
      break;
    case KEY_POS_X:
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
        out.x += 1f;
      }
      break;
    case KEY_NEG_Y:
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
        out.y -= 1f;
      }
      break;
    case KEY_POS_Y:
      if (Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.keyOrAxis)) {
        out.y += 1f;
      }
      break;
    case GAMEPAD_AXIS_X:
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.x += Flixel.gamepads.getAxis(b.gamepadSlot, b.keyOrAxis);
      }
      break;
    case GAMEPAD_AXIS_Y:
      if (Flixel.gamepads != null && Flixel.gamepads.enabled) {
        out.y += Flixel.gamepads.getAxis(b.gamepadSlot, b.keyOrAxis);
      }
      break;
    default :
      break;
    }
  }

  @Override
  void endFrameAction() {
    prevX = x;
    prevY = y;
  }

  @Override
  void resetAction() {
    x = 0f;
    y = 0f;
    prevX = 0f;
    prevY = 0f;
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
}
