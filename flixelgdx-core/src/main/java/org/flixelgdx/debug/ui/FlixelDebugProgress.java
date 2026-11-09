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
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * A bar that fills up from left to right, with optional text on top, such as a loading bar or a memory gauge.
 *
 * <pre>{@code
 * FlixelDebugProgress health = new FlixelDebugProgress(() -> player.health / (double) player.maxHealth);
 * }</pre>
 */
public class FlixelDebugProgress extends FlixelDebugWidget<FlixelDebugProgress> implements FlixelDebugBound {

  private double value;
  private final FlixelString text = new FlixelString(8);
  private final DoubleSupplier getter;

  /** Creates an empty bar. */
  public FlixelDebugProgress() {
    this(null);
  }

  /**
   * Creates a bar that reads how full it is every frame.
   *
   * @param getter Returns a value from 0 (empty) to 1 (full), or {@code null} to set it by hand.
   */
  public FlixelDebugProgress(@Nullable DoubleSupplier getter) {
    super(FlixelDebugProtocol.WIDGET_PROGRESS);
    this.getter = getter;
  }

  @Override
  public void refresh() {
    if (getter != null) {
      setValue(getter.getAsDouble());
    }
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (value != 0.0) {
      mask |= bit(FlixelDebugProtocol.PROP_VALUE);
    }
    if (!text.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_TEXT);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_VALUE -> writer.writeValue(value);
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      default -> super.writeProp(writer, key);
    }
  }

  public double getValue() {
    return value;
  }

  /**
   * Sets how full the bar is.
   *
   * @param value A value from 0 to 1. Values outside are clamped.
   * @return This bar, for chaining.
   */
  @NotNull
  public FlixelDebugProgress setValue(double value) {
    double clamped = Double.isNaN(value) ? 0.0 : Math.max(0.0, Math.min(1.0, value));
    if (this.value != clamped) {
      this.value = clamped;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return this;
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Sets the text drawn over the bar. The text is copied, so a reused buffer is safe to pass.
   *
   * @param text The new text, or {@code null} for none.
   * @return This bar, for chaining.
   */
  @NotNull
  public FlixelDebugProgress setText(@Nullable CharSequence text) {
    if (copyText(this.text, text)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }
}
