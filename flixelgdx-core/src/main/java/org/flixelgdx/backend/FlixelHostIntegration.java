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
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.util.signal.FlixelSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Host platform integration for notifications, display management, and clipboard access.
 *
 * <p>This is separate from {@link FlixelAlerter}, which shows blocking dialog popups.
 * Use {@link Flixel#host Flixel.host} from game code.
 *
 * <p>Desktop LWJGL3 ships a full implementation: freedesktop {@code notify-send} on Linux,
 * {@code osascript} on macOS, WinRT toasts via PowerShell on Windows, GLFW window attention,
 * AWT clipboard, and platform-specific screen wake lock. Web (TeaVM) implements the Browser
 * Notification API, tab-title attention, the Screen Wake Lock API, the {@code beforeunload}
 * exit guard, and the Clipboard API. Mobile builds use the safe no-op implementation.
 *
 * <h2>Web notification permission</h2>
 *
 * <p>Browsers require explicit user permission before showing notifications. On the web backend,
 * {@link #supportsDesktopNotification()} returns {@code false} until permission has been granted.
 * Call {@link #requestNotificationPermission()} early, ideally during a loading screen or in
 * response to a user gesture before sending any notifications. On desktop, permission is implicit
 * and {@link #requestNotificationPermission()} is a no-op.
 *
 * <h2>Clipboard paste callbacks</h2>
 *
 * <p>Paste operations are asynchronous. Register handlers on {@link #onTextPasted()} or
 * {@link #onImagePasted()} before calling {@link #pasteFromClipboard()} or
 * {@link #pasteImageFromClipboard()}. The signal fires once the platform has retrieved the data.
 * Handlers may not be called on the GL thread; because of this, wrap any libGDX calls with
 * {@code Gdx.app.postRunnable(...)}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // Notifications
 * Flixel.host.sendNotification("Ready", "Your level finished loading.");
 * Flixel.host.requestAttention();
 *
 * // Clipboard (copy).
 * Flixel.host.copyToClipboard(saveCode);
 *
 * // Clipboard (paste).
 * Flixel.host.onTextPasted().add(text -> {
 *   if (text != null) {
 *     Gdx.app.postRunnable(() -> saveCodeField.setText(text));
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
   * ignore it. Once granted, {@link #supportsDesktopNotification()} returns {@code true} and
   * subsequent {@link #sendNotification(String, String)} calls will display toasts.
   *
   * <p>On desktop, permission is implicit and this method does nothing.
   */
  void requestNotificationPermission();

  /**
   * Asks the window manager to highlight this app (taskbar entry flash, dock bounce, and similar).
   *
   * <p>On the web backend, this flashes the browser tab title while the tab is in the background.
   */
  void requestAttention();

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
  void keepScreenAwake(boolean awake);

  /**
   * Sets a message shown to the user when they attempt to close or navigate away from the game.
   *
   * <p>On the web backend this hooks {@code window.beforeunload}. Pass {@code null} to remove the
   * guard. On desktop this is a no-op.
   *
   * @param message The warning message, or {@code null} to clear the exit guard.
   */
  void setExitConfirmation(@Nullable String message);

  /**
   * Shows a non-blocking desktop notification using the platform provider (Action Center on
   * Windows, Notification Center on macOS, D-Bus or libnotify on Linux, browser toasts on web).
   *
   * <p>On the web backend, notifications require prior permission. Call
   * {@link #requestNotificationPermission()} first and confirm {@link #supportsDesktopNotification()}
   * returns {@code true} before calling this method.
   *
   * @param title Short title, or {@code null} to use a blank title when the OS allows it.
   * @param message Body text; must not be {@code null}.
   */
  void sendNotification(@Nullable String title, @NotNull String message);

  /**
   * Copies {@code text} to the system clipboard.
   *
   * <p>Has no effect on platforms where {@link #supportsClipboard()} returns {@code false}.
   *
   * @param text The text to copy; must not be {@code null}.
   */
  void copyToClipboard(@NotNull String text);

  /**
   * Copies {@code graphic} to the system clipboard as a PNG image.
   *
   * <p>Has no effect on platforms where {@link #supportsImageClipboard()} returns {@code false}.
   *
   * @param graphic The graphic to copy; must not be {@code null}.
   */
  void copyImageToClipboard(@NotNull FlixelGraphic graphic);

  /**
   * Requests a text read from the system clipboard.
   *
   * <p>The result is delivered asynchronously via {@link #onTextPasted()}. Register a handler
   * on that signal before calling this method. If the clipboard is empty or does not contain
   * text, the signal is not dispatched.
   *
   * <p>Has no effect on platforms where {@link #supportsClipboard()} returns {@code false}.
   */
  void pasteFromClipboard();

  /**
   * Requests an image read from the system clipboard.
   *
   * <p>The result is delivered asynchronously via {@link #onImagePasted()}. Register a handler
   * on that signal before calling this method. The dispatched {@link FlixelGraphic} is an owned
   * graphic with a reference count of zero - call {@link FlixelGraphic#retain()} if you intend
   * to keep it, and {@link FlixelGraphic#release()} when done. If the clipboard does not contain
   * an image, the signal is not dispatched.
   *
   * <p>Has no effect on platforms where {@link #supportsImageClipboard()} returns {@code false}.
   */
  void pasteImageFromClipboard();

  /**
   * @return {@code true} if {@link #sendNotification(String, String)} is expected to do useful
   *     work on this platform session. On the web backend, returns {@code true} only after
   *     {@link #requestNotificationPermission()} has been granted by the user.
   */
  boolean supportsDesktopNotification();

  /**
   * @return {@code true} if {@link #keepScreenAwake(boolean)} is supported on this platform.
   */
  boolean supportsWakeLock();

  /**
   * @return {@code true} if text clipboard operations ({@link #copyToClipboard(String)} and
   *     {@link #pasteFromClipboard()}) are supported on this platform.
   */
  boolean supportsClipboard();

  /**
   * @return {@code true} if image clipboard operations ({@link #copyImageToClipboard(FlixelGraphic)}
   *     and {@link #pasteImageFromClipboard()}) are supported on this platform.
   */
  boolean supportsImageClipboard();

  /**
   * Signal dispatched when {@link #pasteFromClipboard()} resolves with text content.
   *
   * <p>The dispatched value is the pasted text. Handlers may be called off the GL thread.
   * Wrap any libGDX calls with {@code Gdx.app.postRunnable(...)}.
   *
   * @return The signal; never {@code null}.
   */
  @NotNull
  FlixelSignal<String> onTextPasted();

  /**
   * Signal dispatched when {@link #pasteImageFromClipboard()} resolves with image content.
   *
   * <p>The dispatched {@link FlixelGraphic} is an owned graphic at reference count zero. Call
   * {@link FlixelGraphic#retain()} to keep it alive past the handler. Handlers may be called
   * off the GL thread. Wrap any libGDX calls with {@code Gdx.app.postRunnable(...)}.
   *
   * @return The signal; never {@code null}.
   */
  @NotNull
  FlixelSignal<FlixelGraphic> onImagePasted();
}
