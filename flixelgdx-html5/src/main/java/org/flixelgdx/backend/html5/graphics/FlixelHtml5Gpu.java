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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.backend.gpu.FlixelGpuBootstrap;
import org.flixelgdx.backend.gpu.FlixelGpuGraphics;
import org.flixelgdx.backend.html5.FlixelHtml5Runner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.GpuSurface;
import io.github.stringdotjar.jwebgpu.GpuSurfaceDescriptor;
import io.github.stringdotjar.jwebgpu.JWebGpu;
import io.github.stringdotjar.jwebgpu.teavm.JWebGpuTeaVm;
import io.github.stringdotjar.jwebgpu.teavm.TeaVmSurfaceSourceCanvas;

/**
 * Opens the browser's WebGPU for a canvas and hands it to the shared renderer, without ever blocking.
 *
 * <p>A browser cannot wait: asking for an adapter and then a device are two promises that resolve on later turns
 * of the event loop, so the page's one thread must be given back in between. This class runs the whole startup as a
 * small state machine. {@link #start} installs the web backend, turns the canvas into a WebGPU surface (this is the
 * moment the canvas locks to the WebGPU context type, so nothing else may ask it for a context first), and begins
 * the adapter request. A timer then polls {@link FlixelGpuBootstrap#isDone()} until both requests have answered,
 * builds the renderer's GPU resources, and finally calls the listener.
 *
 * <p>Game developers never use this class. {@link FlixelHtml5Runner} calls it before the
 * assets are downloaded.
 *
 * <pre>{@code
 * FlixelHtml5Gpu gpu = new FlixelHtml5Gpu(graphics);
 * if (FlixelHtml5Gpu.isSupported()) {
 *   gpu.start(canvas, width, height, false, listener); // listener runs later, from a timer
 * }
 * }</pre>
 */
public final class FlixelHtml5Gpu {

  /** How often, in milliseconds, the startup checks whether the adapter and device have arrived. */
  private static final int POLL_MILLIS = 5;

  /** How long, in milliseconds, the browser may take to answer before startup is given up. */
  private static final double TIMEOUT_MILLIS = 20_000.0;

  private double deadline;

  private final FlixelGpuGraphics graphics;
  private final TimerHandler poller = this::poll;

  private GpuInstance instance;
  private GpuSurface surface;
  private FlixelGpuBootstrap bootstrap;
  private Listener listener;

  private int width;
  private int height;

  private boolean transparent;
  private boolean started;

  /**
   * Creates the starter for a renderer.
   *
   * @param graphics The renderer to initialize once the browser has handed out a device.
   */
  public FlixelHtml5Gpu(@NotNull FlixelGpuGraphics graphics) {
    this.graphics = graphics;
  }

  /**
   * Tells whether the browser offers WebGPU at all.
   *
   * @return {@code true} when {@code navigator.gpu} exists. The page must also be a secure context (HTTPS or
   *     localhost), which the browser enforces by hiding {@code navigator.gpu} otherwise.
   */
  public static boolean isSupported() {
    return JWebGpuTeaVm.isSupported();
  }

  /**
   * Starts opening the GPU. Call once, and only when {@link #isSupported()} is {@code true}.
   *
   * <p>Everything that can fail here (a canvas that already has another context, no adapter, a device that is
   * refused) is reported to {@link Listener#onFailed(String)} instead of being thrown, so the caller can show the
   * player a message.
   *
   * @param canvas The canvas to draw into. It must not have been given a 2D or WebGL context.
   * @param width The initial back buffer width in pixels.
   * @param height The initial back buffer height in pixels.
   * @param transparent Whether the page behind the canvas should show through where the game draws nothing.
   * @param listener Told when the renderer is ready, or why it could not be.
   */
  public void start(@NotNull HTMLCanvasElement canvas, int width, int height, boolean transparent,
      @NotNull Listener listener) {
    if (started) {
      return;
    }
    started = true;
    this.listener = listener;
    this.width = width;
    this.height = height;
    this.transparent = transparent;
    try {
      JWebGpuTeaVm.install();
      instance = JWebGpu.createInstance();
      GpuSurfaceDescriptor descriptor = new GpuSurfaceDescriptor();
      descriptor.nextInChain = TeaVmSurfaceSourceCanvas.ofElement(canvas);
      surface = instance.createSurface(descriptor);
      bootstrap = new FlixelGpuBootstrap(instance, surface);
      bootstrap.start();
    } catch (RuntimeException e) {
      fail("The canvas could not be used for WebGPU: " + e.getMessage());
      return;
    }
    deadline = nowMillis() + TIMEOUT_MILLIS;
    Window.setTimeout(poller, POLL_MILLIS);
  }

  private void poll() {
    if (!bootstrap.isDone()) {
      if (nowMillis() > deadline) {
        fail("The browser did not answer the request for a GPU in time.");
      } else {
        Window.setTimeout(poller, POLL_MILLIS);
      }
      return;
    }
    if (bootstrap.getDevice() == null || bootstrap.getAdapter() == null) {
      String error = bootstrap.getError();
      fail(error != null ? error : "No GPU device could be created.");
      return;
    }
    try {
      graphics.initialize(bootstrap.getAdapter(), bootstrap.getDevice(), surface, width, height, transparent,
          FlixelHtml5ImageDecoder.INSTANCE);
    } catch (RuntimeException e) {
      fail("The renderer could not be set up: " + e.getMessage());
      return;
    }
    listener.onReady();
  }

  private void fail(String message) {
    listener.onFailed(message);
  }

  @JSBody(script = "return (window.performance && window.performance.now) ? window.performance.now() : Date.now();")
  private static native double nowMillis();

  /**
   * Returns the WebGPU instance, which exists once {@link #start} has run.
   *
   * @return The instance, or {@code null} before startup or when it failed early.
   */
  @Nullable
  public GpuInstance getInstance() {
    return instance;
  }

  /**
   * Returns the surface of the canvas, which exists once {@link #start} has run.
   *
   * @return The surface, or {@code null} before startup or when it failed early.
   */
  @Nullable
  public GpuSurface getSurface() {
    return surface;
  }

  /**
   * Returns the renderer this starter initializes.
   *
   * @return The renderer.
   */
  @NotNull
  public FlixelGpuGraphics getGraphics() {
    return graphics;
  }

  /** Receives the outcome of {@link FlixelHtml5Gpu#start}. */
  public interface Listener {

    /** Called once, from a timer, after the renderer has been initialized and can draw. */
    void onReady();

    /**
     * Called once when the GPU could not be opened.
     *
     * @param message A technical description of what went wrong. The listener is expected to log it.
     */
    void onFailed(@NotNull String message);
  }
}
