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
package org.flixelgdx.backend.android;

import org.flixelgdx.FlixelGame;
import org.jetbrains.annotations.NotNull;

/**
 * Placeholder launcher for the Android backend.
 *
 * <p>The Android backend is not implemented on the framework's current rendering and platform stack
 * (bgfx for drawing, SDL3 for windowing and input). The desktop backend is the reference those
 * systems are proven against first; Android is brought online in a later phase, using bgfx and SDL3
 * through the NDK.
 *
 * <p>This entry point fails fast with a clear message rather than starting a half-initialized game.
 * Use {@code flixelgdx-desktop} to run games today.
 */
public final class FlixelAndroidLauncher {

  private FlixelAndroidLauncher() {}

  /**
   * Fails fast: the Android backend is not available yet.
   *
   * @param game The game instance that would be launched.
   * @throws UnsupportedOperationException Always, because the Android backend is not implemented.
   */
  public static void launch(@NotNull FlixelGame game) {
    throw new UnsupportedOperationException("The Android backend is not available yet.");
  }
}
