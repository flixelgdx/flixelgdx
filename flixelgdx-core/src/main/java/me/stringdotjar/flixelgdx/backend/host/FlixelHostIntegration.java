/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Host operating system integration, which allows desktop notifications, taskbar attention, and tray icons.
 *
 * <p>This is separate from {@link me.stringdotjar.flixelgdx.backend.alert.FlixelAlerter}, which shows blocking
 * dialog popups. Use {@link me.stringdotjar.flixelgdx.Flixel#host} from game code.
 *
 * <p>Desktop LWJGL3 ships a full implementation (freedesktop {@code notify-send} on Linux rather than AWT for toasts,
 * AWT {@code java.awt.SystemTray} for tray icons, GLFW plus AWT {@code java.awt.Taskbar} for attention where available).
 * Mobile and web builds keep the safe no-op implementation.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.host.sendDesktopNotification("Ready", "Your level finished loading.");
 * Flixel.host.requestUserAttention();
 * Flixel.host.addTrayIcon("ui/tray.png", "My Game");
 * }</pre>
 *
 * @see me.stringdotjar.flixelgdx.Flixel#host
 */
public interface FlixelHostIntegration {

  /**
   * Shows a non-blocking desktop notification using the platform provider (Action Center on Windows, Notification Center on macOS,
   * D-Bus or libnotify on Linux).
   *
   * @param title Short title, or {@code null} to use a blank title when the OS allows it.
   * @param message Body text; must not be {@code null}.
   */
  void sendDesktopNotification(@Nullable String title, @NotNull String message);

  /**
   * Asks the window manager to highlight this app (taskbar entry flash, dock bounce, and similar).
   */
  void requestUserAttention();

  /**
   * Adds a tray icon using an internal asset path resolved like other game assets.
   *
   * @param internalAssetPath Path passed to {@link me.stringdotjar.flixelgdx.asset.FlixelAssetManager#extractAssetPath(String)}.
   * @param tooltip Optional tooltip; may be {@code null}.
   */
  void addTrayIcon(@NotNull String internalAssetPath, @Nullable String tooltip);

  /** Removes the tray icon installed by {@link #addTrayIcon(String, String)}, if any. */
  void removeTrayIcon();

  /**
   * @return {@code true} if {@link #sendDesktopNotification(String, String)} is expected to do useful work on this platform session.
   */
  boolean supportsDesktopNotification();

  /**
   * @return {@code true} if {@link #addTrayIcon(String, String)} is expected to work on this platform session.
   */
  boolean supportsTrayIcon();
}
