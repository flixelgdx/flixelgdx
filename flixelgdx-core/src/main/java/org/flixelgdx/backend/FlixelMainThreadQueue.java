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
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.jetbrains.annotations.Nullable;

/**
 * A thread-safe list of tasks waiting to run on the game's main thread, which a
 * {@link FlixelRuntimeDevice} uses to implement {@link FlixelRuntimeDevice#queueMainThread(Runnable)}.
 *
 * <p>Any thread may {@link #add(Runnable) add} a task, while the platform's game loop calls {@link #run()} once per
 * frame on the main thread. Think of it like a mailbox: anyone can drop a letter in at any time, and the owner empties
 * it once a day and reads every letter at their own desk.
 *
 * <p>Game code never uses this class directly; it calls
 * {@link FlixelRuntimeDevice#queueMainThread(Runnable) Flixel.runtime.queueMainThread(...)}. Backend authors keep one
 * queue per runtime device:
 *
 * <pre>{@code
 * public class MyRuntimeDevice implements FlixelRuntimeDevice {
 *
 *   private final FlixelMainThreadQueue mainThreadQueue = new FlixelMainThreadQueue();
 *
 *   @Override
 *   public void queueMainThread(Runnable action) {
 *     mainThreadQueue.add(action);
 *   }
 *
 *   @Override
 *   public void runMainThreadTasks() {
 *     mainThreadQueue.run();
 *   }
 * }
 * }</pre>
 *
 * <p>The queue keeps two lists and swaps them when it runs, so a background thread adding a task never waits for the
 * main thread to finish running the others, and no memory is allocated per frame once both lists have grown.
 *
 * @see FlixelRuntimeDevice#queueMainThread(Runnable)
 * @see Flixel#runtime
 */
public final class FlixelMainThreadQueue {

  private final Object lock = new Object();

  private FlixelArray<Runnable> pending = new FlixelArray<>();
  private FlixelArray<Runnable> running = new FlixelArray<>();

  private boolean draining;

  /**
   * Adds a task to run the next time {@link #run()} is called. Safe to call from any thread.
   *
   * @param action The task to run on the main thread; ignored when {@code null}.
   */
  public void add(@Nullable Runnable action) {
    if (action == null) {
      return;
    }
    synchronized (lock) {
      pending.add(action);
    }
  }

  /**
   * Runs every task added before this call, in the order they were added, then forgets them.
   *
   * <p>Call this from the main thread only. A task that adds another task while it runs does not run that new task
   * now; it waits for the next call, so a task that keeps queueing itself cannot freeze the game. Calling this from
   * inside a running task does nothing.
   *
   * <p>If a task throws, the exception is passed on to the caller (and from there to the crash handler), and the
   * tasks after it in this batch are dropped so they are not run twice.
   */
  public void run() {
    FlixelArray<Runnable> batch;
    synchronized (lock) {
      if (draining || pending.isEmpty()) {
        return;
      }
      draining = true;
      batch = pending;
      pending = running;
      running = batch;
    }
    try {
      for (int i = 0; i < batch.getSize(); i++) {
        batch.get(i).run();
      }
    } finally {
      batch.clear();
      synchronized (lock) {
        draining = false;
      }
    }
  }

  /** Drops every task that has not run yet. Safe to call from any thread. */
  public void clear() {
    synchronized (lock) {
      pending.clear();
    }
  }

  /**
   * Returns how many tasks are waiting for the next {@link #run()}. Safe to call from any thread.
   *
   * @return The number of waiting tasks.
   */
  public int getSize() {
    synchronized (lock) {
      return pending.getSize();
    }
  }
}
