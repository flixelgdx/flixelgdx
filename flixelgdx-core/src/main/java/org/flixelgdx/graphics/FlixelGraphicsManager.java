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

import java.util.Collections;
import java.util.List;

/**
 * The single public entry point for drawing and GPU state, reached through
 * {@link org.flixelgdx.Flixel#graphics Flixel.graphics}.
 *
 * <p>This is the only graphics surface game code touches. It owns the shared sprite batch, reports
 * timing and display information, and compiles shaders. The real GPU library lives behind the
 * internal {@link FlixelGraphicsBackend} seam and is never exposed here, so the same game code runs
 * unchanged no matter which backend is active.
 *
 * <p>A safe default is installed before startup, so {@code Flixel.graphics} is never {@code null};
 * on headless or not-yet-initialized sessions its methods simply do nothing and report neutral
 * values.
 *
 * <p>Every method has a safe default here so backends can implement only what a platform supports;
 * unsupported queries return neutral values and unsupported actions do nothing. Read
 * {@link #getBackendType()} to learn what is actually running.
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
 * @see FlixelBackendType
 */
public interface FlixelGraphicsManager {

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
   * @return Which graphics backend is running this session. Defaults to {@link FlixelBackendType#Noop}.
   */
  @NotNull
  default FlixelBackendType getBackendType() {
    return FlixelBackendType.Noop;
  }

  /**
   * Returns an opaque native handle to the underlying graphics library for the rare power user who
   * genuinely needs it.
   *
   * <p><b>Explicitly unsafe.</b> The meaning of this value depends entirely on
   * {@link #getBackendType()}, it may be {@code 0} on any backend, and reaching past the seam with
   * it voids the framework's cross-platform guarantees. Almost no game should call this.
   *
   * @return A backend-specific native handle, or {@code 0} when there is none.
   */
  default long getNativeHandle() {
    return 0L;
  }

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
