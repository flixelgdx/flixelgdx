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
package org.flixelgdx.backend.desktop;

import org.flixelgdx.backend.FlixelMonitor;
import org.jetbrains.annotations.NotNull;

/**
 * A data container for a monitor display, instantiated whenever SDL receives a monitor
 * refresh event.
 *
 * <p>You shouldn't create a new object of this class manually. This class is only used when
 * the {@link FlixelDesktopRunner} inside of the events handler receives any SDL {@code DISPLAY_*}
 * events. You're better off pulling the current monitors held inside of {@link FlixelDesktopHostIntegration},
 * which is much more reliable.
 */
class FlixelSdlMonitor implements FlixelMonitor {

  @NotNull
  final String name;

  final int virtualX;
  final int virtualY;
  final int width;
  final int height;
  final float refreshRate;

  final boolean isPrimary;

  FlixelSdlMonitor(@NotNull String name, int virtualX, int virtualY, int width, int height,
      float refreshRate, boolean isPrimary) {
    this.name = name;
    this.virtualX = virtualX;
    this.virtualY = virtualY;
    this.width = width;
    this.height = height;
    this.refreshRate = refreshRate;
    this.isPrimary = isPrimary;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  @Override
  public int getVirtualX() {
    return virtualX;
  }

  @Override
  public int getVirtualY() {
    return virtualY;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public float getRefreshRate() {
    return refreshRate;
  }

  @Override
  public boolean isPrimary() {
    return isPrimary;
  }
}
