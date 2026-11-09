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

import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.consumer.BooleanConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * A button that stays pressed until it is clicked again: a checkbox that looks like a small button.
 *
 * <p>Toggles fit in toolbars, where a row of them works like a set of filters (for example which log levels to show).
 * They behave exactly like a {@link FlixelDebugCheckbox}.
 *
 * <pre>{@code
 * FlixelDebugToggle showWarnings = new FlixelDebugToggle("Warn", () -> showWarn, v -> showWarn = v);
 * }</pre>
 */
public class FlixelDebugToggle extends FlixelDebugBooleanWidget<FlixelDebugToggle> {

  /**
   * Creates a toggle that is not pressed.
   *
   * @param text The text on the toggle.
   */
  public FlixelDebugToggle(@NotNull CharSequence text) {
    super(FlixelDebugProtocol.WIDGET_TOGGLE, text, null, null);
  }

  /**
   * Creates a toggle bound to a setting.
   *
   * @param text The text on the toggle.
   * @param getter Reads the setting every frame, or {@code null} to set the value by hand.
   * @param onChange Changes the setting when the person clicks, or {@code null} for nothing.
   */
  public FlixelDebugToggle(@NotNull CharSequence text, @Nullable BooleanSupplier getter,
      @Nullable BooleanConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_TOGGLE, text, getter, onChange);
  }
}
