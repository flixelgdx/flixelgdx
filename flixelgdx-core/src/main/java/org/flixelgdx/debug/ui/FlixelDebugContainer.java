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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that holds other widgets, such as a {@link FlixelDebugStack} or a {@link FlixelDebugTabs}.
 *
 * <p>A container is like a shelf: it decides how the things on it are lined up, but the things themselves are
 * separate widgets. Adding or removing a child changes the shape of the panel, so the panel sends its whole tree
 * again. Do that when the layout really changes (for example when a new section appears), not every frame; to show
 * or hide a child often, use {@link FlixelDebugWidget#setVisible(boolean)} instead, which is a tiny patch.
 *
 * @param <W> The container's own class, so that setters can return it for chaining.
 */
public abstract class FlixelDebugContainer<W extends FlixelDebugContainer<W>> extends FlixelDebugWidget<W> {

  private final FlixelArray<FlixelDebugWidget<?>> children = new FlixelArray<>();
  private final int maxChildren;

  /**
   * Creates a container.
   *
   * @param type One of the {@code WIDGET_*} constants.
   * @param maxChildren The most children the container takes, for example 1 for a scroll area.
   */
  protected FlixelDebugContainer(int type, int maxChildren) {
    super(type);
    this.maxChildren = maxChildren;
  }

  /**
   * Adds widgets to the end of this container.
   *
   * @param widgets The widgets to add. A widget that already has another parent is moved here.
   * @return This container, for chaining.
   * @throws IllegalStateException If the container would hold more children than it can.
   */
  @NotNull
  public W add(@NotNull FlixelDebugWidget<?>... widgets) {
    for (FlixelDebugWidget<?> widget : widgets) {
      insert(children.getSize(), widget);
    }
    return self();
  }

  /**
   * Inserts a widget at a position.
   *
   * @param index The position, from 0 to {@link #getChildCount()}.
   * @param widget The widget to insert. A widget that already has another parent is moved here.
   * @return This container, for chaining.
   * @throws IllegalStateException If the container would hold more children than it can.
   */
  @NotNull
  public W insert(int index, @NotNull FlixelDebugWidget<?> widget) {
    if (children.getSize() >= maxChildren) {
      throw new IllegalStateException("This container holds at most " + maxChildren + " children.");
    }
    FlixelDebugContainer<?> old = widget.getParent();
    if (old != null) {
      old.remove(widget);
    }
    children.insert(index, widget);
    widget.setParent(this);
    widget.attach(getPanel());
    markStructureChanged();
    return self();
  }

  /**
   * Removes a widget from this container.
   *
   * @param widget The widget to remove.
   * @return {@code true} when the widget was a child of this container.
   */
  public boolean remove(@NotNull FlixelDebugWidget<?> widget) {
    if (!children.removeValue(widget, true)) {
      return false;
    }
    widget.setParent(null);
    widget.attach(null);
    markStructureChanged();
    return true;
  }

  /** Removes every child. */
  public void clear() {
    if (children.getSize() == 0) {
      return;
    }
    for (int i = 0, n = children.getSize(); i < n; i++) {
      FlixelDebugWidget<?> child = children.get(i);
      child.setParent(null);
      child.attach(null);
    }
    children.clear();
    markStructureChanged();
  }

  @Override
  int writeChildren(@NotNull FlixelDebugMessageWriter writer, int depth) {
    // Every level of the tree keeps one writer array open (its children), and a property value can open a few more
    // (a table row inside the rows inside the property list), so a very deep tree is cut instead of overflowing the
    // writer.
    if (depth >= FlixelDebugMessageWriter.MAX_DEPTH - 6) {
      return 0;
    }
    int count = 0;
    for (int i = 0, n = children.getSize(); i < n; i++) {
      children.get(i).writeTree(writer, depth + 1);
      count++;
    }
    return count;
  }

  @Override
  void attach(@Nullable FlixelDebugPanel panel) {
    super.attach(panel);
    for (int i = 0, n = children.getSize(); i < n; i++) {
      children.get(i).attach(panel);
    }
  }

  /**
   * Returns how many children this container holds.
   *
   * @return The count.
   */
  public int getChildCount() {
    return children.getSize();
  }

  /**
   * Returns a child by position.
   *
   * @param index A position from 0 to {@link #getChildCount()} minus 1.
   * @return The child.
   */
  @NotNull
  public FlixelDebugWidget<?> getChild(int index) {
    return children.get(index);
  }
}
