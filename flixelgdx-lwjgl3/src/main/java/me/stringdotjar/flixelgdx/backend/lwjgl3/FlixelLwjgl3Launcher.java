/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import java.util.Arrays;
import java.util.Objects;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.lwjgl3.alert.FlixelLwjgl3Alerter;
import me.stringdotjar.flixelgdx.backend.lwjgl3.runtime.reflect.FlixelReflectASMHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;

import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;

/**
 * Launches the desktop (LWJGL3) version of the Flixel game.
 */
public class FlixelLwjgl3Launcher {

  /**
   * Launches the LWJGL3 version of the Flixel game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode and with a default configuration object. This should be called from the main method of the
   * libGDX LWJGL3 launcher class, and the game instance should be created in the same general area.
   *
   * @param game The game instance to launch.
   */
  public static void launch(FlixelGame game) {
    launch(game, FlixelRuntimeMode.RELEASE, "");
  }

  /**
   * Launches the LWJGL3 version of the Flixel game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode and with pre-made configuration object. This should be called from the main method of the
   * libGDX LWJGL3 launcher class, and the game instance should be created in the same general area.
   *
   * @param game The game instance to launch.
   * @param icons Window icon paths. Make sure your icons actually exist and are valid!
   */
  public static void launch(FlixelGame game, String... icons) {
    launch(game, FlixelRuntimeMode.RELEASE, icons);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game with the given runtime mode and a pre-made configuration object.
   * This should be called from the main method of the libGDX LWJGL3 launcher class, and the game instance
   * should be created in the same general area.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param icons Window icon paths. Make sure your icons actually exist and are valid!
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode, String... icons) {
    Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
    configuration.setTitle(game.getTitle());
    configuration.useVsync(game.isVsync());
    configuration.setForegroundFPS(game.getFramerate());
    if (game.isFullscreen()) {
      configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
    } else {
      configuration.setWindowedMode(game.getViewWidth(), game.getViewHeight());
    }
    // Ensure the icons are not null, empty, or whitespace only.
    configuration.setWindowIcon(Arrays.stream(icons)
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toArray(String[]::new));
    configuration.setWindowListener(new Lwjgl3WindowAdapter() {
      @Override
      public void focusGained() {
        super.focusGained();
        if (Flixel.getGame() == null) {
          return;
        }
        Flixel.getGame().onWindowFocused();
      }

      @Override
      public void focusLost() {
        if (Flixel.getGame() == null) {
          return;
        }
        if (!Flixel.getGame().isMinimized()) {
          super.focusLost();
          Flixel.getGame().onWindowUnfocused();
        }
      }

      @Override
      public void iconified(boolean isIconified) {
        super.iconified(isIconified);
        if (Flixel.getGame() == null) {
          return;
        }
        Flixel.getGame().onWindowMinimized(isIconified);
      }
    });

    launch(game, runtimeMode, configuration);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode using the given configuration. This should be called from the main method of the libGDX LWJGL3 launcher class.
   *
   * <p>This method is useful if you have an existing libGDX project with an already made configuration object and
   * you want to integrate FlixelGDX into it.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param configuration The {@link Lwjgl3ApplicationConfiguration} to use.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode, Lwjgl3ApplicationConfiguration configuration) {
    Flixel.setAlerter(new FlixelLwjgl3Alerter());
    Flixel.setStackTraceProvider(new FlixelDefaultStackTraceProvider());
    Flixel.setReflection(new FlixelReflectASMHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    Flixel.initialize(game);

    new Lwjgl3Application(game, configuration);
  }
}
