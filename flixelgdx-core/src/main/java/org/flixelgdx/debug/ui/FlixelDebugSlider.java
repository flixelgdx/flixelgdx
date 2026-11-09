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
 * A bar with a thumb that the person drags to pick a value in a range.
 *
 * <p>A slider is a volume knob laid flat: good for values where "about this much" is precise enough, such as a zoom
 * or a sample rate. It behaves like a {@link FlixelDebugNumber} and reports the value when the person lets go of the
 * thumb. Its range defaults to 0 to 1; set a step to snap to whole steps.
 *
 * <pre>{@code
 * FlixelDebugSlider rate = new FlixelDebugSlider(() -> controls.getRate(), v -> controls.setRate((int) v));
 * rate.setRange(1, 30).setStep(1);
 * }</pre>
 */
public class FlixelDebugSlider extends FlixelDebugNumberWidget<FlixelDebugSlider> {

  /** Creates a slider from 0 to 1. */
  public FlixelDebugSlider() {
    this(null, null);
  }

  /**
   * Creates a slider from 0 to 1 bound to a value of the game.
   *
   * @param getter Reads the value every frame, or {@code null} to set it by hand.
   * @param onChange Receives the value the person chose, or {@code null} for nothing.
   */
  public FlixelDebugSlider(@Nullable DoubleSupplier getter, @Nullable DoubleConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_SLIDER, getter, onChange);
    setRange(0.0, 1.0);
    setStep(0.0);
  }
}
