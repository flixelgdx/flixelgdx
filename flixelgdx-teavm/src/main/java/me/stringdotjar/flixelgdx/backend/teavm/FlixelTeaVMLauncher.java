/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.web.assetloader.AssetInstance;
import com.github.xpenatan.gdx.teavm.backends.web.assetloader.AssetLoaderListener;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelDefaultReflectionHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;
import me.stringdotjar.flixelgdx.backend.teavm.alert.FlixelTeaVMAlerter;
import me.stringdotjar.flixelgdx.backend.teavm.audio.FlixelDefaultSoundHandler;
import me.stringdotjar.flixelgdx.backend.teavm.logging.TeaVMStackTraceProvider;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

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
 * The {@link me.stringdotjar.flixelgdx.logging.FlixelLogFileHandler} is not registered, so {@link Flixel#startFileLogging()} is a safe no-op.
 * Console output still works through {@code System.out.println}, which TeaVM maps to {@code console.log}.
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
    launch(game, FlixelRuntimeMode.RELEASE, null);
  }

  /**
   * Launches the web version of the game with the given runtime mode and
   * default configuration.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode) {
    launch(game, runtimeMode, null);
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
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode, @Nullable Consumer<WebApplicationConfiguration> configCustomizer) {
    Flixel.setAlerter(new FlixelTeaVMAlerter());
    Flixel.setStackTraceProvider(new TeaVMStackTraceProvider());
    Flixel.setReflection(new FlixelDefaultReflectionHandler());
    Flixel.setSoundBackendFactory(new FlixelDefaultSoundHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    Flixel.initialize(game);

    Flixel.setCanStoreLogs(false);

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
        addInitQueue();
        AssetInstance.getLoaderInstance().loadScript("freetype.js", new AssetLoaderListener<>() {
          @Override
          public void onSuccess(String url, String result) {
            subtractInitQueue();
          }

          @Override
          public void onFailure(String url) {
            subtractInitQueue();
            System.err.println("[FlixelGDX] freetype.js failed to load. FreeType fonts will not work.");
          }
        });
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
