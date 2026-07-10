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

import org.jetbrains.annotations.NotNull;

/**
 * Platform native cursor styling, exposed through {@link FlixelMouseManager#icons}.
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
 * @see FlixelMouseManager#icons
 */
public interface FlixelMouseIconManager {

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
   * @return {@code true} when {@link #setCursor(FlixelMouseCursor)} may change what
   *     the user sees for this target.
   */
  boolean supportsCursors();
}
