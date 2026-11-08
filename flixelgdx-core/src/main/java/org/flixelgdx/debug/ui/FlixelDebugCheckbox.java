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
 * A box the person can tick or untick, with text next to it.
 *
 * <p>A checkbox is a light switch for one setting of the game. Build it with a getter and a setter and it stays in
 * sync on its own: the getter is read every frame (so the box follows changes made by the game, such as a pause
 * hotkey) and the setter runs when the person clicks.
 *
 * <pre>{@code
 * FlixelDebugCheckbox hitboxes = new FlixelDebugCheckbox("Show hitboxes",
 *     Flixel.debug.controls::isHitboxes, Flixel.debug.controls::setHitboxes);
 * }</pre>
 *
 * @see FlixelDebugToggle
 */
public class FlixelDebugCheckbox extends FlixelDebugBooleanWidget<FlixelDebugCheckbox> {

  /**
   * Creates an unticked checkbox.
   *
   * @param text The text next to the box.
   */
  public FlixelDebugCheckbox(@NotNull CharSequence text) {
    super(FlixelDebugProtocol.WIDGET_CHECKBOX, text, null, null);
  }

  /**
   * Creates a checkbox bound to a setting of the game.
   *
   * @param text The text next to the box.
   * @param getter Reads the setting every frame, or {@code null} to set the value by hand.
   * @param onChange Changes the setting when the person clicks, or {@code null} for nothing.
   */
  public FlixelDebugCheckbox(@NotNull CharSequence text, @Nullable BooleanSupplier getter,
      @Nullable BooleanConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_CHECKBOX, text, getter, onChange);
  }
}
