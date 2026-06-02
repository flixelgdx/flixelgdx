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

/**
 * Portable native cursor presets for {@link FlixelMouseIconManager}.
 *
 * <p>Backends map these to OS or browser cursors where available. When a preset has no exact match, each backend chooses the closest
 * alternative and documents limits in its own Javadoc. Plain HTML/CSS builds can expose richer cursor sets through standard CSS keywords,
 * while GLFW on some Linux desktops may fall back to the arrow for waits, grabs, diagonal resizes, or blocked icons when the host
 * cursor theme omits matching glyphs.
 */
public enum FlixelNativeMouseCursor {

  /** Default arrow pointer. */
  ARROW,

  /** Text caret, suitable for editable text. */
  IBEAM,

  /** Busy or loading indicator. LWJGL3 maps this to {@link #ARROW}. TeaVM/CSS uses native {@code wait}. */
  WAIT,

  /** Simple crosshair. */
  CROSSHAIR,

  /** Hand, often used for links or buttons. */
  HAND,

  /**
   * Open hand suggesting draggable content ({@code grab} in CSS backends). GLFW does not expose a universal grab shape here, so LWJGL3
   * maps this to {@link #ARROW}.
   */
  GRAB,

  /**
   * Closed hand while dragging ({@code grabbing}). LWJGL3 maps this to {@link #ARROW}.
   *
   * @see #GRAB
   */
  GRABBING,

  /** Horizontal resize. */
  HORIZONTAL_RESIZE,

  /** Vertical resize. */
  VERTICAL_RESIZE,

  /** Diagonal resize (north-west to south-east). LWJGL3 may use {@link #ARROW} on some Linux desktops. */
  NORTH_WEST_SOUTH_EAST_RESIZE,

  /** Diagonal resize (north-east to south-west). LWJGL3 may use {@link #ARROW} on some Linux desktops. */
  NORTH_EAST_SOUTH_WEST_RESIZE,

  /** Move or resize in all directions. */
  ALL_RESIZE,

  /** Not allowed or unavailable. LWJGL3 may use {@link #ARROW} on some Linux desktops. */
  NOT_ALLOWED,

  /**
   * Invisible cursor. Use {@link FlixelMouseIconManager#clearNativeCursor()} to restore the
   * default instead of selecting this when possible.
   */
  NONE
}
