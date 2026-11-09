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
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * A number field with small up and down buttons.
 *
 * <p>Use it for values the person types exactly, such as a speed or a level number. Set a range with
 * {@link #setRange(double, double)}, the size of one up or down step with {@link #setStep(double)}, and how many
 * decimals it shows with {@link #setDecimals(int)} ({@code 0} for whole numbers). Values the app sends outside the
 * range are clamped before the game sees them.
 *
 * <pre>{@code
 * FlixelDebugNumber speed = new FlixelDebugNumber(() -> player.speed, v -> player.speed = (float) v)
 *     .setRange(0, 1000).setStep(10).setDecimals(0);
 * }</pre>
 *
 * @see FlixelDebugSlider
 */
public class FlixelDebugNumber extends FlixelDebugNumberWidget<FlixelDebugNumber> {

  /** Creates a number field that shows 0. */
  public FlixelDebugNumber() {
    this(null, null);
  }

  /**
   * Creates a number field bound to a value of the game.
   *
   * @param getter Reads the value every frame, or {@code null} to set it by hand.
   * @param onChange Receives the value the person entered, or {@code null} for nothing.
   */
  public FlixelDebugNumber(@Nullable DoubleSupplier getter, @Nullable DoubleConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_NUMBER, getter, onChange);
  }
}
