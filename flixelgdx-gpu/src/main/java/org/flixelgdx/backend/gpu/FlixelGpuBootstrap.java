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

import org.flixelgdx.Flixel;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuAdapter;
import io.github.stringdotjar.jwebgpu.GpuBackendType;
import io.github.stringdotjar.jwebgpu.GpuCallbackMode;
import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuDeviceDescriptor;
import io.github.stringdotjar.jwebgpu.GpuDeviceLostCallback;
import io.github.stringdotjar.jwebgpu.GpuDeviceLostReason;
import io.github.stringdotjar.jwebgpu.GpuFeatureName;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuPowerPreference;
import io.github.stringdotjar.jwebgpu.GpuRequestAdapterOptions;
import io.github.stringdotjar.jwebgpu.GpuSurface;

/**
 * Asks the GPU for an adapter and then a device, step by step, without ever blocking.
 *
 * <p>Both requests are asynchronous in WebGPU, because the browser cannot block. This class runs the two requests
 * one after the other and exposes the outcome through {@link #isDone()}. A platform that can block (the desktop)
 * simply pumps {@code instance.processEvents()} until {@link #isDone()} is true; a platform that cannot (the web)
 * checks it once per frame and starts the game when it flips.
 *
 * <pre>{@code
 * FlixelGpuBootstrap boot = new FlixelGpuBootstrap(instance, surface);
 * boot.start();
 * while (!boot.isDone()) {
 *   instance.processEvents();
 * }
 * GpuDevice device = boot.getDevice(); // null when boot.getError() is set
 * }</pre>
 *
 * <p>The device is created with every texture compression feature (BC, ETC2 and ASTC) the adapter offers, so
 * compressed KTX2 textures can use them. Turning a feature on costs nothing when it goes unused.
 */
public final class FlixelGpuBootstrap {

  private static final FlixelLogger LOG = Flixel.log.tagged("Graphics/Device");
  private static final GpuFeatureName[] COMPRESSION_FEATURES = {
      GpuFeatureName.TEXTURE_COMPRESSION_BC,
      GpuFeatureName.TEXTURE_COMPRESSION_ETC2,
      GpuFeatureName.TEXTURE_COMPRESSION_ASTC
  };

  private final GpuInstance instance;
  private final GpuSurface surface;

  private volatile GpuAdapter adapter;
  private volatile GpuDevice device;
  private volatile String error;

  private GpuDeviceLostCallback lostCallback;
  private GpuPowerPreference powerPreference = GpuPowerPreference.HIGH_PERFORMANCE;
  private GpuBackendType backendType = GpuBackendType.UNDEFINED;

  private volatile boolean done;

  private boolean compressionRequested = true;

  /**
   * Creates a bootstrap for an instance.
   *
   * @param instance The WebGPU instance.
   * @param surface The surface the adapter must be able to present to, or {@code null} for off-screen use.
   */
  public FlixelGpuBootstrap(@NotNull GpuInstance instance, @Nullable GpuSurface surface) {
    this.instance = instance;
    this.surface = surface;
  }

  /** Starts the adapter request. Call once. */
  public void start() {
    GpuRequestAdapterOptions options = new GpuRequestAdapterOptions();
    options.compatibleSurface = surface;
    options.powerPreference = powerPreference;
    options.backendType = backendType;
    instance.requestAdapter(options, GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, result, message) -> {
      if (result == null) {
        fail("No compatible GPU adapter was found (" + status + "): " + message);
        return;
      }
      adapter = result;
      requestDevice(result);
    });
  }

  private void requestDevice(GpuAdapter chosen) {
    GpuDeviceDescriptor descriptor = new GpuDeviceDescriptor(COMPRESSION_FEATURES.length);
    descriptor.label = "FlixelGDX device";
    if (compressionRequested) {
      for (int i = 0; i < COMPRESSION_FEATURES.length; i++) {
        if (chosen.hasFeature(COMPRESSION_FEATURES[i])) {
          descriptor.requiredFeatures[descriptor.requiredFeatureCount++] = COMPRESSION_FEATURES[i];
        }
      }
    }
    descriptor.uncapturedErrorCallback = (dev, type, message) -> LOG.error("WebGPU error ({}): {}", type, message);
    descriptor.deviceLostCallback = this::onLost;
    descriptor.deviceLostMode = GpuCallbackMode.ALLOW_PROCESS_EVENTS;
    chosen.requestDevice(descriptor, GpuCallbackMode.ALLOW_PROCESS_EVENTS, (status, result, message) -> {
      if (result == null) {
        fail("The GPU device could not be created (" + status + "): " + message);
        return;
      }
      device = result;
      done = true;
    });
  }

  private void onLost(GpuDevice lostDevice, GpuDeviceLostReason reason, String message) {
    if (reason == GpuDeviceLostReason.DESTROYED) {
      // A destroyed device is the normal result of shutting down, so it only needs a debug line.
      LOG.debug("The GPU device was destroyed ({}): {}", reason, message);
    } else {
      LOG.error("The GPU device was lost ({}): {}", reason, message);
    }
    GpuDeviceLostCallback callback = lostCallback;
    if (callback != null) {
      callback.onDeviceLost(lostDevice, reason, message);
    }
  }

  private void fail(String message) {
    error = message;
    done = true;
  }

  /**
   * Returns whether both requests have finished, successfully or not.
   *
   * @return {@code true} when {@link #getDevice()} or {@link #getError()} can be read.
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Returns the adapter that was chosen.
   *
   * @return The adapter, or {@code null} if the request has not succeeded.
   */
  @Nullable
  public GpuAdapter getAdapter() {
    return adapter;
  }

  /**
   * Returns the device that was created.
   *
   * @return The device, or {@code null} if the request has not finished or failed.
   */
  @Nullable
  public GpuDevice getDevice() {
    return device;
  }

  /**
   * Returns why the bootstrap failed.
   *
   * @return A message for the log, or {@code null} when nothing failed.
   */
  @Nullable
  public String getError() {
    return error;
  }

  /**
   * Returns the power preference used when choosing an adapter.
   *
   * @return The preference.
   */
  @NotNull
  public GpuPowerPreference getPowerPreference() {
    return powerPreference;
  }

  /**
   * Sets the power preference. Takes effect only if called before {@link #start()}.
   *
   * @param powerPreference The preference.
   */
  public void setPowerPreference(@NotNull GpuPowerPreference powerPreference) {
    this.powerPreference = powerPreference;
  }

  /**
   * Returns the backend type that is forced when choosing an adapter.
   *
   * @return The backend type, {@code UNDEFINED} for automatic.
   */
  @NotNull
  public GpuBackendType getBackendType() {
    return backendType;
  }

  /**
   * Forces a native graphics API (for example Vulkan or the null backend). Takes effect only if called before
   * {@link #start()}.
   *
   * @param backendType The type, or {@code UNDEFINED} to let the implementation choose.
   */
  public void setBackendType(@NotNull GpuBackendType backendType) {
    this.backendType = backendType;
  }

  /**
   * Returns whether the device asks for the adapter's texture compression features.
   *
   * @return {@code true} by default.
   */
  public boolean isCompressionRequested() {
    return compressionRequested;
  }

  /**
   * Sets whether the device asks for the adapter's texture compression features (BC, ETC2 and ASTC). Without them,
   * compressed KTX2 textures fall back to RGBA8 or fail to load. Takes effect only if called before {@link #start()}.
   *
   * @param compressionRequested Whether to request the features.
   */
  public void setCompressionRequested(boolean compressionRequested) {
    this.compressionRequested = compressionRequested;
  }

  /**
   * Sets a callback for when the device is lost (a GPU reset, a driver crash, or the device being destroyed on
   * purpose). Takes effect only if called before {@link #start()}.
   *
   * <p>The loss reason and message are always logged (an error, or a debug line when the device was destroyed on
   * purpose, such as during shutdown), whether or not a callback is set.
   *
   * <p>With the mode used here, the callback runs from {@code instance.processEvents()}, on the thread that calls
   * it. A platform that wants to survive a lost device checks a flag set by this callback once per frame.
   *
   * @param callback The callback, or {@code null} for none.
   */
  public void setDeviceLostCallback(@Nullable GpuDeviceLostCallback callback) {
    this.lostCallback = callback;
  }
}
