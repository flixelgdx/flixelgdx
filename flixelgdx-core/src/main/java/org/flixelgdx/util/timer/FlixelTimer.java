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
package org.flixelgdx.util.timer;

import com.badlogic.gdx.utils.Pool;

import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.functional.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Frame-based countdown timer.
 *
 * <p>Advance time by calling {@link FlixelTimerManager#update(float)} once per frame (done automatically for
 * {@link #getGlobalManager()}). The elapsed argument should already include
 * {@link org.flixelgdx.Flixel#timeScale Flixel.timeScale}.
 *
 * <p>Prefer {@link FlixelTimerManager#start(float, FlixelTimerListener, int)}, {@link FlixelTimer#wait(float, FlixelTimerListener)},
 * or {@link FlixelTimer#loop(float, FlixelTimerListener, int)} so pooled instances are reused.
 *
 * <p>The callback runs on the next {@code update} (one frame later), {@code loops} is ignored.
 *
 * <p>The timer is removed from its manager before the final callback runs.
 */
public class FlixelTimer implements FlixelUpdatable, FlixelDestroyable, Pool.Poolable {

  /** Global timer manager, updated from {@link org.flixelgdx.FlixelGame#update(float) FlixelGame.update(float)}. */
  @NotNull
  private static final FlixelTimerManager GLOBAL_MANAGER = new FlixelTimerManager();

  /** The time to complete the timer. */
  public float time;

  /** Requested loop count. {@code 0} means infinite (only when {@link #time} is greater than zero). */
  public int loops;

  /** The number of loops the timer has completed. */
  public int elapsedLoops;

  /** The time elapsed since the timer started. */
  public float elapsedTime;

  /** The callback to run when the timer completes. */
  @Nullable
  public FlixelTimerListener onComplete;

  /** The parent manager of the timer. */
  @NotNull
  protected FlixelTimerManager manager;

  /** The time remaining for the timer. */
  private float timeLeft;

  /** Whether the timer is currently active. */
  public boolean active;

  /** Whether the timer has finished. */
  public boolean finished;

  /** Whether to fire the next update. */
  private boolean fireNextUpdate;

  /** Whether the timer has a zero duration. */
  private boolean zeroDuration;

  /** Constructs a new timer object with the global manager. */
  public FlixelTimer() {
    this(GLOBAL_MANAGER);
  }

  /**
   * Constructs a new timer object with a parent manager.
   *
   * @param manager The parent manager of the timer.
   */
  public FlixelTimer(@Nullable FlixelTimerManager manager) {
    if (manager != null) {
      this.manager = manager;
    }
  }

  public float getTimeLeft() {
    if (!active || finished) {
      return 0f;
    }
    return fireNextUpdate ? 0f : Math.max(0f, timeLeft);
  }

  public int getLoopsLeft() {
    if (!active || finished) {
      return 0;
    }
    if (zeroDuration) {
      return 0;
    }
    if (loops == 0) {
      return Integer.MAX_VALUE;
    }
    return Math.max(0, loops - elapsedLoops);
  }

  public float getProgress() {
    if (zeroDuration || !active || finished) {
      return 1f;
    }
    if (time <= 0f) {
      return 1f;
    }
    return Math.min(1f, 1f - timeLeft / time);
  }

  /**
   * Starts the timer.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onComplete The callback to run when the timer completes.
   * @param loopCount The number of loops to run the timer.
   */
  void startInternal(float timeSeconds, @Nullable FlixelTimerListener onComplete, int loopCount) {
    this.time = Math.max(0f, timeSeconds);
    this.onComplete = onComplete;
    this.elapsedLoops = 0;
    this.elapsedTime = 0f;
    this.finished = false;
    this.zeroDuration = (this.time <= 0f);
    if (this.zeroDuration) {
      this.loops = 1;
      this.fireNextUpdate = true;
      this.timeLeft = 0f;
    } else {
      this.loops = Math.max(0, loopCount);
      this.fireNextUpdate = false;
      this.timeLeft = this.time;
    }
    this.active = true;
  }

  /**
   * Starts the timer.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onComplete The callback to run when the timer completes.
   * @return The timer.
   */
  @NotNull
  public FlixelTimer start(float timeSeconds, @Nullable FlixelTimerListener onComplete) {
    return start(timeSeconds, onComplete, 1);
  }

  /**
   * Restarts this timer on its current {@link #manager} without allocating a new instance.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onComplete The callback to run when the timer completes.
   * @param loopCount The number of loops to run the timer.
   * @return The timer.
   */
  @NotNull
  public FlixelTimer start(float timeSeconds, @Nullable FlixelTimerListener onComplete, int loopCount) {
    manager.detachOnly(this);
    startInternal(timeSeconds, onComplete, loopCount);
    manager.addIfMissing(this);
    return this;
  }

  /**
   * Restarts the timer.
   *
   * @param newTimeSeconds The new time to complete the timer.
   * @return The timer.
   */
  @NotNull
  public FlixelTimer restart(float newTimeSeconds) {
    float t = newTimeSeconds < 0f ? time : Math.max(0f, newTimeSeconds);
    int lc = zeroDuration ? 1 : loops;
    return start(t, onComplete, lc);
  }

  /**
   * Cancels the timer and removes it from its manager.
   */
  public void cancel() {
    if (!active) {
      return;
    }
    markFinished();
    manager.removeAndFree(this);
  }

  /**
   * Marks the timer as finished.
   */
  public void markFinished() {
    active = false;
    finished = true;
  }

  @Override
  public void update(float elapsed) {
    if (!active || finished) {
      return;
    }
    elapsedTime += elapsed;

    if (fireNextUpdate) {
      fireNextUpdate = false;
      completeLoop(true, 0f);
      return;
    }

    if (zeroDuration) {
      return;
    }

    timeLeft -= elapsed;
    while (active && !finished && timeLeft <= 0f) {
      float remainder = timeLeft;
      completeLoop(false, remainder);
    }
  }

  @Override
  public void destroy() {
    cancel();
  }

  private void completeLoop(boolean fromZeroDelay, float negativeRemainder) {
    elapsedLoops++;
    boolean last = fromZeroDelay || (loops > 0 && elapsedLoops >= loops);
    FlixelTimerListener cb = onComplete;

    if (last) {
      manager.finishLast(this, cb);
      return;
    }

    if (cb != null) {
      cb.onComplete(this);
    }

    timeLeft = time + negativeRemainder;
  }

  @Override
  public void reset() {
    active = false;
    finished = true;
    time = 0f;
    loops = 0;
    elapsedLoops = 0;
    elapsedTime = 0f;
    timeLeft = 0f;
    fireNextUpdate = false;
    zeroDuration = false;
    onComplete = null;
    manager = GLOBAL_MANAGER;
  }

  /** Cancels all active timers in the global manager. */
  public static void cancelAll() {
    GLOBAL_MANAGER.cancelAll();
  }

  /** Completes all active timers in the global manager. */
  public static void completeAll() {
    GLOBAL_MANAGER.completeAll();
  }

  /**
   * Starts a pooled timer registered with the global manager.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onComplete The callback to run when the timer completes.
   * @return The timer.
   */
  @NotNull
  public static FlixelTimer wait(float timeSeconds, @NotNull FlixelTimerListener onComplete) {
    return GLOBAL_MANAGER.start(timeSeconds, onComplete, 1);
  }

  /**
   * Starts a pooled timer registered with the global manager.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onEachLoop The callback to run when the timer completes each loop.
   * @param loopCount The number of loops to run the timer.
   * @return The timer.
   */
  @NotNull
  public static FlixelTimer loop(float timeSeconds, @NotNull FlixelTimerListener onEachLoop, int loopCount) {
    return GLOBAL_MANAGER.start(timeSeconds, onEachLoop, loopCount);
  }

  @NotNull
  public static FlixelTimerManager getGlobalManager() {
    return GLOBAL_MANAGER;
  }
}
