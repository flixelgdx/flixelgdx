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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Shows one of its children at a time, with a row of tabs on top to pick which.
 *
 * <p>Tabs are like the dividers in a binder: every page has a title on its tab, and opening a tab shows that page.
 * Use them to fit several views into one panel without making it crowded.
 *
 * <pre>{@code
 * FlixelDebugTabs tabs = new FlixelDebugTabs()
 *     .addPage("Frame", frameGraphs)
 *     .addPage("Memory", memoryGraphs);
 * }</pre>
 */
public class FlixelDebugTabs extends FlixelDebugContainer<FlixelDebugTabs> {

  private final FlixelArray<String> titles = new FlixelArray<>();
  private final FlixelDebugListBuffer items = new FlixelDebugListBuffer();
  private IntConsumer onChange;
  private int selected;

  /** Creates tabs without pages. Add pages with {@link #addPage(String, FlixelDebugWidget)}. */
  public FlixelDebugTabs() {
    super(FlixelDebugProtocol.WIDGET_TABS, Integer.MAX_VALUE);
  }

  /**
   * Adds a page.
   *
   * @param title The title on the page's tab.
   * @param page The page's content.
   * @return These tabs, for chaining.
   */
  @NotNull
  public FlixelDebugTabs addPage(@NotNull String title, @NotNull FlixelDebugWidget<?> page) {
    add(page);
    titles.add(title);
    rebuildTitles();
    return this;
  }

  @Override
  public boolean remove(@NotNull FlixelDebugWidget<?> widget) {
    for (int i = 0, n = getChildCount(); i < n; i++) {
      if (getChild(i) == widget) {
        titles.removeIndex(i);
        break;
      }
    }
    boolean removed = super.remove(widget);
    rebuildTitles();
    return removed;
  }

  @Override
  public void clear() {
    super.clear();
    titles.clear();
    rebuildTitles();
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_ITEMS);
    if (selected != 0) {
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
    int index = args.nextInt(-1);
    if (index >= 0 && index < getChildCount()) {
      selected = index;
      if (onChange != null) {
        onChange.accept(index);
      }
    }
  }

  /**
   * Sets what runs when the person opens another page.
   *
   * @param onChange Receives the position of the opened page, or {@code null} for nothing.
   * @return These tabs, for chaining.
   */
  @NotNull
  public FlixelDebugTabs onChange(@Nullable IntConsumer onChange) {
    this.onChange = onChange;
    return this;
  }

  private void rebuildTitles() {
    FlixelDebugMessageWriter writer = items.begin();
    int n = Math.min(titles.getSize(), getChildCount());
    for (int i = 0; i < n; i++) {
      writer.writeValue(titles.get(i));
    }
    if (items.end(n)) {
      markDirty(FlixelDebugProtocol.PROP_ITEMS);
    }
  }

  public int getSelected() {
    return selected;
  }

  /**
   * Opens a page.
   *
   * @param selected The position of the page.
   * @return These tabs, for chaining.
   */
  @NotNull
  public FlixelDebugTabs setSelected(int selected) {
    if (this.selected != selected) {
      this.selected = selected;
      markDirty(FlixelDebugProtocol.PROP_VALUE);
    }
    return this;
  }
}
