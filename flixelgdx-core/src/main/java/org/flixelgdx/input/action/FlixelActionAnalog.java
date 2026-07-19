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
import com.badlogic.gdx.utils.ObjectMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Two-axis vector built from {@link FlixelAnalogBinding} contributors plus optional Steam analog
 * for {@link #getName()}.
 *
 * <h2>How values combine</h2>
 *
 * <p>Each frame, key halves add {@code -1}, {@code 0}, or {@code +1} per axis;
 * {@link org.flixelgdx.Flixel#gamepads Flixel.gamepads} axis bindings add smooth stick values.
 * Steam {@link FlixelSteamActionReader#getAnalogX} / {@code getAnalogY} are added on top. The
 * result is clamped to a maximum length of {@code 1} so diagonals do not exceed unit speed when
 * mixing keys and sticks.
 *
 * <h2>Typical setup</h2>
 *
 * <pre>{@code
 * move = new FlixelActionAnalog("move");
 * move.addBinding("negX", FlixelAnalogBinding.negXKey(FlixelKey.LEFT));
 * move.addBinding("posX", FlixelAnalogBinding.posXKey(FlixelKey.RIGHT));
 * move.addBinding("negY", FlixelAnalogBinding.negYKey(FlixelKey.DOWN));
 * move.addBinding("posY", FlixelAnalogBinding.posYKey(FlixelKey.UP));
 * move.addBinding("stickX", FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadInput.AXIS_LEFT_X));
 * move.addBinding("stickY", FlixelAnalogBinding.gamepadAxisY(0, FlixelGamepadInput.AXIS_LEFT_Y));
 * }</pre>
 *
 * <h2>Reading</h2>
 *
 * <p>Use {@link #getX()} and {@link #getY()} after {@code super.update(elapsed)} in your state.
 * {@link #getPrevX()} / {@link #getPrevY()} mirror the previous frame after
 * {@link FlixelActionSet#endFrame()}. {@link #moved()} is a small helper for non-zero length.
 *
 * <h2>Flick detection and hold-repeat</h2>
 *
 * <p>{@link #flicked()} returns {@code true} for exactly one frame when the stick first crosses
 * {@link #flickThreshold}. It resets to {@code false} as long as the stick stays past the
 * threshold, and fires again only after the stick returns below the threshold and crosses it again.
 * This mirrors the single-frame contract of
 * {@link FlixelActionDigital#justPressed() FlixelActionDigital.justPressed()} and is useful for
 * menu navigation where each stick deflection should trigger exactly one action.
 *
 * <p>{@link #flickedRepeating()} extends that with hold-repeat: it fires on the initial flick,
 * then fires again after {@link FlixelAction#getHoldDelay() FlixelAction.holdDelay} seconds if
 * the stick is still past the threshold, and continues every
 * {@link FlixelAction#getHoldInterval() FlixelAction.holdInterval} seconds after that. Use this
 * for menus that should keep scrolling when the stick is held.
 *
 * <pre>{@code
 * // Navigate a menu; hold the stick to keep scrolling.
 * if (navigate.flickedRepeating()) {
 *   if (navigate.getY() > 0) selectPreviousItem();
 *   else if (navigate.getY() < 0) selectNextItem();
 * }
 * }</pre>
 *
 * <p>Key bindings contribute {@code +-1.0} per axis, so pressing any bound key immediately exceeds
 * the default threshold and fires both methods on that frame. Adjust {@link #flickThreshold} before
 * the game loop if your game needs a different sensitivity.
 */
public final class FlixelActionAnalog extends FlixelAction {

  /**
   * Minimum stick magnitude (0 to 1) required for {@link #flicked()} and
   * {@link #flickedRepeating()} to fire. The comparison is made against the normalized vector
   * length after all bindings are accumulated, so a value of {@code 0.3} means roughly 30%
   * deflection. Defaults to {@code 0.3f}; adjust before the game loop if your game needs a
   * different sensitivity.
   */
  private float flickThreshold = 0.3f;

  private final ObjectMap<String, FlixelAnalogBinding> namedBindings = new ObjectMap<>(12);

  private final Vector2 scratch = new Vector2();

  private float x;
  private float y;
  private float prevX;
  private float prevY;
  private float flickHoldAccum;

  private boolean flickHoldRepeating;
  private boolean flickRepeated;
  private boolean flickState;
  private boolean prevFlickState;

  public FlixelActionAnalog(@Nullable String name) {
    super(name);
  }

  /**
   * Removes all bindings from this action.
   *
   * <p>Use this before re-populating bindings from scratch, or to leave an action with no active
   * sources (it will produce a zero vector until at least one binding is added again).
   */
  public void clearBindings() {
    namedBindings.clear();
  }

  /**
   * Adds a binding under a named slot (allocation-free after this call).
   *
   * <p>If a binding is already registered under {@code slot}, it is removed and replaced. Named
   * slots are the natural model for a rebinding screen: one slot per axis or device, replaced
   * in-place when the player picks a new key or stick.
   *
   * <pre>{@code
   * move.addBinding("leftKey", FlixelAnalogBinding.negXKey(FlixelKeys.LEFT));
   * move.addBinding("rightKey", FlixelAnalogBinding.posXKey(FlixelKeys.RIGHT));
   * move.addBinding("stick", FlixelAnalogBinding.gamepadAxisX(0, FlixelGamepadInput.AXIS_LEFT_X));
   *
   * // Player rebinds the left key at runtime.
   * move.addBinding("leftKey", FlixelAnalogBinding.negXKey(newKey));
   * }</pre>
   *
   * @param slot Non-null, non-empty identifier for this binding (for example {@code "leftKey"}).
   * @param binding Non-null binding.
   */
  public void addBinding(@NotNull String slot, @NotNull FlixelAnalogBinding binding) {
    var s = Objects.requireNonNull(slot, "slot cannot be null.");
    var b = Objects.requireNonNull(binding, "binding cannot be null.");
    namedBindings.put(s, b);
  }

  /**
   * Removes the binding registered under the given slot name.
   *
   * @param slot Slot name passed to {@link #addBinding(String, FlixelAnalogBinding)}.
   * @return {@code true} if a binding was found and removed, {@code false} if the slot was unknown.
   */
  public boolean removeBinding(@NotNull String slot) {
    FlixelAnalogBinding old = namedBindings.remove(Objects.requireNonNull(slot, "slot cannot be null."));
    return old != null;
  }

  /**
   * Removes a specific binding by reference identity.
   *
   * <p>If the binding was added via {@link #addBinding(String, FlixelAnalogBinding)}, its slot
   * entry is also cleared.
   *
   * @param binding The exact binding instance to remove.
   * @return {@code true} if the binding was found and removed.
   */
  public boolean removeBinding(@NotNull FlixelAnalogBinding binding) {
    ObjectMap.Keys<String> keys = namedBindings.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      if (namedBindings.get(key) == binding) {
        keys.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the binding registered under the given slot name, or {@code null} if the slot is
   * unknown.
   *
   * <p>The primary use is a rebinding screen that needs to read the currently active binding in
   * order to display it. Note that a {@link FlixelAnalogBinding} is a functional interface, so the
   * returned instance is only meaningful when compared by reference or when your code holds a
   * separate record of what the binding represents (for example, a keycode stored alongside it).
   *
   * @param slot Slot name passed to {@link #addBinding(String, FlixelAnalogBinding)}.
   * @return The registered binding, or {@code null} if no binding is registered under that slot.
   */
  @Nullable
  public FlixelAnalogBinding getBinding(@NotNull String slot) {
    return namedBindings.get(Objects.requireNonNull(slot, "slot cannot be null."));
  }

  @Override
  void updateAction(float elapsed) {
    if (!active) {
      x = 0f;
      y = 0f;
      flickState = false;
      flickHoldAccum = 0f;
      flickHoldRepeating = false;
      flickRepeated = false;
      return;
    }
    scratch.set(0f, 0f);
    ObjectMap.Values<FlixelAnalogBinding> vals = namedBindings.values();
    while (vals.hasNext()) {
      vals.next().accumulate(scratch);
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
    if (flickState) {
      if (!prevFlickState) {
        flickHoldAccum = 0f;
        flickHoldRepeating = false;
        flickRepeated = true;
      } else {
        flickHoldAccum += elapsed;
        flickRepeated = false;
        if (!flickHoldRepeating) {
          if (flickHoldAccum >= getHoldDelay()) {
            flickHoldAccum -= getHoldDelay();
            flickHoldRepeating = true;
            flickRepeated = true;
          }
        } else if (flickHoldAccum >= getHoldInterval()) {
          flickHoldAccum -= getHoldInterval();
          flickRepeated = true;
        }
      }
    } else {
      flickHoldAccum = 0f;
      flickHoldRepeating = false;
      flickRepeated = false;
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
    flickHoldAccum = 0f;
    flickHoldRepeating = false;
    flickRepeated = false;
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

  public float getFlickThreshold() {
    return flickThreshold;
  }

  public void setFlickThreshold(float flickThreshold) {
    this.flickThreshold = Math.max(0f, Math.min(1f, flickThreshold));
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
   * {@link FlixelActionDigital#justPressed() FlixelActionDigital.justPressed()}, making it safe to
   * use for menu navigation where one deflection should trigger exactly one action.
   *
   * <p>Key and button bindings contribute {@code +-1.0} per axis, so any bound key press that
   * brings the magnitude past {@link #flickThreshold} fires {@code flicked()} on that frame.
   *
   * @return {@code true} for one frame when the stick crosses the threshold from below.
   */
  public boolean flicked() {
    return active && flickState && !prevFlickState;
  }

  /**
   * Returns {@code true} on the initial flick and again on each hold-repeat tick.
   *
   * <p>Fires on the same frame as {@link #flicked()} when the stick first crosses
   * {@link #flickThreshold}, then fires again after
   * {@link FlixelAction#getHoldDelay() FlixelAction.holdDelay} seconds if the stick is still past
   * the threshold, and continues every
   * {@link FlixelAction#getHoldInterval() FlixelAction.holdInterval} seconds after that. Returning
   * the stick below the threshold resets the timer.
   *
   * <p>Use this instead of {@link #flicked()} when a held stick deflection should keep triggering,
   * such as scrolling through a long menu list.
   *
   * <pre>{@code
   * if (navigate.flickedRepeating()) {
   *   if (navigate.getY() > 0) selectPreviousItem();
   *   else if (navigate.getY() < 0) selectNextItem();
   * }
   * }</pre>
   *
   * @return {@code true} on the initial flick frame and on each repeat tick while held past the threshold.
   */
  public boolean flickedRepeating() {
    return active && flickRepeated;
  }
}
