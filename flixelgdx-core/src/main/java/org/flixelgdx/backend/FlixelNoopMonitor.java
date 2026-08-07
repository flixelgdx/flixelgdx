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

import org.jetbrains.annotations.NotNull;

/**
 * Safe placeholder {@link FlixelMonitor} for platforms that cannot report a real display.
 *
 * <p>Returning this instead of {@code null} means game code that reads the current monitor never
 * has to null-check and never crashes on web, mobile, or headless targets. Every value is neutral:
 * an empty name, a zero position, a zero size, and not primary.
 */
public enum FlixelNoopMonitor implements FlixelMonitor {

  /** Shared no-op instance. */
  INSTANCE;

  @Override
  @NotNull
  public String getName() {
    return "";
  }

  @Override
  public int getVirtualX() {
    return 0;
  }

  @Override
  public int getVirtualY() {
    return 0;
  }

  @Override
  public int getWidth() {
    return 0;
  }

  @Override
  public int getHeight() {
    return 0;
  }

  @Override
  public int getRefreshRate() {
    return 0;
  }

  @Override
  public boolean isPrimary() {
    return false;
  }
}
