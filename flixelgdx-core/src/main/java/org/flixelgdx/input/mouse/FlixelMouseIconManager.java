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
package org.flixelgdx.input.mouse;

import org.flixelgdx.graphics.FlixelGraphic;
import org.jetbrains.annotations.NotNull;

/**
 * Platform native cursor styling, exposed through {@link FlixelMouseInputManager#icons}.
 *
 * <p>Games that need the default Flixel cursor behavior never have to touch this type. When you
 * do need OS-level feedback (text field, busy state, resize handles), call
 * {@link #setCursor(FlixelMouseCursor)} during UI transitions and
 * {@link #resetCursor()} when you are done.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.mouse.icons.setCursor(FlixelMouseCursor.IBEAM);
 * Flixel.mouse.icons.resetCursor();
 * }</pre>
 *
 * @see FlixelMouseInputManager#icons
 */
public interface FlixelMouseIconManager {

  /**
   * Returns the cursor that was last set via {@link #setCursor(FlixelMouseCursor)}, or
   * {@link FlixelMouseCursor#ARROW} after construction or {@link #resetCursor()}.
   *
   * @return The active cursor, never {@code null}.
   */
  @NotNull
  default FlixelMouseCursor getCursor() {
    return FlixelMouseCursor.ARROW;
  }

  /**
   * Applies a preset native cursor for this session.
   *
   * @param cursor Non-null cursor kind; ignored on noop backends.
   */
  void setCursor(@NotNull FlixelMouseCursor cursor);

  /**
   * Restores the default cursor for this session.
   */
  void resetCursor();

  /**
   * Replaces the mouse cursor with a custom image.
   *
   * <p>Use this for a themed game cursor instead of one of the OS presets. The hotspot is the exact
   * pixel inside the image that counts as "the point" of the cursor (for an arrow, its tip; for a
   * crosshair, its center), given relative to the image's top-left corner.
   *
   * <p>This is primarily a desktop feature. On backends where {@link #supportsCustomCursors()}
   * returns {@code false}, this call is ignored. Call {@link #resetCursor()} to return to the
   * default pointer.
   *
   * <p>Example:
   *
   * <pre>{@code
   * Flixel.mouse.icons.setCustomCursor(cursorGraphic, 0, 0); // hotspot at the top-left tip
   * }</pre>
   *
   * @param image The cursor image; must not be {@code null}.
   * @param hotspotX The click point's X offset from the image's left edge, in pixels.
   * @param hotspotY The click point's Y offset from the image's top edge, in pixels.
   */
  default void setCustomCursor(@NotNull FlixelGraphic image, int hotspotX, int hotspotY) {}

  /**
   * Returns {@code true} when {@link #setCursor(FlixelMouseCursor)} may change what
   * the user sees for this target.
   */
  boolean supportsCursors();

  /**
   * Returns {@code true} when {@link #setCustomCursor(FlixelGraphic, int, int)} can change the
   * cursor image on this session.
   */
  default boolean supportsCustomCursors() {
    return false;
  }
}
