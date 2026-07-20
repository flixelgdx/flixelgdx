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

import org.flixelgdx.functional.FlixelDrawable;
import org.jetbrains.annotations.NotNull;

/**
 * Global registry of {@link FlixelActionSet} instances. {@link org.flixelgdx.FlixelGame FlixelGame} invokes
 * {@link #update(float)} and {@link #endFrameAll()} so every registered set stays on the same contract as
 * {@link org.flixelgdx.input.FlixelInputManager FlixelInputManager} (keys, mouse, gamepads).
 *
 * <h2>When {@code updateAll} runs</h2>
 *
 * <p>Order inside {@link org.flixelgdx.FlixelGame#update(float) FlixelGame.update(float)}: {@code Flixel.keys.update()},
 * {@code Flixel.mouse.update()}, {@code Flixel.gamepads.update()}, then {@link #update(float)}. Gameplay code in
 * {@link org.flixelgdx.FlixelState FlixelState} runs after that, so {@code jump.justPressed()} reflects this frame's input.
 *
 * <h2>When {@code endFrameAll} runs</h2>
 *
 * <p>After {@link FlixelDrawable#draw FlixelGame.draw} and the input managers' {@code endFrame()} calls,
 * {@link #endFrameAll()} copies digital and analog edge state for the next frame.
 *
 * <h2>Registration</h2>
 *
 * <p>{@link FlixelActionSet} registers in its constructor unless you use {@link FlixelActionSet#FlixelActionSet(boolean)}
 * with {@code false} (tests). Call {@link FlixelActionSet#destroy()} to unregister. Avoid registering or destroying sets
 * from inside another set's {@link FlixelActionSet#update(float)} unless you know the iteration order is safe.
 */
public final class FlixelActionSets {

  private static final Array<FlixelActionSet> registered = new Array<>(8);

  private FlixelActionSets() {}

  static void register(@NotNull FlixelActionSet set) {
    for (int i = 0, n = registered.size; i < n; i++) {
      if (registered.get(i) == set) {
        return;
      }
    }
    registered.add(set);
  }

  static void unregister(@NotNull FlixelActionSet set) {
    registered.removeValue(set, true);
  }

  /**
   * Invoked from {@link org.flixelgdx.FlixelGame#update(float) FlixelGame.update(float)} after gamepad polling.
   *
   * @param elapsed Seconds since last frame (same as game update).
   */
  public static void update(float elapsed) {
    for (int i = 0, n = registered.size; i < n; i++) {
      registered.get(i).update(elapsed);
    }
  }

  /**
   * Invoked from {@link org.flixelgdx.FlixelGame#render() FlixelGame.render()} after keys, mouse, and gamepads
   * {@code endFrame()}.
   */
  public static void endFrameAll() {
    for (int i = 0, n = registered.size; i < n; i++) {
      registered.get(i).endFrame();
    }
  }

  /** For tests: number of registered sets. */
  public static int registeredCountForTests() {
    return registered.size;
  }

  /** For tests: clear registry without calling destroy on sets. */
  public static void clearRegistryForTests() {
    registered.clear();
  }
}
