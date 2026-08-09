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

import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;

/**
 * The platform's main loop: creates the window (where one exists) and drives the game's
 * update/draw cycle until the application exits.
 *
 * <p>Each backend installs its runner on {@link org.flixelgdx.Flixel#runner Flixel.runner}
 * before {@link org.flixelgdx.Flixel#start Flixel.start} is called; {@code start} wires the
 * core systems, then hands the game to the runner, which owns the loop from there. Desktop
 * pumps SDL events and renders through bgfx; web schedules browser animation frames; a
 * headless session leaves the default no-op runner in place so {@code start} returns
 * immediately.
 *
 * <p>The runner is expected to call {@link FlixelGame#create()} once the platform surface is
 * ready, then {@link FlixelGame#render()} every frame, {@link FlixelGame#resize(int, int)} on
 * size changes, and the focus/lifecycle hooks as its platform reports them.
 */
@FunctionalInterface
public interface FlixelGameRunner {

  /** A runner that returns immediately, used on headless sessions and in unit tests. */
  FlixelGameRunner NOOP = game -> {};

  /**
   * Runs the game to completion. On most platforms this blocks until the game exits; on
   * browser-style platforms it schedules callbacks and returns.
   *
   * @param game The game to drive.
   */
  void run(@NotNull FlixelGame game);
}
