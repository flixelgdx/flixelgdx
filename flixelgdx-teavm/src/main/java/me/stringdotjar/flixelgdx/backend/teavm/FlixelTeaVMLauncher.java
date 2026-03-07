package me.stringdotjar.flixelgdx.backend.teavm;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.teavm.alert.FlixelTeaVMAlerter;
import me.stringdotjar.flixelgdx.backend.teavm.logging.TeaVMStackTraceProvider;

/**
 * Launches the web (TeaVM) version of the FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and a launcher class with a
 * {@code main(String[] args)} method that creates the game instance and calls {@link #launch(FlixelGame)}.
 * Set that launcher class as the TeaVM {@code mainClass} in your web module's build.gradle.
 *
 * <pre>{@code
 * public class MyTeaVMLauncher {
 *   public static void main(String[] args) {
 *     FlixelTeaVMLauncher.launch(new MyGame("My Game", 800, 600, new InitialState()));
 *   }
 * }
 * }</pre>
 */
public class FlixelTeaVMLauncher {

  /**
   * Launches the web version of the game with the given game instance.
   *
   * <p>Call this from your TeaVM entry point (the class configured as {@code mainClass} in the
   * TeaVM block of your web module's build.gradle). Create your {@link FlixelGame} subclass
   * instance and pass it here.
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   */
  public static void launch(FlixelGame game) {
    Flixel.initialize(game, new FlixelTeaVMAlerter(), new TeaVMStackTraceProvider());

    WebApplicationConfiguration configuration = new WebApplicationConfiguration();
    configuration.canvasID = "flixelgdx-canvas";
    if (game.getWindowWidth() > 0 && game.getWindowHeight() > 0) {
      configuration.width = game.getWindowWidth();
      configuration.height = game.getWindowHeight();
    }

    new WebApplication(game, configuration);
  }

  /**
   * Default TeaVM entry point. Games should use their own launcher class as {@code mainClass}
   * and call {@link #launch(FlixelGame)} with their game instance.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    throw new UnsupportedOperationException(
        "Configure your game's launcher class as mainClass in the teavm block, "
            + "e.g. mainClass = \"com.mygame.MyTeaVMLauncher\", and in its main() call "
            + "FlixelTeaVMLauncher.launch(new YourGame(...));");
  }
}
