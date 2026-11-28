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
import io.github.stringdotjar.jwebgpu.GpuFeatureLevel;
import io.github.stringdotjar.jwebgpu.GpuFeatureName;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuLimits;
import io.github.stringdotjar.jwebgpu.GpuPowerPreference;
import io.github.stringdotjar.jwebgpu.GpuRequestAdapterOptions;
import io.github.stringdotjar.jwebgpu.GpuStatus;
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
 *
 * <p><b>Compatibility mode.</b> WebGPU has two feature levels: core (full WebGPU, on Vulkan, Metal and Direct3D 12)
 * and compatibility (a slightly smaller subset that also runs on OpenGL, OpenGL ES and Direct3D 11). OpenGL, OpenGL
 * ES and Direct3D 11 adapters exist only in compatibility mode, so when one of them is forced with
 * {@link #setBackendType(GpuBackendType)}, the adapter is requested at the compatibility level automatically (see
 * {@link #featureLevelOf(GpuBackendType)}). Think of it as asking for a camping stove instead of a full kitchen: it
 * cooks every recipe FlixelGDX needs, just with fewer burners.
 *
 * <p>When the adapter of a compatibility request can do core anyway (for example a Vulkan adapter), the device is
 * upgraded to core, unless {@link #setCoreUpgrade(boolean)} turned that off. Otherwise the device stays in
 * compatibility mode, and its texture size and uniform buffer limits are raised to what the adapter supports, because
 * the compatibility defaults (4096 pixel textures and 16 KiB uniform buffers) are lower than the core ones.
 * {@link #isCompatibilityMode()} tells which kind of device was created.
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
  private final GpuLimits supportedLimits = new GpuLimits();
  private final GpuLimits requiredLimits = new GpuLimits();

  private volatile GpuAdapter adapter;
  private volatile GpuDevice device;
  private volatile String error;

  private GpuDeviceLostCallback lostCallback;
  private GpuPowerPreference powerPreference = GpuPowerPreference.HIGH_PERFORMANCE;
  private GpuBackendType backendType = GpuBackendType.UNDEFINED;
  private GpuFeatureLevel featureLevel = GpuFeatureLevel.UNDEFINED;
  private GpuFeatureLevel requestedLevel = GpuFeatureLevel.CORE;

  private volatile boolean done;
  private volatile boolean compatibilityMode;

  private boolean compressionRequested = true;
  private boolean coreUpgrade = true;

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

  /**
   * Returns the feature level an adapter request needs for a backend type.
   *
   * <p>OpenGL, OpenGL ES and Direct3D 11 adapters exist only in compatibility mode, so they give
   * {@link GpuFeatureLevel#COMPATIBILITY}. Every other backend type, including the automatic choice, gives
   * {@link GpuFeatureLevel#CORE}.
   *
   * @param backendType The forced backend type, or {@code UNDEFINED} for the automatic choice.
   * @return The feature level to request.
   */
  @NotNull
  public static GpuFeatureLevel featureLevelOf(@NotNull GpuBackendType backendType) {
    return switch (backendType) {
      case OPENGL, OPENGLES, D3D11 -> GpuFeatureLevel.COMPATIBILITY;
      default -> GpuFeatureLevel.CORE;
    };
  }

  /** Starts the adapter request. Call once. */
  public void start() {
    requestedLevel = featureLevel == GpuFeatureLevel.UNDEFINED ? featureLevelOf(backendType) : featureLevel;
    GpuRequestAdapterOptions options = new GpuRequestAdapterOptions();
    options.compatibleSurface = surface;
    options.powerPreference = powerPreference;
    options.backendType = backendType;
    options.featureLevel = requestedLevel;
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
    // One extra slot for the core upgrade of a compatibility adapter.
    GpuDeviceDescriptor descriptor = new GpuDeviceDescriptor(COMPRESSION_FEATURES.length + 1);
    descriptor.label = "FlixelGDX device";
    if (requestedLevel == GpuFeatureLevel.COMPATIBILITY) {
      if (coreUpgrade && chosen.hasFeature(GpuFeatureName.CORE_FEATURES_AND_LIMITS)) {
        descriptor.requiredFeatures[descriptor.requiredFeatureCount++] = GpuFeatureName.CORE_FEATURES_AND_LIMITS;
      } else {
        compatibilityMode = true;
        descriptor.requiredLimits = raiseCompatibilityLimits(chosen);
      }
    }
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

  /**
   * Fills the required limits of a compatibility device with the adapter's texture size and uniform buffer limits.
   *
   * <p>A compatibility device starts with lower defaults than a core one (4096 pixel textures instead of 8192, and
   * 16 KiB uniform buffer bindings instead of 64 KiB), which would reject large texture atlases and custom shaders
   * with many uniforms. Most compatibility hardware supports more, so the device asks for what the adapter offers.
   *
   * @param chosen The adapter.
   * @return The limits to require, or {@code null} to keep the defaults when the adapter's limits cannot be read.
   */
  @Nullable
  private GpuLimits raiseCompatibilityLimits(GpuAdapter chosen) {
    supportedLimits.reset();
    if (chosen.getLimits(supportedLimits) != GpuStatus.SUCCESS) {
      return null;
    }
    requiredLimits.reset();
    requiredLimits.maxTextureDimension2D = supportedLimits.maxTextureDimension2D;
    requiredLimits.maxUniformBufferBindingSize = supportedLimits.maxUniformBufferBindingSize;
    LOG.debug("Using WebGPU compatibility mode (textures up to {} pixels, uniform buffers up to {} bytes).",
        supportedLimits.maxTextureDimension2D, supportedLimits.maxUniformBufferBindingSize);
    return requiredLimits;
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
   * Returns whether the device runs in compatibility mode instead of full WebGPU.
   *
   * <p>A compatibility device has a few extra validation rules and lower limits (see the class documentation). It is
   * only created when the adapter was requested at the compatibility level and the core upgrade did not happen.
   *
   * @return {@code true} when the device is limited to compatibility mode. Meaningful once {@link #getDevice()}
   *     returns a device.
   */
  public boolean isCompatibilityMode() {
    return compatibilityMode;
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
   * Returns the feature level forced for the adapter request.
   *
   * @return The feature level, {@code UNDEFINED} for automatic.
   */
  @NotNull
  public GpuFeatureLevel getFeatureLevel() {
    return featureLevel;
  }

  /**
   * Forces the feature level of the adapter request. Takes effect only if called before {@link #start()}.
   *
   * <p>Left at {@code UNDEFINED}, the level follows the backend type (see {@link #featureLevelOf(GpuBackendType)}).
   * Forcing {@link GpuFeatureLevel#COMPATIBILITY} together with {@code setCoreUpgrade(false)} gives a compatibility
   * device on any hardware, which is the easiest way to check that a game runs within compatibility mode.
   *
   * @param featureLevel The feature level, or {@code UNDEFINED} to choose it from the backend type.
   */
  public void setFeatureLevel(@NotNull GpuFeatureLevel featureLevel) {
    this.featureLevel = featureLevel;
  }

  /**
   * Returns whether a compatibility adapter that can do core gets a core device.
   *
   * @return {@code true} by default.
   */
  public boolean isCoreUpgrade() {
    return coreUpgrade;
  }

  /**
   * Sets whether a compatibility adapter that can do core gets a core device. Takes effect only if called before
   * {@link #start()}.
   *
   * <p>The upgrade is on by default, because a core device has every feature and higher limits. Turn it off to keep
   * the device in compatibility mode, for example to test a game against the compatibility rules on a modern GPU.
   *
   * @param coreUpgrade Whether to upgrade to core when the adapter allows it.
   */
  public void setCoreUpgrade(boolean coreUpgrade) {
    this.coreUpgrade = coreUpgrade;
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
