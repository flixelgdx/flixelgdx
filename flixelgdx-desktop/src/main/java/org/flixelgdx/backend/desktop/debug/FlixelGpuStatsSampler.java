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
package org.flixelgdx.backend.desktop.debug;

import org.flixelgdx.backend.gpu.FlixelGpuStats;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps a rolling history of the renderer's per-frame statistics so the overlay can plot them.
 *
 * <p>Each series is a fixed-size ring buffer in {@link FlixelDebugOverlay#PERF_HISTORY_SIZE} slots. Sampling
 * overwrites the oldest slot, so it never allocates. The overlay throttles how often {@link #sample} runs
 * (the overlay update rate), which keeps the cost of the debug panels low.
 */
public final class FlixelGpuStatsSampler {

  private static final int SIZE = FlixelDebugOverlay.PERF_HISTORY_SIZE;

  private static final float BYTES_PER_MB = 1024f * 1024f;

  private final float[] recordMs = new float[SIZE];
  private final float[] presentMs = new float[SIZE];
  private final float[] draws = new float[SIZE];
  private final float[] triangles = new float[SIZE];
  private final float[] ringKb = new float[SIZE];
  private final float[] textureMb = new float[SIZE];

  private int head;
  private int count;

  /**
   * Records one sample of the renderer's counters.
   *
   * @param stats The live stats object, or {@code null} to skip the sample.
   */
  public void sample(@Nullable FlixelGpuStats stats) {
    if (stats == null) {
      return;
    }
    int i = head;
    recordMs[i] = stats.cpuMs;
    presentMs[i] = stats.presentMs;
    draws[i] = stats.draws;
    triangles[i] = stats.triangles;
    ringKb[i] = (stats.vertexBytes + stats.uniformBytes) / 1024f;
    textureMb[i] = stats.textureBytes / BYTES_PER_MB;
    head = (i + 1) % SIZE;
    if (count < SIZE) {
      count++;
    }
  }

  /**
   * Returns the newest value of a series.
   *
   * @param ring One of the arrays returned by this sampler.
   * @return The latest sample, or {@code 0} when nothing has been sampled yet.
   */
  public float latest(float[] ring) {
    if (count == 0) {
      return 0f;
    }
    return ring[(head - 1 + SIZE) % SIZE];
  }

  /**
   * Returns the offset of the oldest sample, which is where plotting starts.
   *
   * @return The read offset.
   */
  public int getPlotOffset() {
    return count < SIZE ? 0 : head;
  }

  /**
   * Returns the number of valid samples.
   *
   * @return The sample count, up to the history size.
   */
  public int getCount() {
    return count;
  }

  /**
   * Returns the series of milliseconds spent recording and submitting each frame.
   *
   * @return The ring buffer.
   */
  public float[] getRecordMs() {
    return recordMs;
  }

  /**
   * Returns the series of milliseconds each frame spent presenting, which is mostly waiting for vertical sync.
   *
   * @return The ring buffer.
   */
  public float[] getPresentMs() {
    return presentMs;
  }

  /**
   * Returns the series of draw call counts.
   *
   * @return The ring buffer.
   */
  public float[] getDraws() {
    return draws;
  }

  /**
   * Returns the series of triangle counts.
   *
   * @return The ring buffer.
   */
  public float[] getTriangles() {
    return triangles;
  }

  /**
   * Returns the series of vertex and uniform ring buffer use, in kilobytes.
   *
   * @return The ring buffer.
   */
  public float[] getRingKb() {
    return ringKb;
  }

  /**
   * Returns the series of texture memory, in megabytes.
   *
   * @return The ring buffer.
   */
  public float[] getTextureMb() {
    return textureMb;
  }
}
