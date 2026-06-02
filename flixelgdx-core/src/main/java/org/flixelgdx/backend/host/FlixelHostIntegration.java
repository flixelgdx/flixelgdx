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
package org.flixelgdx.backend.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Host operating system integration, which allows desktop notifications and taskbar attention.
 *
 * <p>This is separate from {@link org.flixelgdx.backend.alert.FlixelAlerter}, which shows blocking
 * dialog popups. Use {@link org.flixelgdx.Flixel#host} from game code.
 *
 * <p>Desktop LWJGL3 ships a full implementation (freedesktop {@code notify-send} on Linux rather than AWT for toasts,
 * GLFW plus AWT {@code java.awt.Taskbar} for attention where available). Mobile and web builds keep the safe no-op implementation.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.host.sendDesktopNotification("Ready", "Your level finished loading.");
 * Flixel.host.requestUserAttention();
 * }</pre>
 *
 * @see org.flixelgdx.Flixel#host
 */
public interface FlixelHostIntegration {

  /**
   * Shows a non-blocking desktop notification using the platform provider (Action Center on Windows, Notification Center on macOS,
   * D-Bus, or libnotify on Linux).
   *
   * @param title Short title, or {@code null} to use a blank title when the OS allows it.
   * @param message Body text; must not be {@code null}.
   */
  void sendNotification(@Nullable String title, @NotNull String message);

  /**
   * Asks the window manager to highlight this app (taskbar entry flash, dock bounce, and similar).
   */
  void requestAttention();

  /**
   * @return {@code true} if {@link #sendNotification(String, String)} is expected to do useful work on this platform session.
   */
  boolean supportsDesktopNotification();
}
