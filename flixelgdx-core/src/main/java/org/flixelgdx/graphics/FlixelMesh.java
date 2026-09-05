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

import org.flixelgdx.functional.FlixelDestroyable;
import org.jetbrains.annotations.NotNull;

/**
 * A reusable chunk of GPU geometry: a vertex buffer, an optional index buffer, and the layout that
 * describes them.
 *
 * <p>Most 2D games never touch this directly. The sprite batch handles the common case with its own
 * fast, throwaway per-frame buffers. A {@code FlixelMesh} is for geometry you keep around and draw
 * many times without rebuilding it every frame, such as a static tilemap chunk or a custom effect.
 *
 * <p>Each backend provides its own implementation (bgfx buffers, WebGPU buffers, or WebGL buffers).
 * Create one through the active graphics backend, fill it with {@link #setVertices(float[], int, int)}
 * and optionally {@link #setIndices(short[], int, int)}, and call {@link #destroy()} when you are
 * done so the backend can free the GPU memory.
 *
 * @see FlixelVertexLayout
 */
public interface FlixelMesh extends FlixelDestroyable {

  /**
   * Returns the layout describing how each vertex in this mesh is arranged.
   *
   * @return The vertex layout; never {@code null}.
   */
  @NotNull
  FlixelVertexLayout getLayout();

  /**
   * Uploads vertex data into the mesh's vertex buffer, replacing whatever was there.
   *
   * <p>The floats are interpreted according to {@link #getLayout()}.
   *
   * @param vertices Source array of packed vertex floats.
   * @param offset Index of the first float to upload.
   * @param count Number of floats to upload.
   */
  void setVertices(float @NotNull [] vertices, int offset, int count);

  /**
   * Uploads index data into the mesh's index buffer, replacing whatever was there.
   *
   * @param indices Source array of vertex indices.
   * @param offset Index of the first entry to upload.
   * @param count Number of entries to upload.
   */
  void setIndices(short @NotNull [] indices, int offset, int count);

  /**
   * Returns how many vertices this mesh currently holds.
   *
   * @return The number of vertices stored in the vertex buffer.
   */
  int getVertexCount();

  /**
   * Returns how many indices this mesh currently holds, or {@code 0} when it has no index buffer.
   *
   * @return The number of indices stored in the index buffer, or {@code 0} if none.
   */
  int getIndexCount();
}
