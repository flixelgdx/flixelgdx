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
package org.flixelgdx.backend.android;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelCrashHandler;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.android.graphics.FlixelAndroidGraphics;
import org.flixelgdx.backend.android.graphics.FlixelAndroidImageDecoder;
import org.flixelgdx.backend.gpu.FlixelGpuBootstrap;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.stringdotjar.jwebgpu.GpuDevice;
import io.github.stringdotjar.jwebgpu.GpuDeviceLostReason;
import io.github.stringdotjar.jwebgpu.GpuInstance;
import io.github.stringdotjar.jwebgpu.JWebGpu;
import io.github.stringdotjar.jwebgpu.jni.android.AndroidSurface;

import java.util.concurrent.locks.LockSupport;

import android.os.Build;
import android.os.Process;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * The Android game loop: a dedicated render thread that owns the GPU and runs the game, fed by the surface and
 * activity callbacks that arrive on the UI thread.
 *
 * <p>Android can create and destroy the game's surface many times while the app lives (every time the app goes to
 * the background and comes back), and the GPU device does not need to die with it. This class keeps the two apart,
 * the way a TV and a game console are separate: the console (the WebGPU device, the textures, the game) keeps
 * running its memory while the TV (the surface) is unplugged and plugged back in.
 *
 * <h2>Threads</h2>
 *
 * <ul>
 *   <li>The <b>UI thread</b> calls {@link #surfaceCreated}, {@link #surfaceChanged}, {@link #surfaceDestroyed},
 *       {@link #pause()}, {@link #resume()} and {@link #destroy()}. These only write a few flags under a lock and wake
 *       the render thread; they never touch the GPU or the game.</li>
 *   <li>The <b>render thread</b> (named {@code flixel-render}) creates the WebGPU instance, the device and the
 *       surface, calls {@link FlixelGame#create()}, and then runs input draining, the game update and the draw every
 *       frame. Game code, input handling and all GPU calls therefore happen on this one thread, which is the same
 *       threading model the framework had when it drew through a {@code GLSurfaceView}.</li>
 * </ul>
 *
 * <h2>Surface lifecycle</h2>
 *
 * <ul>
 *   <li><b>Created / changed:</b> the first surface starts the GPU (the adapter must be able to present to it).
 *       Later surfaces only need a new WebGPU surface, which is attached to the existing renderer.</li>
 *   <li><b>Size change (rotation, split screen):</b> the renderer reconfigures the surface and the game gets
 *       {@link FlixelGame#resize(int, int)}.</li>
 *   <li><b>Destroyed:</b> {@link #surfaceDestroyed} does not return until the render thread has unconfigured and
 *       released the WebGPU surface, because Android forbids touching a surface after that callback returns.
 *       Rendering is idle until the next surface. Textures, shaders and game state are kept.</li>
 *   <li><b>Device lost:</b> if the GPU driver resets, the renderer is rebuilt on a new device as soon as a surface
 *       is available. The game keeps running, but textures that lived on the lost device have to be loaded
 *       again.</li>
 * </ul>
 *
 * <p>Frame delta is measured in nanoseconds with {@link System#nanoTime()} and clamped to 1/15 s so a slow frame
 * cannot cause a "spiral of death". Each frame is wrapped in a try/catch that forwards unhandled exceptions to the
 * framework crash handler.
 */
public class FlixelAndroidRunner implements FlixelGameRunner, SurfaceHolder.Callback {

  private static final FlixelLogger LOG = Flixel.log.tagged("Android");

  private static final long BOOT_TIMEOUT_NANOS = 20_000_000_000L;
  private static final long DETACH_TIMEOUT_MILLIS = 3000L;
  private static final long IDLE_FRAME_NANOS = 4_000_000L;
  private static final long SPIN_MARGIN_NANOS = 500_000L;

  /** Maximum delta clamped to avoid physics/game logic explosions after very slow frames. */
  private static final float MAX_DELTA = 1f / 15f;

  private static final int WORK_QUIT = 0;
  private static final int WORK_FOCUS_LOST = 1;
  private static final int WORK_FOCUS_GAINED = 2;
  private static final int WORK_DETACH = 3;
  private static final int WORK_ATTACH = 4;
  private static final int WORK_RESIZE = 5;
  private static final int WORK_FRAME = 6;

  private long lastNanos;

  /** Absolute {@link System#nanoTime()} target the current frame should not finish before. */
  private long frameDeadlineNanos;

  private final Object lock = new Object();

  @NotNull
  private final FlixelAndroidWindow window;

  @NotNull
  private final FlixelAndroidInputDevice input;

  @NotNull
  private final FlixelAndroidGraphics graphics;

  @Nullable
  private FlixelGame game;

  @Nullable
  private FlixelCrashHandler crashHandler;

  @Nullable
  private Thread thread;

  @Nullable
  private GpuInstance instance;

  @Nullable
  private AndroidSurface target;

  /** The Android surface the UI thread last reported. Guarded by {@link #lock}. */
  @Nullable
  private Surface surface;

  /** The surface size the UI thread last reported. Guarded by {@link #lock}. */
  private int surfaceWidth;
  private int surfaceHeight;

  private int width;
  private int height;

  /** Set by the UI thread when a surface appears or changes size. Guarded by {@link #lock}. */
  private boolean surfaceDirty;

  /** Set by the UI thread when the surface is gone and not yet released. Guarded by {@link #lock}. */
  private boolean detachPending;

  /** Whether the activity is in the foreground. Guarded by {@link #lock}. */
  private boolean resumed = true;

  private boolean focusLostPending;
  private boolean focusGainedPending;
  private boolean focusLost;
  private boolean quit;

  /** Set after a crash so the loop stops calling into a game that is in a broken state. */
  private boolean crashed;

  /** Whether the GPU device exists. Only the render thread touches it. */
  private boolean deviceReady;

  /** Whether {@link FlixelGame#create()} has run. Only the render thread touches it. */
  private boolean gameCreated;

  /** Set while the renderer is disposed on purpose, so the resulting device loss is not treated as an error. */
  private volatile boolean disposing;

  /** Set from the device lost callback. Read by the render thread once per frame. */
  private volatile boolean deviceLost;

  /**
   * Creates a runner wired to the given window, input device and graphics manager.
   *
   * @param window The Android window implementation.
   * @param input The Android input device.
   * @param graphics The Android graphics manager that renders through WebGPU.
   */
  public FlixelAndroidRunner(@NotNull FlixelAndroidWindow window, @NotNull FlixelAndroidInputDevice input,
      @NotNull FlixelAndroidGraphics graphics) {
    this.window = window;
    this.input = input;
    this.graphics = graphics;
  }

  /**
   * Stores the game and starts the render thread, which then waits for the first surface.
   *
   * @param game The game to drive.
   */
  @Override
  public void run(@NotNull FlixelGame game) {
    FlixelConfig config = Flixel.config;
    graphics.setVSync(config.isVsync());
    graphics.setTargetFps(config.getFramerate());
    this.game = game;
    Thread t = new Thread(this::renderLoop, "flixel-render");
    synchronized (lock) {
      thread = t;
    }
    t.start();
  }

  /**
   * Stores the crash handler so render thread exceptions can be forwarded. Called by the launcher after
   * {@link Flixel#start} has wired the runtime.
   *
   * @param handler The handler installed by the framework.
   */
  public void setCrashHandler(@Nullable FlixelCrashHandler handler) {
    this.crashHandler = handler;
  }

  /**
   * Called on the UI thread when Android creates the surface. The size follows in {@link #surfaceChanged}.
   *
   * @param holder The holder of the new surface.
   */
  @Override
  public void surfaceCreated(@NotNull SurfaceHolder holder) {
    synchronized (lock) {
      surface = holder.getSurface();
    }
  }

  /**
   * Called on the UI thread when the surface appears with its first size, or changes size afterwards.
   *
   * @param holder The holder of the surface.
   * @param format The pixel format of the surface (ignored).
   * @param w The surface width in pixels.
   * @param h The surface height in pixels.
   */
  @Override
  public void surfaceChanged(@NotNull SurfaceHolder holder, int format, int w, int h) {
    applyFrameRateHint(holder.getSurface());
    synchronized (lock) {
      surface = holder.getSurface();
      surfaceWidth = w;
      surfaceHeight = h;
      surfaceDirty = true;
      lock.notifyAll();
    }
  }

  /**
   * Called on the UI thread when Android takes the surface away (the app went to the background, or the view was
   * removed).
   *
   * <p>Android forbids drawing to a surface once this method returns, so it blocks until the render thread has
   * released the WebGPU surface. The wait is bounded, so a stuck GPU can delay the callback but never freeze the
   * app for good.
   *
   * @param holder The holder of the surface that is going away.
   */
  @Override
  public void surfaceDestroyed(@NotNull SurfaceHolder holder) {
    synchronized (lock) {
      surface = null;
      surfaceDirty = false;
      detachPending = true;
      lock.notifyAll();
      long deadline = System.nanoTime() + DETACH_TIMEOUT_MILLIS * 1_000_000L;
      while (detachPending && thread != null && thread.isAlive()) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0L) {
          LOG.error("The render thread did not release the surface in time.");
          break;
        }
        try {
          lock.wait(Math.max(1L, remaining / 1_000_000L));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
  }

  /**
   * Called on the UI thread when the activity leaves the foreground. The game is told it lost focus (on the
   * render thread, before rendering stops), and no frames run until {@link #resume()}.
   */
  public void pause() {
    synchronized (lock) {
      if (!resumed) {
        return;
      }
      resumed = false;
      focusLostPending = true;
      lock.notifyAll();
    }
  }

  /** Called on the UI thread when the activity returns to the foreground. Frames continue and the game regains focus. */
  public void resume() {
    synchronized (lock) {
      if (resumed) {
        return;
      }
      resumed = true;
      focusGainedPending = true;
      lock.notifyAll();
    }
  }

  /**
   * Called on the UI thread when the activity is destroyed. The render thread destroys the game and releases every
   * GPU resource, then ends. This does not wait for it.
   */
  public void destroy() {
    synchronized (lock) {
      quit = true;
      lock.notifyAll();
    }
  }

  private void renderLoop() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
    try {
      boolean running = true;
      while (running) {
        switch (awaitWork()) {
          case WORK_QUIT -> running = false;
          case WORK_FOCUS_LOST -> {
            focusLost = true;
            callFocus(false);
          }
          case WORK_FOCUS_GAINED -> {
            if (focusLost) {
              focusLost = false;
              callFocus(true);
            }
            lastNanos = System.nanoTime();
            frameDeadlineNanos = 0L;
          }
          case WORK_DETACH -> detach();
          case WORK_ATTACH -> attach();
          case WORK_RESIZE -> resize();
          default -> frame();
        }
      }
    } catch (Throwable t) {
      dispatchCrash(Thread.currentThread(), t);
    } finally {
      shutdown();
    }
  }

  /**
   * Sleeps until there is something to do and says what it is. Pending lifecycle work always comes before a
   * frame, and the surface is always released before anything else, so Android never waits on a frame.
   */
  private int awaitWork() {
    synchronized (lock) {
      while (true) {
        if (quit) {
          return WORK_QUIT;
        }
        if (detachPending) {
          return WORK_DETACH;
        }
        if (focusLostPending) {
          focusLostPending = false;
          return WORK_FOCUS_LOST;
        }
        if (focusGainedPending) {
          focusGainedPending = false;
          return WORK_FOCUS_GAINED;
        }
        if (surface != null && surfaceDirty) {
          return target == null ? WORK_ATTACH : WORK_RESIZE;
        }
        if (resumed && target != null) {
          return WORK_FRAME;
        }
        try {
          lock.wait();
        } catch (InterruptedException e) {
          quit = true;
        }
      }
    }
  }

  private void callFocus(boolean gained) {
    FlixelGame g = game;
    if (g == null || crashed || !gameCreated) {
      return;
    }
    try {
      if (gained) {
        g.onFocusGained();
      } else {
        g.onFocusLost();
      }
    } catch (Throwable t) {
      dispatchCrash(Thread.currentThread(), t);
    }
  }

  /**
   * Releases the WebGPU surface in answer to {@link #surfaceDestroyed}, then wakes the UI thread. The renderer, the
   * device and everything on it stay alive.
   */
  private void detach() {
    try {
      releaseTarget();
    } finally {
      synchronized (lock) {
        detachPending = false;
        lock.notifyAll();
      }
    }
  }

  private void releaseTarget() {
    AndroidSurface t = target;
    if (t == null) {
      return;
    }
    target = null;
    if (deviceReady) {
      graphics.detachSurface();
    }
    t.release();
  }

  /** Turns the Android surface into a WebGPU surface and, the first time, builds the device and starts the game. */
  private void attach() {
    Surface s;
    synchronized (lock) {
      s = surface;
      width = surfaceWidth;
      height = surfaceHeight;
      surfaceDirty = false;
    }
    if (s == null) {
      return;
    }
    window.setBackBufferSize(width, height);
    input.setScale(width, height, width, height);
    GpuInstance gpu = instance;
    if (gpu == null) {
      try {
        gpu = JWebGpu.createInstance();
      } catch (RuntimeException e) {
        fatal("WebGPU could not be started: " + e.getMessage());
        return;
      }
      instance = gpu;
    }
    try {
      target = AndroidSurface.create(gpu, s);
    } catch (RuntimeException e) {
      fatal("The screen could not be used for WebGPU: " + e.getMessage());
      return;
    }
    if (deviceReady) {
      graphics.attachSurface(target.getSurface(), width, height);
      LOG.info("Resumed rendering on a new {}x{} surface.", width, height);
      lastNanos = System.nanoTime();
      return;
    }
    if (!createDevice(gpu)) {
      releaseTarget();
      fatal("No GPU that supports Vulkan 1.1 could be opened. FlixelGDX on Android needs a Vulkan 1.1 device.");
      return;
    }
    FlixelGame g = game;
    if (g != null && !gameCreated && !crashed) {
      gameCreated = true;
      try {
        g.create();
        g.resize(width, height);
      } catch (Throwable t) {
        dispatchCrash(Thread.currentThread(), t);
      }
    } else if (g != null && !crashed) {
      g.resize(width, height);
    }
    lastNanos = System.nanoTime();
    frameDeadlineNanos = 0L;
  }

  /**
   * Asks the GPU for an adapter and a device for the current surface and hands them to the renderer.
   *
   * <p>Both requests are asynchronous in WebGPU, so this pumps the instance's events until both have answered. The
   * first request is the slow one, because the driver is loaded then.
   */
  private boolean createDevice(@NotNull GpuInstance gpu) {
    AndroidSurface t = target;
    if (t == null) {
      return false;
    }
    FlixelGpuBootstrap bootstrap = new FlixelGpuBootstrap(gpu, t.getSurface());
    bootstrap.setDeviceLostCallback(this::onDeviceLost);
    bootstrap.start();
    long deadline = System.nanoTime() + BOOT_TIMEOUT_NANOS;
    while (!bootstrap.isDone() && System.nanoTime() < deadline) {
      gpu.processEvents();
      LockSupport.parkNanos(1_000_000L);
    }
    if (!bootstrap.isDone() || bootstrap.getDevice() == null || bootstrap.getAdapter() == null) {
      String error = bootstrap.getError();
      LOG.error("{}", error != null ? error : "The GPU did not answer in time.");
      return false;
    }
    deviceLost = false;
    graphics.setEventPump(gpu::processEvents);
    graphics.initialize(bootstrap.getAdapter(), bootstrap.getDevice(), t.getSurface(), width, height, false,
        new FlixelAndroidImageDecoder());
    deviceReady = true;
    return true;
  }

  private void onDeviceLost(@Nullable GpuDevice device,
      @Nullable GpuDeviceLostReason reason, @Nullable String message) {
    if (disposing) {
      return;
    }
    LOG.error("The GPU device was lost ({}): {}", reason, message);
    deviceLost = true;
  }

  private void resize() {
    synchronized (lock) {
      width = surfaceWidth;
      height = surfaceHeight;
      surfaceDirty = false;
    }
    window.setBackBufferSize(width, height);
    input.setScale(width, height, width, height);
    graphics.onResize(width, height);
    FlixelGame g = game;
    if (g != null && gameCreated && !crashed) {
      try {
        g.resize(width, height);
      } catch (Throwable t) {
        dispatchCrash(Thread.currentThread(), t);
      }
    }
  }

  /** Throws away the renderer after a device loss so that the next surface builds a fresh one. */
  private void rebuildDevice() {
    LOG.warn("Rebuilding the renderer on a new GPU device. Textures must be loaded again.");
    releaseTarget();
    disposing = true;
    try {
      graphics.dispose();
    } finally {
      disposing = false;
    }
    deviceReady = false;
    deviceLost = false;
    synchronized (lock) {
      // The surface is still alive, so ask for it to be attached again right away.
      surfaceDirty = surface != null;
    }
  }

  /**
   * Runs one frame. The order follows the desktop convention: drain input, begin frame, advance time, update,
   * draw, end frame.
   */
  private void frame() {
    FlixelGame g = game;
    if (g == null || crashed) {
      LockSupport.parkNanos(IDLE_FRAME_NANOS);
      return;
    }
    try {
      GpuInstance gpu = instance;
      if (gpu != null) {
        gpu.processEvents();
      }
      if (deviceLost) {
        rebuildDevice();
        return;
      }
      long now = System.nanoTime();
      float rawDelta = (now - lastNanos) / 1_000_000_000f;
      lastNanos = now;
      // Clamp the delta to avoid a runaway spiral after a very slow frame.
      if (rawDelta > MAX_DELTA) {
        rawDelta = MAX_DELTA;
      }
      input.drain();
      graphics.beginFrame();
      float elapsed = g.advanceTime(rawDelta);
      g.update(elapsed);
      g.draw(graphics.getBatch());
      g.endFrame();
      boolean drawn = graphics.isFrameActive();
      graphics.endFrame();
      if (!drawn) {
        // The surface could not give a texture (it is being replaced); do not spin while waiting.
        LockSupport.parkNanos(IDLE_FRAME_NANOS);
      } else if (!graphics.isVSyncEnabled()) {
        limitFrameRate();
      }
    } catch (Throwable t) {
      dispatchCrash(Thread.currentThread(), t);
    }
  }

  /**
   * Waits until the game's target frame period has elapsed. Used only when vertical sync is off, because with it on
   * the presentation of each frame already paces the loop (and {@link #applyFrameRateHint} asks the system for the
   * target rate).
   */
  private void limitFrameRate() {
    int fps = graphics.getTargetFps();
    if (fps <= 0) {
      frameDeadlineNanos = 0L;
      return;
    }
    long targetNanos = 1_000_000_000L / fps;
    long now = System.nanoTime();
    if (frameDeadlineNanos == 0L) {
      frameDeadlineNanos = now;
    }
    frameDeadlineNanos += targetNanos;
    if (frameDeadlineNanos <= now) {
      frameDeadlineNanos = now;
      return;
    }
    long remaining = frameDeadlineNanos - now;
    while (remaining > SPIN_MARGIN_NANOS) {
      LockSupport.parkNanos(remaining - SPIN_MARGIN_NANOS);
      remaining = frameDeadlineNanos - System.nanoTime();
    }
    while (System.nanoTime() < frameDeadlineNanos) {
      Thread.onSpinWait();
    }
  }

  /**
   * Tells the system which frame rate the game wants, so a 90 Hz or 120 Hz screen can run at the game's rate
   * instead of the game skipping refreshes. Only used when vertical sync is on and the game has a target rate.
   */
  private void applyFrameRateHint(@NotNull Surface s) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !s.isValid()) {
      return;
    }
    try {
      int fps = graphics.isVSyncEnabled() ? graphics.getTargetFps() : 0;
      s.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
    } catch (RuntimeException e) {
      LOG.warn("The frame rate hint was rejected: {}", e.getMessage());
    }
  }

  /** Releases everything the render thread owns, in the order the GPU requires. Runs once, as the thread ends. */
  private void shutdown() {
    FlixelGame g = game;
    try {
      if (g != null && gameCreated) {
        g.destroy();
        Flixel.sound.destroy();
      }
    } catch (Throwable ignored) {
      // Best-effort; the OS reclaims native memory when the process exits.
    }
    try {
      releaseTarget();
      if (deviceReady) {
        disposing = true;
        graphics.dispose();
        deviceReady = false;
      }
      GpuInstance gpu = instance;
      instance = null;
      if (gpu != null) {
        gpu.release();
      }
    } catch (Throwable t) {
      LOG.warn("Releasing the GPU failed: {}", t.getMessage());
    }
    synchronized (lock) {
      // Never leave the UI thread waiting on a thread that is gone.
      detachPending = false;
      lock.notifyAll();
    }
  }

  private void fatal(@NotNull String message) {
    LOG.error("{}", message);
    crashed = true;
    if (Flixel.alert != null) {
      Flixel.alert.error("FlixelGDX", message);
    }
  }

  private void dispatchCrash(Thread t, Throwable error) {
    crashed = true;
    if (crashHandler != null) {
      crashHandler.onCrash(t, error);
    } else {
      Thread.UncaughtExceptionHandler h = t.getUncaughtExceptionHandler();
      if (h != null) {
        h.uncaughtException(t, error);
      }
    }
  }

  @NotNull
  public FlixelAndroidWindow getWindow() {
    return window;
  }

  @NotNull
  public FlixelAndroidInputDevice getInput() {
    return input;
  }

  @NotNull
  public FlixelAndroidGraphics getGraphics() {
    return graphics;
  }

  @Nullable
  public FlixelGame getGame() {
    return game;
  }

  @Nullable
  public FlixelCrashHandler getCrashHandler() {
    return crashHandler;
  }

  public boolean isCrashed() {
    return crashed;
  }

  public boolean isDeviceReady() {
    return deviceReady;
  }

  /**
   * Returns whether the activity is in the foreground, as last reported by {@link #pause()} and {@link #resume()}.
   *
   * @return {@code true} while frames are allowed to run.
   */
  public boolean isResumed() {
    synchronized (lock) {
      return resumed;
    }
  }
}
