/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util.timer;

import com.badlogic.gdx.utils.Pool;

import me.stringdotjar.flixelgdx.FlixelDestroyable;
import me.stringdotjar.flixelgdx.FlixelUpdatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Frame-based countdown timer.
 *
 * <p>Advance time by calling {@link FlixelTimerManager#update(float)} once per frame (done automatically for
 * {@link #getGlobalManager()}). The elapsed argument should already include
 * {@link me.stringdotjar.flixelgdx.Flixel#getTimeScale()}.
 *
 * <p>Prefer {@link FlixelTimerManager#start(float, FlixelTimerListener, int)}, {@link FlixelTimer#wait(float, FlixelTimerListener)},
 * or {@link FlixelTimer#loop(float, FlixelTimerListener, int)} so pooled instances are reused.
 *
 * <p>The callback runs on the next {@code update} (one frame later), {@code loops} is ignored.
 *
 * <p>The timer is removed from its manager before the final callback runs.
 */
public class FlixelTimer implements FlixelUpdatable, FlixelDestroyable, Pool.Poolable {

  /** Whether the timer is currently active. */
  public boolean active;

  /** Whether the timer has finished. */
  public boolean finished;

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

  /** Global timer manager, updated from {@link me.stringdotjar.flixelgdx.FlixelGame#update(float)}. */
  @NotNull
  private static final FlixelTimerManager GLOBAL_MANAGER = new FlixelTimerManager();

  /** The parent manager of the timer. */
  @NotNull
  protected FlixelTimerManager manager;

  /** The time remaining for the timer. */
  private float timeLeft;

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
