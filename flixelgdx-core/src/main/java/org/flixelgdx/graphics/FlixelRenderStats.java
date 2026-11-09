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
package org.flixelgdx.graphics;

/**
 * Per-frame counters a graphics backend keeps for the remote debugger.
 *
 * <p>The public fields describe the last <em>completed</em> frame, except the memory and cache sizes, which are
 * totals. All time values are CPU time spent recording and submitting commands; the GPU itself is not timed.
 *
 * <p>The draw, triangle, vertex, pass, and submit counts are double buffered. While a frame is being recorded, the
 * backend adds to hidden in-progress counters through {@link #addDraw(int, int)}, {@link #addPass()}, and
 * {@link #addSubmit()}. {@link #endFrame()} then copies them into the public fields in one step. Think of a
 * scoreboard that only changes when the inning ends: anyone who looks at it in the middle of an inning still sees
 * the full score of the last one, never a half-counted number. This matters because game logic (and the debugger)
 * runs between {@link #beginFrame()} and {@link #endFrame()}, when the in-progress counters are still near zero.
 *
 * <p>The values are overwritten in place, so reading them never allocates.
 *
 * <p>Example of a backend frame:
 *
 * <pre>{@code
 * stats.beginFrame();
 * stats.addPass();
 * stats.addDraw(2, 4); // One quad.
 * stats.addSubmit();
 * stats.endFrame(); // Now stats.draws == 1.
 * }</pre>
 */
public final class FlixelRenderStats {

  /** Bytes of texture memory currently allocated (an estimate: four bytes per pixel). */
  public long textureBytes;

  /** Bytes of vertex, index and uniform ring memory allocated. */
  public long ringBytes;

  /** Milliseconds spent between the start and the end of the last frame's command recording. */
  public float cpuMs;

  /** Milliseconds {@code present()} blocked, which is usually the wait for vertical sync. */
  public float presentMs;

  /** Draw calls issued in the last completed frame. */
  public int draws;

  /** Triangles drawn in the last completed frame. */
  public int triangles;

  /** Vertices uploaded in the last completed frame. */
  public int vertices;

  /** Render passes begun in the last completed frame. */
  public int passes;

  /** Command buffers submitted in the last completed frame; more than one means a ring ran full. */
  public int submits;

  /** Bytes of the vertex ring used in the last frame. */
  public int vertexBytes;

  /** Bytes of the uniform ring used in the last frame. */
  public int uniformBytes;

  /** The number of pipelines in the cache. */
  public int pipelines;

  /** The number of live textures. */
  public int textures;

  private int frameDraws;
  private int frameTriangles;
  private int frameVertices;
  private int framePasses;
  private int frameSubmits;

  /**
   * Clears the in-progress counters at the start of a frame.
   *
   * <p>The public fields keep the last completed frame's values until {@link #endFrame()} replaces them.
   */
  public void beginFrame() {
    frameDraws = 0;
    frameTriangles = 0;
    frameVertices = 0;
    framePasses = 0;
    frameSubmits = 0;
  }

  /**
   * Counts one draw call in the frame being recorded.
   *
   * @param triangles The triangles the draw call renders.
   * @param vertices The vertices the draw call uploads.
   */
  public void addDraw(int triangles, int vertices) {
    frameDraws++;
    frameTriangles += triangles;
    frameVertices += vertices;
  }

  /** Counts one render pass begun in the frame being recorded. */
  public void addPass() {
    framePasses++;
  }

  /** Counts one command buffer submitted in the frame being recorded. */
  public void addSubmit() {
    frameSubmits++;
  }

  /**
   * Publishes the frame that just finished recording, copying the in-progress counters into the public fields.
   *
   * <p>A backend that skips a frame (for example because the window was minimized) should not call this, so the
   * public fields keep showing the last frame that was really drawn.
   */
  public void endFrame() {
    draws = frameDraws;
    triangles = frameTriangles;
    vertices = frameVertices;
    passes = framePasses;
    submits = frameSubmits;
  }
}
