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

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One dockable tab of the remote debugger app, built entirely by the game out of {@linkplain FlixelDebugWidget
 * widgets}.
 *
 * <p>The debugger app does not know what a "log panel" or a "save editor" is. It only knows how to draw widgets
 * (buttons, tables, graphs, and so on) and how to report what the person did with them. Every panel, including the
 * ones FlixelGDX ships with, is an ordinary {@code FlixelDebugPanel} registered in {@link FlixelDebugPanels}. That
 * makes the game the single source of truth: replace a built-in panel with your own and the app shows yours, without
 * any change to the app.
 *
 * <p>Think of a panel as a shop window. The game arranges the display (the widget tree) and updates the price tags
 * (the widget properties) whenever prices change. The app is the glass: people look through it and can press the
 * buttons on the display, and every press goes straight back to the shop.
 *
 * <h2>Building a panel</h2>
 *
 * <p>Create the widgets once, put them into the content with {@link #setContent(FlixelDebugWidget)}, and keep
 * references to the ones you update. Refresh them in {@link #update(float)}, which runs every frame while the panel
 * is open in the app:
 *
 * <pre>{@code
 * public class EnemyPanel extends FlixelDebugPanel {
 *
 *   private final FlixelDebugLabel count = new FlixelDebugLabel();
 *   private final FlixelString text = new FlixelString();
 *   private final EnemyGroup enemies;
 *
 *   public EnemyPanel(EnemyGroup enemies) {
 *     super("game.enemies", "Enemies");
 *     this.enemies = enemies;
 *     setContent(FlixelDebugUi.column(
 *         count,
 *         FlixelDebugUi.button("Kill all", enemies::killAll).setStyle(FlixelDebugProtocol.TONE_ERROR),
 *         FlixelDebugUi.checkbox("Frozen", enemies::isFrozen, enemies::setFrozen)));
 *   }
 *
 *   @Override
 *   public void update(float elapsed) {
 *     super.update(elapsed);
 *     count.setText(text.setLength(0).concat(enemies.countLiving()).concat(" alive"));
 *   }
 * }
 *
 * Flixel.debug.panels.register(new EnemyPanel(enemies));
 * }</pre>
 *
 * <p>Setters only send what changed, so refreshing every frame is cheap. A widget built with a getter, such as the
 * checkbox above, refreshes itself.
 *
 * <h2>Lifecycle</h2>
 *
 * <ul>
 *   <li>A panel starts closed. The app opens the panels it shows; {@link #onOpen()} runs and the panel's whole tree
 *       is sent.</li>
 *   <li>While open, {@link #update(float)} runs every frame and changed properties are sent as small patches.</li>
 *   <li>When the person closes the tab (or the app disconnects), {@link #onClose()} runs and updates stop, so a
 *       hidden panel costs nothing. Call {@link #setUpdatedWhileClosed(boolean)} for a panel that must keep
 *       collecting (for example a graph that should have history when it is opened).</li>
 * </ul>
 *
 * <p>Everything runs on the game's main thread. A listener or {@link #update(float)} that throws is logged once and
 * does not crash the game.
 *
 * @see FlixelDebugPanels
 * @see FlixelDebugUi
 * @see FlixelDebugWidget
 */
public class FlixelDebugPanel {

  private final FlixelArray<FlixelDebugWidget<?>> widgets = new FlixelArray<>();
  private final FlixelArray<FlixelDebugWidget<?>> dirtyWidgets = new FlixelArray<>();
  private final FlixelArray<FlixelDebugWidget<?>> pendingWidgets = new FlixelArray<>();
  private final FlixelDebugEventArgs args = new FlixelDebugEventArgs();
  private final String id;
  private final String title;
  private FlixelDebugPanels owner;
  private FlixelDebugWidget<?> content;
  private int nextId = 1;
  private int dock = FlixelDebugProtocol.DOCK_CENTER;

  private boolean shown = true;
  private boolean open;
  private boolean treeChanged = true;
  private boolean urgent;
  private boolean updatedWhileClosed;
  private boolean errorLogged;

  /**
   * Creates an empty panel. Give it content with {@link #setContent(FlixelDebugWidget)}.
   *
   * @param id The stable id, for example {@code "game.enemies"}. The app remembers where the person put the panel by
   *   this id, so do not change it between versions of your game.
   * @param title The name on the panel's tab.
   */
  public FlixelDebugPanel(@NotNull String id, @NotNull String title) {
    this.id = id;
    this.title = title;
  }

  /**
   * Creates a panel with content.
   *
   * @param id The stable id, for example {@code "game.enemies"}.
   * @param title The name on the panel's tab.
   * @param content The root widget, usually a {@link FlixelDebugStack}.
   */
  public FlixelDebugPanel(@NotNull String id, @NotNull String title, @NotNull FlixelDebugWidget<?> content) {
    this(id, title);
    setContent(content);
  }

  /**
   * Refreshes the panel's widgets. Called every frame while the panel is open (or always, see
   * {@link #setUpdatedWhileClosed(boolean)}), even while the game is paused.
   *
   * <p>The default does nothing except refresh widgets that were built with getters. Subclasses that override it
   * should call {@code super.update(elapsed)}. It must not allocate objects.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void update(float elapsed) {
    for (int i = 0, n = widgets.getSize(); i < n; i++) {
      FlixelDebugWidget<?> widget = widgets.get(i);
      if (widget instanceof FlixelDebugBound bound) {
        bound.refresh();
      }
    }
  }

  /**
   * Asks the app to show this panel and bring it to the front, for example after the game selected an object that
   * this panel describes.
   */
  public void show() {
    if (owner != null) {
      owner.show(id);
    }
  }

  /** Asks the app to close this panel's tab. The person can open it again from the app's menu. */
  public void hide() {
    if (owner != null) {
      owner.hide(id);
    }
  }

  /**
   * Finds a widget of this panel by its {@linkplain FlixelDebugWidget#setName(String) name}.
   *
   * @param name The name to look for.
   * @return The first widget with that name, or {@code null} when there is none.
   */
  @Nullable
  public FlixelDebugWidget<?> find(@NotNull String name) {
    for (int i = 0, n = widgets.getSize(); i < n; i++) {
      FlixelDebugWidget<?> widget = widgets.get(i);
      if (widget != null && widget.getName().equals(name)) {
        return widget;
      }
    }
    return null;
  }

  /**
   * Finds a widget of this panel by the id the panel gave it.
   *
   * @param widgetId The id.
   * @return The widget, or {@code null} when no widget has that id.
   */
  @Nullable
  public FlixelDebugWidget<?> findById(int widgetId) {
    for (int i = 0, n = widgets.getSize(); i < n; i++) {
      FlixelDebugWidget<?> widget = widgets.get(i);
      if (widget.getId() == widgetId) {
        return widget;
      }
    }
    return null;
  }

  /**
   * Opens or closes the panel. The network agent calls this when the app shows or hides the panel's tab; game code
   * normally does not.
   *
   * <p>Opening always makes the panel send its whole tree again, even when it was already open.
   *
   * @param open {@code true} to open the panel.
   */
  public void setOpen(boolean open) {
    if (open) {
      treeChanged = true;
    }
    if (this.open == open) {
      return;
    }
    this.open = open;
    try {
      if (open) {
        onOpen();
      } else {
        onClose();
      }
    } catch (RuntimeException e) {
      logError("opening or closing", e);
    }
  }

  /**
   * Runs {@link #update(float)} and catches anything it throws, so one broken panel cannot stop the debugger. The
   * debug manager calls this every frame.
   *
   * @param elapsed Real seconds since the last frame.
   */
  public final void tick(float elapsed) {
    if (!open && !updatedWhileClosed) {
      return;
    }
    try {
      update(elapsed);
    } catch (RuntimeException e) {
      logError("updating", e);
    }
  }

  /**
   * Returns whether the whole tree has to be sent, because the panel was just opened or widgets were added or
   * removed.
   *
   * @return {@code true} when {@link #writeTree(FlixelDebugMessageWriter)} should be sent next.
   */
  public boolean needsTree() {
    return treeChanged;
  }

  /**
   * Returns whether any widget has changes for a patch.
   *
   * @return {@code true} when {@link #writePatch(FlixelDebugMessageWriter)} has something to write.
   */
  public boolean hasChanges() {
    return dirtyWidgets.getSize() > 0 || pendingWidgets.getSize() > 0;
  }

  /**
   * Returns whether the panel handled an event since its last patch, so the patch should go out at once instead of
   * waiting for the next send slot. That keeps clicks feeling instant.
   *
   * @return {@code true} after an event.
   */
  public boolean isUrgent() {
    return urgent;
  }

  /**
   * Writes the {@code root} of a {@code PANEL_TREE} message and forgets all pending changes, because the tree
   * already contains them.
   *
   * @param writer The writer, positioned right after the panel index.
   */
  public void writeTree(@NotNull FlixelDebugMessageWriter writer) {
    if (content == null) {
      // An empty panel is an empty column, so the app still has something to draw.
      writer.writeU8(FlixelDebugProtocol.WIDGET_STACK).writeI32(0).writeString("");
      writer.beginArray().endArray(0);
      writer.beginArray().endArray(0);
    } else {
      content.writeTree(writer, 0);
    }
    for (int i = 0, n = pendingWidgets.getSize(); i < n; i++) {
      pendingWidgets.get(i).clearPending();
    }
    for (int i = 0, n = dirtyWidgets.getSize(); i < n; i++) {
      dirtyWidgets.get(i).clearDirty();
    }
    dirtyWidgets.clear();
    pendingWidgets.clear();
    treeChanged = false;
    urgent = false;
  }

  /**
   * Writes the operation array of a {@code PANEL_PATCH} message and forgets the changes it wrote.
   *
   * @param writer The writer, positioned right after the panel index.
   */
  public void writePatch(@NotNull FlixelDebugMessageWriter writer) {
    writer.beginArray();
    int count = 0;
    for (int i = 0, n = dirtyWidgets.getSize(); i < n; i++) {
      count += dirtyWidgets.get(i).writeChanges(writer);
    }
    for (int i = 0, n = pendingWidgets.getSize(); i < n; i++) {
      FlixelDebugWidget<?> widget = pendingWidgets.get(i);
      if (widget.getPanel() == this) {
        count += widget.writeAppends(writer);
      }
    }
    writer.endArray(count);
    dirtyWidgets.clear();
    pendingWidgets.clear();
    urgent = false;
  }

  /**
   * Handles a {@code PANEL_EVENT} from the app. The network agent calls this.
   *
   * <p>Events for unknown or disabled widgets are ignored. A listener that throws is logged once.
   *
   * @param reader The reader positioned at the event's widget id.
   */
  public void handleEvent(@NotNull FlixelDebugMessageReader reader) {
    int widgetId = reader.readI32();
    int event = reader.readU8();
    int count = reader.readCount();
    FlixelDebugWidget<?> widget = findById(widgetId);
    if (widget == null || !widget.isEnabled() || widget.getPanel() != this) {
      return;
    }
    args.reset(reader, count);
    urgent = true;
    try {
      widget.handleEvent(event, args);
    } catch (RuntimeException e) {
      logError("handling an event", e);
    }
  }

  /**
   * Called when the app starts showing this panel, right before its tree is sent. Override it to refresh data that
   * is expensive to keep up to date while nobody looks.
   */
  protected void onOpen() {}

  /** Called when the app stops showing this panel or disconnects. */
  protected void onClose() {}

  void queueDirty(@NotNull FlixelDebugWidget<?> widget) {
    if (!treeChanged) {
      dirtyWidgets.add(widget);
    } else {
      // The tree is sent next anyway and carries every property.
      widget.clearDirty();
    }
  }

  void queuePending(@NotNull FlixelDebugWidget<?> widget) {
    if (!pendingWidgets.contains(widget, true)) {
      pendingWidgets.add(widget);
    }
  }

  void markTreeChanged() {
    treeChanged = true;
  }

  void adopt(@NotNull FlixelDebugWidget<?> widget) {
    if (widget.getId() == 0) {
      widget.setId(nextId++);
    }
    if (!widgets.contains(widget, true)) {
      widgets.add(widget);
    }
    treeChanged = true;
  }

  void forget(@NotNull FlixelDebugWidget<?> widget) {
    widgets.removeValue(widget, true);
    dirtyWidgets.removeValue(widget, true);
    pendingWidgets.removeValue(widget, true);
    widget.setId(0);
    treeChanged = true;
  }

  void setOwner(@Nullable FlixelDebugPanels owner) {
    this.owner = owner;
  }

  private void logError(String what, RuntimeException e) {
    if (!errorLogged) {
      errorLogged = true;
      Flixel.log.tagged("Debugger/Panels").error("The debug panel '{}' failed while {}: {}", id, what, e.toString());
    }
  }

  @NotNull
  public String getId() {
    return id;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  /**
   * Returns the root widget of this panel.
   *
   * @return The content, or {@code null} before {@link #setContent(FlixelDebugWidget)}.
   */
  @Nullable
  public FlixelDebugWidget<?> getContent() {
    return content;
  }

  /**
   * Replaces the root widget of this panel. The whole tree is sent again.
   *
   * @param content The new root, usually a {@link FlixelDebugStack}, or {@code null} for an empty panel.
   */
  public void setContent(@Nullable FlixelDebugWidget<?> content) {
    if (this.content == content) {
      return;
    }
    if (this.content != null) {
      this.content.attach(null);
    }
    this.content = content;
    if (content != null) {
      FlixelDebugContainer<?> parent = content.getParent();
      if (parent != null) {
        parent.remove(content);
      }
      content.attach(this);
    }
    treeChanged = true;
  }

  /**
   * Returns where the app puts this panel the first time it sees its id.
   *
   * @return One of the {@code DOCK_*} constants of {@link FlixelDebugProtocol}.
   */
  public int getDock() {
    return dock;
  }

  /**
   * Sets where the app puts this panel the first time it sees its id: the middle, the left, the right, or the
   * bottom. After that, the app remembers where the person moved it.
   *
   * @param dock One of the {@code DOCK_*} constants of {@link FlixelDebugProtocol}.
   * @return This panel, for chaining.
   */
  @NotNull
  public FlixelDebugPanel setDock(int dock) {
    this.dock = dock;
    return this;
  }

  public boolean isShown() {
    return shown;
  }

  /**
   * Sets whether the app shows this panel the first time it sees its id. A panel that is not shown is still listed
   * in the app's menu, so the person can open it.
   *
   * @param shown {@code true} to show it at first.
   * @return This panel, for chaining.
   */
  @NotNull
  public FlixelDebugPanel setShown(boolean shown) {
    this.shown = shown;
    return this;
  }

  /**
   * Returns whether the app currently shows this panel.
   *
   * @return {@code true} while the panel is open.
   */
  public boolean isOpen() {
    return open;
  }

  public boolean isUpdatedWhileClosed() {
    return updatedWhileClosed;
  }

  /**
   * Sets whether {@link #update(float)} keeps running while the panel is closed. Turn it on for a panel that
   * collects history, such as graphs, so the history is there when the person opens the panel.
   *
   * @param updatedWhileClosed {@code true} to update even while closed.
   * @return This panel, for chaining.
   */
  @NotNull
  public FlixelDebugPanel setUpdatedWhileClosed(boolean updatedWhileClosed) {
    this.updatedWhileClosed = updatedWhileClosed;
    return this;
  }

  /**
   * Returns the registry this panel is registered in.
   *
   * @return The registry, or {@code null} while the panel is not registered.
   */
  @Nullable
  public FlixelDebugPanels getOwner() {
    return owner;
  }

  /**
   * Returns the debug manager this panel is registered in, which gives access to every feed.
   *
   * @return The manager, or {@code null} while the panel is not registered.
   */
  @Nullable
  public FlixelDebugManager getManager() {
    return owner != null ? owner.getManager() : null;
  }
}
