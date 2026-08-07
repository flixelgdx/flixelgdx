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
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * The graphics device: the one interface a graphics backend implements and the surface game code
 * draws through, reached from {@link org.flixelgdx.Flixel#graphics Flixel.graphics}.
 *
 * <p>Each backend (for example, bgfx on native, WebGPU or WebGL in the browser) implements this
 * interface, so the same game code runs unchanged no matter which one is active. The underlying GPU
 * library is never named in the public API; you only ever talk to this device.
 *
 * <p>The members fall into two groups. Most game code only uses the <b>high-level</b> ones: the
 * shared sprite batch, timing (frame rate, vertical sync), and display information (modes, density).
 * The <b>low-level</b> ones (frame boundaries, texture and mesh creation, shader compilation, the
 * native handle) are what a backend fills in and what the framework's own rendering drives; you can
 * reach for them to do advanced custom rendering, but typical games never need to.
 *
 * <p>A safe default is installed before startup, so {@code Flixel.graphics} is never {@code null};
 * on headless or not-yet-initialized sessions its methods simply do nothing and report neutral
 * values.
 *
 * <p>Every method has a safe default here so a backend can implement only what its platform
 * supports; unsupported queries return neutral values and unsupported actions do nothing. Read
 * {@link #getGraphicsApi()} to learn what is actually running.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelBatch batch = Flixel.graphics.getBatch();
 * Flixel.graphics.setVSync(true);
 * int fps = Flixel.graphics.getFps();
 * }</pre>
 *
 * @see org.flixelgdx.Flixel#graphics
 * @see FlixelGraphicsApi
 */
public interface FlixelGraphicsManager {

  /**
   * @return Which graphics backend is running this session. Defaults to {@link FlixelGraphicsApi#Noop}.
   */
  @NotNull
  default FlixelGraphicsApi getGraphicsApi() {
    return FlixelGraphicsApi.Noop;
  }

  /**
   * Returns the shared sprite batch every drawable in the framework renders through.
   *
   * @return The active batch, or {@code null} when no backend is present (headless or pre-startup).
   */
  @Nullable
  default FlixelBatch getBatch() {
    return null;
  }

  /**
   * Runs a task on the main render thread at a safe point.
   *
   * <p>Some work (loading callbacks, input events, network results) can arrive on a background
   * thread, but GPU calls must happen on the render thread. Pass such work here and the framework
   * runs it on the render thread before the next frame is drawn. On backends with a single thread,
   * it may run immediately.
   *
   * @param action The task to run on the render thread; ignored when {@code null}.
   */
  default void queueMainThread(@Nullable Runnable action) {}

  /**
   * Begins a new frame of rendering. Called once per frame by the framework before any draw work.
   */
  default void beginFrame() {}

  /**
   * Ends the current frame and presents it to the screen. Called once per frame by the framework
   * after all draw work.
   */
  default void endFrame() {}

  /**
   * Uploads pixel data to a new GPU texture.
   *
   * @param width Texture width in pixels.
   * @param height Texture height in pixels.
   * @param rgba Tightly packed 8-bit-per-channel RGBA pixels, row by row.
   * @return An opaque backend texture handle, or {@code 0} on failure or when no backend is present.
   */
  default long createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return 0L;
  }

  /**
   * Releases a texture previously returned by {@link #createTexture(int, int, ByteBuffer)}.
   *
   * @param handle The texture handle to free; ignored when {@code 0}.
   */
  default void destroyTexture(long handle) {}

  /**
   * Compiles a shader source bundle into a usable program on the active backend.
   *
   * @param source The per-backend shader variants (see {@link FlixelShaderSource}).
   * @return A compiled {@link FlixelShader}, or {@code null} when no backend is present. Check
   *     {@link FlixelShader#isValid()} before using the result.
   */
  @Nullable
  default FlixelShader compileShader(@NotNull FlixelShaderSource source) {
    return null;
  }

  /**
   * Allocates a reusable mesh backed by GPU buffers.
   *
   * @param layout Describes how each vertex is arranged.
   * @param maxVertices Maximum number of vertices the buffer must hold.
   * @param maxIndices Maximum number of indices the buffer must hold, or {@code 0} for no index buffer.
   * @param isStatic {@code true} to hint the data changes rarely, {@code false} for frequently updated data.
   * @return A new mesh, or {@code null} when no backend is present.
   */
  @Nullable
  default FlixelMesh createMesh(@NotNull FlixelVertexLayout layout, int maxVertices, int maxIndices, boolean isStatic) {
    return null;
  }

  /**
   * @return The number of frames rendered during the last second (the measured frame rate), or
   *     {@code 0} when unknown.
   */
  default int getFps() {
    return 0;
  }

  /**
   * @return The frame-rate cap in frames per second, or {@code 0} when the frame rate is uncapped.
   */
  default int getTargetFps() {
    return 0;
  }

  /**
   * Requests a frame-rate cap, where the backend supports one.
   *
   * @param fps Target frames per second, or {@code 0} to run uncapped.
   */
  default void setTargetFps(int fps) {}

  /**
   * @return {@code true} when the frame is synchronized to the display's refresh (vertical sync).
   */
  default boolean isVSyncEnabled() {
    return false;
  }

  /**
   * Turns vertical sync on or off, where the backend supports it.
   *
   * @param enabled {@code true} to synchronize presentation to the display refresh.
   */
  default void setVSync(boolean enabled) {}

  /**
   * @return The display mode the game is currently presented with, or {@code null} when the backend
   *     cannot report one (common on web and mobile).
   */
  @Nullable
  default FlixelDisplayMode getDisplayMode() {
    return null;
  }

  /**
   * Returns every video mode the current monitor can switch to, for building a resolution picker.
   *
   * <p>This is a desktop concept; other platforms return an empty list.
   *
   * @return An unmodifiable list of available display modes, possibly empty; never {@code null}.
   */
  @NotNull
  default List<FlixelDisplayMode> getDisplayModes() {
    return Collections.emptyList();
  }

  /**
   * Returns the pixel density of the display, useful for scaling UI on high-DPI screens.
   *
   * <p>A value of {@code 1.0} is the baseline (roughly 96 pixels per inch). A typical Retina or
   * high-DPI display reports around {@code 2.0}.
   *
   * @return The density scale factor, or {@code 1.0} when unknown.
   */
  default float getDensity() {
    return 1f;
  }

  /**
   * @return The display's pixels per inch, or {@code 0} when the backend cannot report it. Prefer
   *     {@link #getDensity()} for scaling; use this only when you need a physical measurement.
   */
  default float getPpi() {
    return 0f;
  }
}
