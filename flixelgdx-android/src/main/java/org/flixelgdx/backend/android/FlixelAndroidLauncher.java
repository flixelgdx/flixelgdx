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

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.android.alert.FlixelAndroidAlerter;
import org.flixelgdx.backend.common.audio.FlixelMiniAudioSoundHandler;
import org.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import org.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import org.flixelgdx.backend.runtime.FlixelRuntimeMode;

/**
 * Launches the Android version of the FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and an Android launcher activity that
 * extends {@link AndroidApplication}. In {@code onCreate}, create the game instance and call
 * {@link #launch(FlixelGame, AndroidApplication)}.
 */
public class FlixelAndroidLauncher {

  /**
   * Launches the Android version of the game in {@link FlixelRuntimeMode#RELEASE RELEASE} mode.
   *
   * @param game The game instance to launch.
   * @param activity The Android application activity.
   */
  public static void launch(FlixelGame game, AndroidApplication activity) {
    launch(game, activity, FlixelRuntimeMode.RELEASE);
  }

  /**
   * Launches the Android version of the game with the given runtime mode.
   *
   * <p>Call this from the {@code onCreate} method of your {@link AndroidApplication} activity.
   * Create your {@link FlixelGame} subclass instance and pass it here along with the activity
   * (typically {@code this}).
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param activity The Android application activity (must extend {@link AndroidApplication}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(FlixelGame game, AndroidApplication activity, FlixelRuntimeMode runtimeMode) {
    launch(game, activity, runtimeMode, null);
  }

  /**
   * Launches the Android version of the game with an optional pre-initialization callback.
   *
   * <p>{@code onBeforeInitialize} fires after all default FlixelGDX backend services (alerter,
   * audio, etc.) have been registered but before {@link Flixel#initialize} is called. Use it to
   * override any of those defaults without needing to duplicate the rest of the launcher wiring:
   *
   * <pre>{@code
   * public class MyAndroidLauncher extends AndroidApplication {
   *   protected void onCreate(Bundle savedInstanceState) {
   *     super.onCreate(savedInstanceState);
   *     FlixelAndroidLauncher.launch(
   *         new MyGame("My Game", 800, 600, new InitialState()),
   *         this,
   *         FlixelRuntimeMode.RELEASE,
   *         () -> Flixel.setAlerter(myCustomAlerter)
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param activity The Android application activity (must extend {@link AndroidApplication}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param onBeforeInitialize Optional callback invoked just before {@link Flixel#initialize}.
   *     Pass {@code null} to skip.
   */
  public static void launch(FlixelGame game, AndroidApplication activity, FlixelRuntimeMode runtimeMode,
      Runnable onBeforeInitialize) {
    FlixelCamera.viewportFactory = ExtendViewport::new;
    Flixel.setAlerter(new FlixelAndroidAlerter(activity));
    Flixel.setStackTraceProvider(new FlixelDefaultStackTraceProvider());
    Flixel.setLogFileHandler(new FlixelJvmLogFileHandler());
    FlixelMiniAudioSoundHandler soundHandler = new FlixelMiniAudioSoundHandler();
    // MiniAudio on Android requires the native AAssetManager to open files from
    // the assets/ folder. setupAndroid() must receive the AssetManager (not the
    // Activity) so the JNI side can read AssetManager.mObject for the native pointer.
    soundHandler.getEngine().setupAndroid(activity.getAssets());
    Flixel.setSoundBackendFactory(soundHandler);
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    if (onBeforeInitialize != null) {
      onBeforeInitialize.run();
    }
    Flixel.initialize(game);

    AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
    configuration.useImmersiveMode = true;

    game.setCompressedTexturesRequested(true);
    activity.initialize(game, configuration);
  }
}
