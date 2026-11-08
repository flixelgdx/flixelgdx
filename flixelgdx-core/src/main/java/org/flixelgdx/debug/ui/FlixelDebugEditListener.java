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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.jetbrains.annotations.NotNull;

/**
 * Runs when the person edits a row of a {@link FlixelDebugProperties} grid whose rows were written by hand.
 *
 * <pre>{@code
 * grid.onEdit((section, row, value) -> {
 *   if (section == 0 && row == 2 && value.isNumber()) {
 *     player.speed = value.asFloat();
 *   }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface FlixelDebugEditListener {

  /**
   * Handles an edited row.
   *
   * @param section The position of the section, as written.
   * @param row The position of the row inside the section, as written.
   * @param value The new value, of the same kind as the row's value. The holder is reused, so read it right away.
   */
  void onEdit(int section, int row, @NotNull FlixelDebugValue value);
}
