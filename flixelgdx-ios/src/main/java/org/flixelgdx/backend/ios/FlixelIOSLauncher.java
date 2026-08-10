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
package org.flixelgdx.backend.ios;

import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholder launcher for the iOS backend.
 *
 * <p>The iOS backend is not implemented on the framework's current rendering and platform stack
 * (bgfx for drawing, SDL3 for windowing and input). The desktop backend is the reference
 * implementation those systems are proven against first; iOS is brought online in a later phase.
 *
 * <p>Every entry point here fails fast with a clear message rather than pretending to start, so a
 * project wired to this backend gets an immediate, understandable error instead of a silent
 * half-initialized game. Use {@code flixelgdx-desktop} to run games today.
 */
public final class FlixelIOSLauncher {

  private FlixelIOSLauncher() {}

  /**
   * Fails fast: the iOS backend is not available yet.
   *
   * @param game The game instance that would be launched.
   * @throws UnsupportedOperationException Always, because the iOS backend is not implemented.
   */
  public static void launch(@NotNull FlixelGame game) {
    throw new UnsupportedOperationException(
        "The iOS backend is not available yet. Run games on flixelgdx-desktop, which is the "
            + "reference backend (bgfx + SDL3); iOS support lands in a later phase.");
  }
}
