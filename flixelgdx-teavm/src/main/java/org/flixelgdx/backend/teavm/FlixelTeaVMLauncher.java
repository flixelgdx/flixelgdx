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
package org.flixelgdx.backend.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.runtime.FlixelRuntimeMode;
import org.flixelgdx.backend.teavm.alert.FlixelTeaVMAlerter;
import org.flixelgdx.backend.teavm.audio.FlixelTeaVMSoundHandler;
import org.flixelgdx.backend.teavm.debug.FlixelTeaVMDebugOverlay;
import org.flixelgdx.backend.teavm.logging.FlixelTeaVMLogConsole;
import org.flixelgdx.backend.teavm.logging.TeaVMStackTraceProvider;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Launches the web (TeaVM) version of a FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and a launcher
 * class with a {@code main(String[] args)} method that creates the game instance and calls one of the {@code launch} overloads.
 * Set that launcher class as the TeaVM {@code mainClass} in your web module's {@code build.gradle}.
 *
 * <h2>Minimal Example</h2>
 *
 * <pre>{@code
 * public class MyTeaVMLauncher {
 *   public static void main(String[] args) {
 *     FlixelTeaVMLauncher.launch(new MyGame("My Game", 800, 600, new InitialState()));
 *   }
 * }
 * }</pre>
 *
 * <h2>Custom Configuration Example</h2>
 *
 * <pre>{@code
 * public class MyTeaVMLauncher {
 *
 *   public static void main(String[] args) {
 *     FlixelTeaVMLauncher.launch(
 *         new MyGame("My Game", 800, 600, new InitialState()),
 *         FlixelRuntimeMode.DEBUG,
 *         config -> {
 *           config.canvasID = "my-canvas";
 *           config.antialiasing = true;
 *         }
 *     );
 *   }
 * }
 * }</pre>
 *
 * <h2>Platform Notes</h2>
 *
 * <p>File logging is intentionally disabled on the web backend because browsers do not expose a host filesystem.
 * The {@link org.flixelgdx.logging.FlixelLogFileHandler FlixelLogFileHandler} is not registered, so {@link FlixelLogger#startFileLogging()} is a safe no-op.
 * Console output uses {@link Flixel#setLogConsoleSink} with a styled {@code console} writer so log lines appear with readable colors
 * in the browser; ANSI {@code System.out} is not used on web.
 *
 * @see FlixelGame
 * @see WebApplicationConfiguration
 */
public class FlixelTeaVMLauncher {

  /** Default canvas element ID used when none is specified. */
  private static final String DEFAULT_CANVAS_ID = "flixelgdx-canvas";

  /**
   * Launches the web version of the game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode with default configuration.
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   */
  public static void launch(FlixelGame game) {
    launch(game, FlixelRuntimeMode.RELEASE, (Runnable) null);
  }

  /**
   * Launches the web version of the game with the given runtime mode and
   * default configuration.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode) {
    launch(game, runtimeMode, null, null);
  }

  /**
   * Launches the web version of the game with the given runtime mode, default configuration, and
   * an optional pre-initialization callback.
   *
   * <p>Use this when you want to inject custom services without supplying a configuration
   * customizer:
   *
   * <pre>{@code
   * FlixelTeaVMLauncher.launch(game, FlixelRuntimeMode.DEBUG, () -> {
   *   Flixel.setDebugOverlay(MyCustomOverlay::new);
   * });
   * }</pre>
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param onBeforeInitialize Optional callback invoked just before {@link Flixel#initialize}.
   *     Pass {@code null} to skip.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      @Nullable Runnable onBeforeInitialize) {
    launch(game, runtimeMode, null, onBeforeInitialize);
  }

  /**
   * Launches the web version of the game with the given runtime mode and
   * an optional configuration customizer.
   *
   * <p>The {@code configCustomizer} receives a pre-populated {@link WebApplicationConfiguration} with sensible defaults (canvas ID,
   * dimensions from the game). Override any field before the consumer returns. Pass {@code null} to accept all defaults.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session.
   * @param configCustomizer Optional consumer that can modify the web configuration before the application starts.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      @Nullable Consumer<WebApplicationConfiguration> configCustomizer) {
    launch(game, runtimeMode, configCustomizer, null);
  }

  /**
   * Launches the web version of the game with an optional pre-initialization callback.
   *
   * <p>{@code onBeforeInitialize} fires after all default FlixelGDX backend services (alerter,
   * audio, debug overlay, etc.) have been registered but before {@link Flixel#initialize} is
   * called. Use it to override any of those defaults without needing to duplicate the rest of
   * the launcher wiring:
   *
   * <pre>{@code
   * FlixelTeaVMLauncher.launch(game, FlixelRuntimeMode.DEBUG, null, () -> {
   *     Flixel.setDebugOverlay(MyCustomOverlay::new);
   * });
   * }</pre>
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session.
   * @param configCustomizer Optional consumer that can modify the web configuration before the application starts.
   * @param onBeforeInitialize Optional callback invoked just before {@link Flixel#initialize}.
   *     Pass {@code null} to skip.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      @Nullable Consumer<WebApplicationConfiguration> configCustomizer,
      @Nullable Runnable onBeforeInitialize) {
    Flixel.setAlerter(new FlixelTeaVMAlerter());
    Flixel.setStackTraceProvider(new TeaVMStackTraceProvider());
    Flixel.setLogConsoleSink(FlixelTeaVMLogConsole::emit);
    Flixel.setSoundBackendFactory(new FlixelTeaVMSoundHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    if (runtimeMode == FlixelRuntimeMode.DEBUG) {
      Flixel.setDebugOverlay(FlixelTeaVMDebugOverlay::new);
    }
    if (onBeforeInitialize != null) {
      onBeforeInitialize.run();
    }
    Flixel.initialize(game);

    Flixel.log.setCanStoreLogs(false);

    WebApplicationConfiguration configuration = new WebApplicationConfiguration();
    configuration.canvasID = DEFAULT_CANVAS_ID;
    if (game.getViewWidth() > 0 && game.getViewHeight() > 0) {
      configuration.width = game.getViewWidth();
      configuration.height = game.getViewHeight();
    }

    if (configCustomizer != null) {
      configCustomizer.accept(configuration);
    }

    new WebApplication(game, configuration) {
      @Override
      protected void init() {
        super.init();
        Flixel.mouse.setMouseIconManager(new FlixelTeaVMMouseIconManager(configuration.canvasID));
      }
    };
  }

  /**
   * Default TeaVM entry point. Games should use their own launcher class
   * as {@code mainClass} and call {@link #launch(FlixelGame)} with their
   * game instance.
   *
   * @param args ignored.
   * @throws UnsupportedOperationException always, because this stub should
   *         never be invoked directly.
   */
  public static void main(String[] args) {
    throw new UnsupportedOperationException(
        "Configure your game's launcher class as mainClass in the teavm block, "
            + "e.g. mainClass = \"com.mygame.MyTeaVMLauncher\", and in its main() call "
            + "FlixelTeaVMLauncher.launch(new YourGame(...));");
  }
}
