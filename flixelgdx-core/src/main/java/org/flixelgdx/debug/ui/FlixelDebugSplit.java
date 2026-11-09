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

/**
 * Two widgets with a bar between them that the person can drag to give one more room than the other.
 *
 * <p>A split works like the divider in a two-pane window: the first child gets {@linkplain #setRatio(double) a share}
 * of the space and the second gets the rest. It is a good fit for a list on top and the details of the selected
 * item below.
 *
 * <pre>{@code
 * FlixelDebugSplit split = new FlixelDebugSplit(false, logTable, detailsBox).setRatio(0.75);
 * }</pre>
 */
public class FlixelDebugSplit extends FlixelDebugContainer<FlixelDebugSplit> {

  private double ratio = 0.5;
  private DoubleConsumer onChange;

  private boolean horizontal;

  /**
   * Creates an empty split. Add exactly two children.
   *
   * @param horizontal {@code true} to put the children side by side, {@code false} to put one above the other.
   */
  public FlixelDebugSplit(boolean horizontal) {
    super(FlixelDebugProtocol.WIDGET_SPLIT, 2);
    this.horizontal = horizontal;
  }

  /**
   * Creates a split with its two children.
   *
   * @param horizontal {@code true} to put the children side by side, {@code false} to put one above the other.
   * @param first The left or top child.
   * @param second The right or bottom child.
   */
  public FlixelDebugSplit(boolean horizontal, @NotNull FlixelDebugWidget<?> first,
      @NotNull FlixelDebugWidget<?> second) {
    this(horizontal);
    add(first, second);
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (horizontal) {
      mask |= bit(FlixelDebugProtocol.PROP_HORIZONTAL);
    }
    if (ratio != 0.5) {
      mask |= bit(FlixelDebugProtocol.PROP_VALUE);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_HORIZONTAL -> writer.writeValue(horizontal);
      case FlixelDebugProtocol.PROP_VALUE -> writer.writeValue(ratio);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE) {
      return;
    }
    double value = args.nextDouble(Double.NaN);
    if (value >= 0.0 && value <= 1.0) {
      ratio = value;
      if (onChange != null) {
        onChange.accept(value);
      }
    }
  }

  /**
   * Sets what runs when the person lets go of the bar.
   *
   * @param onChange Receives the new share of the first child, or {@code null} for nothing.
   * @return This split, for chaining.
   */
  @NotNull
  public FlixelDebugSplit onChange(@Nullable DoubleConsumer onChange) {
    this.onChange = onChange;
    return this;
  }

  public boolean isHorizontal() {
    return horizontal;
  }

  /**
   * Sets whether the two parts sit side by side or one above the other.
   *
   * @param horizontal {@code true} for side by side.
   * @return This split, for chaining.
   */
  @NotNull
  public FlixelDebugSplit setHorizontal(boolean horizontal) {
    if (this.horizontal != horizontal) {
      this.horizontal = horizontal;
      markDirty(FlixelDebugProtocol.PROP_HORIZONTAL);
    }
    return this;
  }

  public double getRatio() {
    return ratio;
  }

  /**
   * Sets the share of the space that the first child gets.
   *
   * @param ratio A share from 0 to 1. Values outside are clamped.
   * @return This split, for chaining.
   */
  @NotNull
  public FlixelDebugSplit setRatio(double ratio) {
    double value = Math.max(0.0, Math.min(1.0, ratio));
    if (this.ratio != value) {
      this.ratio = value;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return this;
  }
}
