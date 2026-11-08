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
import org.flixelgdx.debug.feeds.FlixelDebugWatches;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugProperties;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Watches" panel: every value registered with {@code Flixel.debug.watches}, refreshed ten times a
 * second.
 *
 * <p>A game can replace it under the id {@value FlixelDebugDefaultPanels#WATCHES}.
 */
public class FlixelDebugWatchesPanel extends FlixelDebugPanel {

  /** Seconds between two refreshes of the values. */
  public static final float REFRESH_INTERVAL = 0.1f;

  private final FlixelDebugWatches watches;
  private final FlixelDebugProperties grid = new FlixelDebugProperties();
  private final FlixelDebugLabel empty = FlixelDebugUi.hint(
      "No watches yet. Add some with Flixel.debug.watches.addInt(\"Score\", () -> score).");
  private float timer = REFRESH_INTERVAL;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose watches the panel shows.
   */
  public FlixelDebugWatchesPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.WATCHES, "Watches");
    this.watches = manager.watches;
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
    empty.setVisible(watches.getCount() == 0);
    grid.beginSections();
    grid.beginSection("Watches", true);
    grid.addRawRows(watches.writeListItems(grid.getRowWriter()));
    grid.endSections();
  }

  @Override
  protected void onOpen() {
    timer = REFRESH_INTERVAL;
  }
}
