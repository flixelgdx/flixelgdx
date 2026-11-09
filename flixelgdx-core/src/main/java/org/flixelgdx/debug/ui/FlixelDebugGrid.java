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

/**
 * Puts its children into the cells of a table-like layout, filling one row after another.
 *
 * <p>A grid lines things up in columns, which a stack cannot do across rows. It is perfect for a form of labels and
 * values: with the columns {@code "auto", "*"}, the first column is as wide as the longest label and the second takes
 * the rest, so every value starts at the same place.
 *
 * <pre>{@code
 * FlixelDebugGrid form = new FlixelDebugGrid("auto", "*").add(
 *     FlixelDebugUi.label("Sample rate"), rateLabel,
 *     FlixelDebugUi.label("Round trip"), pingLabel);
 * }</pre>
 *
 * <p>A column size is {@code "auto"} (as wide as its widest cell), {@code "*"} or {@code "2*"} (a share of the
 * leftover width), or a number of pixels such as {@code "120"}.
 */
public class FlixelDebugGrid extends FlixelDebugContainer<FlixelDebugGrid> {

  private final FlixelDebugListBuffer columns = new FlixelDebugListBuffer();
  private float spacing = FlixelDebugStack.DEFAULT_SPACING;

  /**
   * Creates an empty grid.
   *
   * @param columns The size of every column, for example {@code "auto", "*"}.
   */
  public FlixelDebugGrid(@NotNull String... columns) {
    super(FlixelDebugProtocol.WIDGET_GRID, Integer.MAX_VALUE);
    setColumns(columns);
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_COLUMNS);
    if (spacing != FlixelDebugStack.DEFAULT_SPACING) {
      mask |= bit(FlixelDebugProtocol.PROP_SPACING);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_COLUMNS -> columns.write(writer);
      case FlixelDebugProtocol.PROP_SPACING -> writer.writeValue(spacing);
      default -> super.writeProp(writer, key);
    }
  }

  /**
   * Replaces the column sizes.
   *
   * @param sizes The size of every column, for example {@code "auto", "*", "120"}.
   * @return This grid, for chaining.
   */
  @NotNull
  public FlixelDebugGrid setColumns(@NotNull String... sizes) {
    FlixelDebugMessageWriter items = columns.begin();
    for (String size : sizes) {
      items.writeValue(size);
    }
    if (columns.end(sizes.length)) {
      markDirty(FlixelDebugProtocol.PROP_COLUMNS);
    }
    return this;
  }

  public float getSpacing() {
    return spacing;
  }

  /**
   * Sets the gap between cells.
   *
   * @param spacing The gap in pixels.
   * @return This grid, for chaining.
   */
  @NotNull
  public FlixelDebugGrid setSpacing(float spacing) {
    if (this.spacing != spacing) {
      this.spacing = spacing;
      markDirty(FlixelDebugProtocol.PROP_SPACING);
    }
    return this;
  }
}
