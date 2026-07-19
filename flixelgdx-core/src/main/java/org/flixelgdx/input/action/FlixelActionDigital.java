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

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Boolean action: any {@link FlixelDigitalBinding} that evaluates true, OR optional Steam digital
 * for the same {@link #getName()}, makes {@link #pressed()} true for this frame.
 *
 * <h2>Setup</h2>
 *
 * <p>Call {@link #addBinding(FlixelDigitalBinding)} only during construction or loading (not each
 * frame). Multiple bindings are OR'd: a {@code jump} action might accept Space, gamepad A, and a
 * touch region.
 *
 * <h2>Reading in gameplay</h2>
 *
 * <ul>
 *   <li>{@link #pressed()} while a key is held (sustains, movement gates).</li>
 *   <li>{@link #justPressed()} for a single-frame edge (tap notes, menu confirm).</li>
 *   <li>{@link #justReleased()} when the player releases after a hold.</li>
 *   <li>{@link #held()} for hold-repeating navigation: fires on the initial press, then fires
 *       again after {@link FlixelAction#getHoldDelay()} seconds, then every
 *       {@link FlixelAction#getHoldInterval()} seconds while held. Replaces a manual
 *       {@code justPressed()} check when autorepeat is needed.</li>
 * </ul>
 *
 * <p>State is refreshed in {@link FlixelActionSet#update(float)} (via
 * {@link FlixelActionSets#update(float)}). {@link FlixelActionSet#endFrame()} (via
 * {@link FlixelActionSets#endFrameAll()}) runs after
 * {@link org.flixelgdx.FlixelGame#render() FlixelGame.render()} finalizes keys and mouse, matching
 * their {@code justPressed} timing.
 *
 * <h2>Optional callback</h2>
 *
 * <p>{@link FlixelAction#callback} runs on the press edge when assigned; prefer a single static
 * {@link Runnable} to avoid allocating lambdas in hot paths.
 */
public final class FlixelActionDigital extends FlixelAction {

  private final Array<FlixelDigitalBinding> bindings = new Array<>(8);
  @Nullable
  private ObjectMap<String, FlixelDigitalBinding> namedBindings;

  private float holdAccum;

  private boolean holdRepeating;
  private boolean held;
  private boolean pressed;
  private boolean previous;

  public FlixelActionDigital(@Nullable String name) {
    super(name);
  }

  /**
   * Removes all bindings from this action, including any registered under named slots.
   *
   * <p>Use this before re-populating bindings from scratch, or to leave an action with no active
   * sources (it will never fire until at least one binding is added again).
   */
  public void clearBindings() {
    bindings.clear();
    if (namedBindings != null) {
      namedBindings.clear();
    }
  }

  /**
   * Adds an unnamed binding evaluated each frame (allocation-free after this call).
   *
   * <p>Multiple bindings are OR'd: the action fires if any one returns {@code true}. Prefer
   * {@link #addBinding(String, FlixelDigitalBinding)} when you need to replace or remove this
   * binding later (for example, a rebinding screen).
   *
   * @param binding Non-null binding.
   */
  public void addBinding(@NotNull FlixelDigitalBinding binding) {
    bindings.add(binding);
  }

  /**
   * Adds a binding under a named slot (allocation-free after this call).
   *
   * <p>If a binding is already registered under {@code slot}, it is removed and replaced. This
   * makes named slots the natural model for a rebinding screen: one slot per device type, replaced
   * in-place when the player picks a new key.
   *
   * <pre>{@code
   * jump.addBinding("keyboard", FlixelDigitalBinding.key(Input.Keys.SPACE));
   * jump.addBinding("gamepad",  FlixelDigitalBinding.gamepadButton(0, FlixelGamepadInput.A));
   *
   * // Player rebinds the keyboard slot at runtime.
   * jump.addBinding("keyboard", FlixelDigitalBinding.key(newKey));
   * }</pre>
   *
   * @param slot    Non-null, non-empty identifier for this binding (for example {@code "keyboard"}).
   * @param binding Non-null binding.
   */
  public void addBinding(@NotNull String slot, @NotNull FlixelDigitalBinding binding) {
    if (namedBindings == null) {
      namedBindings = new ObjectMap<>(4);
    }
    FlixelDigitalBinding old = namedBindings.put(slot, binding);
    if (old != null) {
      bindings.removeValue(old, true);
    }
    bindings.add(binding);
  }

  /**
   * Removes the binding registered under the given slot name.
   *
   * @param slot Slot name passed to {@link #addBinding(String, FlixelDigitalBinding)}.
   * @return {@code true} if a binding was found and removed, {@code false} if the slot was unknown.
   */
  public boolean removeBinding(@NotNull String slot) {
    if (namedBindings == null) {
      return false;
    }
    FlixelDigitalBinding old = namedBindings.remove(slot);
    if (old == null) {
      return false;
    }
    bindings.removeValue(old, true);
    return true;
  }

  /**
   * Removes a specific binding by reference identity.
   *
   * <p>If the binding was added via {@link #addBinding(String, FlixelDigitalBinding)}, its slot
   * entry is also cleared.
   *
   * @param binding The exact binding instance to remove.
   * @return {@code true} if the binding was found and removed.
   */
  public boolean removeBinding(@NotNull FlixelDigitalBinding binding) {
    boolean removed = bindings.removeValue(binding, true);
    if (removed && namedBindings != null) {
      ObjectMap.Keys<String> keys = namedBindings.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        if (namedBindings.get(key) == binding) {
          namedBindings.remove(key);
          break;
        }
      }
    }
    return removed;
  }

  @Override
  void updateAction(float elapsed) {
    if (!active) {
      pressed = false;
      held = false;
      holdAccum = 0f;
      holdRepeating = false;
      return;
    }
    boolean v = false;
    FlixelSteamActionReader steam = owner != null ? owner.steamReader : null;
    if (steam != null && steam.getDigital(getName())) {
      v = true;
    }
    for (int i = 0, n = bindings.size; i < n; i++) {
      v |= bindings.get(i).evaluate();
    }
    boolean edge = v && !previous;
    Runnable cb = callback;
    if (edge && cb != null) {
      cb.run();
    }
    pressed = v;
    if (v) {
      if (!previous) {
        holdAccum = 0f;
        holdRepeating = false;
        held = true;
      } else {
        holdAccum += elapsed;
        held = false;
        if (!holdRepeating) {
          if (holdAccum >= getHoldDelay()) {
            holdAccum -= getHoldDelay();
            holdRepeating = true;
            held = true;
          }
        } else if (holdAccum >= getHoldInterval()) {
          holdAccum -= getHoldInterval();
          held = true;
        }
      }
    } else {
      holdAccum = 0f;
      holdRepeating = false;
      held = false;
    }
  }

  @Override
  void endFrameAction() {
    previous = pressed;
  }

  @Override
  void resetAction() {
    pressed = false;
    previous = false;
    holdAccum = 0f;
    holdRepeating = false;
    held = false;
  }

  public boolean pressed() {
    return active && pressed;
  }

  public boolean justPressed() {
    return active && pressed && !previous;
  }

  public boolean justReleased() {
    return active && !pressed && previous;
  }

  /**
   * Returns {@code true} on the initial press and again on each hold-repeat tick.
   *
   * <p>Fires immediately on the frame the button is first pressed (same as {@link #justPressed()}),
   * then fires again after {@link FlixelAction#getHoldDelay()} seconds if the button is still held,
   * and continues firing every {@link FlixelAction#getHoldInterval()} seconds after that. Releasing
   * the button resets the timer so the next press starts fresh.
   *
   * <p>Use this instead of {@link #justPressed()} anywhere a held button should keep triggering,
   * such as menu scrolling, cursor movement, or incrementing a value:
   *
   * <pre>{@code
   * if (controls.uiDown.held()) scrollMenu();
   * }</pre>
   *
   * @return {@code true} on the initial press frame and on each repeat tick.
   */
  public boolean held() {
    return active && held;
  }
}
