/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.TrayIcon;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.backend.host.FlixelHostIntegration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

/**
 * Desktop {@link FlixelHostIntegration}: freedesktop or Zenity notifications on Linux, {@code osascript} on macOS,
 * WinRT toasts via PowerShell on Windows, GLFW window attention, and AWT {@link SystemTray} icons.
 */
public final class FlixelLwjgl3HostIntegration implements FlixelHostIntegration {

  private static final int MAX_NOTIFY_ARG_LEN = 6000;

  private final AtomicReference<TrayIcon> trayIcon = new AtomicReference<>();

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
  public void addTrayIcon(@NotNull String internalAssetPath, @Nullable String tooltip) {
    Objects.requireNonNull(internalAssetPath, "internalAssetPath");
    if (!supportsTrayIcon()) {
      Flixel.warn("FlixelHost", "Tray icon is not supported in this desktop session (for example pure Wayland or no system tray).");
      return;
    }
    Runnable work = () -> {
      removeTrayIconOnEdt();
      try {
        String path = Flixel.ensureAssets().extractAssetPath(internalAssetPath);
        Image image = ImageIO.read(new File(path));
        if (image == null) {
          Flixel.warn("FlixelHost", "Tray icon could not be decoded: " + internalAssetPath);
          return;
        }
        Image trayImage = normalizeTrayImage(image);
        TrayIcon icon = new TrayIcon(trayImage, trayTooltipText(tooltip));
        icon.setImageAutoSize(false);
        SystemTray.getSystemTray().add(icon);
        trayIcon.set(icon);
      } catch (Exception ex) {
        Flixel.warn("FlixelHost", "Tray icon failed: " + ex.getMessage());
      }
    };
    if (EventQueue.isDispatchThread()) {
      work.run();
    } else {
      EventQueue.invokeLater(work);
    }
  }

  @Override
  public void removeTrayIcon() {
    runOnEdtAndWait(this::removeTrayIconOnEdt);
  }

  @Override
  public boolean supportsDesktopNotification() {
    return !GraphicsEnvironment.isHeadless();
  }

  @Override
  public boolean supportsTrayIcon() {
    if (GraphicsEnvironment.isHeadless() || !SystemTray.isSupported()) {
      return false;
    }
    return !isLinuxAwtTrayLikelyBroken();
  }

  private static boolean isLinuxAwtTrayLikelyBroken() {
    if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
      return false;
    }
    String sessionType = System.getenv("XDG_SESSION_TYPE");
    return sessionType != null && sessionType.equalsIgnoreCase("wayland");
  }

  private static String trayTooltipText(@Nullable String tooltip) {
    if (tooltip != null && !tooltip.isEmpty()) {
      return tooltip;
    }
    return "";
  }

  /**
   * Builds a multi-resolution tray image and normalizes pixel format so GTK-based trays (for example Cinnamon under X11)
   * can pick a size without corrupt thumbnails or stray popup shells from an unused AWT {@code PopupMenu}.
   *
   * @param raw The raw image icon to be normalized.
   */
  private static Image normalizeTrayImage(Image raw) {
    BufferedImage src = toBufferedImageArgb(raw);
    BufferedImage s16 = scaleTrayArgb(src, 16);
    BufferedImage s24 = scaleTrayArgb(src, 24);
    BufferedImage s32 = scaleTrayArgb(src, 32);
    return new BaseMultiResolutionImage(s16, s24, s32);
  }

  private static BufferedImage scaleTrayArgb(BufferedImage src, int target) {
    int w = src.getWidth();
    int h = src.getHeight();
    if (w == target && h == target) {
      return src;
    }
    BufferedImage scaled = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = scaled.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(src, 0, 0, target, target, null);
    g2.dispose();
    return scaled;
  }

  private static BufferedImage toBufferedImageArgb(Image raw) {
    if (raw instanceof BufferedImage bi) {
      int t = bi.getType();
      if (t == BufferedImage.TYPE_INT_ARGB || t == BufferedImage.TYPE_INT_ARGB_PRE) {
        return bi;
      }
      BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D gc = converted.createGraphics();
      gc.drawImage(bi, 0, 0, null);
      gc.dispose();
      return converted;
    }
    int w = Math.max(1, raw.getWidth(null));
    int h = Math.max(1, raw.getHeight(null));
    BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g0 = tmp.createGraphics();
    g0.drawImage(raw, 0, 0, null);
    g0.dispose();
    return tmp;
  }

  private void removeTrayIconOnEdt() {
    TrayIcon icon = trayIcon.getAndSet(null);
    if (icon == null) {
      return;
    }
    try {
      SystemTray.getSystemTray().remove(icon);
    } catch (IllegalArgumentException ignored) {
    }
  }

  private static void runOnEdtAndWait(Runnable work) {
    if (EventQueue.isDispatchThread()) {
      work.run();
    } else {
      try {
        EventQueue.invokeAndWait(work);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (InvocationTargetException e) {
        Flixel.warn("Flixel.host", "Host UI task failed: " + e.getCause());
      }
    }
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
