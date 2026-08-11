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

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.backend.FlixelMonitor;
import org.flixelgdx.backend.FlixelNoopMonitor;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLClipboard;

/**
 * The desktop host integration.
 *
 * <p>Reports the {@link FlixelPlatform#Desktop Desktop} platform and provides SDL-backed clipboard
 * access. Toast notifications and taskbar attention are platform-specific and left as no-ops for
 * now; they can be layered on per OS later.
 */
public class FlixelDesktopHostIntegration implements FlixelHostIntegration {

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();
  private final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>(FlixelMonitor[]::new);

  @Override
  public void requestNotificationPermission() {}

  @Override
  public void requestAttention() {}

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {}

  @Override
  public void copyToClipboard(@NotNull String text) {
    try {
      SDLClipboard.SDL_SetClipboardText(text);
    } catch (Throwable ignored) {
      // Clipboard access is best-effort.
    }
  }

  @Override
  public void pasteFromClipboard() {
    try {
      String text = SDLClipboard.SDL_GetClipboardText();
      onTextPasted.dispatch(text != null ? text : "");
    } catch (Throwable ignored) {
      // Clipboard access is best-effort.
    }
  }

  @Override
  public boolean supportsNotifications() {
    return true;
  }

  @Override
  public boolean supportsClipboard() {
    return true;
  }

  @NotNull
  @Override
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  @NotNull
  @Override
  public FlixelList<FlixelMonitor> getMonitors() {
    return monitors;
  }

  @NotNull
  @Override
  public FlixelPlatform getPlatform() {
    return FlixelPlatform.Desktop;
  }
}
