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
package org.flixelgdx;

import org.flixelgdx.functional.FlixelCreatable;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelDrawable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.signal.FlixelSignalData.GamePausedSignalData;
import org.flixelgdx.signal.FlixelSignalData.UpdateSignalData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The main game class: it runs the active {@link FlixelState} and drives every manager through
 * the game's lifecycle.
 *
 * <p>Extend this class and hand an instance to the platform launcher. The launcher installs the
 * appropriate backend, wires up the runner, and calls {@link #create()} once the rendering surface
 * is ready. After that, it calls {@link #beginFrame()}, {@link #update(float)},
 * {@link #draw(FlixelBatch)}, and {@link #endFrame()} every frame, in that order.
 *
 * <h2>The game is the state plus the managers</h2>
 *
 * <p>Think of the game like a theater. The state is the play on stage, and it is the only thing
 * this class runs itself: it updates the state chain, draws it when the render manager asks, and
 * switches it out through {@link Flixel#switchState(Supplier)}. Everything else is crew working
 * behind the scenes, and every crew member is a {@link FlixelManagers.Manager} in
 * {@link Flixel#managers}: input, the debugger, tweens, timers, sound, the cameras
 * ({@link Flixel#cameraManager}), the global overlay ({@link Flixel#overlay}), and the renderer
 * ({@link Flixel#render}). This class calls the matching manager hook at every step of the
 * lifecycle, next to the matching {@link Flixel.Signals signal}.
 *
 * <p>So anything that has to follow the whole game (a library that needs to update every frame, a
 * persistent HUD, cleanup when the game closes) belongs in a manager, not in a subclass of this
 * class. Put gameplay logic, UI, and entity management in your states.
 *
 * <h2>Lifecycle</h2>
 *
 * <ol>
 *   <li>{@link #create()} - called once when the rendering surface is ready. Creates every manager,
 *       then switches to the initial state.</li>
 *   <li>{@link #beginFrame()} - called every frame before updating. Runs the managers' beginning
 *       of frame.</li>
 *   <li>{@link #update(float)} - called every frame. Runs the managers' update hooks around the
 *       state update.</li>
 *   <li>{@link #draw(FlixelBatch)} - called every frame. Runs the managers' draw hooks; the render
 *       manager draws the state on every camera through {@link #drawState(FlixelBatch)}.</li>
 *   <li>{@link #endFrame()} - called every frame after drawing. Runs the managers' end of frame.</li>
 *   <li>{@link #destroy()} - called when the game closes. Destroys the state, then every manager.
 *       Call {@link Flixel#quit()} to close the window; calling {@code destroy()} alone only releases
 *       resources without terminating the process.</li>
 * </ol>
 *
 * <h2>Pausing</h2>
 *
 * <p>The game decides when the state runs. It stops updating the state while it is paused with
 * {@link #setGamePaused(boolean)} (the debugger does this) and, when {@link Flixel#autoPause} is
 * {@code true} (the default), while the window is out of focus. {@link #stepFrame()} lets a paused
 * game run exactly one more update. Managers whose {@link FlixelManagers.Manager#isPausable()}
 * returns {@code true} skip their update hooks at the same times, so tweens, timers, sound, and the
 * cameras stop with the state, while input and the debugger keep running.
 *
 * <h2>Example Usage</h2>
 *
 * <p>In a project made with the FlixelGDX IntelliJ IDEA plugin, the window values live in
 * {@code project.toml} and reach the game through the generated {@code Project} class:
 *
 * <pre>{@code
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super(Project.config().build(), MenuState::new);
 *   }
 * }
 * }</pre>
 *
 * <p>Without {@code project.toml}, build the {@link FlixelConfig} yourself:
 *
 * <pre>{@code
 * public class MyGame extends FlixelGame {
 *
 *   public MyGame() {
 *     super(
 *       new FlixelConfig.Builder("My Game")
 *           .company("My Studio")
 *           .size(1280, 720)
 *           .build(),
 *       () -> new MenuState()
 *     );
 *   }
 * }
 * }</pre>
 *
 * <p>Either way, a platform's launcher starts the game:
 *
 * <pre>{@code
 * // In your platform's launcher (i.e. desktop):
 * FlixelDesktopLauncher.launch(new MyGame());
 * }</pre>
 *
 * @see FlixelConfig
 * @see FlixelState
 * @see FlixelManagers
 * @see Flixel
 */
public abstract class FlixelGame implements FlixelCreatable, FlixelUpdatable, FlixelDrawable, FlixelDestroyable {

  private static final String TRACE_UPDATE_STATE = "flixel.update.state";

  /**
   * Produces the root {@link FlixelState} each time {@link #create()} runs.
   *
   * <p>Use {@code () -> new MyState()} for a fresh instance per session, or {@code () -> sharedState} to
   * reuse one object (its {@link FlixelState#destroy()} and {@link FlixelState#create()} lifecycle
   * still runs via {@link Flixel#switchState}).
   */
  @NotNull
  protected Supplier<FlixelState> initialStateFactory;

  /** Scaled elapsed time (seconds) for the current frame, written by {@link #advanceTime(float)}. */
  private float elapsed;

  /** Raw clamped delta before {@link Flixel#timeScale} is applied, written by {@link #advanceTime(float)}. */
  private float rawElapsed;

  /** Reusable signal data for preUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData preUpdateData = new UpdateSignalData();

  /** Reusable signal data for postUpdate dispatch (avoids per-frame allocation). */
  private final UpdateSignalData postUpdateData = new UpdateSignalData();

  /** When true, skips the state and the pausable managers' updates (debug pause). */
  private boolean gamePaused = false;

  /** When true, the next update runs even though the game is paused. Set by {@link #stepFrame()}. */
  private boolean stepPending;

  /**
   * When {@code true}, {@link Flixel#state} was sent {@link FlixelState#onFocusLost()} for a paired
   * app or window pause and {@link FlixelState#onFocusGained()} has not yet been dispatched. Used so
   * duplicate callbacks (such as minimize plus focus lost) only state hooks once.
   */
  private boolean stateLifecyclePauseDispatched;

  /**
   * When true, the update loop will cycle every frame. This is primarily used by
   * {@link #onFocusGained()} and {@link #onFocusLost()} for auto-pausing.
   */
  private boolean shouldUpdate = true;

  /**
   * When {@code true}, {@link Flixel.Signals#gameSuspended} was already dispatched and the game has
   * not come back to the foreground since. Used so one trip to the background only fires it once.
   */
  private boolean suspended;

  /**
   * Creates a new game instance with a default 640x360 window, 60 fps, and VSync enabled.
   *
   * @param title The title of the game's window.
   * @param initialStateFactory A factory that produces the initial state to load when the game starts.
   */
  public FlixelGame(String title, @NotNull Supplier<FlixelState> initialStateFactory) {
    this(new FlixelConfig.Builder(title).build(), initialStateFactory);
  }

  /**
   * Creates a new game instance configured entirely by the supplied {@link FlixelConfig}, using a
   * factory that produces the initial state.
   *
   * <p>This is the primary constructor that all others delegate to. Use {@code () -> new MyState()} for a
   * fresh instance each session, or {@code () -> sharedState} to reuse one object whose
   * {@link FlixelState#destroy()} and {@link FlixelState#create()} lifecycle still runs on each
   * {@link Flixel#switchState(Supplier)} call.
   *
   * @param config The configuration that supplies all startup settings.
   * @param initialStateFactory A factory that produces the initial state to load when the game starts.
   */
  public FlixelGame(@NotNull FlixelConfig config, @NotNull Supplier<FlixelState> initialStateFactory) {
    Flixel.config = Objects.requireNonNull(config, "The game's config object cannot be null.");
    this.initialStateFactory = Objects.requireNonNull(initialStateFactory, "initialStateFactory cannot be null");
  }

  /**
   * Creates a new game instance with a 4-parameter shorthand: title, window size, and initial state.
   * All other settings use their defaults (60 fps, VSync on, windowed).
   *
   * <p>For anything beyond these four parameters, prefer {@link #FlixelGame(FlixelConfig, Supplier)}
   * with a {@link FlixelConfig} instead.
   *
   * @param title The title of the game's window.
   * @param width The starting width of the game's window and how wide the camera should be.
   * @param height The starting height of the game's window and how tall the camera should be.
   * @param initialStateFactory A factory that produces the initial state to load when the game starts.
   */
  public FlixelGame(String title, int width, int height, @NotNull Supplier<FlixelState> initialStateFactory) {
    this(new FlixelConfig.Builder(title).size(width, height).build(), initialStateFactory);
  }

  /**
   * Called when the game is created: creates every manager, then switches to the initial state.
   *
   * <p>The managers are created in order (see {@link FlixelManagers.Manager#getOrder()}): the assets
   * first, then the renderer, the cameras, input, the debugger, and the rest, so the initial state
   * starts with everything in place.
   *
   * <p>This method is called automatically by the platform runner once the backend surface is
   * ready, so it is not necessary to call it manually in most cases.
   */
  @Override
  public void create() {
    stateLifecyclePauseDispatched = false;
    suspended = false;
    Flixel.managers.create();
    Flixel.switchState(initialStateFactory, true, true, true);
  }

  /**
   * Called by the platform runner when the game's drawable surface changes size.
   *
   * @param width The new surface width in pixels.
   * @param height The new surface height in pixels.
   */
  public void resize(int width, int height) {
    // The cameras refit themselves first, as a manager, so the state sees the new sizes.
    Flixel.managers.resize(width, height);
    FlixelState state = Flixel.state;
    if (state != null) {
      state.resize(width, height);
    }
  }

  /**
   * Updates one frame: the managers' update hooks, then the state, then the managers' post update.
   *
   * @param elapsed The amount of time that occurred in the last frame.
   */
  @Override
  public void update(float elapsed) {
    preUpdateData.set(elapsed);
    Flixel.Signals.preUpdate.dispatch(preUpdateData);

    // A pending step lets the pausable managers and the state run this one frame.
    boolean paused = isUpdatePaused();
    stepPending = false;

    // Input reads the hardware first, then the debugger takes the input it owns, then the global
    // tweens, timers, and sound advance. See FlixelManagers.Manager for the full order.
    Flixel.managers.preUpdate(elapsed, paused);
    Flixel.managers.update(elapsed, paused);

    if (!paused) {
      Flixel.profiler.begin(TRACE_UPDATE_STATE);
      try {
        updateState(elapsed);
      } finally {
        Flixel.profiler.end();
      }
    }

    // The cameras and the global overlay update here, right after the state.
    Flixel.managers.postUpdate(elapsed, paused);

    postUpdateData.set(elapsed);
    Flixel.Signals.postUpdate.dispatch(postUpdateData);
  }

  /**
   * Draws one frame by running the managers' draw hooks.
   *
   * <p>{@link Flixel#render} does the actual drawing in the main draw pass: it runs one pass per
   * camera and draws the state on it through {@link #drawState(FlixelBatch)}.
   *
   * @param batch The batch to use for drawing the game.
   */
  @Override
  public void draw(@NotNull FlixelBatch batch) {
    Flixel.Signals.preDraw.dispatch();
    Flixel.managers.preDraw(batch);
    Flixel.managers.draw(batch);
    // The debugger draws its hitboxes here, straight onto the window.
    Flixel.managers.postDraw(batch);
    Flixel.Signals.postDraw.dispatch();
  }

  /**
   * Draws the state chain onto the camera being drawn: the active state, then each open substate.
   *
   * <p>{@link Flixel#render} calls this once per camera pass, with the batch already begun on that
   * camera. A state with an open substate only draws if its {@link FlixelState#persistentDraw} flag is
   * {@code true} (the default).
   *
   * @param batch The framework batch, already begun with the camera's projection.
   */
  public void drawState(@NotNull FlixelBatch batch) {
    FlixelState current = Flixel.state;
    while (current != null) {
      FlixelState sub = current.getSubState();
      if (sub == null || current.persistentDraw) {
        current.draw(batch);
      }
      current = sub;
    }
  }

  /**
   * Clamps the raw platform delta, applies {@link Flixel#timeScale}, and stores the results so
   * {@link Flixel#getElapsed()} and {@link Flixel#getRawElapsed()} return up-to-date values.
   *
   * <p>Platform runners call this once per frame before {@link #update(float)} and
   * {@link #draw(FlixelBatch)}, passing the raw wall-clock delta from their own timing source.
   * The returned value is the scaled elapsed time ready to pass directly to {@link #update(float)}.
   *
   * <pre>{@code
   * float elapsed = game.advanceTime(rawDelta);
   * game.beginFrame();
   * game.update(elapsed);
   * game.draw(game.getBatch());
   * game.endFrame();
   * }</pre>
   *
   * @param rawDelta The raw wall-clock seconds since the last frame.
   * @return The clamped and scaled elapsed time in seconds.
   * @see #endFrame()
   */
  public float advanceTime(float rawDelta) {
    float rawClamped = Math.clamp(rawDelta, Flixel.MIN_ELAPSED, Flixel.MAX_ELAPSED);
    rawElapsed = rawClamped;
    elapsed = rawClamped * Flixel.timeScale;
    return elapsed;
  }

  /**
   * Starts the frame by running every manager's beginning of frame.
   *
   * <p>Platform runners call this once per frame right after {@link #advanceTime(float)} and before
   * {@link #update(float)}, so {@link Flixel#getElapsed()} already holds this frame's time. It runs
   * even while the game is paused.
   *
   * @see FlixelManagers.Manager#beginFrame()
   * @see #endFrame()
   */
  public void beginFrame() {
    Flixel.managers.beginFrame();
  }

  /**
   * Finishes the frame by running every manager's end of frame.
   *
   * <p>Platform runners call this once per frame after {@link #draw(FlixelBatch)} completes. The
   * input managers finalize their state here, after user code runs, which keeps {@code justPressed()}
   * and {@code justReleased()} valid for the entire frame.
   *
   * @see #advanceTime(float)
   */
  public void endFrame() {
    Flixel.managers.endFrame();
  }

  /**
   * Pauses the game's update loop. This is mostly used by the debugger, although
   * you might find it useful for other purposes.
   *
   * <p>Each real change tells every manager through {@link FlixelManagers.Manager#onPaused(boolean)}
   * (the sound pauses, and the cameras remember where they were), then dispatches
   * {@link Flixel.Signals#gamePaused}, so external systems can follow along. Setting the value the game
   * already has does nothing.
   *
   * @param gamePaused Whether the game should be paused or not.
   */
  public void setGamePaused(boolean gamePaused) {
    if (this.gamePaused == gamePaused) {
      return;
    }
    this.gamePaused = gamePaused;
    Flixel.managers.onPaused(gamePaused);
    Flixel.Signals.gamePaused.dispatch(gamePaused ? GamePausedSignalData.PAUSED : GamePausedSignalData.RESUMED);
  }

  /**
   * Lets a paused game run exactly one more update, then stay paused.
   *
   * <p>The step happens during the next call to {@link #update(float)}, and only that one. Calling this
   * while the game is not paused does nothing, and calling it twice before the update runs still
   * results in one step. The debugger uses it for the "step one frame" button.
   */
  public void stepFrame() {
    if (gamePaused) {
      stepPending = true;
    }
  }

  /**
   * Called when the game window loses focus, or the application goes to the background.
   *
   * <p>On mobile and web, this fires when the OS sends the application to the background.
   * On desktop, it fires when the game window loses focus or is minimized (focus loss always
   * arrives before minimize, so this is called once for both events).
   *
   * <p>The default implementation notifies the active state and, when {@link Flixel#autoPause} is
   * {@code true}, stops updating the state. Then every manager is told (the sound pauses and the
   * frame rate drops while auto-pause is on). Duplicate calls without an intervening
   * {@link #onFocusGained()} are silently ignored.
   *
   * @see #onFocusGained()
   * @see #onMinimized()
   * @see Flixel.Signals#windowUnfocused
   */
  public void onFocusLost() {
    if (stateLifecyclePauseDispatched) {
      return;
    }
    stateLifecyclePauseDispatched = true;
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onFocusLost();
    }
    if (Flixel.autoPause) {
      shouldUpdate = false;
    }
    Flixel.managers.onFocusLost();
    Flixel.Signals.windowUnfocused.dispatch();
  }

  /**
   * Called when the game window regains focus, or the application returns to the foreground.
   *
   * <p>On mobile and web, this fires when the OS brings the application back to the foreground.
   * On desktop, it fires when the game window gains focus, including when the window is
   * restored from being minimized.
   *
   * <p>The default implementation notifies the active state, starts updating it again, and tells every
   * manager (the sound resumes and the frame rate comes back). Calls that arrive without a prior
   * {@link #onFocusLost()} are silently ignored.
   *
   * @see #onFocusLost()
   * @see Flixel.Signals#windowFocused
   */
  public void onFocusGained() {
    suspended = false;
    if (!stateLifecyclePauseDispatched) {
      return;
    }
    stateLifecyclePauseDispatched = false;
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onFocusGained();
    }
    if (Flixel.autoPause) {
      shouldUpdate = true;
    }
    Flixel.managers.onFocusGained();
    Flixel.Signals.windowFocused.dispatch();
  }

  /**
   * Called when the platform may stop the game soon without a clean shutdown, so data should be
   * saved now.
   *
   * <p>Backends call this when the Android activity pauses, when the browser tab is hidden or the
   * page unloads, and from {@link #destroy()} on every platform. The default implementation tells
   * every manager, then dispatches {@link Flixel.Signals#gameSuspended}. Repeat calls are ignored until
   * the game comes back to the foreground through {@link #onFocusGained()}.
   *
   * @see Flixel.Signals#gameSuspended
   */
  public void onSuspended() {
    if (suspended) {
      return;
    }
    suspended = true;
    Flixel.managers.onSuspended();
    Flixel.Signals.gameSuspended.dispatch();
  }

  /**
   * Called when the desktop window is minimized (iconified).
   *
   * <p>This is a desktop-only event and is never called on mobile or web platforms.
   * On most operating systems, focus loss fires first so {@link #onFocusLost()} already
   * handles audio and rendering pausing before this is called.
   *
   * <p>The default implementation notifies the active state and every manager, then dispatches
   * {@link Flixel.Signals#windowMinimized}.
   *
   * @see #onFocusLost()
   * @see #onFocusGained()
   * @see Flixel.Signals#windowMinimized
   */
  public void onMinimized() {
    FlixelState state = Flixel.state;
    if (state != null) {
      state.onMinimized();
    }
    Flixel.managers.onMinimized();
    Flixel.Signals.windowMinimized.dispatch();
  }

  /**
   * Destroys the game: the state first, then every manager in reverse order.
   *
   * <p>Note that this doesn't close the game entirely; it just disposes
   * of the game's resources. To close the entire game, use {@link Flixel#quit()}.
   */
  @Override
  public void destroy() {
    onSuspended();
    Flixel.Signals.preGameClose.dispatch();

    if (Flixel.state != null) {
      Flixel.state.destroy();
    }
    // Destroys every manager in reverse order: the sound, tweens, timers, debugger, and input
    // first, then the overlay, cameras, renderer, fonts, and the assets last.
    Flixel.managers.destroy();

    gamePaused = false;
    stepPending = false;
    stateLifecyclePauseDispatched = false;
    suspended = false;

    // The framework stops file logging and retires the asset manager in its own listeners here.
    Flixel.Signals.postGameClose.dispatch();
  }

  /**
   * Returns whether the update hooks that stop for a pause should be skipped this frame: the game is
   * paused (and no {@link #stepFrame()} is waiting), or it lost focus while {@link Flixel#autoPause} is on.
   */
  private boolean isUpdatePaused() {
    return (gamePaused && !stepPending) || !shouldUpdate;
  }

  private void updateState(float elapsed) {
    // Each state in the chain is updated only if it is the active (innermost) state or if its
    // persistentUpdate flag is true.
    FlixelState current = Flixel.state;
    while (current != null) {
      FlixelState sub = current.getSubState();
      if (sub == null || current.persistentUpdate) {
        current.update(elapsed);
      }
      current = sub;
    }
  }

  /**
   * Returns whether {@link #stepFrame()} scheduled a step that has not run yet.
   *
   * @return {@code true} when the next update runs even though the game is paused.
   */
  public boolean isStepPending() {
    return stepPending;
  }

  public boolean isGamePaused() {
    return gamePaused;
  }

  public boolean isShouldUpdate() {
    return shouldUpdate;
  }

  /**
   * Returns the scaled elapsed time (in seconds) for the current frame, as set by the most recent
   * {@link #advanceTime(float)} call.
   *
   * @return The current frame's elapsed time in seconds.
   */
  public float getElapsed() {
    return elapsed;
  }

  /**
   * Returns the raw clamped platform delta (in seconds) for the current frame, before
   * {@link Flixel#timeScale} was applied.
   *
   * @return The raw elapsed time in seconds for the current frame.
   */
  public float getRawElapsed() {
    return rawElapsed;
  }
}
