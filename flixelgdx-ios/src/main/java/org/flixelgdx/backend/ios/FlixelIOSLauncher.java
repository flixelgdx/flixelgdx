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
package org.flixelgdx.backend.ios;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.common.audio.FlixelMiniAudioSoundHandler;
import org.flixelgdx.backend.ios.alert.FlixelIOSAlerter;
import org.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import org.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import org.flixelgdx.backend.runtime.FlixelRuntimeMode;

/**
 * Launches the iOS (RoboVM) version of the FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and an iOS launcher class that
 * extends {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate}. In
 * {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate#createApplication()},
 * create the game instance and return {@link #launch(FlixelGame)}.
 */
public class FlixelIOSLauncher {

  /**
   * Launches the iOS version of the game in {@link FlixelRuntimeMode#RELEASE RELEASE} mode.
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game) {
    return launch(game, FlixelRuntimeMode.RELEASE);
  }

  /**
   * Launches the iOS version of the game with the given runtime mode.
   *
   * <p>Use this from your {@link com.badlogic.gdx.backends.iosrobovm.IOSApplication.Delegate}
   * implementation:
   *
   * <pre>{@code
   * public class MyIOSLauncher extends IOSApplication.Delegate {
   *   @Override
   *   protected IOSApplication createApplication() {
   *     return FlixelIOSLauncher.launch(
   *         new MyGame("My Game", 800, 600, new InitialState()),
   *         FlixelRuntimeMode.DEBUG
   *     );
   *   }
   *   public static void main(String[] argv) {
   *     NSAutoreleasePool pool = new NSAutoreleasePool();
   *     UIApplication.main(argv, null, MyIOSLauncher.class);
   *     pool.close();
   *   }
   * }
   * }</pre>
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game, FlixelRuntimeMode runtimeMode) {
    return launch(game, runtimeMode, null);
  }

  /**
   * Launches the iOS version of the game with an optional pre-initialization callback.
   *
   * <p>{@code onBeforeInitialize} fires after all default FlixelGDX backend services (alerter,
   * audio, etc.) have been registered but before {@link Flixel#initialize} is called. Use it to
   * override any of those defaults without needing to duplicate the rest of the launcher wiring:
   *
   * <pre>{@code
   * public class MyIOSLauncher extends IOSApplication.Delegate {
   *   protected IOSApplication createApplication() {
   *     return FlixelIOSLauncher.launch(
   *         new MyGame("My Game", 800, 600, new InitialState()),
   *         FlixelRuntimeMode.DEBUG,
   *         () -> Flixel.setAlerter(myCustomAlerter)
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param onBeforeInitialize Optional callback invoked just before {@link Flixel#initialize}.
   *     Pass {@code null} to skip.
   * @return The configured {@link IOSApplication} to return from {@code createApplication()}.
   */
  public static IOSApplication launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      Runnable onBeforeInitialize) {
    Flixel.setAlerter(new FlixelIOSAlerter());
    Flixel.setStackTraceProvider(new FlixelDefaultStackTraceProvider());
    Flixel.setLogFileHandler(new FlixelJvmLogFileHandler());
    Flixel.setSoundBackendFactory(new FlixelMiniAudioSoundHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    if (onBeforeInitialize != null) {
      onBeforeInitialize.run();
    }
    Flixel.initialize(game);

    IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
    configuration.preventScreenDimming = false;

    return new IOSApplication(game, configuration);
  }
}
