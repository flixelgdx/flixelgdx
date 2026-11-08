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

/**
 * One piece of a debugger panel's user interface, such as a button, a label, a table, or a row of other widgets.
 *
 * <p>The game builds widgets and the debugger app draws them. Think of a widget as a puppet and the app as the
 * stage: the game holds the strings (the properties, like the text of a label) and pulls them whenever something
 * changes, and the stage shows the puppet moving. When someone in the audience pokes a puppet (clicks a button), the
 * stage tells the game, and the game decides what happens. The app never decides anything on its own, which is why a
 * panel can do anything the game can do.
 *
 * <p>Every setter only marks the property as changed when the new value really differs from the old one. The panel
 * then sends just the changed properties to the app, a few dozen times a second at most. Setters do not allocate, so
 * it is fine to call them every frame from {@link FlixelDebugPanel#update(float)}:
 *
 * <pre>{@code
 * FlixelDebugLabel score = new FlixelDebugLabel();
 *
 * @Override
 * public void update(float elapsed) {
 *   scoreText.setLength(0).concat("Score: ").concat(game.score);
 *   score.setText(scoreText); // Sent only when the score changed.
 * }
 * }</pre>
 *
 * <p>The properties every widget shares are its visibility, whether it is enabled, its tooltip, its preferred size,
 * how much leftover space it takes inside a {@link FlixelDebugStack} ({@link #setGrow(float)}), and its
 * {@linkplain #setStyle(int) style}.
 *
 * <p>Widgets give themselves the right type in their setters, so calls chain:
 * {@code new FlixelDebugButton("Save").setTooltip("Writes the save now").setGrow(1f)}. A widget belongs to at most one
 * panel and one parent at a time.
 *
 * @param <W> The widget's own class, so that setters can return it for chaining.
 * @see FlixelDebugPanel
 * @see FlixelDebugUi
 */
public abstract class FlixelDebugWidget<W extends FlixelDebugWidget<W>> {

  /** The property mask of the shared properties ({@code VISIBLE} up to {@code STYLE}). */
  static final long COMMON_KEYS = 0xFEL;

  private long dirty;

  private FlixelDebugPanel panel;
  private FlixelDebugContainer<?> parent;
  private String name = "";
  private String tooltip = "";
  private float width;
  private float height;
  private float grow;
  private int id;
  private int style;
  private final int type;

  private boolean visible = true;
  private boolean enabled = true;

  /**
   * Creates a widget of one of the protocol's widget types.
   *
   * @param type One of the {@code WIDGET_*} constants of {@link FlixelDebugProtocol}.
   */
  protected FlixelDebugWidget(int type) {
    this.type = type;
  }

  /**
   * Sends one property again, even if it did not change. Useful after the app showed a value that the game did not
   * accept, so the app goes back to the game's value.
   *
   * @param key A {@code PROP_*} constant of {@link FlixelDebugProtocol}.
   */
  public void resend(int key) {
    markDirty(key);
  }

  /**
   * Returns the bit of a property key in a property mask.
   *
   * @param key A {@code PROP_*} constant of {@link FlixelDebugProtocol}.
   * @return The bit.
   */
  protected static long bit(int key) {
    return 1L << key;
  }

  /**
   * Copies text into a widget's text holder when it differs, without allocating.
   *
   * @param target The holder that keeps the widget's text.
   * @param value The new text; {@code null} counts as empty.
   * @return {@code true} when the text changed.
   */
  protected static boolean copyText(@NotNull FlixelString target, @Nullable CharSequence value) {
    if (value == target) {
      return false;
    }
    int length = value != null ? value.length() : 0;
    if (length == target.length()) {
      boolean same = true;
      for (int i = 0; i < length; i++) {
        if (target.charAt(i) != value.charAt(i)) {
          same = false;
          break;
        }
      }
      if (same) {
        return false;
      }
    }
    if (value == null) {
      target.clear();
    } else {
      target.set(value);
    }
    return true;
  }

  /**
   * Marks a property as changed so the panel sends it in its next patch.
   *
   * <p>Call this from a subclass setter after storing a new value. Nothing happens while the widget is not part of a
   * panel, because the whole tree is sent once it is added.
   *
   * @param key A {@code PROP_*} constant of {@link FlixelDebugProtocol}.
   */
  protected final void markDirty(int key) {
    if (panel == null) {
      return;
    }
    if (dirty == 0L) {
      panel.queueDirty(this);
    }
    dirty |= bit(key);
  }

  /**
   * Returns whether a property is waiting to be sent in the next patch.
   *
   * @param key A {@code PROP_*} constant of {@link FlixelDebugProtocol}.
   * @return {@code true} when the property changed since the last patch or tree.
   */
  protected final boolean isDirty(int key) {
    return (dirty & bit(key)) != 0L;
  }

  /**
   * Tells the panel that this widget has data to append (for example new table rows). The panel then calls
   * {@link #writeAppends(FlixelDebugMessageWriter)} in its next patch.
   */
  protected final void markPending() {
    if (panel != null) {
      panel.queuePending(this);
    }
  }

  /**
   * Tells the panel that widgets were added or removed under this widget, so the whole tree has to be sent again.
   */
  protected final void markStructureChanged() {
    if (panel != null) {
      panel.markTreeChanged();
    }
  }

  /**
   * Returns the keys of the properties to include when the whole tree is written: those that differ from their
   * defaults.
   *
   * <p>Subclasses add their own keys to the mask that {@code super.getPropMask()} returns.
   *
   * @return A mask of {@link #bit(int)} values.
   */
  protected long getPropMask() {
    long mask = 0L;
    if (!visible) {
      mask |= bit(FlixelDebugProtocol.PROP_VISIBLE);
    }
    if (!enabled) {
      mask |= bit(FlixelDebugProtocol.PROP_ENABLED);
    }
    if (!tooltip.isEmpty()) {
      mask |= bit(FlixelDebugProtocol.PROP_TOOLTIP);
    }
    if (width != 0f) {
      mask |= bit(FlixelDebugProtocol.PROP_WIDTH);
    }
    if (height != 0f) {
      mask |= bit(FlixelDebugProtocol.PROP_HEIGHT);
    }
    if (grow != 0f) {
      mask |= bit(FlixelDebugProtocol.PROP_GROW);
    }
    if (style != 0) {
      mask |= bit(FlixelDebugProtocol.PROP_STYLE);
    }
    return mask;
  }

  /**
   * Writes the value of one property (without its key).
   *
   * <p>Subclasses handle their own keys and pass every other key to {@code super.writeProp(...)}.
   *
   * @param writer The writer to write into.
   * @param key A {@code PROP_*} constant of {@link FlixelDebugProtocol}.
   */
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_VISIBLE -> writer.writeValue(visible);
      case FlixelDebugProtocol.PROP_ENABLED -> writer.writeValue(enabled);
      case FlixelDebugProtocol.PROP_TOOLTIP -> writer.writeValue(tooltip);
      case FlixelDebugProtocol.PROP_WIDTH -> writer.writeValue(width);
      case FlixelDebugProtocol.PROP_HEIGHT -> writer.writeValue(height);
      case FlixelDebugProtocol.PROP_GROW -> writer.writeValue(grow);
      case FlixelDebugProtocol.PROP_STYLE -> writer.writeValue(style);
      default -> writer.writeNullValue();
    }
  }

  /**
   * Returns whether this widget has data waiting to be appended. Only widgets that call {@link #markPending()} need
   * to override it.
   *
   * @return {@code true} when {@link #writeAppends(FlixelDebugMessageWriter)} has something to write.
   */
  protected boolean hasPending() {
    return false;
  }

  /**
   * Writes the {@code APPEND} operations of this widget into a patch and forgets them.
   *
   * <p>Each operation is {@code i32 widget}, {@code u8 PATCH_APPEND}, {@code u8 key}, and a {@code LIST} value.
   *
   * @param writer The writer, positioned inside the patch's operation array.
   * @return How many operations were written.
   */
  protected int writeAppends(@NotNull FlixelDebugMessageWriter writer) {
    return 0;
  }

  /**
   * Forgets the data waiting to be appended, because the whole tree (which already contains it) is about to be
   * sent.
   */
  protected void clearPending() {}

  /**
   * Handles an event that the app sent for this widget.
   *
   * <p>The arguments are read in order with {@link FlixelDebugEventArgs#next()}. Unknown events must be ignored.
   *
   * @param event One of the {@code EVENT_*} constants of {@link FlixelDebugProtocol}.
   * @param args The event's arguments.
   */
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {}

  /**
   * Returns this widget with its own type, for chaining.
   *
   * @return This widget.
   */
  @SuppressWarnings("unchecked")
  protected final W self() {
    return (W) this;
  }

  /**
   * Writes the children of this widget into the tree. Containers override it.
   *
   * @param writer The writer, positioned inside the children array.
   * @param depth How deep this widget is in the tree.
   * @return How many children were written.
   */
  int writeChildren(@NotNull FlixelDebugMessageWriter writer, int depth) {
    return 0;
  }

  /**
   * Gives this widget (and its children) to a panel, which hands out ids.
   *
   * @param panel The panel, or {@code null} to take the widget out of its panel.
   */
  void attach(@Nullable FlixelDebugPanel panel) {
    if (this.panel != null && this.panel != panel) {
      this.panel.forget(this);
    }
    this.panel = panel;
    dirty = 0L;
    if (panel != null) {
      panel.adopt(this);
    }
  }

  /**
   * Writes this widget, its properties, and its children as a {@code widget} of a {@code PANEL_TREE} message.
   *
   * @param writer The writer.
   * @param depth How deep this widget is in the tree; containers deeper than the writer allows are cut.
   */
  void writeTree(@NotNull FlixelDebugMessageWriter writer, int depth) {
    writer.writeU8(type).writeI32(id).writeString(name);
    long mask = getPropMask();
    int count = 0;
    writer.beginArray();
    for (int key = 1; key < 64; key++) {
      if ((mask & bit(key)) != 0L) {
        writer.writeU8(key);
        writeProp(writer, key);
        count++;
      }
    }
    writer.endArray(count);
    writer.beginArray();
    writer.endArray(writeChildren(writer, depth));
    dirty = 0L;
  }

  /**
   * Writes the {@code SET} operations of every changed property into a patch and forgets the changes.
   *
   * @param writer The writer, positioned inside the patch's operation array.
   * @return How many operations were written.
   */
  int writeChanges(@NotNull FlixelDebugMessageWriter writer) {
    long mask = dirty;
    dirty = 0L;
    int count = 0;
    for (int key = 1; key < 64 && mask != 0L; key++) {
      if ((mask & bit(key)) != 0L) {
        mask &= ~bit(key);
        writer.writeI32(id).writeU8(FlixelDebugProtocol.PATCH_SET).writeU8(key);
        writeProp(writer, key);
        count++;
      }
    }
    return count;
  }

  void clearDirty() {
    dirty = 0L;
  }

  void setParent(@Nullable FlixelDebugContainer<?> parent) {
    this.parent = parent;
  }

  void setId(int id) {
    this.id = id;
  }

  /**
   * Returns the protocol type of this widget.
   *
   * @return One of the {@code WIDGET_*} constants of {@link FlixelDebugProtocol}.
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the id the panel gave this widget, which the app uses in its events.
   *
   * @return The id, or {@code 0} while the widget is not part of a panel.
   */
  public int getId() {
    return id;
  }

  /**
   * Returns the panel this widget belongs to.
   *
   * @return The panel, or {@code null} while the widget is not part of one.
   */
  @Nullable
  public FlixelDebugPanel getPanel() {
    return panel;
  }

  /**
   * Returns the container that holds this widget.
   *
   * @return The parent, or {@code null} for a panel's content or a widget that is not added anywhere.
   */
  @Nullable
  public FlixelDebugContainer<?> getParent() {
    return parent;
  }

  @NotNull
  public String getName() {
    return name;
  }

  /**
   * Sets the stable name that tools (such as AI agents) use to find this widget, for example {@code "pause"}.
   *
   * <p>A name should be unique inside its panel. It is part of the tree, so set it before the panel is shown.
   *
   * @param name The name, or {@code null} for none.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setName(@Nullable String name) {
    String value = name != null ? name : "";
    if (!this.name.equals(value)) {
      this.name = value;
      markStructureChanged();
    }
    return self();
  }

  public boolean isVisible() {
    return visible;
  }

  /**
   * Shows or hides the widget. A hidden widget takes no space.
   *
   * @param visible {@code true} to show it.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setVisible(boolean visible) {
    if (this.visible != visible) {
      this.visible = visible;
      markDirty(FlixelDebugProtocol.PROP_VISIBLE);
    }
    return self();
  }

  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether the person can use the widget. A disabled widget is shown grayed out.
   *
   * @param enabled {@code true} to allow input.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      markDirty(FlixelDebugProtocol.PROP_ENABLED);
    }
    return self();
  }

  @NotNull
  public String getTooltip() {
    return tooltip;
  }

  /**
   * Sets the text shown while the pointer rests on the widget.
   *
   * @param tooltip The tooltip, or {@code null} for none.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setTooltip(@Nullable String tooltip) {
    String value = tooltip != null ? tooltip : "";
    if (!this.tooltip.equals(value)) {
      this.tooltip = value;
      markDirty(FlixelDebugProtocol.PROP_TOOLTIP);
    }
    return self();
  }

  public float getWidth() {
    return width;
  }

  /**
   * Sets the preferred width in device-independent pixels.
   *
   * @param width The width, or {@code 0} to size the widget to its content.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setWidth(float width) {
    if (this.width != width) {
      this.width = width;
      markDirty(FlixelDebugProtocol.PROP_WIDTH);
    }
    return self();
  }

  public float getHeight() {
    return height;
  }

  /**
   * Sets the preferred height in device-independent pixels.
   *
   * @param height The height, or {@code 0} to size the widget to its content.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setHeight(float height) {
    if (this.height != height) {
      this.height = height;
      markDirty(FlixelDebugProtocol.PROP_HEIGHT);
    }
    return self();
  }

  public float getGrow() {
    return grow;
  }

  /**
   * Sets how much of the leftover space this widget takes inside a {@link FlixelDebugStack}.
   *
   * <p>Two children with a grow of {@code 1} share the leftover space evenly; a child with {@code 2} gets twice as
   * much as one with {@code 1}. A grow of {@code 0} (the default) makes the widget only as big as its content. A
   * table that should fill the rest of a panel usually gets {@code setGrow(1f)}.
   *
   * @param grow The share, {@code 0} or more.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setGrow(float grow) {
    if (this.grow != grow) {
      this.grow = grow;
      markDirty(FlixelDebugProtocol.PROP_GROW);
    }
    return self();
  }

  public int getStyle() {
    return style;
  }

  /**
   * Sets the style bits of this widget: a tone (such as {@link FlixelDebugProtocol#TONE_ERROR}) combined with
   * flags such as {@link FlixelDebugProtocol#STYLE_BOLD} or {@link FlixelDebugProtocol#STYLE_MONO}.
   *
   * <p>The app picks the real colors and fonts so every panel matches its theme.
   *
   * <pre>{@code
   * label.setStyle(FlixelDebugProtocol.TONE_WARNING | FlixelDebugProtocol.STYLE_BOLD);
   * }</pre>
   *
   * @param style The style bits.
   * @return This widget, for chaining.
   */
  @NotNull
  public W setStyle(int style) {
    if (this.style != style) {
      this.style = style;
      markDirty(FlixelDebugProtocol.PROP_STYLE);
    }
    return self();
  }
}
