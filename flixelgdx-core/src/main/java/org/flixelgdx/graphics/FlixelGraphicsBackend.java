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

import java.nio.ByteBuffer;

/**
 * The internal seam where the real GPU library plugs in. <b>Not part of the public API.</b>
 *
 * <p>This is the single interface the framework swaps to change graphics libraries. One
 * implementation wraps bgfx (native), another the browser's WebGPU, another WebGL, and a
 * transitional one wraps libGDX during the migration. {@link FlixelGraphicsManager} talks to this
 * seam and nothing else, which is what keeps the backends interchangeable.
 *
 * <p><b>Game code must never touch this.</b> It deals in low-level GPU resources (textures, meshes,
 * shaders, frame boundaries) and exists only so the manager and the sprite batch have a uniform way
 * to reach the hardware. The public surface a game uses is {@link FlixelGraphicsManager} at
 * {@link org.flixelgdx.Flixel#graphics Flixel.graphics}.
 *
 * <p>This interface is expected to grow and change as the native and web backends are built out. It
 * is deliberately small for now: just enough to describe the frame lifecycle and the resources the
 * sprite batch needs. Treat its shape as provisional until Phase 3 lands a real backend.
 *
 * @see FlixelGraphicsManager
 * @see FlixelBackendType
 */
public interface FlixelGraphicsBackend {

  /**
   * @return Which concrete backend this is.
   */
  @NotNull
  FlixelBackendType getType();

  /**
   * @return An opaque native handle for power users who must reach the underlying library, or
   *     {@code 0} when there is none. Using it is explicitly unsafe and backend-specific.
   */
  long getNativeHandle();

  /**
   * Begins a new frame of rendering. Called once per frame before any draw work.
   */
  void beginFrame();

  /**
   * Ends the current frame and presents it to the screen. Called once per frame after all draw work.
   */
  void endFrame();

  /**
   * Uploads pixel data to a new GPU texture.
   *
   * @param width Texture width in pixels.
   * @param height Texture height in pixels.
   * @param rgba Tightly packed 8-bit-per-channel RGBA pixels, row by row.
   * @return An opaque backend texture handle, or {@code 0} on failure.
   */
  long createTexture(int width, int height, @NotNull ByteBuffer rgba);

  /**
   * Releases a texture previously returned by {@link #createTexture(int, int, ByteBuffer)}.
   *
   * @param handle The texture handle to free; ignored when {@code 0}.
   */
  void destroyTexture(long handle);

  /**
   * Compiles a shader source bundle into a usable program for this backend.
   *
   * @param source The per-backend shader variants; this backend reads only the variant it understands.
   * @return A compiled shader handle. Check {@link FlixelShader#isValid()} before using it.
   */
  @NotNull
  FlixelShader createShader(@NotNull FlixelShaderSource source);

  /**
   * Allocates a reusable mesh backed by GPU buffers.
   *
   * @param layout Describes how each vertex is arranged.
   * @param maxVertices Maximum number of vertices the buffer must hold.
   * @param maxIndices Maximum number of indices the buffer must hold, or {@code 0} for no index buffer.
   * @param isStatic {@code true} to hint the data changes rarely, {@code false} for frequently updated data.
   * @return A new mesh owned by this backend.
   */
  @NotNull
  FlixelMesh createMesh(@NotNull FlixelVertexLayout layout, int maxVertices, int maxIndices, boolean isStatic);
}
