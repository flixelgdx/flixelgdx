/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.mouse;

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
