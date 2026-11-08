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

import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugTrackers;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugProperties;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Trackers" panel: one section per tracker registered with {@code Flixel.debug.trackers}, refreshed ten
 * times a second.
 *
 * <p>A game can replace it under the id {@value FlixelDebugDefaultPanels#TRACKERS}.
 */
public class FlixelDebugTrackersPanel extends FlixelDebugPanel {

  /** Seconds between two refreshes of the values. */
  public static final float REFRESH_INTERVAL = 0.1f;

  private final FlixelDebugTrackers trackers;
  private final FlixelDebugProperties grid = new FlixelDebugProperties();
  private final FlixelDebugLabel empty =
      FlixelDebugUi.hint("No trackers yet. Add one with Flixel.debug.trackers.add(...).");
  private float timer = REFRESH_INTERVAL;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose trackers the panel shows.
   */
  public FlixelDebugTrackersPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.TRACKERS, "Trackers");
    this.trackers = manager.trackers;
    setDock(FlixelDebugProtocol.DOCK_RIGHT);
    setContent(FlixelDebugUi.column(empty, grid.setGrow(1f).setName("values")));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    timer += elapsed;
    if (timer < REFRESH_INTERVAL) {
      return;
    }
    timer = 0f;
    int count = trackers.getCount();
    empty.setVisible(count == 0);
    grid.beginSections();
    for (int i = 0; i < count; i++) {
      grid.beginSection(trackers.getEntry(i).getName(), true);
      grid.addRawRows(trackers.writeListItems(i, grid.getRowWriter()));
    }
    grid.endSections();
  }

  @Override
  protected void onOpen() {
    timer = REFRESH_INTERVAL;
  }
}
