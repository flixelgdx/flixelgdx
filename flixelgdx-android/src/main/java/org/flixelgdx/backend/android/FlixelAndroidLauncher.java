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
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.backend.android.asset.FlixelAndroidAssetManager;
import org.flixelgdx.backend.android.file.FlixelAndroidFiles;
import org.flixelgdx.backend.android.graphics.FlixelAndroidGraphics;
import org.flixelgdx.backend.android.input.FlixelAndroidGamepadProvider;
import org.flixelgdx.backend.android.logging.FlixelAndroidLogFileSink;
import org.flixelgdx.backend.android.logging.FlixelAndroidLogSiteResolver;
import org.flixelgdx.backend.android.logging.FlixelLogcatSink;
import org.flixelgdx.backend.android.runtime.FlixelAndroidRuntimeDevice;
import org.flixelgdx.backend.android.text.FlixelAndroidFontRasterizer;
import org.flixelgdx.backend.miniaudio.FlixelMiniAudio;
import org.flixelgdx.backend.miniaudio.FlixelMiniAudioFactory;
import org.flixelgdx.graphics.FlixelViewport;
import org.flixelgdx.text.FlixelFontRegistry;
import org.jetbrains.annotations.NotNull;

import io.github.stringdotjar.jwebgpu.jni.JWebGpuJni;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.Window;

/**
 * The one-line entry point for an Android FlixelGDX game.
 *
 * <p>Call {@link #launch(Activity, FlixelGame)} from your {@code Activity.onCreate} and nothing
 * else is required. The launcher installs every Android backend piece (files, audio, haptics,
 * logging, window, input, gamepads), loads the WebGPU native library, and starts the game loop on a
 * {@link FlixelAndroidSurfaceView} that the shared WebGPU renderer draws into. Game developers never install the
 * WebGPU backend themselves.
 *
 * <p>The device needs Android 8.0 (API 26) and Vulkan 1.1, and the app must be built for a 64-bit ABI
 * (arm64-v8a or x86_64).
 *
 * <p>Example:
 *
 * <pre>{@code
 * public final class GameActivity extends Activity {
 *   @Override
 *   protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     FlixelAndroidLauncher.launch(this, new MyGame());
 *   }
 * }
 * }</pre>
 *
 * @see FlixelRuntimeMode
 */
public final class FlixelAndroidLauncher {

  private FlixelAndroidLauncher() {}

  /**
   * Launches the game, defaulting to {@link FlixelRuntimeMode#RELEASE}.
   *
   * @param activity The host activity.
   * @param game The game instance to run.
   */
  public static void launch(@NotNull Activity activity, @NotNull FlixelGame game) {
    launch(activity, game, FlixelRuntimeMode.RELEASE);
  }

  /**
   * Launches the game with the specified runtime mode.
   *
   * @param activity The host activity.
   * @param game The game instance to run.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session.
   */
  public static void launch(@NotNull Activity activity, @NotNull FlixelGame game,
      @NotNull FlixelRuntimeMode runtimeMode) {
    // Game developers never call this: the launcher loads the WebGPU native library (Dawn) and registers the JNI
    // backend, so everything after this point only talks to the platform-neutral WebGPU API.
    JWebGpuJni.install();

    FlixelAndroidRuntimeDevice runtime = new FlixelAndroidRuntimeDevice(activity);
    Flixel.runtime = runtime;
    Flixel.alert = new FlixelAndroidAlerter(activity);
    Flixel.files = new FlixelAndroidFiles(activity);
    Flixel.assets = new FlixelAndroidAssetManager();
    Flixel.host = new FlixelAndroidHostIntegration(activity);

    Flixel.log.setSiteResolver(new FlixelAndroidLogSiteResolver());
    Flixel.log.setConsoleSink(new FlixelLogcatSink());
    Flixel.log.setFileSink(new FlixelAndroidLogFileSink());

    FlixelAndroidWindow window = new FlixelAndroidWindow(activity);
    Flixel.window = window;

    FlixelAndroidGraphics graphics = new FlixelAndroidGraphics(activity);
    Flixel.graphics = graphics;

    Flixel.haptics = new FlixelAndroidHaptics(activity);

    FlixelFontRegistry.setRasterizer(new FlixelAndroidFontRasterizer(activity));

    // Use an EXTEND viewport so the game fills the device screen without letterboxing.
    FlixelCamera.viewportFactory = (w, h) -> new FlixelViewport(w, h, FlixelViewport.Scaling.EXTEND);

    FlixelMiniAudio.setLoader(() -> System.loadLibrary("flixel_miniaudio"));
    FlixelSoundManager.defaultFactory = FlixelMiniAudioFactory.create();

    FlixelAndroidInputDevice input = new FlixelAndroidInputDevice();
    Flixel.input = input;

    FlixelAndroidRunner runner = new FlixelAndroidRunner(window, input, graphics);

    // The view is a plain SurfaceView. The runner turns its surface into a WebGPU surface on the render thread and
    // keeps the GPU device alive when Android destroys and recreates the surface (the app going to the background).
    FlixelAndroidSurfaceView view = new FlixelAndroidSurfaceView(activity, input);
    view.getHolder().addCallback(runner);

    // Wire all input listeners.
    view.setOnTouchListener(input.createTouchListener());
    view.setOnKeyListener(input.createKeyListener());
    view.setOnGenericMotionListener(input.createGenericMotionListener());
    view.setFocusable(true);
    view.setFocusableInTouchMode(true);

    input.setSurfaceAndActivity(view, activity);

    FlixelAndroidGamepadProvider gamepadProvider = new FlixelAndroidGamepadProvider(activity);
    input.setGamepadProvider(gamepadProvider);

    // Drop the title bar before any content is added; requesting a window feature afterwards
    // throws. Some activity themes (for example AppCompat ones) reject this, in which case
    // install() below hides the bar instead.
    try {
      activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
    } catch (RuntimeException ignored) {
      // The theme already decided; install() handles the action bar.
    }
    activity.setContentView(view);
    window.install(view);

    activity.getApplication().registerActivityLifecycleCallbacks(
        new ActivityLifecycleHandler(activity, runner, gamepadProvider));

    Flixel.runtime.setMode(runtimeMode);

    Flixel.boot.afterStart(() -> {
      Flixel.touches.enabled = true;
      Flixel.gamepads.setGamepadProvider(gamepadProvider);
      Flixel.gamepads.addMappingResolver(gamepadProvider);
      gamepadProvider.start();
    });

    Flixel.start(game, runner);

    runner.setCrashHandler(runtime.getCrashHandler());
  }

  /**
   * Lifecycle callbacks that route Android activity events to the runner.
   *
   * <p>Only events for the specific activity that launched the game are handled; all others are
   * ignored. Window focus changes cannot be intercepted here - they are handled by overriding
   * {@code onWindowFocusChanged} in the activity itself. Game code that needs focus notifications
   * should override the activity method or use {@link Flixel#autoPause}.
   *
   * <p>The runner moves the work onto the render thread, so none of these callbacks touch the game or the GPU
   * directly.
   */
  private record ActivityLifecycleHandler(@NotNull Activity activity, @NotNull FlixelAndroidRunner runner,
      @NotNull FlixelAndroidGamepadProvider gamepadProvider)
      implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityPaused(@NotNull Activity a) {
      if (a == activity) {
        runner.pause();
      }
    }

    @Override
    public void onActivityResumed(@NotNull Activity a) {
      if (a == activity) {
        runner.resume();
      }
    }

    @Override
    public void onActivityDestroyed(@NotNull Activity a) {
      if (a != activity) {
        return;
      }
      gamepadProvider.stop();
      runner.destroy();
      try {
        activity.getApplication().unregisterActivityLifecycleCallbacks(this);
      } catch (Throwable ignored) {
        // Unregister is best-effort.
      }
    }

    @Override
    public void onActivityCreated(@NotNull Activity a, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NotNull Activity a) {}

    @Override
    public void onActivityStopped(@NotNull Activity a) {}

    @Override
    public void onActivitySaveInstanceState(@NotNull Activity a, @NotNull Bundle outState) {}
  }
}
