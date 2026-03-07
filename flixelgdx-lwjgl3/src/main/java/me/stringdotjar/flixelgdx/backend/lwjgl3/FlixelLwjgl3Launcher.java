package me.stringdotjar.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.lwjgl3.alert.FlixelLwjgl3Alerter;

import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;

/**
 * Launches the desktop (LWJGL3) version of the Flixel game.
 */
public class FlixelLwjgl3Launcher {

  /**
   * Launches the LWJGL3 version of the Flixel game with the given game instance. This should be called from the main
   * method of the libGDX LWJGL3 launcher class, and the game instance should be created in the same general area.
   *
   * @param game The game instance to launch. This should already be initialized with the desired configuration values.
   */
  public static void launch(FlixelGame game, String... icons) {
    Flixel.initialize(game, new FlixelLwjgl3Alerter(), new FlixelDefaultStackTraceProvider());

    Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
    configuration.setTitle(game.getTitle());
    configuration.useVsync(game.isVsync());
    configuration.setForegroundFPS(game.getFramerate());
    if (game.isFullscreen()) {
      configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
    } else {
      configuration.setWindowedMode(game.getWindowWidth(), game.getWindowHeight());
    }
    configuration.setWindowIcon(icons);
    configuration.setWindowListener(new Lwjgl3WindowAdapter() {
      @Override
      public void focusGained() {
        super.focusGained();
        Flixel.getGame().onWindowFocused();
      }

      @Override
      public void focusLost() {
        if (!Flixel.getGame().isMinimized()) {
          super.focusLost();
          Flixel.getGame().onWindowUnfocused();
        }
      }

      @Override
      public void iconified(boolean isIconified) {
        super.iconified(isIconified);
        Flixel.getGame().onWindowMinimized(isIconified);
      }
    });

    new Lwjgl3Application(game, configuration);
  }
}
