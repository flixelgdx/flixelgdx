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
 * Lines its children up in a column (top to bottom) or a row (left to right). This is the widget most panels are
 * built from.
 *
 * <p>Think of a stack as a bookshelf: books stand next to each other in order, with a small gap between them. A child
 * with a {@linkplain FlixelDebugWidget#setGrow(float) grow} above zero is a book that stretches to fill the empty
 * space on the shelf, which is how a table takes all the height that is left under a toolbar:
 *
 * <pre>{@code
 * FlixelDebugStack content = FlixelDebugUi.column(
 *     FlixelDebugUi.row(refreshButton, filterBox),
 *     table.setGrow(1f));
 * }</pre>
 */
public class FlixelDebugStack extends FlixelDebugContainer<FlixelDebugStack> {

  /** The gap between children that the app uses when none is set. */
  public static final float DEFAULT_SPACING = 6f;

  private float spacing = DEFAULT_SPACING;

  private boolean horizontal;

  /** Creates an empty column. */
  public FlixelDebugStack() {
    this(false);
  }

  /**
   * Creates an empty stack.
   *
   * @param horizontal {@code true} for a row, {@code false} for a column.
   */
  public FlixelDebugStack(boolean horizontal) {
    super(FlixelDebugProtocol.WIDGET_STACK, Integer.MAX_VALUE);
    this.horizontal = horizontal;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask();
    if (horizontal) {
      mask |= bit(FlixelDebugProtocol.PROP_HORIZONTAL);
    }
    if (spacing != DEFAULT_SPACING) {
      mask |= bit(FlixelDebugProtocol.PROP_SPACING);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_HORIZONTAL -> writer.writeValue(horizontal);
      case FlixelDebugProtocol.PROP_SPACING -> writer.writeValue(spacing);
      default -> super.writeProp(writer, key);
    }
  }

  public boolean isHorizontal() {
    return horizontal;
  }

  /**
   * Sets whether the children form a row or a column.
   *
   * @param horizontal {@code true} for a row.
   * @return This stack, for chaining.
   */
  @NotNull
  public FlixelDebugStack setHorizontal(boolean horizontal) {
    if (this.horizontal != horizontal) {
      this.horizontal = horizontal;
      markDirty(FlixelDebugProtocol.PROP_HORIZONTAL);
    }
    return this;
  }

  public float getSpacing() {
    return spacing;
  }

  /**
   * Sets the gap between children.
   *
   * @param spacing The gap in pixels.
   * @return This stack, for chaining.
   */
  @NotNull
  public FlixelDebugStack setSpacing(float spacing) {
    if (this.spacing != spacing) {
      this.spacing = spacing;
      markDirty(FlixelDebugProtocol.PROP_SPACING);
    }
    return this;
  }
}
