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
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.tween.FlixelTween;

/**
 * The game's window, browser tab, or mobile activity, with controls that stay safe on every platform.
 *
 * <p>This is the single surface for talking to whatever hosts the game on screen: the title,
 * the window size, fullscreen, and closing all live here. Where a control has no meaning on a
 * platform (for example, moving a browser tab), it simply does nothing, so the same code is safe
 * everywhere. Anything about the drawing surface itself (frame rate, vertical sync, display modes,
 * pixel density) lives on {@link org.flixelgdx.graphics.FlixelGraphicsManager Flixel.graphics}
 * instead.
 *
 * <p>Use {@link Flixel#window} after {@link Flixel#start(FlixelGame, FlixelGameRunner)}. The implementation only
 * adjusts backdrop drawing and, on desktop with a transparent-capable framebuffer, an end-of-frame
 * alpha fix so normal gameplay is not composited through the desktop unless this mode is on.
 *
 * <p>Example:
 * <pre>{@code
 * // Change the title of the window, tab or task label.
 * Flixel.window.setTitle("New Title");
 * // Have sprites render over the desktop.
 * Flixel.window.setTransparencyActive(true);
 * // Disable resizing the window.
 * Flixel.window.setResizable(false);
 * // Close the game.
 * Flixel.window.close();
 * }</pre>
 *
 * <p>This interface extends {@link FlixelShakeable} so you can pass {@link Flixel#window} to
 * {@link FlixelTween#shake} when you want the OS window itself to jitter.
 *
 * @see Flixel#window
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
   * Returns whether an alpha-capable (transparent) framebuffer was requested at launch via
   * {@link FlixelGame.Config.Builder#transparentFramebuffer(boolean)}.
   *
   * <p>When {@code true}, the window was created with compositor support, so
   * {@link #setTransparencyActive(boolean)} can blend the game with the desktop. When
   * {@code false}, transparency has no effect because the back buffer has no alpha channel.
   *
   * @return {@code true} when an alpha-capable framebuffer was requested in the game config.
   */
  default boolean isTransparentFramebufferRequested() {
    return Flixel.game.isTransparentFramebufferRequested();
  }

  /**
   * Turns desktop-composited transparency on or off.
   *
   * <p>When {@code true}, clears and camera backdrop fills use alpha zero so unchanged
   * pixels show whatever is behind the window (when the framebuffer was created with transparency support).
   * When {@code false}, restores backdrop colors cached the first time transparency was enabled this session,
   * or falls back to opaque black if transparency was never enabled.
   *
   * @param active {@code true} to composite with the desktop through alpha; {@code false} for a normal opaque window interior.
   */
  default void setTransparencyActive(boolean active) {
    Flixel.game.applyBackdropForDesktopTransparency(active);
  }

  /**
   * @return last value applied by {@link #setTransparencyActive(boolean)} for this game session.
   */
  default boolean isTransparencyActive() {
    return Flixel.game != null && Flixel.game.isTransparencyActive();
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
  default boolean supportsOpacity() {
    return false;
  }

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
   * When {@code true}, the close event is absorbed so the window does not exit until you stop absorbing or call
   * {@link #close()} (or {@link Flixel#exit()}) yourself.
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

  /**
   * Returns the current window title.
   *
   * <p>On desktop this is the text in the title bar, on web it is the browser tab title
   * ({@code document.title}), and on mobile it is the task or recents label where the platform
   * exposes one.
   *
   * @return The current title, or an empty string when unknown; never {@code null}.
   */
  default String getTitle() {
    return "";
  }

  /**
   * Sets the window title (title bar on desktop, browser tab on web, task label on mobile).
   *
   * @param title The new title; ignored when {@code null}.
   */
  default void setTitle(String title) {}

  /**
   * Returns the window's width in logical screen coordinates.
   *
   * <p><b>Do not confuse this with {@link #getBackBufferWidth()}.</b> This is the logical size of
   * the window as the operating system reports it, which is what you use for placing and sizing the
   * window. On a high-DPI display (Retina and similar), one logical unit covers several physical
   * pixels, so this value is smaller than the back buffer width. For anything that touches actual
   * pixels on the GPU, use {@link #getBackBufferWidth()} instead.
   *
   * @return Logical window width, or {@code 0} when unknown.
   */
  default int getWidth() {
    return 0;
  }

  /**
   * Returns the window's height in logical screen coordinates.
   *
   * <p><b>Do not confuse this with {@link #getBackBufferHeight()}.</b> See {@link #getWidth()} for
   * the full explanation of logical size versus back buffer size.
   *
   * @return Logical window height, or {@code 0} when unknown.
   */
  default int getHeight() {
    return 0;
  }

  /**
   * Returns the drawing surface's width in real physical pixels.
   *
   * <p><b>This is not the same as {@link #getWidth()}.</b> The back buffer is the actual grid of
   * pixels the GPU renders into. On a high-DPI display it is larger than the logical window width
   * (often by 2x). Use this value for framebuffer math, viewports, and anything measured in real
   * pixels; use {@link #getWidth()} for logical window sizing and positioning.
   *
   * @return Back buffer width in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferWidth() {
    return 0;
  }

  /**
   * Returns the drawing surface's height in real physical pixels.
   *
   * <p><b>This is not the same as {@link #getHeight()}.</b> See {@link #getBackBufferWidth()} for
   * the full explanation of back buffer pixels versus logical size.
   *
   * @return Back buffer height in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferHeight() {
    return 0;
  }

  /**
   * Resizes the window's client area, in logical screen coordinates, when supported.
   *
   * @param width Target logical width.
   * @param height Target logical height.
   */
  default void setSize(int width, int height) {}

  /**
   * @return {@code true} while the game is presented fullscreen (or in the browser's fullscreen
   *     state on web); {@code false} for a normal windowed presentation.
   */
  default boolean isFullscreen() {
    return false;
  }

  /**
   * Switches to fullscreen at the given display mode, when supported.
   *
   * <p>Obtain a mode from
   * {@link org.flixelgdx.graphics.FlixelGraphicsManager#getDisplayModes() Flixel.graphics.getDisplayModes()}.
   * On web this requests the browser's fullscreen state; on mobile it toggles immersive mode. Return
   * to a window with {@link #setWindowed(int, int)}.
   *
   * @param mode The display mode to use; ignored when {@code null} or unsupported.
   */
  default void setFullscreen(FlixelDisplayMode mode) {}

  /**
   * Returns to a normal window at the given logical size, leaving fullscreen if it was active.
   *
   * @param width Target logical width.
   * @param height Target logical height.
   */
  default void setWindowed(int width, int height) {}

  /**
   * @return {@code true} if {@link #setFullscreen(FlixelDisplayMode)} can take effect on this session.
   */
  default boolean supportsFullscreen() {
    return false;
  }

  /**
   * Sets whether the user may resize the window by dragging its edges, when supported.
   *
   * @param resizable {@code true} to allow user resizing, {@code false} to lock the size.
   */
  default void setResizable(boolean resizable) {}

  /**
   * @return {@code true} when the window may currently be resized by the user, if the backend can
   *     report it; {@code false} otherwise.
   */
  default boolean isResizable() {
    return false;
  }

  /**
   * @return {@code true} when the window (or browser tab) currently has input focus.
   */
  default boolean isFocused() {
    return true;
  }

  /**
   * Turns continuous rendering on or off.
   *
   * <p>When continuous rendering is off, the loop only redraws when {@link #requestRendering()}
   * is called, which the framework uses to idle the game while the window is unfocused. Backends
   * that always render every frame may ignore this.
   *
   * @param continuous {@code true} to render every frame, {@code false} to render on request only.
   */
  default void setContinuousRendering(boolean continuous) {}

  /**
   * Requests that at least one more frame be rendered while continuous rendering is off. Has no
   * effect when continuous rendering is on.
   */
  default void requestRendering() {}

  /**
   * Requests that the game's window closes, ending the game.
   *
   * <p>This is the same request the user makes by clicking the window's close control. If close
   * absorption is active (see {@link #setAbsorbCloseRequests(boolean)}), it still applies. On web
   * and mobile, where the host owns the lifecycle, this may do nothing.
   *
   * <p>Prefer {@link Flixel#exit()} from game code, which forwards here.
   */
  default void close() {}
}
