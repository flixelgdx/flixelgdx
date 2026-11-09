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

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A swatch that opens a color picker, for tweaking colors of the running game.
 *
 * <p>Colors travel as {@code 0xAARRGGBB} ints: alpha, red, green, and blue, one byte each.
 *
 * <pre>{@code
 * FlixelDebugColorPicker sky = new FlixelDebugColorPicker(() -> skyColor, c -> skyColor = c);
 * }</pre>
 */
public class FlixelDebugColorPicker extends FlixelDebugWidget<FlixelDebugColorPicker> implements FlixelDebugBound {

  private IntSupplier getter;
  private IntConsumer onChange;
  private int argb = 0xFFFFFFFF;

  /** Creates a white color picker. */
  public FlixelDebugColorPicker() {
    this(null, null);
  }

  /**
   * Creates a color picker bound to a color of the game.
   *
   * @param getter Reads the color as {@code 0xAARRGGBB} every frame, or {@code null} to set it by hand.
   * @param onChange Receives the color the person picked, or {@code null} for nothing.
   */
  public FlixelDebugColorPicker(@Nullable IntSupplier getter, @Nullable IntConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_COLOR);
    this.getter = getter;
    this.onChange = onChange;
    if (getter != null) {
      argb = getter.getAsInt();
    }
  }

  @Override
  public void refresh() {
    if (getter != null) {
      setArgb(getter.getAsInt());
    }
  }

  /**
   * Sets what runs when the person picks a color.
   *
   * @param onChange Receives the color as {@code 0xAARRGGBB}, or {@code null} for nothing.
   * @return This color picker, for chaining.
   */
  @NotNull
  public FlixelDebugColorPicker onChange(@Nullable IntConsumer onChange) {
    this.onChange = onChange;
    return this;
  }

  @Override
  protected long getPropMask() {
    return super.getPropMask() | bit(FlixelDebugProtocol.PROP_VALUE);
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    if (key == FlixelDebugProtocol.PROP_VALUE) {
      writer.writeValue(argb);
    } else {
      super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE) {
      return;
    }
    int wanted = args.nextInt(argb);
    argb = wanted;
    if (onChange != null) {
      onChange.accept(wanted);
    }
    if (getter != null) {
      refresh();
    }
  }

  public int getArgb() {
    return argb;
  }

  /**
   * Sets the shown color.
   *
   * @param argb The color as {@code 0xAARRGGBB}.
   * @return This picker, for chaining.
   */
  @NotNull
  public FlixelDebugColorPicker setArgb(int argb) {
    if (this.argb != argb) {
      this.argb = argb;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return this;
  }
}
