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

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * The shared base of widgets that hold one number in a range: {@link FlixelDebugNumber} and {@link FlixelDebugSlider}.
 *
 * <p>Set a range with {@link #setRange(double, double)}, the size of one step with {@link #setStep(double)}, and (for a
 * number field) how many decimals it shows with {@link #setDecimals(int)}. Values the app sends outside the range are
 * clamped before the game sees them. Built with a getter, the widget follows the game's value every frame.
 *
 * @param <W> The widget's own class, so that setters can return it for chaining.
 */
public abstract class FlixelDebugNumberWidget<W extends FlixelDebugNumberWidget<W>> extends FlixelDebugWidget<W>
    implements FlixelDebugBound {

  /** The number of decimals a number field shows when none is set. */
  public static final int DEFAULT_DECIMALS = 2;

  private double value;
  private double min = Double.NEGATIVE_INFINITY;
  private double max = Double.POSITIVE_INFINITY;
  private double step = 1.0;
  private DoubleSupplier getter;
  private DoubleConsumer onChange;
  private int decimals = DEFAULT_DECIMALS;

  /**
   * Creates the widget.
   *
   * @param type The widget type, such as {@link FlixelDebugProtocol#WIDGET_NUMBER}.
   * @param getter Reads the value every frame, or {@code null}.
   * @param onChange Receives the value the person chose, or {@code null}.
   */
  protected FlixelDebugNumberWidget(int type, @Nullable DoubleSupplier getter, @Nullable DoubleConsumer onChange) {
    super(type);
    this.getter = getter;
    this.onChange = onChange;
    if (getter != null) {
      value = getter.getAsDouble();
    }
  }

  @Override
  public void refresh() {
    if (getter != null) {
      setValue(getter.getAsDouble());
    }
  }

  /**
   * Sets what runs when the person changes the value.
   *
   * @param onChange Receives the new value, or {@code null} for nothing.
   * @return This widget, for chaining.
   */
  @NotNull
  public W onChange(@Nullable DoubleConsumer onChange) {
    this.onChange = onChange;
    return self();
  }

  /**
   * Sets the smallest and largest value the person may choose.
   *
   * @param min The smallest value.
   * @param max The largest value.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setRange(double min, double max) {
    if (this.min != min) {
      this.min = min;
      markDirty(FlixelDebugProtocol.PROP_MIN);
    }
    if (this.max != max) {
      this.max = max;
      markDirty(FlixelDebugProtocol.PROP_MAX);
    }
    return self();
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (value != 0.0) {
      mask |= bit(FlixelDebugProtocol.PROP_VALUE);
    }
    if (!Double.isInfinite(min)) {
      mask |= bit(FlixelDebugProtocol.PROP_MIN);
    }
    if (!Double.isInfinite(max)) {
      mask |= bit(FlixelDebugProtocol.PROP_MAX);
    }
    if (step != 1.0) {
      mask |= bit(FlixelDebugProtocol.PROP_STEP);
    }
    if (decimals != DEFAULT_DECIMALS) {
      mask |= bit(FlixelDebugProtocol.PROP_DECIMALS);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_VALUE -> writer.writeValue(value);
      case FlixelDebugProtocol.PROP_MIN -> writer.writeValue(min);
      case FlixelDebugProtocol.PROP_MAX -> writer.writeValue(max);
      case FlixelDebugProtocol.PROP_STEP -> writer.writeValue(step);
      case FlixelDebugProtocol.PROP_DECIMALS -> writer.writeValue(decimals);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE) {
      return;
    }
    double wanted = args.nextDouble(Double.NaN);
    if (Double.isNaN(wanted)) {
      resend(FlixelDebugProtocol.PROP_VALUE);
      return;
    }
    double clamped = Math.max(min, Math.min(max, wanted));
    value = clamped;
    if (clamped != wanted) {
      resend(FlixelDebugProtocol.PROP_VALUE);
    }
    if (onChange != null) {
      onChange.accept(clamped);
    }
    if (getter != null) {
      refresh();
    }
  }

  public double getValue() {
    return value;
  }

  /**
   * Sets the shown number.
   *
   * @param value The new value.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setValue(double value) {
    if (Double.compare(this.value, value) != 0) {
      this.value = value;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return self();
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public double getStep() {
    return step;
  }

  /**
   * Sets how much one up or down step (or one arrow key) changes the value.
   *
   * @param step The step size. For a slider, {@code 0} allows any value.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setStep(double step) {
    if (this.step != step) {
      this.step = step;
      markDirty(FlixelDebugProtocol.PROP_STEP);
    }
    return self();
  }

  public int getDecimals() {
    return decimals;
  }

  /**
   * Sets how many decimals the field shows.
   *
   * @param decimals The number of decimals, {@code 0} for whole numbers.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setDecimals(int decimals) {
    if (this.decimals != decimals) {
      this.decimals = decimals;
      markDirty(FlixelDebugProtocol.PROP_DECIMALS);
    }
    return self();
  }
}
