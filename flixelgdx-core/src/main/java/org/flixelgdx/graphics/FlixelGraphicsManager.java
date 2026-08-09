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

import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.functional.FlixelDrawable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * The graphics device: the one interface a graphics backend implements and the surface game code
 * draws through, reached from {@link org.flixelgdx.Flixel#graphics Flixel.graphics}.
 *
 * <p>Each backend (for example, bgfx on native, WebGPU or WebGL in the browser) implements this
 * interface, so the same game code runs unchanged no matter which one is active. The underlying GPU
 * library is never named in the public API; you only ever talk to this manager.
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
 * {@link #getApi()} to learn what is actually running.
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
  default FlixelGraphicsApi getApi() {
    return FlixelGraphicsApi.Noop;
  }

  /**
   * Returns the shared sprite batch every {@link FlixelDrawable} in the framework renders through.
   *
   * <p>When no backend is present (headless or pre-startup) this returns {@link FlixelUnsupportedBatch},
   * a no-op implementation whose operations do nothing. Check {@link #getApi()} if you need to know
   * whether a real GPU is available.
   *
   * @return The active batch; never {@code null}.
   */
  @NotNull
  default FlixelBatch getBatch() {
    return FlixelUnsupportedBatch.INSTANCE;
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
   * @return A texture handle; a size-only stand-in when no backend is present.
   */
  @NotNull
  default FlixelTexture createTexture(int width, int height, @NotNull ByteBuffer rgba) {
    return new FlixelNoopTexture(width, height);
  }

  /**
   * Uploads a CPU-side image to a new GPU texture.
   *
   * @param image The pixels to upload.
   * @return A texture handle; a size-only stand-in when no backend is present.
   */
  @NotNull
  default FlixelTexture createTexture(@NotNull FlixelImage image) {
    return createTexture(image.getWidth(), image.getHeight(), image.getPixels());
  }

  /**
   * Decodes an encoded image file (PNG, JPEG, and other common formats) into CPU-side pixels.
   *
   * <p>Decoding is a backend service because the codec differs per platform (stb on desktop, the
   * browser on web). Returns {@code null} when the data cannot be decoded or no backend is
   * present. Most games load images through the asset manager instead of calling this directly.
   *
   * @param encoded The raw bytes of the encoded file.
   * @return The decoded image, or {@code null} when decoding is unavailable or fails.
   */
  @Nullable
  default FlixelImage decodeImage(@NotNull ByteBuffer encoded) {
    return null;
  }

  /**
   * Creates an off-screen render target for post-processing passes.
   *
   * <p>When no backend is present this returns {@link FlixelUnsupportedRenderTarget}, whose
   * operations do nothing.
   *
   * @param width Target width in pixels.
   * @param height Target height in pixels.
   * @return A new render target; never {@code null}.
   */
  @NotNull
  default FlixelRenderTarget createRenderTarget(int width, int height) {
    return FlixelUnsupportedRenderTarget.INSTANCE;
  }

  /**
   * Clears the current draw surface (screen or active render target) to one color.
   *
   * @param r Red component in {@code [0, 1]}.
   * @param g Green component in {@code [0, 1]}.
   * @param b Blue component in {@code [0, 1]}.
   * @param a Alpha component in {@code [0, 1]}.
   */
  default void clear(float r, float g, float b, float a) {}

  /**
   * Restricts drawing to a rectangle of the draw surface, in framebuffer pixels measured from
   * the bottom-left corner. Used for sprite clip rectangles.
   *
   * @param x Left edge of the scissor rectangle.
   * @param y Bottom edge of the scissor rectangle.
   * @param width Scissor width; values below {@code 1} are clamped to {@code 1}.
   * @param height Scissor height; values below {@code 1} are clamped to {@code 1}.
   */
  default void setScissor(int x, int y, int width, int height) {}

  /** Removes the scissor rectangle so drawing covers the whole surface again. */
  default void clearScissor() {}

  /**
   * Sets the rectangle of the draw surface that rendering maps into, in framebuffer pixels
   * measured from the bottom-left corner. Cameras call this to place their viewport.
   *
   * @param x Left edge of the viewport.
   * @param y Bottom edge of the viewport.
   * @param width Viewport width.
   * @param height Viewport height.
   */
  default void setViewport(int x, int y, int width, int height) {}

  /**
   * @return The drawable surface width in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferWidth() {
    return 0;
  }

  /**
   * @return The drawable surface height in physical pixels, or {@code 0} when unknown.
   */
  default int getBackBufferHeight() {
    return 0;
  }

  /**
   * Forces the whole draw surface's alpha channel to fully opaque without touching its color
   * channels.
   *
   * <p>This is only meaningful on desktop backends that requested a transparent-capable
   * framebuffer: after drawing, the framework calls this so tinted sprites do not composite
   * through the real desktop. Every other backend leaves it a no-op.
   */
  default void forceOpaqueAlpha() {}

  /**
   * Compiles a shader source bundle into a usable program on the active backend.
   *
   * <p>When no backend is present (headless or pre-startup) this returns {@link FlixelUnsupportedShader},
   * a no-op implementation. Always check {@link FlixelShader#isValid()} before using the result to
   * distinguish a real compiled shader from this stub.
   *
   * @param source The per-backend shader variants (see {@link FlixelShaderSource}).
   * @return A compiled {@link FlixelShader}; never {@code null}.
   */
  @NotNull
  default FlixelShader compileShader(@NotNull FlixelShaderSource source) {
    return FlixelUnsupportedShader.INSTANCE;
  }

  /**
   * Allocates a reusable mesh backed by GPU buffers.
   *
   * <p>When no backend is present (headless or pre-startup) this returns {@link FlixelUnsupportedMesh},
   * a no-op implementation whose vertex and index counts are always {@code 0}. Check {@link #getApi()}
   * if you need to know whether a real GPU is available.
   *
   * @param layout Describes how each vertex is arranged.
   * @param maxVertices Maximum number of vertices the buffer must hold.
   * @param maxIndices Maximum number of indices the buffer must hold, or {@code 0} for no index buffer.
   * @param isStatic {@code true} to hint the data changes rarely, {@code false} for frequently updated data.
   * @return A new mesh; never {@code null}.
   */
  @NotNull
  default FlixelMesh createMesh(@NotNull FlixelVertexLayout layout, int maxVertices, int maxIndices, boolean isStatic) {
    return FlixelUnsupportedMesh.INSTANCE;
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
  FlixelList<FlixelDisplayMode> getDisplayModes();

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
