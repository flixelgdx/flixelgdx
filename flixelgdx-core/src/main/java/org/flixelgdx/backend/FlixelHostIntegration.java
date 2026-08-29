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

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Host platform integration for notifications, display management, and clipboard access.
 *
 * <h2>Web notification permission</h2>
 *
 * <p>Browsers require explicit user permission before showing notifications. On the web backend,
 * {@link #supportsNotifications()} returns {@code false} until permission has been granted.
 * Call {@link #requestNotificationPermission()} early, ideally during a loading screen or in
 * response to a user gesture before sending any notifications. On desktop, permission is implicit
 * and {@link #requestNotificationPermission()} is a no-op.
 *
 * <h2>Clipboard paste callbacks</h2>
 *
 * <p>Paste operations are asynchronous. Register a handler on {@link #onTextPasted()} before
 * calling {@link #pasteFromClipboard()}. The signal fires once the platform has retrieved the
 * data. Handlers may not be called on the GL thread; because of this, wrap any calls
 * with {@link FlixelGraphicsManager#queueMainThread(Runnable) Flixel.graphics.queueMainThread()}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Send a notification and flash the game's icon in the task bar.
 * Flixel.host.sendNotification("Ready", "Your level finished loading.");
 * Flixel.host.requestAttention();
 *
 * // Clipboard (copy).
 * Flixel.host.copyToClipboard(saveCode);
 *
 * // Clipboard (paste).
 * Flixel.host.onTextPasted().add(text -> {
 *   if (text != null) {
 *     Flixel.graphics.queueMainThread(() -> saveCodeField.setText(text));
 *   }
 * });
 * Flixel.host.pasteFromClipboard();
 * }</pre>
 *
 * @see Flixel#host
 */
public interface FlixelHostIntegration {

  /**
   * Prompts the user to grant notification permission for this origin.
   *
   * <p>On the web backend this triggers the browser permission dialog. It must be called in
   * response to a user gesture (button press, key press, etc.) or the browser will silently
   * ignore it. Once granted, {@link #supportsNotifications()} returns {@code true} and
   * subsequent {@link #sendNotification(String, String)} calls will display toasts.
   *
   * <p>On desktop, permission is implicit and this method does nothing.
   */
  default void requestNotificationPermission() {}

  /**
   * Prompts the user to grant permission for obtaining monitor information for this origin.
   *
   * <p>On the web backend this triggers the browser permission dialog. It must be called in
   * response to a user gesture (button press, key press, etc.) or the browser will silently
   * ignore it. If granted, {@link #getMonitors()} returns a real list of all the monitors that
   * are connected to the user's computer; otherwise, it returns empty.
   *
   * <p>On desktop, permission is implicit and this method does nothing.
   */
  default void requestMonitorPermission() {}

  /**
   * Asks the window manager to highlight this app (taskbar entry flash, dock bounce, and similar).
   *
   * <p>On the web backend, this flashes the browser tab title while the tab is in the background.
   */
  default void requestAttention() {}

  /**
   * Prevents the display from sleeping while the game is running.
   *
   * <p>Pass {@code true} to acquire a wake lock and {@code false} to release it. On desktop this
   * uses platform-specific inhibit commands ({@code caffeinate} on macOS,
   * {@code systemd-inhibit} or {@code xdg-screensaver} on Linux). On the web backend this uses the
   * Screen Wake Lock API. Has no effect on platforms where {@link #supportsWakeLock()} returns
   * {@code false}.
   *
   * @param awake {@code true} to keep the screen on, {@code false} to release the lock.
   */
  default void keepScreenAwake(boolean awake) {}

  /**
   * Sets a message shown to the user when they attempt to close the game.
   *
   * <p>On the web backend this hooks {@code window.beforeunload}. Pass {@code null} to remove the
   * guard. On desktop this is a no-op.
   *
   * @param message The warning message, or {@code null} to clear the exit guard.
   */
  default void setExitConfirmation(@Nullable String message) {}

  /**
   * Shows a non-blocking desktop notification using the platform provider (Action Center on
   * Windows, Notification Center on macOS, D-Bus or libnotify on Linux, browser toasts on web).
   *
   * <p>On the web backend, notifications require prior permission. Call
   * {@link #requestNotificationPermission()} first and confirm {@link #supportsNotifications()}
   * returns {@code true} before calling this method.
   *
   * @param title Short title, or {@code null} to use a blank title when the OS allows it.
   * @param message Body text; must not be {@code null}.
   */
  default void sendNotification(@Nullable String title, @NotNull String message) {}

  /**
   * Copies {@code text} to the system clipboard.
   *
   * <p>Has no effect on platforms where {@link #supportsClipboard()} returns {@code false}.
   *
   * @param text The text to copy; must not be {@code null}.
   */
  default void copyToClipboard(@NotNull String text) {}

  /**
   * Requests a text read from the system clipboard.
   *
   * <p>The result is delivered asynchronously via {@link #onTextPasted()}. Register a handler
   * on that signal before calling this method. If the clipboard is empty or does not contain
   * text, the signal is not dispatched.
   *
   * <p>Has no effect on platforms where {@link #supportsClipboard()} returns {@code false}.
   */
  default void pasteFromClipboard() {}

  /**
   * @return {@code true} if {@link #sendNotification(String, String)} is expected to do useful
   *     work on this platform session. On the web backend, returns {@code true} only after
   *     {@link #requestNotificationPermission()} has been granted by the user.
   */
  default boolean supportsNotifications() {
    return false;
  }

  /**
   * @return {@code true} if {@link #keepScreenAwake(boolean)} is supported on this platform.
   */
  default boolean supportsWakeLock() {
    return false;
  }

  /**
   * @return {@code true} if text clipboard operations ({@link #copyToClipboard(String)} and
   *     {@link #pasteFromClipboard()}) are supported on this platform.
   */
  default boolean supportsClipboard() {
    return false;
  }

  /**
   * @return {@code true} if monitor information is supported or allowed on this platform.
   */
  default boolean supportsMonitors() {
    return false;
  }

  /**
   * Signal dispatched when {@link #pasteFromClipboard()} resolves with text content.
   *
   * <p>The dispatched value is the pasted text. Handlers may be called off the game thread.
   * Synchronize access or post to the main thread via
   * {@link FlixelGraphicsManager#queueMainThread(Runnable) Flixel.graphics.queueMainThread(() -> {...})}
   * before modifying shared game state.
   *
   * @return The signal; never {@code null}.
   */
  @NotNull
  FlixelSignal<String> onTextPasted();

  /**
   * Lists the physical monitors attached to the machine.
   *
   * <p>This is mainly a desktop feature: use it to build a "which screen" setting or to place a
   * fullscreen window on a chosen display. Each returned {@link FlixelMonitor} also reports its own
   * size and position, so this is where you obtain monitor dimensions too. On platforms without a
   * real monitor list, return an empty array.
   *
   * <p>Implementations should return a cached array rather than building a new one per call, so
   * reading this every frame does not allocate.
   *
   * @return The attached monitors, possibly empty; never {@code null}.
   */
  @NotNull
  FlixelList<FlixelMonitor> getMonitors();

  /**
   * Returns the primary monitor, where the OS usually places new windows and system UI.
   *
   * <p>Platforms that cannot report a monitor return {@link FlixelNoopMonitor#INSTANCE} rather than
   * {@code null}, so callers never have to null-check.
   *
   * @return The primary monitor; never {@code null}.
   */
  @NotNull
  default FlixelMonitor getPrimaryMonitor() {
    return FlixelNoopMonitor.INSTANCE;
  }

  /**
   * Returns the platform this game is running on.
   *
   * <p>Compare against the {@link FlixelPlatform} constants with {@code ==}, for example
   * {@code Flixel.host.getPlatform() == FlixelPlatform.Desktop}. Defaults to
   * {@link FlixelPlatform#Unknown} until a host integration is installed.
   *
   * @return The current platform; never {@code null}.
   */
  @NotNull
  default FlixelPlatform getPlatform() {
    return FlixelPlatform.Unknown;
  }
}
