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
package org.flixelgdx.backend.gpu;

/**
 * Per-frame counters the renderer keeps for the debug overlay.
 *
 * <p>The values describe the last completed frame, except the memory and cache sizes, which are totals. The
 * renderer overwrites the fields in place, so reading them never allocates. All time values are CPU time spent
 * recording and submitting commands; the GPU itself is not timed.
 */
public final class FlixelGpuStats {

  /** Milliseconds spent between the start and the end of the last frame's command recording. */
  public float cpuMs;

  /** Milliseconds {@code present()} blocked, which is usually the wait for vertical sync. */
  public float presentMs;

  /** Bytes of texture memory currently allocated (an estimate: four bytes per pixel). */
  public long textureBytes;

  /** Bytes of vertex, index and uniform ring memory allocated. */
  public long ringBytes;

  /** Draw calls issued in the last frame. */
  public int draws;

  /** Triangles drawn in the last frame. */
  public int triangles;

  /** Vertices uploaded in the last frame. */
  public int vertices;

  /** Render passes begun in the last frame. */
  public int passes;

  /** Command buffers submitted in the last frame; more than one means a ring ran full. */
  public int submits;

  /** Bytes of the vertex ring used in the last frame. */
  public int vertexBytes;

  /** Bytes of the uniform ring used in the last frame. */
  public int uniformBytes;

  /** The number of pipelines in the cache. */
  public int pipelines;

  /** The number of live textures. */
  public int textures;

  /** Clears the per-frame counters at the start of a frame. */
  public void beginFrame() {
    draws = 0;
    triangles = 0;
    vertices = 0;
    passes = 0;
    submits = 0;
  }
}
