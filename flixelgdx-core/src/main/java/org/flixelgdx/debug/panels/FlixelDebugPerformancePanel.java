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
import org.flixelgdx.debug.feeds.FlixelDebugControls;
import org.flixelgdx.debug.feeds.FlixelDebugStats;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugExpander;
import org.flixelgdx.debug.ui.FlixelDebugGraph;
import org.flixelgdx.debug.ui.FlixelDebugGrid;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugScroll;
import org.flixelgdx.debug.ui.FlixelDebugStack;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Performance" panel: graphs of the {@linkplain FlixelDebugManager#stats stats feed} over the last
 * {@value #HISTORY} samples, grouped into frame, render, memory, and network sections.
 *
 * <p>The panel keeps sampling while it is closed, so the graphs already have history when the person opens it. A game
 * can replace it under the id {@value FlixelDebugDefaultPanels#PERFORMANCE}.
 */
public class FlixelDebugPerformancePanel extends FlixelDebugPanel {

  /** How many samples every graph keeps. */
  public static final int HISTORY = 120;

  private static final String[] RATE_TEXTS = buildRateTexts();

  private long seenRevision = -1L;

  private final FlixelDebugStats stats;
  private final FlixelDebugControls controls;
  private final FlixelDebugLabel rate = new FlixelDebugLabel().setStyle(FlixelDebugProtocol.STYLE_MONO);
  private final FlixelDebugGraph fps = graph("FPS", "FPS", "");
  private final FlixelDebugGraph frameMs = graph("Frame ms", "Frame", "ms");
  private final FlixelDebugGraph draws = graph("Draw calls", "Draws", "");
  private final FlixelDebugGraph cpuMs = graph("CPU ms", "CPU", "ms");
  private final FlixelDebugGraph presentMs = graph("Present ms", "Present", "ms");
  private final FlixelDebugGraph triangles = graph("Triangles", "Triangles", "");
  private final FlixelDebugGraph vertices = graph("Vertices", "Vertices", "");
  private final FlixelDebugGraph passes = graph("Passes", "Passes", "");
  private final FlixelDebugGraph submits = graph("Submits", "Submits", "");
  private final FlixelDebugGraph pipelines = graph("Pipelines", "Pipelines", "");
  private final FlixelDebugGraph heapMb = graph("Heap MB", "Heap", "MB");
  private final FlixelDebugGraph nativeMb = graph("Native MB", "Native", "MB");
  private final FlixelDebugGraph textureMb = graph("Texture MB", "Textures", "MB");
  private final FlixelDebugGraph ringKb = graph("Ring KB", "Ring", "KB");
  private final FlixelDebugGraph sendKbps = graph("Send KB/s", "Send", "KB/s");
  private final FlixelDebugGraph recvKbps = graph("Receive KB/s", "Receive", "KB/s");
  private final FlixelDebugGraph pingMs = graph("Ping ms", "Ping", "ms");

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose stats feed the panel graphs.
   */
  public FlixelDebugPerformancePanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.PERFORMANCE, "Performance");
    this.stats = manager.stats;
    this.controls = manager.controls;
    setDock(FlixelDebugProtocol.DOCK_LEFT);
    setUpdatedWhileClosed(true);
    FlixelDebugGrid header = new FlixelDebugGrid("2*", "3*").add(FlixelDebugUi.label("Sample rate"), rate);
    FlixelDebugStack content = FlixelDebugUi.column(
        header,
        new FlixelDebugExpander("Core", FlixelDebugUi.column(fps, frameMs, draws)),
        new FlixelDebugExpander("Render",
            FlixelDebugUi.column(cpuMs, presentMs, triangles, vertices, passes, submits, pipelines))
            .setExpanded(false),
        new FlixelDebugExpander("Memory", FlixelDebugUi.column(heapMb, nativeMb, textureMb, ringKb))
            .setExpanded(false),
        new FlixelDebugExpander("Network", FlixelDebugUi.column(sendKbps, recvKbps, pingMs)).setExpanded(false));
    setContent(new FlixelDebugScroll(content));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    long revision = stats.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    fps.push(stats.getFps());
    frameMs.push(stats.getFrameMs());
    draws.push(stats.getDraws());
    cpuMs.push(stats.getCpuMs());
    presentMs.push(stats.getPresentMs());
    triangles.push(stats.getTriangles());
    vertices.push(stats.getVertices());
    passes.push(stats.getPasses());
    submits.push(stats.getSubmits());
    pipelines.push(stats.getPipelines());
    heapMb.push(stats.getHeapMb());
    nativeMb.push(stats.getNativeMb());
    textureMb.push(stats.getTextureMb());
    ringKb.push(stats.getRingKb());
    sendKbps.push(stats.getSendKbps());
    recvKbps.push(stats.getRecvKbps());
    pingMs.push(Math.max(0f, stats.getRoundTripMs()));
    rate.setText(rateText(controls.getRate()));
  }

  private static String rateText(int hz) {
    // The rate only has 30 possible values, so their texts are made once instead of every sample.
    return RATE_TEXTS[Math.max(0, Math.min(RATE_TEXTS.length - 1, hz))];
  }

  private static FlixelDebugGraph graph(String title, String series, String unit) {
    return new FlixelDebugGraph(title).series(series, 0, unit).setLimit(HISTORY).setRange(0.0, Double.NaN)
        .setHeight(64f);
  }

  private static String[] buildRateTexts() {
    String[] texts = new String[FlixelDebugControls.MAX_RATE + 1];
    for (int i = 0; i < texts.length; i++) {
      texts[i] = i + " Hz";
    }
    return texts;
  }
}
