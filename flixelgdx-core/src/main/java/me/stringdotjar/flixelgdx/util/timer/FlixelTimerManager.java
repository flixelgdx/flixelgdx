/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util.timer;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import me.stringdotjar.flixelgdx.FlixelBasic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Owns active {@link FlixelTimer} instances and advances them once per frame. Extends {@link FlixelBasic} like
 * HaxeFlixel plugin-style managers so {@link #active}, {@link #exists}, {@link #kill}, and {@link #destroy} gate
 * {@link #update(float)} the same way as other Flixel objects.
 *
 * <p>Use {@link FlixelTimer#getGlobalManager()} with {@link me.stringdotjar.flixelgdx.FlixelGame} (already wired) or
 * construct a dedicated manager for isolated groups (for example add it to a {@link me.stringdotjar.flixelgdx.FlixelState}).
 *
 * <p>Timers are backed by a {@link Pool} to avoid per-delay allocations. {@link #start(float, FlixelTimerListener, int)}
 * obtains from the pool; {@link FlixelTimer#cancel()} and completed runs return instances to the pool.
 */
public class FlixelTimerManager extends FlixelBasic {

  /** Timers currently stepped by {@link #update(float)}. Named {@code activeTimers} to avoid clashing with {@link #active}. */
  protected final Array<FlixelTimer> activeTimers = new Array<>(false, 32);

  protected final Pool<FlixelTimer> pool = new Pool<>(32) {
    @Override
    protected FlixelTimer newObject() {
      return new FlixelTimer();
    }
  };

  /**
   * Creates a new timer manager with a default pool of capacity {@code 32}.
   */
  public FlixelTimerManager() {
    super();
    visible = false;
  }

  /**
   * Starts a pooled timer registered with {@code this} manager.
   *
   * @param timeSeconds See the documentation for {@link FlixelTimer#start(float, FlixelTimerListener, int)}.
   * @param onComplete Optional callback (each loop, including the last).
   * @param loopCount {@code 0} for infinite.
   * @return The started timer.
   */
  @NotNull
  public FlixelTimer start(float timeSeconds, @Nullable FlixelTimerListener onComplete, int loopCount) {
    FlixelTimer t = pool.obtain();
    t.manager = this;
    t.startInternal(timeSeconds, onComplete, loopCount);
    addIfMissing(t);
    return t;
  }

  /**
   * Starts a pooled timer registered with {@code this} manager.
   *
   * @param timeSeconds The time to complete the timer.
   * @param onComplete The callback to run when the timer completes.
   * @return The timer.
   */
  @NotNull
  public FlixelTimer start(float timeSeconds, @Nullable FlixelTimerListener onComplete) {
    return start(timeSeconds, onComplete, 1);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    if (!active || !exists || !alive) {
      return;
    }
    for (int i = activeTimers.size - 1; i >= 0; i--) {
      activeTimers.get(i).update(elapsed);
    }
  }

  @Override
  public final void draw(Batch batch) {}

  @Override
  public void destroy() {
    cancelAll();
    super.destroy();
  }

  /**
   * Adds a timer to the active list if it is not already present.
   *
   * @param t The timer to add.
   */
  public void addIfMissing(@NotNull FlixelTimer t) {
    if (!activeTimers.contains(t, true)) {
      activeTimers.add(t);
    }
  }

  /**
   * Removes the timer from the active list without returning it to the pool (used before restart).
   *
   * @param t The timer to remove.
   */
  public void detachOnly(@NotNull FlixelTimer t) {
    activeTimers.removeValue(t, true);
  }

  /**
   * Removes the timer from the active list and frees it from the pool.
   *
   * @param t The timer to remove.
   */
  public void removeAndFree(@NotNull FlixelTimer t) {
    activeTimers.removeValue(t, true);
    pool.free(t);
  }

  /**
   * Removes the timer from the active list, invokes the listener, then returns the instance to the pool.
   * Used for the final iteration so the callback sees a finished timer that is no longer managed.
   *
   * @param t The timer to finish.
   * @param cb The callback to run when the timer finishes.
   */
  public void finishLast(@NotNull FlixelTimer t, @Nullable FlixelTimerListener cb) {
    activeTimers.removeValue(t, true);
    t.markFinished();
    if (cb != null) {
      cb.onComplete(t);
    }
    pool.free(t);
  }

  /** Stops every running timer and returns instances to the pool. */
  public void cancelAll() {
    while (activeTimers.size > 0) {
      FlixelTimer timer = activeTimers.pop();
      timer.markFinished();
      pool.free(timer);
    }
    activeTimers.clear();
  }

  /** Completes all active timers in the manager. */
  public void completeAll() {
    cancelAll();
  }
}
