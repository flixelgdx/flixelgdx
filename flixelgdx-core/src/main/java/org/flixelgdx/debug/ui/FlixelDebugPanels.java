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
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The list of every panel the remote debugger app shows, exposed as {@code Flixel.debug.panels}.
 *
 * <p>The registry is completely neutral: the panels FlixelGDX ships with (stats, logs, console, scene, inspector,
 * save, and the rest) are registered here exactly like your own, and none of them is special. That means you can
 * remove a built-in panel, replace it with your own design under the same id, or add brand new ones, and the
 * debugger app follows without any change on its side.
 *
 * <p>Think of the registry as the program of a theater. The theater (the app) does not care which plays are on the
 * program; it stages whatever the program lists, in that order. The game edits the program.
 *
 * <pre>{@code
 * // Add a panel.
 * Flixel.debug.panels.register(new EnemyPanel(enemies));
 *
 * // Swap the built-in save editor for your own one. The app keeps it in the same place.
 * Flixel.debug.panels.replace(new MySavePanel()); // Its id is "save".
 *
 * // Remove a built-in panel you do not need.
 * Flixel.debug.panels.unregister("trackers");
 *
 * // Find a panel again later.
 * EnemyPanel panel = Flixel.debug.panels.get("game.enemies", EnemyPanel.class);
 * }</pre>
 *
 * <p>The registry can also ask the app to {@linkplain #show(String) bring a panel to the front},
 * {@linkplain #resetLayout() reset its layout}, or {@linkplain #toast(int, String) show a short message}.
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugPanel
 */
public class FlixelDebugPanels {

  /** How many app requests wait at most; older ones are dropped when more arrive before they are sent. */
  public static final int MAX_REQUESTS = 32;

  private long revision;

  private final FlixelArray<FlixelDebugPanel> panels = new FlixelArray<>();
  private final int[] requestKinds = new int[MAX_REQUESTS];
  private final int[] requestTones = new int[MAX_REQUESTS];
  private final String[] requestTexts = new String[MAX_REQUESTS];
  private final FlixelDebugManager manager;
  private int requestHead;
  private int requestCount;

  /**
   * Creates an empty registry.
   *
   * @param manager The debug manager the panels belong to, which gives them access to every feed.
   */
  public FlixelDebugPanels(@Nullable FlixelDebugManager manager) {
    this.manager = manager;
  }

  /**
   * Adds a panel to the end of the list. The app shows it from now on.
   *
   * @param panel The panel to add.
   * @param <T> The panel type, so the call can be chained: {@code EnemyPanel p = panels.register(new EnemyPanel())}.
   * @return The same panel.
   * @throws IllegalArgumentException If a panel with the same id is already registered, or the panel is registered
   *   somewhere else.
   */
  @NotNull
  public <T extends FlixelDebugPanel> T register(@NotNull T panel) {
    if (indexOf(panel.getId()) >= 0) {
      throw new IllegalArgumentException("A debug panel with the id '" + panel.getId() + "' is already registered."
          + " Use replace(...) to swap it.");
    }
    if (panel.getOwner() != null) {
      throw new IllegalArgumentException("The debug panel '" + panel.getId() + "' is registered elsewhere.");
    }
    panels.add(panel);
    panel.setOwner(this);
    revision++;
    return panel;
  }

  /**
   * Puts a panel in the place of the registered panel with the same id, or adds it when there is none.
   *
   * <p>This is how a game redesigns a built-in panel: build your own panel with the built-in id (for example
   * {@code "save"}) and replace the old one. The app keeps the panel where the person put it.
   *
   * @param panel The new panel.
   * @param <T> The panel type.
   * @return The same panel.
   */
  @NotNull
  public <T extends FlixelDebugPanel> T replace(@NotNull T panel) {
    int index = indexOf(panel.getId());
    if (index < 0) {
      return register(panel);
    }
    FlixelDebugPanel old = panels.get(index);
    if (old == panel) {
      return panel;
    }
    old.setOpen(false);
    old.setOwner(null);
    panels.set(index, panel);
    panel.setOwner(this);
    revision++;
    return panel;
  }

  /**
   * Removes a panel. The app closes its tab.
   *
   * @param id The id of the panel to remove.
   * @return The removed panel, or {@code null} when no panel has that id.
   */
  @Nullable
  public FlixelDebugPanel unregister(@NotNull String id) {
    int index = indexOf(id);
    if (index < 0) {
      return null;
    }
    FlixelDebugPanel removed = panels.removeIndex(index);
    if (removed != null) {
      removed.setOpen(false);
      removed.setOwner(null);
    }
    revision++;
    return removed;
  }

  /** Removes every panel, the built-in ones included. */
  public void clear() {
    for (int i = 0, n = panels.getSize(); i < n; i++) {
      FlixelDebugPanel panel = panels.get(i);
      panel.setOpen(false);
      panel.setOwner(null);
    }
    if (panels.getSize() > 0) {
      panels.clear();
      revision++;
    }
  }

  /**
   * Moves a panel to another position in the list. The list order is the order of the app's menu.
   *
   * @param id The id of the panel to move.
   * @param index The new position, from 0 to {@link #getCount()} minus 1.
   * @return {@code true} when the panel was found.
   */
  public boolean move(@NotNull String id, int index) {
    int from = indexOf(id);
    if (from < 0) {
      return false;
    }
    int to = Math.max(0, Math.min(panels.getSize() - 1, index));
    if (from != to) {
      FlixelDebugPanel panel = panels.removeIndex(from);
      panels.insert(to, panel);
      revision++;
    }
    return true;
  }

  /**
   * Finds a panel by id.
   *
   * @param id The id to look for.
   * @return The panel, or {@code null} when no panel has that id.
   */
  @Nullable
  public FlixelDebugPanel get(@NotNull String id) {
    int index = indexOf(id);
    return index >= 0 ? panels.get(index) : null;
  }

  /**
   * Finds a panel by id and checks its type.
   *
   * @param id The id to look for.
   * @param type The class the panel must be an instance of.
   * @param <T> The panel type.
   * @return The panel, or {@code null} when no panel has that id or it is not a {@code type}.
   */
  @Nullable
  public <T extends FlixelDebugPanel> T get(@NotNull String id, @NotNull Class<T> type) {
    FlixelDebugPanel panel = get(id);
    return type.isInstance(panel) ? type.cast(panel) : null;
  }

  /**
   * Returns the position of a panel in the list.
   *
   * @param id The id to look for.
   * @return The position, or {@code -1} when no panel has that id.
   */
  public int indexOf(@NotNull String id) {
    for (int i = 0, n = panels.getSize(); i < n; i++) {
      if (panels.get(i).getId().equals(id)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Updates every panel that is open (or that asked to be updated while closed). The debug manager calls this every
   * frame. It does not allocate.
   *
   * @param elapsed Real seconds since the last frame.
   */
  public void update(float elapsed) {
    for (int i = 0, n = panels.getSize(); i < n; i++) {
      panels.get(i).tick(elapsed);
    }
  }

  /** Closes every panel, for example because the app disconnected. */
  public void closeAll() {
    for (int i = 0, n = panels.getSize(); i < n; i++) {
      panels.get(i).setOpen(false);
    }
  }

  /**
   * Asks the app to show a panel and bring it to the front.
   *
   * @param id The panel's id.
   */
  public void show(@NotNull String id) {
    request(FlixelDebugProtocol.REQUEST_SHOW_PANEL, 0, id);
  }

  /**
   * Asks the app to close a panel's tab. The person can open it again from the app's menu.
   *
   * @param id The panel's id.
   */
  public void hide(@NotNull String id) {
    request(FlixelDebugProtocol.REQUEST_HIDE_PANEL, 0, id);
  }

  /** Asks the app to put every panel back where its dock position says. */
  public void resetLayout() {
    request(FlixelDebugProtocol.REQUEST_RESET_LAYOUT, 0, null);
  }

  /**
   * Asks the app to show a short message to the person, such as "Save written".
   *
   * @param tone A tone such as {@link FlixelDebugProtocol#TONE_SUCCESS} or {@link FlixelDebugProtocol#TONE_ERROR}.
   * @param text The message.
   */
  public void toast(int tone, @NotNull String text) {
    request(FlixelDebugProtocol.REQUEST_NOTIFY, tone, text);
  }

  /**
   * Returns whether a request waits to be sent to the app.
   *
   * @return {@code true} when {@link #writeRequest(FlixelDebugMessageWriter)} has something to write.
   */
  public boolean hasRequest() {
    return requestCount > 0;
  }

  /**
   * Writes the oldest waiting request as the body of an {@code APP_REQUEST} message and forgets it. The network
   * agent calls this.
   *
   * @param writer The writer, positioned right after the message type.
   */
  public void writeRequest(@NotNull FlixelDebugMessageWriter writer) {
    if (requestCount == 0) {
      return;
    }
    int kind = requestKinds[requestHead];
    writer.writeU8(kind);
    switch (kind) {
      case FlixelDebugProtocol.REQUEST_SHOW_PANEL, FlixelDebugProtocol.REQUEST_HIDE_PANEL ->
        writer.writeString(requestTexts[requestHead]);
      case FlixelDebugProtocol.REQUEST_NOTIFY ->
        writer.writeU8(requestTones[requestHead]).writeString(requestTexts[requestHead]);
      default -> {
      }
    }
    requestTexts[requestHead] = null;
    requestHead = (requestHead + 1) % MAX_REQUESTS;
    requestCount--;
  }

  /** Forgets every waiting request, for example because the app disconnected. */
  public void clearRequests() {
    for (int i = 0; i < MAX_REQUESTS; i++) {
      requestTexts[i] = null;
    }
    requestHead = 0;
    requestCount = 0;
  }

  private void request(int kind, int tone, String text) {
    if (requestCount == MAX_REQUESTS) {
      requestTexts[requestHead] = null;
      requestHead = (requestHead + 1) % MAX_REQUESTS;
      requestCount--;
    }
    int slot = (requestHead + requestCount) % MAX_REQUESTS;
    requestKinds[slot] = kind;
    requestTones[slot] = tone;
    requestTexts[slot] = text;
    requestCount++;
  }

  /**
   * Returns how many panels are registered. Together with {@link #get(int)} this walks the panels in order without
   * creating an iterator.
   *
   * @return The panel count.
   */
  public int getCount() {
    return panels.getSize();
  }

  /**
   * Returns the panel at a position.
   *
   * @param index A position from 0 to {@link #getCount()} minus 1.
   * @return The panel.
   */
  @NotNull
  public FlixelDebugPanel get(int index) {
    return panels.get(index);
  }

  /**
   * Returns a number that goes up whenever the list changes. The network agent sends the list again when it differs
   * from the one it sent last.
   *
   * @return The list revision.
   */
  public long getRevision() {
    return revision;
  }

  /**
   * Returns the debug manager the panels belong to.
   *
   * @return The manager, or {@code null} for a registry made without one (in tests).
   */
  @Nullable
  public FlixelDebugManager getManager() {
    return manager;
  }
}
