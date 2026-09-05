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

import org.flixelgdx.debug.FlixelDebugOverlay;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.bgfx.BGFXStats;

/**
 * Samples bgfx's per-frame render statistics into rolling ring buffers for the debug overlay.
 *
 * <p>bgfx tracks a lot of numbers each frame (CPU and GPU frame timings, how long each thread waited
 * on the other, draw-call counts, transient buffer usage, and VRAM totals). The ones that change
 * every frame are worth plotting over time, so this class keeps a fixed-size history of each and lets
 * the overlay draw them as graphs. The remaining one-off counters (resource counts, peak draw calls,
 * and so on) do not need a history and are read straight off the live {@link BGFXStats} by the panel.
 *
 * <p>bgfx reports CPU and GPU times in raw timer ticks against separate frequencies, so this class
 * also converts them to milliseconds and exposes the two conversion factors it computed, so the panel
 * can format the instantaneous timing values with the same math.
 *
 * <p>Every buffer is a primitive {@code float[]} sized to {@link FlixelDebugOverlay#PERF_HISTORY_SIZE},
 * and {@link #sample(BGFXStats)} reads from the graphics manager's cached stats object, so collecting
 * a sample never allocates.
 */
public final class FlixelBgfxStatsSampler {

  /** History length of each ring, matched to the core performance graphs for a consistent time window. */
  private static final int SIZE = FlixelDebugOverlay.PERF_HISTORY_SIZE;

  /** Bytes in one megabyte, used to convert bgfx's byte counters to the megabytes the panel shows. */
  private static final float BYTES_PER_MB = 1024f * 1024f;

  private double cpuToMs;
  private double gpuToMs;

  private final float[] cpuFrameMs = new float[SIZE];
  private final float[] cpuSubmitMs = new float[SIZE];
  private final float[] gpuMs = new float[SIZE];
  private final float[] waitSubmitMs = new float[SIZE];
  private final float[] waitRenderMs = new float[SIZE];
  private final float[] drawCalls = new float[SIZE];
  private final float[] gpuMemoryMb = new float[SIZE];

  private int head;
  private int count;

  /**
   * Appends one frame of bgfx stats to every ring buffer.
   *
   * <p>A {@code null} argument (bgfx not initialized yet) is ignored so the caller does not have to
   * null-check before sampling.
   *
   * @param stats The live, cached bgfx stats for the just-completed frame, or {@code null}.
   */
  public void sample(@Nullable BGFXStats stats) {
    if (stats == null) {
      return;
    }
    // Each timer runs at its own frequency; a GPU frequency of zero means the backend cannot time the
    // GPU, in which case the GPU series stays flat at zero rather than dividing by zero.
    cpuToMs = stats.cpuTimerFreq() > 0 ? 1000.0 / stats.cpuTimerFreq() : 0.0;
    gpuToMs = stats.gpuTimerFreq() > 0 ? 1000.0 / stats.gpuTimerFreq() : 0.0;

    int i = head;
    cpuFrameMs[i] = (float) (stats.cpuTimeFrame() * cpuToMs);
    cpuSubmitMs[i] = (float) ((stats.cpuTimeEnd() - stats.cpuTimeBegin()) * cpuToMs);
    gpuMs[i] = (float) ((stats.gpuTimeEnd() - stats.gpuTimeBegin()) * gpuToMs);
    waitSubmitMs[i] = (float) (stats.waitSubmit() * cpuToMs);
    waitRenderMs[i] = (float) (stats.waitRender() * cpuToMs);
    drawCalls[i] = stats.numDraw();
    gpuMemoryMb[i] = stats.gpuMemoryUsed() < 0 ? 0f : stats.gpuMemoryUsed() / BYTES_PER_MB;

    head = (i + 1) % SIZE;
    if (count < SIZE) {
      count++;
    }
  }

  /**
   * Returns the most recent value written into {@code ring}, accounting for wraparound, or {@code 0}
   * when no samples have been collected yet.
   *
   * @param ring One of this sampler's ring buffers.
   * @return The latest sample in {@code ring}.
   */
  public float latest(float[] ring) {
    if (count == 0) {
      return 0f;
    }
    int last = (head - 1 + SIZE) % SIZE;
    return ring[last];
  }

  /**
   * The read offset into each ring for a plot covering {@code count} samples.
   *
   * <p>While the ring is still filling, the oldest sample is at index {@code 0}; once it is full the
   * oldest sample is at {@link #getHead()} (the next write position). Graph widgets that read a fixed
   * number of samples with wraparound use this to start at the oldest one.
   *
   * @return The index of the oldest valid sample.
   */
  public int getPlotOffset() {
    return count < SIZE ? 0 : head;
  }

  public int getHead() {
    return head;
  }

  public int getCount() {
    return count;
  }

  /**
   * Milliseconds per CPU timer tick from the most recent {@link #sample(BGFXStats)}.
   *
   * @return The milliseconds per CPU timer tick.
   */
  public double getCpuToMs() {
    return cpuToMs;
  }

  /**
   * Milliseconds per GPU timer tick from the most recent {@link #sample(BGFXStats)}, or {@code 0} if untimed.
   *
   * @return The milliseconds per GPU timer tick, or {@code 0} if GPU timing is unavailable.
   */
  public double getGpuToMs() {
    return gpuToMs;
  }

  public float[] getCpuFrameMs() {
    return cpuFrameMs;
  }

  public float[] getCpuSubmitMs() {
    return cpuSubmitMs;
  }

  public float[] getGpuMs() {
    return gpuMs;
  }

  public float[] getWaitSubmitMs() {
    return waitSubmitMs;
  }

  public float[] getWaitRenderMs() {
    return waitRenderMs;
  }

  public float[] getDrawCalls() {
    return drawCalls;
  }

  public float[] getGpuMemoryMb() {
    return gpuMemoryMb;
  }
}
