/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.action;

import com.badlogic.gdx.utils.Array;

import org.jetbrains.annotations.NotNull;

/**
 * Global registry of {@link FlixelActionSet} instances. {@link me.stringdotjar.flixelgdx.FlixelGame} invokes
 * {@link #updateAll(float)} and {@link #endFrameAll()} so every registered set stays on the same contract as
 * {@link me.stringdotjar.flixelgdx.input.FlixelInputManager} (keys, mouse, gamepads).
 *
 * <h2>When {@code updateAll} runs</h2>
 *
 * <p>Order inside {@link me.stringdotjar.flixelgdx.FlixelGame#update(float)}: {@code Flixel.keys.update()},
 * {@code Flixel.mouse.update()}, {@code Flixel.gamepads.update()}, then {@link #updateAll(float)}. Gameplay code in
 * {@link me.stringdotjar.flixelgdx.FlixelState} runs after that, so {@code jump.justPressed()} reflects this frame's input.
 *
 * <h2>When {@code endFrameAll} runs</h2>
 *
 * <p>After {@link me.stringdotjar.flixelgdx.FlixelGame#draw} and the input managers' {@code endFrame()} calls,
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
   * Invoked from {@link me.stringdotjar.flixelgdx.FlixelGame#update(float)} after gamepad polling.
   *
   * @param elapsed Seconds since last frame (same as game update).
   */
  public static void updateAll(float elapsed) {
    for (int i = 0, n = registered.size; i < n; i++) {
      registered.get(i).update(elapsed);
    }
  }

  /**
   * Invoked from {@link me.stringdotjar.flixelgdx.FlixelGame#render()} after keys, mouse, and gamepads
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
