/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.animation;

import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.util.FlixelSignal;

import org.jetbrains.annotations.NotNull;

/**
 * Optional indirection between <strong>game logic state labels</strong> and <strong>registered animation clip
 * names</strong> on a {@link FlixelAnimationController}.
 *
 * <h2>What this is for</h2>
 * <p>
 * {@link me.stringdotjar.flixelgdx.FlixelSprite} stores clips on its {@link FlixelAnimationController} (from
 * {@link me.stringdotjar.flixelgdx.FlixelSprite#ensureAnimation()}) when you call {@code sprite.ensureAnimation().addAnimation} or
 * {@code .addAnimationByPrefix}. Those keys often match asset or sheet naming ({@code "player_run"},
 * {@code "hit_01"}) while your gameplay code thinks in terms of behavior ({@code "running"},
 * {@code "hurt"}).
 * </p>
 * <p>
 * This class keeps a map {@code logicalState -> clipName}. When you {@link #setState(String,
 * FlixelAnimationController, boolean, boolean) setState}, it updates the remembered logical state,
 * optionally notifies listeners, and if a mapping exists, calls {@link FlixelAnimationController#playAnimation(String,
 * boolean, boolean)} so the correct clip plays. That lets you rename or reorganize clips in one place
 * (the map) instead of hunting every {@code playAnimation} call.
 * </p>
 *
 * <h2>What this is not</h2>
 * <ul>
 *   <li>It does not register frames or build {@link com.badlogic.gdx.graphics.g2d.Animation}; clips must
 *       already exist on the controller (via the owning {@link me.stringdotjar.flixelgdx.FlixelSprite}).</li>
 *   <li>It does not poll input or physics; your code must call {@link #setState} when the character's
 *       situation changes.</li>
 *   <li>It does not auto-transition when a non-looping clip ends; listen to
 *       {@link FlixelAnimationController#onAnimationFinished} if you need that.</li>
 *   <li>A {@link #setState} to a state with <em>no</em> mapping still updates {@link #getState()} and fires
 *       {@link #onStateChanged}, but does not call {@code playAnimation} (useful for invisible states).</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <p>
 * Hold one instance per character (or shared rules object). After loading graphics and registering clips on
 * the sprite, wire the map once, then drive states from movement, AI, or timers.
 * </p>
 * <pre>{@code
 * // After player.loadGraphic(...) and player.ensureAnimation().addAnimation("walk", ...), etc.:
 * FlixelAnimationStateMachine sm = new FlixelAnimationStateMachine();
 * sm.mapStateToAnimation("idle", "idle");
 * sm.mapStateToAnimation("run", "walk");
 * sm.onStateChanged.add(s -> reactToLogicalState(s));  // optional: audio, VFX, etc.
 * sm.setState("idle", player.ensureAnimation(), true, true);
 *
 * // Each frame or on events:
 * if (onGround && speed > threshold) {
 *   sm.setState("run", player.ensureAnimation(), true, false);
 * } else if (onGround) {
 *   sm.setState("idle", player.ensureAnimation(), true, false);
 * }
 * }</pre>
 *
 * <p>
 * Use {@code forceRestart == false} when you may call {@code setState} repeatedly for the same state
 * (e.g. every frame while running) so the clip is not restarted from frame 0 each time. Use
 * {@code forceRestart == true} when re-entering the same state should replay from the start (e.g. hurt
 * stun).
 * </p>
 *
 * <p>
 * {@link me.stringdotjar.flixelgdx.FlixelSprite#update(float)} must still run each frame so the {@link FlixelAnimationController}
 * advances time and updates the drawn frame.
 * </p>
 *
 * @see FlixelAnimationController
 * @see me.stringdotjar.flixelgdx.FlixelSprite#ensureAnimation()
 */
public class FlixelAnimationStateMachine {

  @NotNull
  private String state = "";

  private final ObjectMap<String, String> stateToAnimation = new ObjectMap<>();

  /**
   * Fired immediately after the internal state string changes in {@link #setState}, with the new state as
   * payload. Not fired when {@code setState} is called with the same state as {@link #getState()} (no-op).
   */
  @NotNull
  public final FlixelSignal<String> onStateChanged = new FlixelSignal<>();

  /**
   * Associates a logical state label with an animation clip name that must already be registered on the
   * sprite/controller (same string you passed to {@code addAnimation} / {@code addAnimationByPrefix}).
   * Later {@link #setState} calls for {@code state} will {@link FlixelAnimationController#playAnimation(String,
   * boolean, boolean)} the given {@code animationName}.
   *
   * @param state arbitrary non-null key (e.g. {@code "air"}, {@code "crouch"})
   * @param animationName clip key on the controller; if wrong, {@code setState} will not find a clip to play
   */
  public void mapStateToAnimation(@NotNull String state, @NotNull String animationName) {
    stateToAnimation.put(state, animationName);
  }

  /**
   * Removes a mapping so {@link #setState} for that label no longer triggers {@code playAnimation}.
   * Does not change {@link #getState()} by itself.
   */
  public void removeState(@NotNull String state) {
    stateToAnimation.remove(state);
  }

  /**
   * Switches the logical state and, when {@code newState} differs from {@link #getState()}, dispatches
   * {@link #onStateChanged}. If {@code stateToAnimation} contains {@code newState}, calls
   * {@link FlixelAnimationController#playAnimation(String, boolean, boolean)} on {@code controller} with
   * the given {@code loop} and {@code forceRestart}.
   *
   * <p>
   * If {@code newState} equals the current state, returns immediately (no signal, no {@code playAnimation}).
   * </p>
   *
   * @param newState The logical state to enter.
   * @param controller The animation controller to use. Typically {@code sprite.ensureAnimation()}. Must be the controller that owns the mapped clips.
   * @param loop Forwarded to {@code playAnimation} (often {@code true} for idle/run, {@code false} for one-shot clips).
   * @param forceRestart Forwarded to {@code playAnimation}; use {@code false} when polling the same state every frame.
   */
  public void setState(
    @NotNull String newState,
    @NotNull FlixelAnimationController controller,
    boolean loop,
    boolean forceRestart) {
    if (state.equals(newState)) {
      return;
    }
    state = newState;
    onStateChanged.dispatch(newState);
    String anim = stateToAnimation.get(newState);
    if (anim != null) {
      controller.playAnimation(anim, loop, forceRestart);
    }
  }

  /**
   * The last logical state set by {@link #setState}, or {@code ""} if none yet.
   */
  @NotNull
  public String getState() {
    return state;
  }

  /**
   * Resets logical state to empty and clears all {@code mapStateToAnimation} entries. Does not stop or
   * clear clips on any {@link FlixelAnimationController}; call {@link FlixelAnimationController#clear} on
   * the sprite if you need that.
   */
  public void clear() {
    state = "";
    stateToAnimation.clear();
  }
}
