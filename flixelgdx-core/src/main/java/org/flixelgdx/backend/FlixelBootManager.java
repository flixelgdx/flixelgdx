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
import org.jetbrains.annotations.NotNull;

/**
 * The centralized startup manager for FlixelGDX, accessible via {@link Flixel#boot}.
 *
 * <p>This is the central area that every platform uses to configure the boot sequence.
 * Register {@link Runnable} callbacks that execute before and after {@link Flixel#start}
 * finishes, or supply the {@link FlixelGameRunner} that owns the update/draw loop.
 *
 * <p>Unless you are an advanced user who wishes to extend the framework in your game, you will
 * likely never interact with this class directly. Note that if you try to register callbacks after
 * the game has already started, this class will throw an exception.
 *
 * <p>Example:
 * <pre>{@code
 * Flixel.boot.beforeStart(() -> {
 *   Flixel.log.info("Setting up custom asset loader...");
 * });
 * Flixel.boot.afterStart(() -> {
 *   Flixel.debug.registerCommand("reload", args -> reloadLevel());
 * });
 * }</pre>
 *
 * @author stringdotjar
 */
public class FlixelBootManager {

  private final FlixelArray<Runnable> beforeStart = new FlixelArray<>();
  private final FlixelArray<Runnable> afterStart = new FlixelArray<>();
  private boolean initialized = false;

  /**
   * Adds a new callback that runs <i>before</i> {@link Flixel#start} is executed.
   *
   * @param runnable The new callback to run before the start method is executed.
   * @throws RuntimeException If a callback is attempted to be added after the boot sequence finishes.
   */
  public void beforeStart(@NotNull Runnable runnable) {
    if (initialized) {
      throw new RuntimeException(
          "The game has already finished its boot sequence, this method can no longer be used.");
    }
    beforeStart.add(runnable);
  }

  /**
   * Adds a new callback that runs <i>after</i> {@link Flixel#start} is executed.
   *
   * @param runnable The new callback to run after the start method is executed.
   * @throws RuntimeException If a callback is attempted to be added after the boot sequence finishes.
   */
  public void afterStart(@NotNull Runnable runnable) {
    if (initialized) {
      throw new RuntimeException(
          "The game has already finished its boot sequence, this method can no longer be used.");
    }
    afterStart.add(runnable);
  }

  /** Returns the registered before-start callbacks. Called internally by {@link Flixel#start}. */
  @NotNull
  public FlixelArray<Runnable> getBeforeStart() {
    return beforeStart;
  }

  /** Returns the registered after-start callbacks. Called internally by {@link Flixel#start}. */
  @NotNull
  public FlixelArray<Runnable> getAfterStart() {
    return afterStart;
  }

  /** Marks the boot sequence as complete. Called internally by {@link Flixel#start}. */
  public void markInitialized() {
    initialized = true;
  }

  /** Returns whether the boot sequence has finished. */
  public boolean isInitialized() {
    return initialized;
  }
}
