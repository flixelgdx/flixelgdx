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
import org.flixelgdx.debug.feeds.FlixelDebugStats;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugGrid;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugScroll;
import org.flixelgdx.debug.ui.FlixelDebugStack;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Stats" panel: the newest numbers of the {@linkplain FlixelDebugManager#stats stats feed} at a glance,
 * such as the frame rate, memory, the game state, and the camera position.
 *
 * <p>Like every built-in panel it is an ordinary {@link FlixelDebugPanel}, so a game can replace it with its own
 * design under the id {@value FlixelDebugDefaultPanels#STATS}. The numbers refresh every time the stats feed takes a
 * sample (20 times a second by default).
 */
public class FlixelDebugStatsPanel extends FlixelDebugPanel {

  private long seenRevision = -1L;

  private final FlixelString text = new FlixelString(16);
  private final FlixelDebugStats stats;
  private final FlixelDebugLabel fps = value();
  private final FlixelDebugLabel frame = value();
  private final FlixelDebugLabel heap = value();
  private final FlixelDebugLabel nativeMemory = value();
  private final FlixelDebugLabel objects = value();
  private final FlixelDebugLabel draws = value();
  private final FlixelDebugLabel triangles = value();
  private final FlixelDebugLabel ping = value();
  private final FlixelDebugLabel state = value();
  private final FlixelDebugLabel elapsed = value();
  private final FlixelDebugLabel cameraX = value();
  private final FlixelDebugLabel cameraY = value();
  private final FlixelDebugLabel zoom = value();

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose stats feed the panel shows.
   */
  public FlixelDebugStatsPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.STATS, "Stats");
    this.stats = manager.stats;
    setDock(FlixelDebugProtocol.DOCK_LEFT);
    FlixelDebugStack content = FlixelDebugUi.column(
        FlixelDebugUi.heading("Client"),
        grid(new String[] { "FPS", "Frame ms", "Java heap MB", "Native MB", "Objects", "Draw calls", "Triangles",
            "Ping" }, fps, frame, heap, nativeMemory, objects, draws, triangles, ping),
        FlixelDebugUi.heading("World"),
        grid(new String[] { "State", "Elapsed" }, state, elapsed),
        FlixelDebugUi.heading("Camera"),
        grid(new String[] { "X", "Y", "Zoom" }, cameraX, cameraY, zoom));
    setContent(new FlixelDebugScroll(content));
  }

  @Override
  public void update(float elapsedSeconds) {
    super.update(elapsedSeconds);
    long revision = stats.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    fps.setText(text.setLength(0).concatFloatRounded(stats.getFps(), 0));
    frame.setText(text.setLength(0).concatFloatRounded(stats.getFrameMs(), 2));
    heap.setText(text.setLength(0).concatFloatRounded(stats.getHeapMb(), 1));
    nativeMemory.setText(text.setLength(0).concatFloatRounded(stats.getNativeMb(), 1));
    objects.setText(text.setLength(0).concat(stats.getObjectCount()));
    draws.setText(text.setLength(0).concat(stats.getDraws()));
    triangles.setText(text.setLength(0).concat(stats.getTriangles()));
    float roundTrip = stats.getRoundTripMs();
    if (roundTrip < 0f) {
      ping.setText("n/a");
    } else {
      ping.setText(text.setLength(0).concatFloatRounded(roundTrip, 0).concat(" ms"));
    }
    boolean paused = stats.isPaused();
    state.setText(paused ? "PAUSED" : "RUNNING");
    state.setStyle((paused ? FlixelDebugProtocol.TONE_WARNING : FlixelDebugProtocol.TONE_SUCCESS)
        | FlixelDebugProtocol.STYLE_MONO | FlixelDebugProtocol.STYLE_BOLD);
    elapsed.setText(text.setLength(0).concatFloatRounded((float) stats.getElapsedSeconds(), 1).concat(" s"));
    cameraX.setText(text.setLength(0).concatFloatRounded(stats.getCamX(), 1));
    cameraY.setText(text.setLength(0).concatFloatRounded(stats.getCamY(), 1));
    zoom.setText(text.setLength(0).concatFloatRounded(stats.getCamZoom(), 2));
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }

  private static FlixelDebugLabel value() {
    return new FlixelDebugLabel("-").setStyle(FlixelDebugProtocol.STYLE_MONO);
  }

  private static FlixelDebugGrid grid(@NotNull String[] names, @NotNull FlixelDebugLabel... values) {
    FlixelDebugGrid grid = new FlixelDebugGrid("2*", "3*").setSpacing(2f);
    for (int i = 0; i < names.length && i < values.length; i++) {
      grid.add(FlixelDebugUi.label(names[i]), values[i]);
    }
    return grid;
  }
}
