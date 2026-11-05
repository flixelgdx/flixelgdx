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
import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;

/**
 * Marks named time ranges so an external profiler can show where a frame spends its time.
 *
 * <p>Think of it like a stopwatch with labels: you call {@link #begin(String)} when a chunk of work
 * starts and {@link #end()} when it finishes, and a profiling tool draws a labeled bar for that
 * span. Access the active implementation via {@link Flixel#profiler}.
 *
 * <p>The default implementation does nothing, so markers cost almost nothing when no profiler is
 * attached. Launchers on supported platforms (for example, Android, which feeds the system trace)
 * install a real implementation before {@link Flixel#start(FlixelGame, FlixelGameRunner)
 * Flixel.start(...)} runs. You should not need to assign {@link Flixel#profiler} from game code
 * unless you are providing a custom backend.
 *
 * <p>Calls must be balanced and strictly nested: every {@link #begin(String)} needs exactly one
 * matching {@link #end()} on the same thread, so always pair them with {@code try}/{@code finally}.
 * Pass a constant string for the name so no garbage is created. Android trace sections are limited
 * to 127 characters.
 *
 * <p>Example:
 *
 * <pre>{@code
 * private static final String SPAWN = "game.spawnWave";
 *
 * Flixel.profiler.begin(SPAWN);
 * try {
 *   spawnWave();
 * } finally {
 *   Flixel.profiler.end();
 * }
 * }</pre>
 *
 * @see Flixel#profiler
 */
public interface FlixelProfiler {

  /**
   * Starts a named section on the calling thread.
   *
   * @param name The section label. Use a compile-time constant of 127 characters or fewer.
   */
  void begin(@NotNull String name);

  /** Ends the most recently started section on the calling thread. */
  void end();
}
