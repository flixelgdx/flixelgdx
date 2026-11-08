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
 * A closed list that opens when clicked and lets the person pick one item.
 *
 * <p>A dropdown is a menu at a restaurant: it shows the dish you picked, and you open it to choose another. The game
 * sets the items, keeps track of which one is chosen (by position), and runs code when the person picks.
 *
 * <pre>{@code
 * FlixelDebugDropdown level = new FlixelDebugDropdown(() -> difficulty, i -> difficulty = i)
 *     .setItems("Easy", "Normal", "Hard");
 * }</pre>
 *
 * <p>To change the items every frame without allocating, write them with {@link #beginItems()} and
 * {@link #endItems(int)} instead.
 */
public class FlixelDebugDropdown extends FlixelDebugWidget<FlixelDebugDropdown> implements FlixelDebugBound {

  private final FlixelDebugListBuffer items = new FlixelDebugListBuffer();
  private IntSupplier getter;
  private IntConsumer onChange;
  private int selected = -1;

  /** Creates an empty dropdown with nothing chosen. */
  public FlixelDebugDropdown() {
    this(null, null);
  }

  /**
   * Creates a dropdown bound to a setting.
   *
   * @param getter Reads the chosen position every frame, or {@code null} to set it by hand.
   * @param onChange Receives the position the person picked, or {@code null} for nothing.
   */
  public FlixelDebugDropdown(@Nullable IntSupplier getter, @Nullable IntConsumer onChange) {
    super(FlixelDebugProtocol.WIDGET_DROPDOWN);
    this.getter = getter;
    this.onChange = onChange;
  }

  @Override
  public void refresh() {
    if (getter != null) {
      setSelected(getter.getAsInt());
    }
  }

  /**
   * Starts replacing the items. Write each item with {@code writeValue(text)}, then call {@link #endItems(int)}.
   *
   * @return The writer to write the items into.
   */
  @NotNull
  public FlixelDebugMessageWriter beginItems() {
    return items.begin();
  }

  /**
   * Finishes replacing the items. They are only sent when they changed.
   *
   * @param count How many items were written.
   * @return This dropdown, for chaining.
   */
  @NotNull
  public FlixelDebugDropdown endItems(int count) {
    if (items.end(count)) {
      markDirty(FlixelDebugProtocol.PROP_ITEMS);
    }
    return this;
  }

  /**
   * Replaces the items.
   *
   * @param texts The text of every item, in order.
   * @return This dropdown, for chaining.
   */
  @NotNull
  public FlixelDebugDropdown setItems(@NotNull CharSequence... texts) {
    FlixelDebugMessageWriter writer = beginItems();
    for (CharSequence text : texts) {
      writer.writeValue(text);
    }
    return endItems(texts.length);
  }

  /**
   * Sets what runs when the person picks an item.
   *
   * @param onChange Receives the picked position, or {@code null} for nothing.
   * @return This dropdown, for chaining.
   */
  @NotNull
  public FlixelDebugDropdown onChange(@Nullable IntConsumer onChange) {
    this.onChange = onChange;
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_ITEMS);
    if (selected != -1) {
      mask |= bit(FlixelDebugProtocol.PROP_VALUE);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_ITEMS -> items.write(writer);
      case FlixelDebugProtocol.PROP_VALUE -> writer.writeValue(selected);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_CHANGE) {
      return;
    }
    int index = args.nextInt(-2);
    if (index < -1 || index >= items.getCount()) {
      resend(FlixelDebugProtocol.PROP_VALUE);
      return;
    }
    selected = index;
    if (onChange != null) {
      onChange.accept(index);
    }
    if (getter != null && getter.getAsInt() != index) {
      selected = getter.getAsInt();
      resend(FlixelDebugProtocol.PROP_VALUE);
    }
  }

  public int getSelected() {
    return selected;
  }

  /**
   * Chooses an item.
   *
   * @param selected The position of the item, or {@code -1} for none.
   * @return This dropdown, for chaining.
   */
  @NotNull
  public FlixelDebugDropdown setSelected(int selected) {
    if (this.selected != selected) {
      this.selected = selected;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return this;
  }

  /**
   * Returns how many items the dropdown holds.
   *
   * @return The count.
   */
  public int getItemCount() {
    return items.getCount();
  }
}
