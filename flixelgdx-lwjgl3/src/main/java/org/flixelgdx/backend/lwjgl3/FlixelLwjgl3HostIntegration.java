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
package org.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import org.flixelgdx.backend.FlixelHostIntegration;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * Desktop {@link FlixelHostIntegration}: freedesktop or Zenity notifications on Linux,
 * {@code osascript} on macOS, WinRT toasts via PowerShell on Windows, GLFW window attention,
 * AWT {@link Taskbar} attention, platform screen wake lock, and AWT text clipboard access.
 */
public final class FlixelLwjgl3HostIntegration implements FlixelHostIntegration {

  private static final int MAX_NOTIFY_ARG_LEN = 6000;
  private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

  private final FlixelSignal<String> onTextPasted = new FlixelSignal<>();

  @Nullable
  private Process wakeLockProcess;

  @Override
  public void requestNotificationPermission() {}

  @Override
  public void requestAttention() {
    if (Gdx.graphics instanceof Lwjgl3Graphics g) {
      g.getWindow().postRunnable(() -> GLFW.glfwRequestWindowAttention(g.getWindow().getWindowHandle()));
    }
    if (GraphicsEnvironment.isHeadless()) {
      return;
    }
    try {
      EventQueue.invokeLater(() -> {
        if (!Taskbar.isTaskbarSupported()) {
          return;
        }
        try {
          Taskbar.getTaskbar().requestUserAttention(true, false);
        } catch (Exception ignored) {
          // Ignored.
        }
      });
    } catch (Exception ignored) {
      // Ignored.
    }
  }

  @Override
  public void keepScreenAwake(boolean awake) {
    if (!supportsWakeLock()) {
      return;
    }
    if (awake) {
      if (wakeLockProcess != null) {
        return;
      }
      if (isMac()) {
        wakeLockProcess = tryStartWakeLockProcess(new ProcessBuilder("caffeinate", "-d", "-i"));
      } else if (isLinux()) {
        wakeLockProcess = tryStartWakeLockProcess(new ProcessBuilder(
            "systemd-inhibit", "--what=idle:sleep",
            "--who=FlixelGDX", "--why=Game",
            "--mode=block", "sleep", "infinity"));
      }
    } else {
      if (wakeLockProcess != null) {
        wakeLockProcess.destroyForcibly();
        wakeLockProcess = null;
      }
    }
  }

  @Override
  public void setExitConfirmation(@Nullable String message) {}

  @Override
  public void sendNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
    if (!supportsDesktopNotification()) {
      return;
    }
    if (isLinux()) {
      notifyLinux(title, message);
    } else if (isMac()) {
      notifyMac(title, message);
    } else if (isWindows()) {
      notifyWindows(title, message);
    }
  }

  @Override
  public void copyToClipboard(@NotNull String text) {
    Objects.requireNonNull(text, "text");
    if (!supportsClipboard()) {
      return;
    }
    EventQueue.invokeLater(() -> Toolkit.getDefaultToolkit().getSystemClipboard()
        .setContents(new StringSelection(text), null));
  }

  @Override
  public void pasteFromClipboard() {
    if (!supportsClipboard()) {
      return;
    }
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable contents = clipboard.getContents(null);
      if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        return;
      }
      String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
      onTextPasted.dispatch(text);
    } catch (UnsupportedFlavorException | IOException ignored) {
      // Ignored.
    }
  }

  @Override
  public boolean supportsDesktopNotification() {
    return !GraphicsEnvironment.isHeadless();
  }

  @Override
  public boolean supportsWakeLock() {
    return !GraphicsEnvironment.isHeadless() && (isMac() || isLinux());
  }

  @Override
  public boolean supportsClipboard() {
    return !GraphicsEnvironment.isHeadless();
  }

  @Override
  @NotNull
  public FlixelSignal<String> onTextPasted() {
    return onTextPasted;
  }

  private static void notifyLinux(@Nullable String title, String message) {
    String summary = truncateArg(title == null || title.isEmpty() ? " " : title, MAX_NOTIFY_ARG_LEN);
    String body = truncateArg(message, MAX_NOTIFY_ARG_LEN);
    if (tryStartProcess(new ProcessBuilder("notify-send", summary, body))) {
      return;
    }
    tryStartProcess(new ProcessBuilder("zenity", "--notification", "--text", summary + ": " + body));
  }

  private static void notifyMac(@Nullable String title, String message) {
    String t = title == null ? "" : truncateArg(title, 200);
    String m = truncateArg(message, MAX_NOTIFY_ARG_LEN);
    String script = "display notification \"" + escapeOsascript(m) + "\" with title \"" + escapeOsascript(t) + "\"";
    tryStartProcess(new ProcessBuilder("osascript", "-e", script));
  }

  private static void notifyWindows(@Nullable String title, String message) {
    try {
      String t = title == null ? "" : title;
      ProcessBuilder pb = new ProcessBuilder(
          "powershell.exe",
          "-NoProfile",
          "-NonInteractive",
          "-WindowStyle",
          "Hidden",
          "-Command",
          windowsToastCommand());
      pb.environment().put("FGX_TITLE_B64", Base64.getEncoder().encodeToString(t.getBytes(StandardCharsets.UTF_8)));
      pb.environment().put("FGX_BODY_B64",
          Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)));
      tryStartProcess(pb);
    } catch (Exception ignored) {
    }
  }

  private static String windowsToastCommand() {
    return "$t=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($env:FGX_TITLE_B64));"
        + "$b=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($env:FGX_BODY_B64));"
        + "$t=$t -replace '[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]',' ';"
        + "$b=$b -replace '[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]',' ';"
        + "$xe=[System.Security.SecurityElement]::Escape($t);"
        + "$be=[System.Security.SecurityElement]::Escape($b);"
        + "[void][Windows.UI.Notifications.ToastNotificationManager,Windows.UI.Notifications,ContentType=WindowsRuntime];"
        + "[void][Windows.Data.Xml.Dom.XmlDocument,Windows.Data.Xml.Dom,ContentType=WindowsRuntime];"
        + "$d=New-Object Windows.Data.Xml.Dom.XmlDocument;"
        + "$d.LoadXml(\"<toast><visual><binding template=\\\"ToastText02\\\"><text id=\\\"1\\\">$xe</text><text id=\\\"2\\\">$be</text></binding></visual></toast>\");"
        + "$n=[Windows.UI.Notifications.ToastNotification]::new($d);"
        + "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('FlixelGDX').Show($n);";
  }

  @Nullable
  private static Process tryStartWakeLockProcess(ProcessBuilder pb) {
    try {
      pb.redirectError(ProcessBuilder.Redirect.DISCARD);
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      return pb.start();
    } catch (IOException e) {
      return null;
    }
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

  private static boolean isMac() {
    return OS.contains("mac");
  }

  private static boolean isLinux() {
    return OS.contains("linux");
  }

  private static boolean isWindows() {
    return OS.contains("windows");
  }
}
