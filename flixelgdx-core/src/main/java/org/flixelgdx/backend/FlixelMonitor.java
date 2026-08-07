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
 * A physical display attached to the machine, as reported by the host platform.
 *
 * <p>Monitors are essentially a desktop concept: a laptop with an external screen has two, a
 * typical phone or browser tab effectively has one. Query them from
 * {@link FlixelHostIntegration#getMonitors() Flixel.host.getMonitors()} to build a "which screen"
 * setting, then pair a chosen monitor with a display mode when going fullscreen.
 *
 * <p>Each backend returns its own implementations of this interface, so game code can hold and
 * compare {@code FlixelMonitor} values without knowing anything about the underlying windowing
 * system.
 *
 * <p>The position fields ({@link #getVirtualX()} / {@link #getVirtualY()}) place the monitor inside
 * the combined "virtual desktop" that spans every screen, so you can tell which monitor is to the
 * left of which.
 *
 * @see FlixelHostIntegration#getMonitors()
 */
public interface FlixelMonitor {

  /**
   * @return A human-readable name for the monitor (for example, its model), or a generic label when
   *     the platform does not provide one; never {@code null}.
   */
  @NotNull
  String getName();

  /**
   * @return The monitor's left edge, in virtual-desktop coordinates that span all screens.
   */
  int getVirtualX();

  /**
   * @return The monitor's top edge, in virtual-desktop coordinates that span all screens.
   */
  int getVirtualY();

  /**
   * @return The monitor's width in physical pixels at its current mode.
   */
  int getWidth();

  /**
   * @return The monitor's height in physical pixels at its current mode.
   */
  int getHeight();

  /**
   * @return The monitor's refresh rate in hertz, or {@code 0} when the platform cannot report it.
   */
  int getRefreshRate();

  /**
   * @return {@code true} if this is the primary monitor, where the OS typically places new windows
   *     and system UI.
   */
  boolean isPrimary();
}
