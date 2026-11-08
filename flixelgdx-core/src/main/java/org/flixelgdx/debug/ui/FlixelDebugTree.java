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

import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongConsumer;

/**
 * Nodes nested under other nodes, which the person can fold open and closed, like the scene of a game or the folders
 * of a disk.
 *
 * <p>Every node has an id that the game chooses and the id of its parent ({@code -1} for a top-level node). Parents
 * must be written before their children. Like a {@link FlixelDebugTable}, the tree is rebuilt in one go and only sent
 * when it changed; the app remembers which nodes the person opened, by id, across rebuilds.
 *
 * <pre>{@code
 * tree.beginNodes();
 * tree.node(1, -1, "PlayState").style(FlixelDebugProtocol.STYLE_EXPANDED);
 * tree.node(2, 1, "FlixelGroup").detail("state.0");
 * tree.node(3, 2, "Player").detail("state.0.0").badge("agent", FlixelDebugProtocol.TONE_ACCENT);
 * tree.endNodes();
 * }</pre>
 *
 * <p>A node can carry a muted {@linkplain Node#detail(CharSequence) detail} after its text and small colored
 * {@linkplain Node#badge(CharSequence, int) badges}. Selecting, double clicking, and the right-click menu work as in a
 * table, and a {@linkplain #setFilter(FlixelDebugTextBox) filter box} hides nodes that do not match while keeping
 * the parents of those that do.
 */
public class FlixelDebugTree extends FlixelDebugWidget<FlixelDebugTree> {

  private long selected = -1L;

  private final FlixelDebugListBuffer nodes = new FlixelDebugListBuffer();
  private final FlixelDebugListBuffer menu = new FlixelDebugListBuffer();
  private final Node node = new Node();
  private FlixelDebugMessageWriter builder;
  private FlixelDebugTextBox filter;
  private LongConsumer onSelect;
  private LongConsumer onActivate;
  private FlixelDebugMenuListener onMenu;
  private int count;

  private boolean building;

  /** Creates an empty tree. */
  public FlixelDebugTree() {
    super(FlixelDebugProtocol.WIDGET_TREE);
  }

  /** Starts rebuilding all nodes. Add them with {@link #node(long, long, CharSequence)}, then call {@link #endNodes()}. */
  public void beginNodes() {
    node.finish();
    builder = nodes.begin();
    count = 0;
    building = true;
  }

  /**
   * Adds a node. The previous node is finished automatically.
   *
   * @param id The node's id, which events report.
   * @param parent The id of the parent node, or {@code -1} for a top-level node. The parent must already be added.
   * @param text The node's text; it is copied.
   * @return The node, to add a detail, a style, or badges to.
   * @throws IllegalStateException When {@link #beginNodes()} was not called first.
   */
  @NotNull
  public Node node(long id, long parent, @Nullable CharSequence text) {
    if (!building) {
      throw new IllegalStateException("Call beginNodes() before node(...).");
    }
    node.finish();
    node.start(builder, id, parent, text);
    count++;
    return node;
  }

  /**
   * Finishes rebuilding. The nodes are sent only when they differ from the ones the app has.
   *
   * @return {@code true} when the nodes changed.
   */
  public boolean endNodes() {
    node.finish();
    building = false;
    if (nodes.end(count)) {
      markDirty(FlixelDebugProtocol.PROP_ITEMS);
      return true;
    }
    return false;
  }

  /**
   * Replaces the items of the right-click menu that every node gets.
   *
   * @param items The item texts, in order. Pass none to remove the menu.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree setMenu(@NotNull String... items) {
    FlixelDebugMessageWriter writer = menu.begin();
    for (String item : items) {
      writer.writeValue(item);
    }
    if (menu.end(items.length)) {
      markDirty(FlixelDebugProtocol.PROP_MENU);
    }
    return this;
  }

  /**
   * Sets what runs when the person selects a node or clears the selection.
   *
   * @param onSelect Receives the node id, or {@code -1} when nothing is selected; {@code null} for nothing.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree onSelect(@Nullable LongConsumer onSelect) {
    this.onSelect = onSelect;
    return this;
  }

  /**
   * Sets what runs when the person double-clicks a node or presses Enter on it.
   *
   * @param onActivate Receives the node id, or {@code null} for nothing.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree onActivate(@Nullable LongConsumer onActivate) {
    this.onActivate = onActivate;
    return this;
  }

  /**
   * Sets what runs when the person picks an item of the right-click menu.
   *
   * @param onMenu The listener, or {@code null} for nothing.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree onMenu(@Nullable FlixelDebugMenuListener onMenu) {
    this.onMenu = onMenu;
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_ITEMS);
    if (selected != -1L) {
      mask |= bit(FlixelDebugProtocol.PROP_SELECTED);
    }
    if (menu.getCount() > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_MENU);
    }
    if (filter != null) {
      mask |= bit(FlixelDebugProtocol.PROP_FILTER);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_ITEMS -> nodes.write(writer);
      case FlixelDebugProtocol.PROP_SELECTED -> writer.writeValue(selected);
      case FlixelDebugProtocol.PROP_MENU -> menu.write(writer);
      case FlixelDebugProtocol.PROP_FILTER -> writer.writeValue(filter != null ? filter.getId() : 0);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    switch (event) {
      case FlixelDebugProtocol.EVENT_SELECT -> {
        FlixelDebugValue value = args.next();
        selected = value != null && value.isNumber() ? value.asLong() : -1L;
        if (onSelect != null) {
          onSelect.accept(selected);
        }
      }
      case FlixelDebugProtocol.EVENT_ACTIVATE -> {
        long id = args.nextLong(-1L);
        if (onActivate != null) {
          onActivate.accept(id);
        }
      }
      case FlixelDebugProtocol.EVENT_MENU -> {
        long id = args.nextLong(-1L);
        int item = args.nextInt(-1);
        if (onMenu != null && item >= 0 && item < menu.getCount()) {
          onMenu.onMenu(id, item);
        }
      }
      default -> {
      }
    }
  }

  /**
   * Returns how many nodes the tree holds.
   *
   * @return The node count.
   */
  public int getNodeCount() {
    return nodes.getCount();
  }

  public long getSelected() {
    return selected;
  }

  /**
   * Selects a node, or clears the selection. The app opens the node's parents so it can be seen.
   *
   * @param selected The node id, or {@code -1} for none.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree setSelected(long selected) {
    if (this.selected != selected) {
      this.selected = selected;
      markDirty(FlixelDebugProtocol.PROP_SELECTED);
    }
    return this;
  }

  @Nullable
  public FlixelDebugTextBox getFilter() {
    return filter;
  }

  /**
   * Sets a text box whose text filters the nodes. The box must be in the same panel.
   *
   * @param filter The text box, or {@code null} for no filter.
   * @return This tree, for chaining.
   */
  @NotNull
  public FlixelDebugTree setFilter(@Nullable FlixelDebugTextBox filter) {
    if (this.filter != filter) {
      this.filter = filter;
      markDirty(FlixelDebugProtocol.PROP_FILTER);
    }
    return this;
  }

  /**
   * One node being written. The same object is reused for every node, so do not keep it.
   */
  public static final class Node {

    private final FlixelString detail = new FlixelString(0);
    private FlixelDebugMessageWriter out;
    private int style;
    private int badges;

    private boolean open;
    private boolean inBadges;

    private Node() {}

    /**
     * Sets muted text drawn after the node's text, such as its address.
     *
     * @param text The text; it is copied.
     * @return This node, for chaining.
     * @throws IllegalStateException After a badge was added.
     */
    @NotNull
    public Node detail(@Nullable CharSequence text) {
      checkBeforeBadges();
      if (text == null) {
        detail.clear();
      } else {
        detail.set(text);
      }
      return this;
    }

    /**
     * Sets the node's style, for example {@link FlixelDebugProtocol#STYLE_EXPANDED} to show its children the first
     * time.
     *
     * @param style The style bits.
     * @return This node, for chaining.
     * @throws IllegalStateException After a badge was added.
     */
    @NotNull
    public Node style(int style) {
      checkBeforeBadges();
      this.style = style;
      return this;
    }

    /**
     * Adds a small colored tag after the node's text, such as {@code "tracked"}.
     *
     * @param text The tag's text.
     * @param tone A tone such as {@link FlixelDebugProtocol#TONE_ACCENT}.
     * @return This node, for chaining.
     */
    @NotNull
    public Node badge(@NotNull CharSequence text, int tone) {
      if (!inBadges) {
        out.writeValue(detail).writeValue(style).beginList();
        inBadges = true;
      }
      out.beginList().writeValue(text).writeValue(tone).endList(2);
      badges++;
      return this;
    }

    void start(FlixelDebugMessageWriter out, long id, long parent, CharSequence text) {
      this.out = out;
      detail.clear();
      style = 0;
      badges = 0;
      inBadges = false;
      open = true;
      out.beginList().writeValue(id).writeValue(parent).writeValue(text);
    }

    void finish() {
      if (!open) {
        return;
      }
      if (!inBadges) {
        out.writeValue(detail).writeValue(style).beginList();
      }
      out.endList(badges);
      out.endList(6);
      open = false;
    }

    private void checkBeforeBadges() {
      if (inBadges) {
        throw new IllegalStateException("Set the detail and the style before adding badges.");
      }
    }
  }
}
