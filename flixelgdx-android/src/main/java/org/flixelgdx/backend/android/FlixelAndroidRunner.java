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

import android.opengl.GLSurfaceView;
import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelCrashHandler;
import org.flixelgdx.backend.FlixelGameRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The Android game loop: drives the game's update/draw cycle from the GL thread via
 * {@link GLSurfaceView.Renderer}.
 *
 * <p>Frame timing is measured in nanoseconds with {@link System#nanoTime()} and converted to
 * seconds so the game loop receives the same delta semantics as other backends. Frame delta is
 * clamped to 1/15 s to avoid a "spiral of death" after a slow frame.
 *
 * <p>Context loss is communicated through an optional {@link FlixelAndroidContextListener}. When
 * {@code GLSurfaceView} reports a new surface after a prior one existed, the runner calls
 * {@link FlixelAndroidContextListener#onContextLost()} followed by
 * {@link FlixelAndroidContextListener#onContextRestored()} so the graphics manager can re-upload
 * GPU resources. The GLES graphics task installs this listener when it is ready.
 *
 * <p>Crash handling mirrors the HTML5 runner: each frame is wrapped in a try/catch that forwards
 * unhandled exceptions to the framework crash handler.
 */
public class FlixelAndroidRunner implements FlixelGameRunner, GLSurfaceView.Renderer {

  /** Maximum delta clamped to avoid physics/game logic explosions after very slow frames. */
  private static final float MAX_DELTA = 1f / 15f;

  private long lastNanos;

  @NotNull
  private final FlixelAndroidWindow window;

  @NotNull
  private final FlixelAndroidInputDevice input;

  @Nullable
  private FlixelAndroidContextListener contextListener;

  @Nullable
  private FlixelGame game;

  @Nullable
  private FlixelCrashHandler crashHandler;

  private boolean surfaceCreatedOnce = false;

  /** Set after a crash so the loop stops calling into a game that is in a broken state. */
  private boolean crashed;

  /**
   * Creates a runner wired to the given window and input device.
   *
   * @param window The Android window implementation.
   * @param input The Android input device.
   */
  public FlixelAndroidRunner(@NotNull FlixelAndroidWindow window,
      @NotNull FlixelAndroidInputDevice input) {
    this.window = window;
    this.input = input;
  }

  /**
   * Stores the game but does not start anything; the GL thread calls {@link #onSurfaceCreated}
   * once the EGL context is ready and will call {@link FlixelGame#create()} from there.
   *
   * @param game The game to drive.
   */
  @Override
  public void run(@NotNull FlixelGame game) {
    this.game = game;
    // The GLSurfaceView drives the loop; run() returns immediately.
    // The crash handler is wired by the launcher via setCrashHandler() after Flixel.start().
  }

  /**
   * Installs the optional context listener that the GLES graphics manager uses to react to EGL
   * context loss and restoration.
   *
   * @param listener The listener to notify, or {@code null} to remove any existing one.
   */
  public void setContextListener(@Nullable FlixelAndroidContextListener listener) {
    this.contextListener = listener;
  }

  /**
   * Stores the crash handler so GL-thread exceptions can be forwarded. Called by the launcher
   * after {@link Flixel#start} has wired the runtime.
   *
   * @param handler The handler installed by the framework.
   */
  public void setCrashHandler(@Nullable FlixelCrashHandler handler) {
    this.crashHandler = handler;
  }

  /**
   * Called by the GL thread when the EGL context is ready (and after context recreation).
   *
   * <p>On the very first call, the graphics manager is notified of initialization and
   * {@link FlixelGame#create()} is invoked. On subsequent calls (EGL context was recreated after
   * being lost), the context listener is notified so GPU resources can be restored.
   *
   * @param gl Unused; framework calls go through GLES30 directly.
   * @param config The EGL config chosen by the system.
   */
  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    if (!surfaceCreatedOnce) {
      surfaceCreatedOnce = true;
      lastNanos = System.nanoTime();
      // Notify the graphics manager that the GL context is available. The GLES graphics task
      // will override Flixel.graphics before the runner is run(), so this call routes there.
      Flixel.graphics.beginFrame();
      Flixel.graphics.endFrame();
      if (game != null) {
        try {
          game.create();
        } catch (Throwable t) {
          dispatchCrash(Thread.currentThread(), t);
        }
      }
    } else {
      // Context was recreated after a loss. Notify the graphics manager and the listener.
      if (contextListener != null) {
        contextListener.onContextLost();
        contextListener.onContextRestored();
      }
    }
  }

  /**
   * Called by the GL thread when the surface dimensions change or the surface is first created.
   *
   * @param gl Unused.
   * @param width Back-buffer width in physical pixels.
   * @param height Back-buffer height in physical pixels.
   */
  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    window.setBackBufferSize(width, height);
    input.setScale(width, height, width, height);
    Flixel.graphics.beginFrame();
    Flixel.graphics.endFrame();
    if (game != null) {
      try {
        game.resize(width, height);
      } catch (Throwable t) {
        dispatchCrash(Thread.currentThread(), t);
      }
    }
  }

  /**
   * Called by the GL thread once per frame to advance and draw the game.
   *
   * <p>The frame order follows the desktop convention:
   * drain input, begin frame, advance time, update, draw, end frame.
   *
   * @param gl Unused.
   */
  @Override
  public void onDrawFrame(GL10 gl) {
    if (game == null || crashed) {
      return;
    }
    try {
      long now = System.nanoTime();
      float rawDelta = (now - lastNanos) / 1_000_000_000f;
      lastNanos = now;
      // Clamp the delta to avoid a runaway spiral after a very slow frame.
      if (rawDelta > MAX_DELTA) {
        rawDelta = MAX_DELTA;
      }
      input.drain();
      Flixel.graphics.beginFrame();
      float elapsed = game.advanceTime(rawDelta);
      game.update(elapsed);
      game.draw(Flixel.graphics.getBatch());
      game.endFrame();
      Flixel.graphics.endFrame();
    } catch (Throwable t) {
      dispatchCrash(Thread.currentThread(), t);
    }
  }

  private void dispatchCrash(Thread thread, Throwable t) {
    crashed = true;
    if (crashHandler != null) {
      crashHandler.onCrash(thread, t);
    } else {
      Thread.UncaughtExceptionHandler h = thread.getUncaughtExceptionHandler();
      if (h != null) {
        h.uncaughtException(thread, t);
      }
    }
  }
}
