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
package org.flixelgdx.signal;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelState;

/**
 * Convenience class for holding all signal data types used in the default signals,
 * which are stored in the global {@link Flixel} manager class.
 *
 * <p>{@link UpdateSignalData} is a mutable, reusable class rather than a record
 * because it is dispatched every frame. Allocating a new object 120 times per second
 * (pre+post, and assuming the FPS is 60) adds GC pressure that causes frame stutters.
 * Signal handlers must not hold a reference to the data object past the callback return.
 */
public final class FlixelSignalData {

  /**
   * Mutable carrier for per-frame update data. Reuse the same instance across frames to
   * avoid GC pressure. Do NOT store a reference to this object; read values during the
   * callback only.
   */
  public static final class UpdateSignalData {
    private float elapsed;

    /**
     * Returns the elapsed time in seconds for this update frame.
     *
     * @return The elapsed time in seconds since the last frame.
     */
    public float elapsed() {
      return elapsed;
    }

    /**
     * Populates this carrier with update data for the current frame.
     *
     * @param elapsed Seconds elapsed since the last frame.
     */
    public void set(float elapsed) {
      this.elapsed = elapsed;
    }
  }

  /**
   * Carries the state involved in a state switch, and what else the switch clears, when a state
   * switch signal fires or a manager's state switch hook runs.
   *
   * @param state The state being left when the switch begins, or the new state when it ends.
   * @param clearTweens Whether the switch cancels every global tween and clears the tween pools.
   * @param clearTimers Whether the switch cancels every global timer.
   */
  public record StateSwitchSignalData(FlixelState state, boolean clearTweens, boolean clearTimers) {

    /**
     * Creates the data for a switch that clears both the global tweens and the global timers.
     *
     * @param state The state involved in the switch.
     */
    public StateSwitchSignalData(FlixelState state) {
      this(state, true, true);
    }
  }

  /**
   * Carries whether the game loop was just paused or resumed when {@link Flixel.Signals#gamePaused}
   * fires.
   *
   * <p>The framework only ever dispatches the two shared constants, {@link #PAUSED} and
   * {@link #RESUMED}, so toggling the pause never allocates a new object.
   *
   * @param paused {@code true} when the game loop was just paused, {@code false} when it resumed.
   */
  public record GamePausedSignalData(boolean paused) {

    /** Shared data dispatched when the game loop is paused. */
    public static final GamePausedSignalData PAUSED = new GamePausedSignalData(true);

    /** Shared data dispatched when the game loop is resumed. */
    public static final GamePausedSignalData RESUMED = new GamePausedSignalData(false);
  }

  private FlixelSignalData() {}
}
