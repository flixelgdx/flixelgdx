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

import org.jetbrains.annotations.NotNull;

/**
 * No-op {@link FlixelMesh} returned by
 * {@link FlixelGraphicsManager#createMesh(FlixelVertexLayout, int, int, boolean)} on headless and
 * pre-startup sessions.
 *
 * <p>Every operation does nothing and every query returns a safe neutral value so callers never
 * need to null-check the result of
 * {@link FlixelGraphicsManager#createMesh(FlixelVertexLayout, int, int, boolean)}.
 *
 * @see FlixelGraphicsManager#createMesh(FlixelVertexLayout, int, int, boolean)
 */
public enum FlixelUnsupportedMesh implements FlixelMesh {

  /** Shared no-op instance. */
  INSTANCE;

  private final FlixelVertexLayout layout = FlixelVertexLayout.builder().build();

  @Override
  public void destroy() {}

  @Override
  public void setVertices(@NotNull float @NotNull [] vertices, int offset, int count) {}

  @Override
  public void setIndices(@NotNull short @NotNull [] indices, int offset, int count) {}

  @Override
  public @NotNull FlixelVertexLayout getLayout() {
    return layout;
  }

  @Override
  public int getVertexCount() {
    return 0;
  }

  @Override
  public int getIndexCount() {
    return 0;
  }
}
