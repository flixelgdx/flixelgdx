/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.window;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;

/**
 * Desktop window controls that stay safe on every platform.
 *
 * <p>Use {@link me.stringdotjar.flixelgdx.Flixel#window} after {@link me.stringdotjar.flixelgdx.Flixel#initialize(me.stringdotjar.flixelgdx.FlixelGame)}.
 * The implementation only adjusts backdrop drawing and, on desktop with a transparent-capable framebuffer, an end-of-frame
 * alpha fix so normal gameplay is not composited through the desktop unless this mode is on.
 *
 * <p>Desktop games default to an alpha-capable framebuffer (see {@link FlixelGame#isTransparentFramebufferRequested()}).
 * Call {@link #setDesktopTransparencyActive(boolean)} any time to show the real desktop through unused pixels, or turn it back off.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.window.setDesktopTransparencyActive(true);  // Sprites over the desktop.
 * // ...
 * Flixel.window.setDesktopTransparencyActive(false); // Normal opaque letterboxing again.
 * }</pre>
 *
 * @see me.stringdotjar.flixelgdx.Flixel#window
 */
public interface FlixelWindow {

  /**
   * When {@code true}, the LWJGL3 launcher enables {@code Lwjgl3ApplicationConfiguration#setTransparentFramebuffer(boolean)}.
   * This is {@code true} by default so runtime transparency can work; set {@code false} before launch only if you need a classic
   * opaque framebuffer or hit a driver issue (then {@link #setDesktopTransparencyActive(boolean)} cannot composite with the desktop).
   *
   * @return current launch-time request for an alpha-capable default framebuffer.
   */
  default boolean isTransparentFramebufferRequested() {
    FlixelGame g = Flixel.getGame();
    return g != null && g.isTransparentFramebufferRequested();
  }

  /**
   * Turns desktop-composited transparency on or off. When {@code true}, clears and camera backdrop fills use alpha zero so unchanged
   * pixels show whatever is behind the window (when the framebuffer was created with transparency support).
   * When {@code false}, restores backdrop colors cached the first time transparency was enabled this session, or falls back to
   * opaque {@link com.badlogic.gdx.graphics.Color#BLACK} if transparency was never enabled.
   *
   * @param active {@code true} to composite with the desktop through alpha; {@code false} for a normal opaque window interior.
   */
  default void setDesktopTransparencyActive(boolean active) {
    FlixelGame g = Flixel.getGame();
    if (g != null) {
      g.applyBackdropForDesktopTransparency(active);
    }
  }

  /**
   * @return last value applied by {@link #setDesktopTransparencyActive(boolean)} for this game session.
   */
  default boolean isDesktopTransparencyActive() {
    FlixelGame g = Flixel.getGame();
    return g != null && g.isDesktopTransparencyActive();
  }

  /**
   * Same as {@linkplain #setDesktopTransparencyActive(boolean) setDesktopTransparencyActive(true)}.
   */
  default void applyTransparentWorldBackdrop() {
    setDesktopTransparencyActive(true);
  }

  /**
   * Same as {@linkplain #setDesktopTransparencyActive(boolean) setDesktopTransparencyActive(false)}.
   */
  default void clearDesktopTransparency() {
    setDesktopTransparencyActive(false);
  }

  /**
   * Sets whole-window opacity where the backend supports it (GLFW 3.4+, LWJGL3 desktop).
   *
   * @param opacity Opacity in {@code [0, 1]}; non-finite values are ignored.
   */
  void setWindowOpacity(float opacity);

  /**
   * @return {@code true} if {@link #setWindowOpacity(float)} can affect the window on this session.
   */
  boolean supportsWindowOpacity();

  /**
   * Sets whether the window uses native title bar and border decorations, when supported.
   *
   * @param decorated {@code false} for a borderless window.
   */
  default void setWindowDecorated(boolean decorated) {}

  /**
   * @return {@code true} if {@link #setWindowDecorated(boolean)} is supported on this session.
   */
  default boolean supportsSetWindowDecorated() {
    return false;
  }

  /**
   * Window X position in screen coordinates, when supported.
   *
   * @return horizontal position, or {@code 0} when unknown.
   */
  default int getX() {
    return 0;
  }

  /**
   * Window Y position in screen coordinates, when supported.
   *
   * @return vertical position, or {@code 0} when unknown.
   */
  default int getY() {
    return 0;
  }

  /**
   * Sets the window's X position in screen coordinates, when supported.
   *
   * @param x Target X in screen coordinates.
   */
  default void setX(int x) {}

  /**
   * Sets the window's Y position in screen coordinates, when supported.
   *
   * @param y Target Y in screen coordinates.
   */
  default void setY(int y) {}

  /**
   * Moves the window so its client area origin is placed at the given screen coordinates, when supported.
   *
   * @param x Target X in screen coordinates.
   * @param y Target Y in screen coordinates.
   */
  default void setPosition(int x, int y) {}

  /**
   * Moves the window horizontally by a delta in screen pixels.
   *
   * @param deltaX Pixels to add to the current X position (negative values move left).
   */
  default void changeX(int deltaX) {}

  /**
   * Moves the window vertically by a delta in screen pixels.
   *
   * @param deltaY Pixels to add to the current Y position (negative values move up on backends that use upper-left origin).
   */
  default void changeY(int deltaY) {}

  /**
   * Asks the OS to focus this game window (desktop only where GLFW allows it).
   *
   * <p><b>CAUTION:</b> Pulling focus away from another application is disruptive. It's advised you warn players ahead of
   * time on your store page, in a first-run dialog, or in an in-game settings label before calling this from normal gameplay.
   */
  default void bringWindowToForeground() {}

  /**
   * @return {@code true} if {@link #bringWindowToForeground()} can run on this session.
   */
  default boolean supportsBringWindowToForeground() {
    return false;
  }

  /**
   * When {@code true}, requests a floating (often "always on top") window using {@code GLFW_FLOATING} where the backend supports it.
   *
   * <p><b>CAUTION:</b> Keeping the game above everything else can hide important system UI or other apps. Disclose
   * this in plain language before you enable it, and ideally expose a user-visible toggle.
   *
   * @param floating {@code true} to keep the window above normal stacking, {@code false} for default stacking.
   */
  default void setWindowFloating(boolean floating) {}

  /**
   * @return {@code true} if {@link #setWindowFloating(boolean)} may take effect on this session.
   */
  default boolean supportsSetWindowFloating() {
    return false;
  }

  /**
   * When {@code true}, the GLFW close event is absorbed so the window does not exit until you stop absorbing or call
   * {@code Gdx.app.exit()} yourself.
   *
   * <p><b>CAUTION:</b> Players expect the window close control to quit. If you absorb close requests, you must
   * explain that ahead of time (splash text, settings, store description) and always provide another obvious way to exit.
   *
   * @param absorb {@code true} to cancel the default close handling from the windowing system.
   */
  default void setAbsorbCloseRequests(boolean absorb) {}

  /**
   * @return {@code true} if {@link #setAbsorbCloseRequests(boolean)} is wired for this session.
   */
  default boolean supportsAbsorbCloseRequests() {
    return false;
  }

  /**
   * @return {@code true} while close absorption is enabled and the listener chain is active.
   *   When {@link #supportsAbsorbCloseRequests()} is {@code false}, this is always {@code false}.
   */
  default boolean isAbsorbCloseRequestsEnabled() {
    return false;
  }

  /**
   * @return {@code true} when the window is currently floating (always on top), if the backend can query it.
   *   When unsupported, returns {@code false}.
   */
  default boolean isWindowFloating() {
    return false;
  }

  /**
   * @return {@code true} when the window has native decorations (title bar and border), if the backend can query it.
   *   When unsupported, returns {@code true} so game code treats the common case as decorated.
   */
  default boolean isWindowDecorated() {
    return true;
  }
}
