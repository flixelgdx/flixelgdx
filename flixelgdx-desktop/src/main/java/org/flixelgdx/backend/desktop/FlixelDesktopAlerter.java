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

import org.flixelgdx.backend.FlixelAlerter;
import org.lwjgl.sdl.SDLMessageBox;

/**
 * The desktop alert dialog provider, backed by SDL's simple message boxes.
 *
 * <p>These are blocking modal dialogs; reserve them for critical events. Non-blocking OS toasts go
 * through {@link org.flixelgdx.Flixel#host Flixel.host} instead.
 */
public class FlixelDesktopAlerter implements FlixelAlerter {

  @Override
  public void info(String title, String message) {
    show(SDLMessageBox.SDL_MESSAGEBOX_INFORMATION, title, message);
  }

  @Override
  public void warn(String title, String message) {
    show(SDLMessageBox.SDL_MESSAGEBOX_WARNING, title, message);
  }

  @Override
  public void error(String title, String message) {
    show(SDLMessageBox.SDL_MESSAGEBOX_ERROR, title, message);
  }

  private static void show(int flags, String title, String message) {
    try {
      SDLMessageBox.SDL_ShowSimpleMessageBox(flags,
          title != null ? title : "",
          message != null ? message : "",
          0L);
    } catch (Throwable ignored) {
      // Never let a failed dialog crash the game; the message is already logged by the caller.
    }
  }
}
