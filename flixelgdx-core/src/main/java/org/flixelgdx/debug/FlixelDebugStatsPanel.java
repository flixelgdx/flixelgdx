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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelRenderStats;
import org.flixelgdx.util.FlixelDebugUtil;
import org.jetbrains.annotations.NotNull;

/**
 * The performance panel of the remote debugger: frame rate, frame time, draw calls, memory, and the
 * camera.
 *
 * <p>Think of it as the dashboard of a car. Every so often (the sample rate, 20 times per second by
 * default) it glances at all the gauges at once and writes the numbers down. The debugger app draws the
 * graphs from those samples, so this panel only keeps the latest numbers plus three short histories
 * of {@value #HISTORY_SIZE} samples (frame rate, frame time, and Java heap) for tools that want them
 * without the app.
 *
 * <p>The sample rate lives on {@link FlixelDebugControlsPanel} and can be changed from the app or with
 * the {@code rate} console command. Sampling and writing never allocate.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugStatsPanel stats = Flixel.debug.stats;
 * Flixel.info("Draw calls last sample: {}", stats.getDraws());
 * }</pre>
 *
 * @see FlixelDebugControlsPanel#setRate(int)
 */
public class FlixelDebugStatsPanel extends FlixelDebugPanelBase {

  /** The number of samples kept in each history ring. */
  public static final int HISTORY_SIZE = 120;

  /** Seconds between object count refreshes, because counting walks the whole scene. */
  private static final float OBJECT_COUNT_INTERVAL = 0.5f;

  private static final float BYTES_PER_MB = 1024f * 1024f;

  private final long startNanos = System.nanoTime();

  private double elapsedSeconds;

  private final FlixelDebugControlsPanel controls;
  private final FlixelDebugManager owner;
  private final float[] fpsHistory = new float[HISTORY_SIZE];
  private final float[] frameMsHistory = new float[HISTORY_SIZE];
  private final float[] heapMbHistory = new float[HISTORY_SIZE];

  private float timer;
  private float objectTimer = OBJECT_COUNT_INTERVAL;
  private float liveFrameMs;
  private float fps;
  private float frameMs;
  private float cpuMs;
  private float presentMs;
  private float heapMb;
  private float nativeMb;
  private float textureMb;
  private float ringKb;
  private float sendKbps;
  private float recvKbps;
  private float camX;
  private float camY;
  private float camZoom = 1f;
  private int draws;
  private int triangles;
  private int vertices;
  private int passes;
  private int submits;
  private int pipelines;
  private int objectCount;
  private int historyHead;
  private int historyCount;

  private boolean paused;

  /**
   * Creates the stats panel.
   *
   * @param controls The controls panel that holds the sample rate.
   * @param owner The manager whose tracked batches add to the draw call count.
   */
  public FlixelDebugStatsPanel(@NotNull FlixelDebugControlsPanel controls, @NotNull FlixelDebugManager owner) {
    super("stats", "Stats", FlixelDebugProtocol.KIND_STATS);
    this.controls = controls;
    this.owner = owner;
  }

  @Override
  public void update(float elapsed) {
    liveFrameMs = elapsed * 1000f;
    objectTimer += elapsed;
    timer += elapsed;
    float interval = 1f / controls.getRate();
    if (timer >= interval) {
      timer -= interval;
      if (timer > interval) {
        timer = 0f;
      }
      sample();
    }
  }

  /**
   * Reads every statistic once and stores it as the latest sample.
   *
   * <p>{@link #update(float)} calls this at the sample rate. It is public so tools and tests can take a
   * sample on demand. It does not allocate.
   */
  public void sample() {
    elapsedSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
    fps = Flixel.graphics.getFps();
    frameMs = liveFrameMs;
    heapMb = Flixel.runtime.getJavaHeap() / BYTES_PER_MB;
    nativeMb = Flixel.runtime.getNativeHeap() / BYTES_PER_MB;

    FlixelRenderStats stats = Flixel.graphics.getStats();
    if (stats != null) {
      cpuMs = stats.cpuMs;
      presentMs = stats.presentMs;
      draws = stats.draws;
      triangles = stats.triangles;
      vertices = stats.vertices;
      passes = stats.passes;
      submits = stats.submits;
      pipelines = stats.pipelines;
      textureMb = stats.textureBytes / BYTES_PER_MB;
      ringKb = (stats.vertexBytes + stats.uniformBytes) / 1024f;
    } else {
      cpuMs = 0f;
      presentMs = 0f;
      draws = countRenderCalls();
      triangles = 0;
      vertices = 0;
      passes = 0;
      submits = 0;
      pipelines = 0;
      textureMb = 0f;
      ringKb = 0f;
    }

    if (objectTimer >= OBJECT_COUNT_INTERVAL) {
      objectTimer = 0f;
      objectCount = FlixelDebugUtil.countActiveMembers();
    }

    FlixelGame game = Flixel.game;
    paused = game != null && game.isGamePaused();

    if (Flixel.cameras.getSize() > 0) {
      FlixelCamera cam = Flixel.cameras.get(0);
      camX = cam.scrollX;
      camY = cam.scrollY;
      camZoom = cam.getZoom();
    }

    fpsHistory[historyHead] = fps;
    frameMsHistory[historyHead] = frameMs;
    heapMbHistory[historyHead] = heapMb;
    historyHead = (historyHead + 1) % HISTORY_SIZE;
    if (historyCount < HISTORY_SIZE) {
      historyCount++;
    }
    markChanged();
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeF32(fps)
        .writeF32(frameMs)
        .writeF32(cpuMs)
        .writeF32(presentMs)
        .writeI32(draws)
        .writeI32(triangles)
        .writeI32(vertices)
        .writeI32(passes)
        .writeI32(submits)
        .writeI32(pipelines)
        .writeF32(heapMb)
        .writeF32(nativeMb)
        .writeF32(textureMb)
        .writeF32(ringKb)
        .writeI32(objectCount)
        .writeF32(sendKbps)
        .writeF32(recvKbps)
        .writeBool(paused)
        .writeF64(elapsedSeconds)
        .writeF32(camX)
        .writeF32(camY)
        .writeF32(camZoom);
  }

  @Override
  public void reset() {
    timer = 0f;
    objectTimer = OBJECT_COUNT_INTERVAL;
    historyHead = 0;
    historyCount = 0;
    markChanged();
  }

  /**
   * Sums the render calls of the framework's batch and every batch registered with
   * {@link FlixelDebugManager#trackBatch(FlixelBatch)}.
   */
  private int countRenderCalls() {
    int total = 0;
    FlixelGame game = Flixel.game;
    if (game != null) {
      total += game.getFrameRenderCalls();
    }
    FlixelArray<FlixelBatch> extra = owner.getTrackedBatches();
    for (int i = 0, n = extra.getSize(); i < n; i++) {
      FlixelBatch batch = extra.get(i);
      if (batch != null) {
        total += batch.getRenderCalls();
      }
    }
    return total;
  }

  /**
   * Stores the transfer rates of the network agent so they show up in the next sample.
   *
   * @param sendKbps Kilobytes per second the agent sends.
   * @param recvKbps Kilobytes per second the agent receives.
   */
  public void setAgentRates(float sendKbps, float recvKbps) {
    this.sendKbps = sendKbps;
    this.recvKbps = recvKbps;
  }

  public float getFps() {
    return fps;
  }

  public float getFrameMs() {
    return frameMs;
  }

  public float getCpuMs() {
    return cpuMs;
  }

  public float getPresentMs() {
    return presentMs;
  }

  public int getDraws() {
    return draws;
  }

  public int getTriangles() {
    return triangles;
  }

  public int getVertices() {
    return vertices;
  }

  public int getPasses() {
    return passes;
  }

  public int getSubmits() {
    return submits;
  }

  public int getPipelines() {
    return pipelines;
  }

  public float getHeapMb() {
    return heapMb;
  }

  public float getNativeMb() {
    return nativeMb;
  }

  public float getTextureMb() {
    return textureMb;
  }

  public float getRingKb() {
    return ringKb;
  }

  public int getObjectCount() {
    return objectCount;
  }

  public float getSendKbps() {
    return sendKbps;
  }

  public float getRecvKbps() {
    return recvKbps;
  }

  public boolean isPaused() {
    return paused;
  }

  public double getElapsedSeconds() {
    return elapsedSeconds;
  }

  public float getCamX() {
    return camX;
  }

  public float getCamY() {
    return camY;
  }

  public float getCamZoom() {
    return camZoom;
  }

  /**
   * Returns the ring of frame rate samples. Read it starting at {@link #getHistoryStart()}.
   *
   * @return The live ring, {@value #HISTORY_SIZE} slots long.
   */
  @NotNull
  public float[] getFpsHistory() {
    return fpsHistory;
  }

  /**
   * Returns the ring of frame time samples in milliseconds. Read it starting at {@link #getHistoryStart()}.
   *
   * @return The live ring, {@value #HISTORY_SIZE} slots long.
   */
  @NotNull
  public float[] getFrameMsHistory() {
    return frameMsHistory;
  }

  /**
   * Returns the ring of Java heap samples in megabytes. Read it starting at {@link #getHistoryStart()}.
   *
   * @return The live ring, {@value #HISTORY_SIZE} slots long.
   */
  @NotNull
  public float[] getHeapMbHistory() {
    return heapMbHistory;
  }

  /**
   * Returns the slot where the next sample will be written.
   *
   * @return A ring index.
   */
  public int getHistoryHead() {
    return historyHead;
  }

  /**
   * Returns the slot of the oldest valid sample, which is where reading a history in order starts.
   *
   * @return A ring index.
   */
  public int getHistoryStart() {
    return historyCount < HISTORY_SIZE ? 0 : historyHead;
  }

  /**
   * Returns how many samples the history rings hold.
   *
   * @return A count from 0 to {@value #HISTORY_SIZE}.
   */
  public int getHistoryCount() {
    return historyCount;
  }
}
