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
import org.flixelgdx.FlixelGame;
import org.flixelgdx.functional.FlixelShakeable;

/**
 * Desktop window controls that stay safe on every platform.
 *
 * <p>Use {@link org.flixelgdx.Flixel#window Flixel.window} after {@link org.flixelgdx.Flixel#initialize(org.flixelgdx.FlixelGame) Flixel.initialize(FlixelGame)}.
 * The implementation only adjusts backdrop drawing and, on desktop with a transparent-capable framebuffer, an end-of-frame
 * alpha fix so normal gameplay is not composited through the desktop unless this mode is on.
 *
 * <p>Desktop games default to an alpha-capable framebuffer (see {@link FlixelGame#isTransparentFramebufferRequested()}).
 * Call {@link #setTransparencyActive(boolean)} any time to show the real desktop through unused pixels, or turn it back off.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.window.setDesktopTransparencyActive(true);  // Sprites over the desktop.
 * // ...
 * Flixel.window.setDesktopTransparencyActive(false); // Normal opaque letterboxing again.
 * }</pre>
 *
 * <p>This interface extends {@link org.flixelgdx.functional.FlixelShakeable FlixelShakeable} so you can pass {@code Flixel.window} to
 * {@link org.flixelgdx.tween.FlixelTween#shake FlixelTween.shake} when you want the OS window itself to jitter.
 *
 * @see org.flixelgdx.Flixel#window
 */
public interface FlixelWindow extends FlixelShakeable {

  /** {@inheritDoc} */
  @Override
  default float getShakeX() {
    return (float) getX();
  }

  /** {@inheritDoc} */
  @Override
  default float getShakeY() {
    return (float) getY();
  }

  /** {@inheritDoc} */
  @Override
  default void setShake(float x, float y) {
    setPosition(Math.round(x), Math.round(y));
  }

  /** {@inheritDoc} */
  @Override
  default float getShakeWidth() {
    return shakeFractionWidthFromGraphics();
  }

  /** {@inheritDoc} */
  @Override
  default float getShakeHeight() {
    return shakeFractionHeightFromGraphics();
  }

  /**
   * When {@code true}, the LWJGL3 launcher enables {@code Lwjgl3ApplicationConfiguration#setTransparentFramebuffer(boolean)}.
   * This is {@code true} by default so runtime transparency can work; set {@code false} before launch only if you need a classic
   * opaque framebuffer or hit a driver issue (then {@link #setTransparencyActive(boolean)} cannot composite with the desktop).
   *
   * @return current launch-time request for an alpha-capable default framebuffer.
   */
  default boolean isTransparentFramebufferRequested() {
    FlixelGame g = Flixel.getGame();
    return g != null && g.isTransparentFramebufferRequested();
  }

  /**
   * Turns desktop-composited transparency on or off.
   *
   * <p>When {@code true}, clears and camera backdrop fills use alpha zero so unchanged
   * pixels show whatever is behind the window (when the framebuffer was created with transparency support).
   * When {@code false}, restores backdrop colors cached the first time transparency was enabled this session,
   * or falls back to opaque {@link com.badlogic.gdx.graphics.Color#BLACK} if transparency was never enabled.
   *
   * @param active {@code true} to composite with the desktop through alpha; {@code false} for a normal opaque window interior.
   */
  default void setTransparencyActive(boolean active) {
    FlixelGame g = Flixel.getGame();
    if (g != null) {
      g.applyBackdropForDesktopTransparency(active);
    }
  }

  /**
   * @return last value applied by {@link #setTransparencyActive(boolean)} for this game session.
   */
  default boolean isTransparencyActive() {
    FlixelGame g = Flixel.getGame();
    return g != null && g.isTransparencyActive();
  }

  /**
   * @return The current opacity level of the game's window.
   */
  default float getOpacity() {
    return 1;
  }

  /**
   * Sets whole-window opacity where the backend supports it (GLFW 3.4+, LWJGL3 desktop).
   *
   * @param opacity Opacity in {@code [0, 1]}; non-finite values are ignored.
   */
  default void setOpacity(float opacity) {}

  /**
   * @return {@code true} if {@link #setOpacity(float)} can affect the window on this session.
   */
  boolean supportsWindowOpacity();

  /**
   * Sets whether the window uses native title bar and border decorations, when supported.
   *
   * @param decorated {@code false} for a borderless window.
   */
  default void setDecorated(boolean decorated) {}

  /**
   * @return {@code true} if {@link #setDecorated(boolean)} is supported on this session.
   */
  default boolean supportsDecorated() {
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
  default void bringToForeground() {}

  /**
   * @return {@code true} if {@link #bringToForeground()} can run on this session.
   */
  default boolean supportsBringToForeground() {
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
  default void setFloating(boolean floating) {}

  /**
   * @return {@code true} if {@link #setFloating(boolean)} may take effect on this session.
   */
  default boolean supportsFloating() {
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
  default boolean isAbsorbCloseRequests() {
    return false;
  }

  /**
   * @return {@code true} when the window is currently floating (always on top), if the backend can query it.
   *   When unsupported, returns {@code false}.
   */
  default boolean isFloating() {
    return false;
  }

  /**
   * @return {@code true} when the window has native decorations (title bar and border), if the backend can query it.
   *   When unsupported, returns {@code true} so game code treats the common case as decorated.
   */
  default boolean isDecorated() {
    return true;
  }
}
