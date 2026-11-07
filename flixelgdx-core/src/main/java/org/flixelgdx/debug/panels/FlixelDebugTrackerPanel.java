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
package org.flixelgdx.debug.panels;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.FlixelDebugTrackerEntry;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;

/**
 * The tracker panel of the remote debugger: named groups of related values, such as an inventory or the
 * weights of an AI.
 *
 * <p>Think of it as a set of index cards. Each {@link FlixelDebugTrackerEntry} is one card with a title
 * and a list of {@code label -> value} lines. Every time the app asks, each card is asked for its current
 * lines and they are sent sorted by label, so the order never jumps around. Cards without any lines are
 * left out.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.trackers.add(new EnemyTrackerEntry(enemyManager));
 * Flixel.debug.trackers.remove(entry);
 * }</pre>
 *
 * <p>See {@link FlixelDebugTrackerEntry} for how to write an entry without creating garbage. All methods
 * must be called from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_TRACKERS
 */
public class FlixelDebugTrackerPanel extends FlixelDebugPanelBase {

  /** Seconds between revision bumps, which is how often the app is offered fresh values. */
  private static final float REFRESH_INTERVAL = 0.1f;

  private final FlixelArray<FlixelDebugTrackerEntry> entries = new FlixelArray<>(FlixelDebugTrackerEntry[]::new);
  private final FlixelArray<String> sortedKeys = new FlixelArray<>();

  private float timer;

  /** Creates the tracker panel. */
  public FlixelDebugTrackerPanel() {
    super("trackers", "Trackers", FlixelDebugProtocol.KIND_TRACKERS);
  }

  /**
   * Adds an entry. Adding an entry that is already present does nothing.
   *
   * @param entry The entry to show.
   */
  public void add(@NotNull FlixelDebugTrackerEntry entry) {
    if (entry == null || entries.contains(entry, true)) {
      return;
    }
    entries.add(entry);
    markChanged();
  }

  /**
   * Removes an entry. Removing an entry that was never added does nothing.
   *
   * @param entry The entry to stop showing.
   */
  public void remove(@NotNull FlixelDebugTrackerEntry entry) {
    if (entry != null && entries.removeValue(entry, true)) {
      markChanged();
    }
  }

  /** Removes every entry. */
  public void clear() {
    if (entries.getSize() > 0) {
      entries.clear();
      markChanged();
    }
  }

  @Override
  public void update(float elapsed) {
    timer += elapsed;
    if (timer >= REFRESH_INTERVAL) {
      timer = 0f;
      if (entries.getSize() > 0) {
        markChanged();
      }
    }
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    int blocks = 0;
    writer.beginArray();
    for (int e = 0, en = entries.getSize(); e < en && blocks < FlixelDebugProtocol.MAX_ARRAY_COUNT; e++) {
      FlixelDebugTrackerEntry entry = entries.get(e);
      FlixelMap<String, String> values = entry != null ? entry.getTrackedValues() : null;
      if (values == null || values.getSize() == 0) {
        continue;
      }
      sortedKeys.clear();
      for (FlixelMap.Entry<String, String> pair : values.entries()) {
        insertSorted(pair.key);
      }
      writer.writeString(entry.getName());
      int rows = Math.min(sortedKeys.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
      writer.beginArray();
      for (int i = 0; i < rows; i++) {
        String key = sortedKeys.get(i);
        String value = values.get(key);
        writer.writeString(key);
        writer.writeValue(value != null ? value : "");
      }
      writer.endArray(rows);
      blocks++;
    }
    writer.endArray(blocks);
    sortedKeys.clear();
  }

  @Override
  public void reset() {
    timer = 0f;
  }

  /** Inserts a key into {@link #sortedKeys} so the list stays sorted, without creating temporary objects. */
  private void insertSorted(String key) {
    int i = sortedKeys.getSize();
    sortedKeys.add(key);
    while (i > 0 && sortedKeys.get(i - 1).compareTo(key) > 0) {
      sortedKeys.set(i, sortedKeys.get(i - 1));
      i--;
    }
    sortedKeys.set(i, key);
  }

  /**
   * Returns how many entries are registered.
   *
   * @return The entry count.
   */
  public int getCount() {
    return entries.getSize();
  }
}
