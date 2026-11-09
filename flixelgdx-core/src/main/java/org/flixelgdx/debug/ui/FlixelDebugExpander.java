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
import org.flixelgdx.functional.consumer.BooleanConsumer;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A header that folds its one child away or shows it again when clicked.
 *
 * <p>An expander is like a drawer with a label on the front: the label is always visible and the person opens the
 * drawer to see what is inside. Use it to group things that are only interesting sometimes.
 *
 * <pre>{@code
 * FlixelDebugExpander memory = new FlixelDebugExpander("Memory", memoryGraphs).setExpanded(false);
 * }</pre>
 */
public class FlixelDebugExpander extends FlixelDebugContainer<FlixelDebugExpander> {

  private final FlixelString text = new FlixelString(16);
  private BooleanConsumer onChange;

  private boolean expanded = true;

  /**
   * Creates an empty expander.
   *
   * @param header The text on the header.
   */
  public FlixelDebugExpander(@NotNull CharSequence header) {
    super(FlixelDebugProtocol.WIDGET_EXPANDER, 1);
    text.set(header);
  }

  /**
   * Creates an expander around a widget.
   *
   * @param header The text on the header.
   * @param child The widget the expander shows or hides.
   */
  public FlixelDebugExpander(@NotNull CharSequence header, @NotNull FlixelDebugWidget<?> child) {
    this(header);
    add(child);
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_TEXT);
    if (!expanded) {
      mask |= bit(FlixelDebugProtocol.PROP_EXPANDED);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(text);
      case FlixelDebugProtocol.PROP_EXPANDED -> writer.writeValue(expanded);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event == FlixelDebugProtocol.EVENT_CHANGE) {
      expanded = args.nextBoolean(expanded);
      if (onChange != null) {
        onChange.accept(expanded);
      }
    }
  }

  /**
   * Sets what runs when the person opens or closes the expander.
   *
   * @param onChange Receives {@code true} when it was opened, or {@code null} for nothing.
   * @return This expander, for chaining.
   */
  @NotNull
  public FlixelDebugExpander onChange(@Nullable BooleanConsumer onChange) {
    this.onChange = onChange;
    return this;
  }

  @NotNull
  public CharSequence getText() {
    return text;
  }

  /**
   * Sets the header text. The text is copied, so a reused buffer is safe to pass.
   *
   * @param header The new header, or {@code null} for none.
   * @return This expander, for chaining.
   */
  @NotNull
  public FlixelDebugExpander setText(@Nullable CharSequence header) {
    if (copyText(text, header)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }

  public boolean isExpanded() {
    return expanded;
  }

  /**
   * Opens or folds the expander.
   *
   * @param expanded {@code true} to show the content.
   * @return This expander, for chaining.
   */
  @NotNull
  public FlixelDebugExpander setExpanded(boolean expanded) {
    if (this.expanded != expanded) {
      this.expanded = expanded;
      markDirty(FlixelDebugProtocol.PROP_EXPANDED);
    }
    return this;
  }
}
