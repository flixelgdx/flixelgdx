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
package org.flixelgdx.animation;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

import org.flixelgdx.FlixelSprite;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A lightweight finite state machine that drives a sprite's animations.
 *
 * <h2>Why this exists</h2>
 * Calling {@link FlixelAnimationController#playAnimation(String, boolean, boolean) playAnimation}
 * directly from gameplay code works, but it spreads animation rules everywhere: which clip plays for
 * which behavior, which transitions are legal, what should happen when a one-shot clip finishes, and
 * what side effects (sound, particles) a transition triggers. A state machine gathers those rules in
 * one place so the gameplay code can simply say "I am now {@code running}" and trust the machine to
 * play the right clip, reject illegal moves, and chain follow-up animations on its own.
 *
 * <h2>What a state can describe</h2>
 * <ul>
 *   <li>the animation clip to play (or none, for an invisible logic-only state),</li>
 *   <li>whether that clip loops,</li>
 *   <li>an {@link State#autoAdvanceTo(String) auto-advance} target, so a one-shot clip (such as an
 *       attack) automatically returns to another state the moment it finishes, optionally after a
 *       {@link State#delay(float) delay},</li>
 *   <li>{@link State#onEnter(Runnable) onEnter} / {@link State#onExit(Runnable) onExit} hooks for
 *       side effects, and</li>
 *   <li>the set of {@link State#allowTo(String...) legal transitions} out of it.</li>
 * </ul>
 *
 * <h2>Concepts and terminology</h2>
 * <ul>
 *   <li><strong>State</strong>: one named mode the sprite can be in, such as {@code "idle"} or
 *       {@code "attack"}. The machine is in exactly one state at a time. Register states with
 *       {@link #addState(String, String)}.</li>
 *   <li><strong>Transition</strong>: a move from one state to another, requested with
 *       {@link #setState(String)}. Asking for the state that is already active is a no-op.</li>
 *   <li><strong>Guard</strong>: a rule that decides whether a transition is allowed. Configured per
 *       source state with {@link State#allowTo(String...)}; a state with no rule allows any move.</li>
 *   <li><strong>Entry / exit actions</strong>: callbacks run when a state is entered or left
 *       ({@link State#onEnter(Runnable)} / {@link State#onExit(Runnable)}).</li>
 *   <li><strong>Auto-advance</strong>: an automatic transition that fires when a one-shot clip
 *       finishes ({@link State#autoAdvanceTo(String)}), optionally after a delay.</li>
 *   <li><strong>Self-transition</strong>: deliberately re-entering the current state with
 *       {@link #setState(String, boolean) setState(name, true)}, which replays its clip.</li>
 * </ul>
 *
 * <h2>How it drives the sprite</h2>
 * The machine holds a reference to the sprite's {@link FlixelAnimationController} and subscribes to
 * its {@link FlixelAnimationController#onAnimationFinished} signal, which is what powers
 * auto-advance. You must still call {@code sprite.update(...)} every frame so the controller advances
 * animation time and reports when clips end. If any state uses a {@link State#delay(float) delayed}
 * auto-advance, also call this machine's {@link #update(float)} each frame so the delay can elapse.
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // After loading frames and registering clips on the sprite:
 * var fsm = new FlixelAnimationStateMachine(player);
 * fsm.addState("idle", "idle").allowTo("run", "attack");
 * fsm.addState("run", "run").allowTo("idle", "attack");
 * fsm.addState("attack", "attack")
 *    .autoAdvanceTo("idle")          // one-shot: snaps back to idle when the clip ends
 *    .onEnter(() -> sword.swing());
 * fsm.setState("idle");
 *
 * // Each frame, just describe the situation; calling setState with the current state is a no-op:
 * if (attackPressed)        fsm.setState("attack");
 * else if (speed > 0.1f)    fsm.setState("run");
 * else                      fsm.setState("idle");
 * }</pre>
 *
 * @see FlixelAnimationController
 * @see org.flixelgdx.FlixelSprite#ensureAnimation()
 */
public class FlixelAnimationStateMachine implements FlixelDestroyable, FlixelUpdatable {

  /** The controller whose clips this machine plays. */
  @NotNull
  private final FlixelAnimationController controller;

  /** Registered states, keyed by their logical name. */
  @NotNull
  private final ObjectMap<String, State> states = new ObjectMap<>();

  /** Listener kept so it can be detached again in {@link #destroy()}. */
  @NotNull
  private final FlixelSignal.SignalHandler<FlixelAnimationFrameSignalData> finishListener;

  /** The current logical state, or {@code ""} before the first {@link #setState}. */
  @NotNull
  private String state = "";

  /** Target of a delayed auto-advance waiting on {@link #update(float)}, or {@code null} when idle. */
  @Nullable
  private String pendingState;

  /** Seconds remaining before {@link #pendingState} is entered. */
  private float pendingTimer;

  /**
   * Fired with the new state name whenever the machine moves to a different state. Not fired for a
   * no-op {@link #setState} call that targets the current state.
   */
  @NotNull
  public final FlixelSignal<String> onStateChanged = new FlixelSignal<>();

  /**
   * Creates a machine that drives the given sprite's {@link FlixelAnimationController}.
   *
   * @param sprite The sprite to animate; its controller is obtained via {@link FlixelSprite#ensureAnimation()}.
   */
  public FlixelAnimationStateMachine(@NotNull FlixelSprite sprite) {
    this(sprite.ensureAnimation());
  }

  /**
   * Creates a machine that drives the given controller.
   *
   * @param controller The controller whose registered clips the states refer to.
   */
  public FlixelAnimationStateMachine(@NotNull FlixelAnimationController controller) {
    if (controller == null) {
      throw new IllegalArgumentException("Controller cannot be null.");
    }
    this.controller = controller;
    this.finishListener = this::handleAnimationFinished;
    controller.onAnimationFinished.add(finishListener);
  }

  /**
   * Registers a logic-only state with no animation clip. Entering it still fires hooks and
   * {@link #onStateChanged}, but plays nothing.
   *
   * @param name The logical state name.
   * @return The new {@link State} for fluent configuration.
   */
  @NotNull
  public State addState(@NotNull String name) {
    return addState(name, null);
  }

  /**
   * Registers a state that plays {@code clipName} (a clip already registered on the controller via
   * {@code addAnimation} / {@code addAnimationByPrefix}) when entered.
   *
   * @param name The logical state name.
   * @param clipName The animation clip to play, or {@code null} for a logic-only state.
   * @return The new {@link State} for fluent configuration.
   */
  @NotNull
  public State addState(@NotNull String name, @Nullable String clipName) {
    State newState = new State(clipName);
    states.put(name, newState);
    return newState;
  }

  /**
   * Removes a registered state. Does not change the current {@link #getState()}.
   *
   * @param name The state to remove.
   */
  public void removeState(@NotNull String name) {
    states.remove(name);
  }

  /**
   * Moves to {@code newState} if the transition is legal. Calling this with the state that is already
   * active is a cheap no-op, so it is safe to call every frame.
   *
   * @param newState The state to enter.
   * @return {@code true} if the machine is now in {@code newState}, {@code false} if the transition
   *     was rejected as illegal.
   */
  public boolean setState(@NotNull String newState) {
    return transition(newState, false, true);
  }

  /**
   * Moves to {@code newState}, optionally re-entering it even if it is already active.
   *
   * <p>A forced re-entry replays the state's clip from the start and runs its
   * {@link State#onEnter(Runnable) onEnter} hook again (but not {@code onExit}, since the machine
   * never left). This is handy for retriggering a one-shot, such as attacking again mid-swing.
   *
   * @param newState The state to enter.
   * @param force {@code true} to re-enter even when {@code newState} is already active.
   * @return {@code true} if the machine is now in {@code newState}, {@code false} if a real
   *     (state-changing) transition was rejected as illegal.
   */
  public boolean setState(@NotNull String newState, boolean force) {
    return transition(newState, force, true);
  }

  /**
   * Core transition routine shared by the public {@code setState} overloads and the internal
   * auto-advance path.
   *
   * @param newState The state to enter.
   * @param force {@code true} to re-enter {@code newState} even if it is already active.
   * @param enforceLegality {@code true} to honor the source state's {@link State#allowTo} rules;
   *     auto-advance passes {@code false} because it follows a transition the developer configured.
   * @return {@code true} if the machine ends up in {@code newState}.
   */
  private boolean transition(@NotNull String newState, boolean force, boolean enforceLegality) {
    boolean sameState = state.equals(newState);
    if (sameState && !force) {
      return true;
    }

    State current = states.get(state);
    State next = states.get(newState);

    if (!sameState && enforceLegality && current != null && !current.canTransitionTo(newState)) {
      return false;
    }

    // Any real (or forced) transition cancels an auto-advance that was waiting on a delay.
    pendingState = null;
    pendingTimer = 0f;

    // Leaving the old state (skipped on a forced self-replay, where the machine never actually left).
    if (!sameState && current != null && current.onExit != null) {
      current.onExit.run();
    }

    state = newState;

    // Entering the new state: run its entry hook, then (re)start its clip from the beginning.
    if (next != null) {
      if (next.onEnter != null) {
        next.onEnter.run();
      }
      if (next.clipName != null) {
        controller.playAnimation(next.clipName, next.loop, true);
      }
    }

    if (!sameState) {
      onStateChanged.dispatch(newState);
    }
    return true;
  }

  /**
   * Auto-advance hook: when the clip belonging to the current state finishes, follow that state's
   * {@link State#autoAdvanceTo(String)} target if one is configured.
   *
   * @param data The finished-animation payload from the controller.
   */
  private void handleAnimationFinished(@NotNull FlixelAnimationFrameSignalData data) {
    State current = states.get(state);
    if (current == null || current.autoNext == null || current.clipName == null) {
      return;
    }
    // Only advance when the clip that finished is the one this state drives, so an unrelated
    // animation ending cannot bounce the machine forward.
    if (!current.clipName.equals(data.getAnimationName())) {
      return;
    }
    if (current.delay > 0f) {
      // Hold on the final frame; update(float) counts the delay down and then advances.
      pendingState = current.autoNext;
      pendingTimer = current.delay;
    } else {
      transition(current.autoNext, false, false);
    }
  }

  /**
   * Advances a pending {@link State#delay(float) delayed} auto-advance. Only needed when a state uses
   * a delay; immediate auto-advance and manual {@link #setState} calls work without it. Safe to call
   * every frame regardless.
   *
   * @param elapsed Seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    if (pendingState == null) {
      return;
    }
    pendingTimer -= elapsed;
    if (pendingTimer <= 0f) {
      String target = pendingState;
      pendingState = null;
      pendingTimer = 0f;
      transition(target, false, false);
    }
  }

  /** Clears all registered states and resets to the empty state, keeping the machine usable. */
  public void clear() {
    states.clear();
    state = "";
    pendingState = null;
    pendingTimer = 0f;
  }

  /**
   * Detaches this machine from its controller and clears all states. Call when the owning sprite is
   * destroyed so the machine does not keep receiving {@code onAnimationFinished} callbacks.
   */
  @Override
  public void destroy() {
    controller.onAnimationFinished.remove(finishListener);
    states.clear();
    state = "";
    pendingState = null;
    pendingTimer = 0f;
    onStateChanged.clear();
  }

  /**
   * @return The current logical state, or {@code ""} if {@link #setState} has not run yet.
   */
  @NotNull
  public String getState() {
    return state;
  }

  /**
   * @param name The state name to look up.
   * @return {@code true} if a state with that name is registered.
   */
  public boolean hasState(@NotNull String name) {
    return states.containsKey(name);
  }

  /**
   * One configurable state in a {@link FlixelAnimationStateMachine}, built fluently.
   *
   * <p>Obtain one from {@link FlixelAnimationStateMachine#addState(String, String)} and chain the
   * setters, for example {@code addState("attack", "attack").autoAdvanceTo("idle").onEnter(...)}.
   */
  public static final class State {

    @Nullable
    private final String clipName;

    @Nullable
    private String autoNext;

    @Nullable
    private Runnable onEnter;

    @Nullable
    private Runnable onExit;

    /** Allowed transition targets, or {@code null} when transitions out of this state are unrestricted. */
    @Nullable
    private ObjectSet<String> allowed;

    /** Seconds to hold the final frame before auto-advancing. {@code 0} advances immediately. */
    private float delay;

    /** Whether the clip loops. Defaults to {@code true}; auto-advance sets it to {@code false}. */
    private boolean loop = true;

    private State(@Nullable String clipName) {
      this.clipName = clipName;
    }

    /**
     * Sets whether this state's clip loops.
     *
     * @param loop {@code true} to loop, {@code false} to play once.
     * @return This state, for chaining.
     */
    public State loop(boolean loop) {
      this.loop = loop;
      return this;
    }

    /**
     * Makes this state automatically transition to {@code target} as soon as its clip finishes. This
     * implies a non-looping clip, so it also sets {@link #loop(boolean) loop} to {@code false}. Pair
     * with {@link #delay(float)} to hold the final frame for a moment before advancing.
     *
     * @param target The state to enter when this state's clip finishes.
     * @return This state, for chaining.
     */
    public State autoAdvanceTo(@NotNull String target) {
      this.autoNext = target;
      this.loop = false;
      return this;
    }

    /**
     * Sets how long, in seconds, to wait after this state's clip finishes before
     * {@link #autoAdvanceTo(String) auto-advancing}. Useful for letting a finished pose linger (for
     * example, holding the last frame of an attack before returning to idle).
     *
     * <p>This only affects the auto-advance transition, so it does nothing unless
     * {@link #autoAdvanceTo(String)} is also set. The owning machine's
     * {@link FlixelAnimationStateMachine#update(float)} must be called each frame for the delay to
     * elapse.
     *
     * @param seconds The hold time in seconds; values {@code <= 0} advance immediately.
     * @return This state, for chaining.
     */
    public State delay(float seconds) {
      this.delay = seconds;
      return this;
    }

    /**
     * Sets an action run when the machine enters this state (after a forced re-entry too).
     *
     * @param action The entry action.
     * @return This state, for chaining.
     */
    public State onEnter(@NotNull Runnable action) {
      this.onEnter = action;
      return this;
    }

    /**
     * Sets an action run when the machine leaves this state for a different one.
     *
     * @param action The exit action.
     * @return This state, for chaining.
     */
    public State onExit(@NotNull Runnable action) {
      this.onExit = action;
      return this;
    }

    /**
     * Restricts which states may be entered directly from this one. Without any call to this method,
     * every transition out of this state is allowed; once called, only the listed targets are legal.
     * May be called more than once to add more targets.
     *
     * @param targets The state names reachable from this state.
     * @return This state, for chaining.
     */
    public State allowTo(@NotNull String... targets) {
      if (allowed == null) {
        allowed = new ObjectSet<>();
      }
      for (String target : targets) {
        allowed.add(target);
      }
      return this;
    }

    private boolean canTransitionTo(@NotNull String target) {
      return allowed == null || allowed.contains(target);
    }
  }
}
