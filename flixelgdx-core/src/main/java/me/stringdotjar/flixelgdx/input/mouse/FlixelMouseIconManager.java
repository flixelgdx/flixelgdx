/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.input.mouse;

import org.jetbrains.annotations.NotNull;

/**
 * Platform native cursor styling, exposed through {@link FlixelMouseManager#icons()}.
 *
 * <p>Games that need the default Flixel cursor behavior never have to touch this type. When you
 * do need OS-level feedback (text field, busy state, resize handles), call
 * {@link #setNativeCursor(FlixelNativeMouseCursor)} during UI transitions and
 * {@link #clearNativeCursor()} when you are done.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.mouse.icons().setNativeCursor(FlixelNativeMouseCursor.IBEAM);
 * // ...
 * Flixel.mouse.icons().clearNativeCursor();
 * }</pre>
 *
 * @see FlixelMouseManager#icons()
 */
public interface FlixelMouseIconManager {

  /**
   * Applies a preset native cursor for this session.
   *
   * @param cursor Non-null cursor kind; ignored on noop backends.
   */
  void setNativeCursor(@NotNull FlixelNativeMouseCursor cursor);

  /**
   * Restores the default cursor for this session.
   */
  void clearNativeCursor();

  /**
   * @return {@code true} when {@link #setNativeCursor(FlixelNativeMouseCursor)} may change what
   *     the user sees for this target.
   */
  boolean supportsNativeCursor();
}
