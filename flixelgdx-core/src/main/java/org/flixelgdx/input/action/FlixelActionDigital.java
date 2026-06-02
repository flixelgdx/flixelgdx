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
import com.badlogic.gdx.utils.Array;

import org.flixelgdx.Flixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Boolean action: any {@link FlixelInputBinding} that evaluates true, OR optional Steam digital for the same
 * {@link #getName()}, makes {@link #pressed()} true for this frame.
 *
 * <h2>Setup</h2>
 *
 * <p>Call {@link #addBinding(FlixelInputBinding)} only during construction or loading (not each frame). Multiple
 * bindings are OR'd: {@code jump} might accept Space, gamepad A, and a touch region.
 *
 * <h2>Reading in gameplay</h2>
 *
 * <ul>
 *   <li>{@link #pressed()} while a key is held (sustains, movement gates).</li>
 *   <li>{@link #justPressed()} or {@link #check()} for a single-frame edge (tap notes, menu confirm).</li>
 *   <li>{@link #justReleased()} when the player releases after a hold.</li>
 * </ul>
 *
 * <p>State is refreshed in {@link FlixelActionSet#update(float)} (via {@link FlixelActionSets#updateAll(float)}).
 * {@link FlixelActionSet#endFrame()} (via {@link FlixelActionSets#endFrameAll()}) runs after
 * {@link org.flixelgdx.FlixelGame#render()} finalizes keys and mouse, matching their {@code justPressed} timing.
 *
 * <h2>Optional callback</h2>
 *
 * <p>{@link #callback} runs on the press edge when assigned; prefer a single static {@link Runnable} to avoid allocating
 * lambdas in hot paths.
 */
public final class FlixelActionDigital extends FlixelAction {

  private static final int MAX_TOUCH_POINTER = 20;

  private final Array<FlixelInputBinding> bindings = new Array<>(8);

  private boolean pressed;
  private boolean previous;

  public FlixelActionDigital(@Nullable String name) {
    super(name);
  }

  /**
   * Adds a binding evaluated each frame (allocation-free after this call).
   *
   * @param binding Non-null binding.
   */
  public void addBinding(@NotNull FlixelInputBinding binding) {
    bindings.add(binding);
  }

  @Override
  void updateAction(float elapsed) {
    if (!active) {
      pressed = false;
      return;
    }
    boolean v = false;
    FlixelSteamActionReader steam = owner != null ? owner.steamReader : null;
    if (steam != null && steam.getDigital(getName())) {
      v = true;
    }
    for (int i = 0, n = bindings.size; i < n; i++) {
      v |= evalBinding(bindings.get(i));
    }
    boolean edge = v && !previous;
    Runnable cb = callback;
    if (edge && cb != null) {
      cb.run();
    }
    pressed = v;
  }

  @Override
  void endFrameAction() {
    previous = pressed;
  }

  @Override
  void resetAction() {
    pressed = false;
    previous = false;
  }

  /**
   * Same as {@link #justPressed()}: true for the single frame the action became active.
   *
   * @return Whether the action triggered this frame.
   */
  public boolean check() {
    return justPressed();
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

  private boolean evalBinding(@NotNull FlixelInputBinding b) {
    switch (b.kind) {
      case KEY:
        return Flixel.keys != null && Flixel.keys.enabled && Flixel.keys.pressed(b.a);
      case GAMEPAD_BUTTON:
        if (Flixel.gamepads == null || !Flixel.gamepads.enabled) {
          return false;
        }
        if (b.b == FlixelInputBinding.GAMEPAD_SLOT_ANY) {
          return Flixel.gamepads.anyPressed(b.a);
        }
        return Flixel.gamepads.pressed(b.b, b.a);
      case POINTER_BUTTON:
        return evalPointer(b.a, b.b);
      case TOUCH_REGION:
        return evalTouchRegion(b.normX, b.normY, b.normW, b.normH);
      default:
        return false;
    }
  }

  private static boolean evalPointer(int pointer, int button) {
    if (pointer == FlixelInputBinding.POINTER_MOUSE) {
      return Flixel.mouse != null && Flixel.mouse.enabled && Flixel.mouse.pressed(button);
    }
    if (!Gdx.input.isTouched(pointer)) {
      return false;
    }
    return button < 0 || Gdx.input.isButtonPressed(button);
  }

  private static boolean evalTouchRegion(float nx, float ny, float nw, float nh) {
    int bw = Gdx.graphics.getBackBufferWidth();
    int bh = Gdx.graphics.getBackBufferHeight();
    if (bw <= 0 || bh <= 0) {
      return false;
    }
    float fx = 1f / bw;
    float fy = 1f / bh;
    for (int p = 0; p <= MAX_TOUCH_POINTER; p++) {
      if (!Gdx.input.isTouched(p)) {
        continue;
      }
      float px = Gdx.input.getX(p) * fx;
      float py = Gdx.input.getY(p) * fy;
      if (px >= nx && px <= nx + nw && py >= ny && py <= ny + nh) {
        return true;
      }
    }
    return false;
  }
}
