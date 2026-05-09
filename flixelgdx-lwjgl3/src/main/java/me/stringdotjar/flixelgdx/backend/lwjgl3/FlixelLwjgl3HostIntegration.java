/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Taskbar;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import me.stringdotjar.flixelgdx.backend.host.FlixelHostIntegration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

/**
 * Desktop {@link FlixelHostIntegration}: freedesktop or Zenity notifications on Linux, {@code osascript} on macOS,
 * WinRT toasts via PowerShell on Windows, GLFW window attention, and AWT {@link Taskbar} attention where available.
 */
public final class FlixelLwjgl3HostIntegration implements FlixelHostIntegration {

  private static final int MAX_NOTIFY_ARG_LEN = 6000;

  @Override
  public void sendDesktopNotification(@Nullable String title, @NotNull String message) {
    Objects.requireNonNull(message, "message");
    if (!supportsDesktopNotification()) {
      return;
    }
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("linux")) {
      notifyLinux(title, message);
    } else if (os.contains("mac")) {
      notifyMac(title, message);
    } else if (os.contains("windows")) {
      notifyWindows(title, message);
    }
  }

  @Override
  public void requestUserAttention() {
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
  public boolean supportsDesktopNotification() {
    return !GraphicsEnvironment.isHeadless();
  }

  private static void notifyLinux(@Nullable String title, String message) {
    String summary = truncateArg(title == null || title.isEmpty() ? "\u00a0" : title, MAX_NOTIFY_ARG_LEN);
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
      pb.environment().put("FGX_BODY_B64", Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)));
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
}
