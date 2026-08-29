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
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLClipboard;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * The desktop host integration.
 *
 * <p>Reports the {@link FlixelPlatform#Desktop Desktop} platform and provides SDL-backed clipboard
 * access. Toast notifications and taskbar attention are platform-specific and left as no-ops for
 * now; they can be layered on per OS later.
 */
public class FlixelDesktopHostIntegration implements FlixelHostIntegration {

  /**
   * The maximum amount of characters allowed for a notification body.
   *
   * <p>Most operating systems cap it around 6000 characters, so we use the same value to follow
   * that convention, assuring no unexpected crashes happen.
   */
  public static final int MAX_NOTIFY_ARG_LEN = 6000;

  final FlixelArray<FlixelMonitor> monitors = new FlixelArray<>(FlixelMonitor[]::new);

  private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message cannot be null.");
    if (isWindows()) {
      // TODO: Figure out how to send a notification on Windows properly, since my poor
      // ass can't afford to get a different computer to test this properly.
    } else if (isMac()) {
      String t = title == null ? "" : truncateArg(title, MAX_NOTIFY_ARG_LEN);
      String m = truncateArg(message, 6000);
      String script = "display notification \"" + escapeOsascript(m) + "\" with title \"" + escapeOsascript(t) + "\"";
      tryStartProcess(new ProcessBuilder("osascript", "-e", script));
    } else if (isLinux()) {
      // I'm not sure if this will work on every distro. It's here because it works, at least for
      // me, although I need to ensure it works reliably. If only I had a community to help me
      // test it... :pensive:
      String summary = truncateArg(title == null || title.isEmpty() ? " " : title, MAX_NOTIFY_ARG_LEN);
      String body = truncateArg(message, MAX_NOTIFY_ARG_LEN);
      if (tryStartProcess(new ProcessBuilder("notify-send", summary, body))) {
        return;
      }
      tryStartProcess(new ProcessBuilder("zenity", "--notification", "--text", summary + ": " + body));
    }
  }

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

  private static boolean tryStartProcess(ProcessBuilder pb) {
    try {
      pb.redirectError(ProcessBuilder.Redirect.DISCARD);
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      pb.start();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private static String truncateArg(String s, int maxLen) {
    if (s.length() <= maxLen) {
      return s;
    }
    return s.substring(0, maxLen);
  }

  private static String escapeOsascript(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private boolean isWindows() {
    return OS.contains("windows");
  }

  private boolean isMac() {
    return OS.contains("mac");
  }

  private boolean isLinux() {
    return OS.contains("linux");
  }
}
