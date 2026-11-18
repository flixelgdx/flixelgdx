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
import org.jetbrains.annotations.Nullable;

/**
 * Platform native cursor styling, exposed through {@link FlixelMouseInputManager#icons}.
 *
 * <p>Games that need the default Flixel cursor behavior never have to touch this type. When you
 * do need OS-level feedback (text field, busy state, resize handles), call
 * {@link #setCursor(FlixelMouseCursor)} during UI transitions and
 * {@link #resetCursor()} when you are done. To theme the cursor, give any preset your own image
 * with {@link #setCustomCursor(FlixelMouseCursor, FlixelGraphic, int, int)}.
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
   * <p>If the preset was given a custom image through
   * {@link #setCustomCursor(FlixelMouseCursor, FlixelGraphic, int, int)}, that image is shown instead.
   *
   * @param cursor Non-null cursor kind; ignored on noop backends.
   */
  void setCursor(@NotNull FlixelMouseCursor cursor);

  /**
   * Restores the default {@link FlixelMouseCursor#ARROW} cursor for this session, or its custom image
   * if it has one.
   */
  void resetCursor();

  /**
   * Gives a cursor preset a custom image, with its hotspot at the image's top-left corner.
   *
   * <p>This is the same as calling {@link #setCustomCursor(FlixelMouseCursor, FlixelGraphic, int, int)}
   * with a hotspot of {@code (0, 0)}, which suits arrow-shaped images whose tip is in that corner.
   *
   * @param cursor The preset to give an image; {@link FlixelMouseCursor#NONE} is ignored.
   * @param image The cursor image, or {@code null} to go back to the system cursor for this preset.
   */
  default void setCustomCursor(@NotNull FlixelMouseCursor cursor, @Nullable FlixelGraphic image) {
    setCustomCursor(cursor, image, 0, 0);
  }

  /**
   * Gives a cursor preset a custom image that is shown whenever that preset is active.
   *
   * <p>Think of the presets as roles and the images as costumes. Your game (or a UI library) still
   * picks the role with {@link #setCursor(FlixelMouseCursor)}, such as {@link FlixelMouseCursor#HAND}
   * over a button, and the cursor simply wears the costume you registered for that role. Presets
   * without an image keep using the system cursor. Images stay registered until you pass
   * {@code null} for that preset, so {@link #resetCursor()} shows your custom
   * {@link FlixelMouseCursor#ARROW} image if you gave it one.
   *
   * <p>The hotspot is the exact pixel inside the image that counts as "the point" of the cursor
   * (for an arrow, its tip; for a crosshair, its center), given relative to the image's top-left
   * corner. {@link FlixelMouseCursor#NONE} cannot have an image, since it hides the cursor.
   *
   * <p>On backends where {@link #supportsCustomCursors()} returns {@code false}, this call is
   * ignored and every preset uses the system cursor.
   *
   * <p>Example:
   *
   * <pre>{@code
   * // A themed pointer everywhere, plus a custom caret over text fields.
   * Flixel.mouse.icons.setCustomCursor(FlixelMouseCursor.ARROW, pointerGraphic);
   * Flixel.mouse.icons.setCustomCursor(FlixelMouseCursor.IBEAM, caretGraphic, 4, 8);
   *
   * // Later, go back to the system caret.
   * Flixel.mouse.icons.setCustomCursor(FlixelMouseCursor.IBEAM, null);
   * }</pre>
   *
   * @param cursor The preset to give an image; {@link FlixelMouseCursor#NONE} is ignored.
   * @param image The cursor image, or {@code null} to go back to the system cursor for this preset.
   * @param hotspotX The click point's X offset from the image's left edge, in pixels.
   * @param hotspotY The click point's Y offset from the image's top edge, in pixels.
   */
  default void setCustomCursor(@NotNull FlixelMouseCursor cursor, @Nullable FlixelGraphic image, int hotspotX,
      int hotspotY) {}

  /**
   * Returns {@code true} when {@link #setCursor(FlixelMouseCursor)} may change what
   * the user sees for this target.
   *
   * @return {@code true} if standard cursor changes are supported, {@code false} otherwise.
   */
  boolean supportsCursors();

  /**
   * Returns {@code true} when {@link #setCustomCursor(FlixelMouseCursor, FlixelGraphic, int, int)} can
   * change cursor images on this session.
   *
   * @return {@code true} if custom cursor images are supported, {@code false} otherwise.
   */
  default boolean supportsCustomCursors() {
    return false;
  }
}
