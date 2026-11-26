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
package org.flixelgdx.debug.feeds;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.debug.DebugTestSupport;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.graphics.FlixelNoopGraphicsManager;
import org.flixelgdx.graphics.FlixelRenderStats;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugStatsTest {

  private FlixelDebugManager manager;
  private FlixelDebugStats stats;

  @BeforeEach
  void setUp() {
    manager = new FlixelDebugManager();
    stats = manager.stats;
  }

  @Test
  void writeEncodesEveryStatsFieldInSpecOrder() {
    FlixelRenderStats render = new FlixelRenderStats();
    render.cpuMs = 2.5f;
    render.presentMs = 1.25f;
    render.draws = 11;
    render.triangles = 22;
    render.vertices = 33;
    render.passes = 4;
    render.submits = 2;
    render.pipelines = 6;
    render.textureBytes = 3L * 1024 * 1024;
    render.vertexBytes = 2048;
    render.uniformBytes = 1024;
    Flixel.graphics = new StatsGraphics(render, 59);

    FlixelCamera camera = new FlixelCamera(200, 150);
    camera.scrollX = 12f;
    camera.scrollY = -3f;
    camera.setZoom(2f);
    Flixel.cameras.add(camera);

    stats.update(0.0125f);
    stats.setAgentRates(1.5f, 2.5f);
    stats.sample();

    FlixelDebugMessageReader r = DebugTestSupport.payload(stats);
    assertEquals(59f, r.readF32(), "fps");
    assertEquals(12.5f, r.readF32(), 1e-3f, "frameMs");
    assertEquals(2.5f, r.readF32(), "cpuMs");
    assertEquals(1.25f, r.readF32(), "presentMs");
    assertEquals(11, r.readI32(), "draws");
    assertEquals(22, r.readI32(), "triangles");
    assertEquals(33, r.readI32(), "vertices");
    assertEquals(4, r.readI32(), "passes");
    assertEquals(2, r.readI32(), "submits");
    assertEquals(6, r.readI32(), "pipelines");
    assertTrue(r.readF32() >= 0f, "heapMb");
    assertTrue(r.readF32() >= 0f, "nativeMb");
    assertEquals(3f, r.readF32(), "textureMb");
    assertEquals(3f, r.readF32(), "ringKb");
    assertTrue(r.readI32() >= 0, "objectCount");
    assertEquals(1.5f, r.readF32(), "sendKbps");
    assertEquals(2.5f, r.readF32(), "recvKbps");
    assertFalse(r.readBool(), "paused");
    assertTrue(r.readF64() >= 0.0, "elapsed");
    assertEquals(12f, r.readF32(), "camX");
    assertEquals(-3f, r.readF32(), "camY");
    assertEquals(2f, r.readF32(), "camZoom");
    assertEquals(0, r.getRemaining());
  }

  @Test
  void theSampleRateComesFromTheControlsPanel() {
    manager.controls.setRate(10);
    stats.update(0.04f);
    stats.update(0.04f);
    assertEquals(0, stats.getHistoryCount(), "0.08 s is shorter than the 0.1 s interval");
    stats.update(0.04f);
    assertEquals(1, stats.getHistoryCount());

    manager.controls.setRate(30);
    long revision = stats.getRevision();
    stats.update(0.034f);
    assertEquals(2, stats.getHistoryCount());
    assertTrue(stats.getRevision() > revision);
  }

  @Test
  void aSlowFrameTakesOneSampleInsteadOfABurst() {
    stats.update(5f);
    assertEquals(1, stats.getHistoryCount());
  }

  @Test
  void historyRingsHold120SamplesAndKeepTheNewestInOrder() {
    Flixel.graphics = new StatsGraphics(null, 0);
    manager.controls.setRate(30);
    for (int i = 0; i < 130; i++) {
      stats.update(0.04f + 0.001f * i);
    }
    assertEquals(FlixelDebugStats.HISTORY_SIZE, stats.getHistoryCount());
    int start = stats.getHistoryStart();
    assertEquals(stats.getHistoryHead(), start);
    float[] ms = stats.getFrameMsHistory();
    assertEquals(50f, ms[start], 1e-2f);
    assertEquals(169f, ms[(start + FlixelDebugStats.HISTORY_SIZE - 1) % FlixelDebugStats.HISTORY_SIZE],
        1e-2f);
  }

  @Test
  void withoutRendererStatsTheDrawsFallBackToTheBatchCount() {
    stats.sample();
    assertEquals(0, stats.getDraws());
    assertEquals(0f, stats.getCpuMs());
  }

  @Test
  void resetClearsTheHistory() {
    stats.sample();
    stats.reset();
    assertEquals(0, stats.getHistoryCount());
  }

  private static final class StatsGraphics implements FlixelGraphicsManager {

    private final FlixelGraphicsManager delegate = FlixelNoopGraphicsManager.INSTANCE;

    @Nullable
    private final FlixelRenderStats stats;

    private final int fps;

    StatsGraphics(@Nullable FlixelRenderStats stats, int fps) {
      this.stats = stats;
      this.fps = fps;
    }

    @Override
    public FlixelList<FlixelDisplayMode> getDisplayModes() {
      return delegate.getDisplayModes();
    }

    @Override
    public int getFps() {
      return fps;
    }

    @Override
    @Nullable
    public FlixelRenderStats getStats() {
      return stats;
    }
  }
}
