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
 * <p>Backends map these to OS or browser cursors where available. When a preset has no exact
 * match (for example a busy spinner on some desktops), the implementation picks the closest
 * alternative and documents that in the backend Javadoc.
 */
public enum FlixelNativeMouseCursor {

  /** Default arrow pointer. */
  ARROW,

  /** Text caret, suitable for editable text. */
  IBEAM,

  /**
   * Busy or loading indicator where the platform exposes one. On some backends this falls back
   * to {@link #ARROW}.
   */
  WAIT,

  /** Simple crosshair. */
  CROSSHAIR,

  /** Hand, often used for links or buttons. */
  HAND,

  /** Horizontal resize. */
  HORIZONTAL_RESIZE,

  /** Vertical resize. */
  VERTICAL_RESIZE,

  /** Diagonal resize (north-west to south-east). */
  NORTH_WEST_SOUTH_EAST_RESIZE,

  /** Diagonal resize (north-east to south-west). */
  NORTH_EAST_SOUTH_WEST_RESIZE,

  /** Move or resize in all directions. */
  ALL_RESIZE,

  /** Not allowed or unavailable. */
  NOT_ALLOWED,

  /**
   * Invisible cursor. Use {@link FlixelMouseIconManager#clearNativeCursor()} to restore the
   * default instead of selecting this when possible.
   */
  NONE
}
