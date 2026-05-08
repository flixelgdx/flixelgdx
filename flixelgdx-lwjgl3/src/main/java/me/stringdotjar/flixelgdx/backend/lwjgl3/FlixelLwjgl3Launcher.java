/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.lwjgl3;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import java.lang.reflect.Field;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.common.audio.FlixelMiniAudioSoundHandler;
import me.stringdotjar.flixelgdx.backend.jvm.runtime.FlixelJvmRuntimeProbe;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import me.stringdotjar.flixelgdx.backend.lwjgl3.alert.FlixelLwjgl3Alerter;
import me.stringdotjar.flixelgdx.backend.lwjgl3.debug.FlixelImGuiDebugOverlay;
import me.stringdotjar.flixelgdx.backend.lwjgl3.runtime.reflect.FlixelReflectASMHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;

import org.fusesource.jansi.AnsiConsole;

/**
 * Launches the desktop (LWJGL3) version of the Flixel game.
 */
public class FlixelLwjgl3Launcher {

  private static volatile boolean linuxAwtCompatibilityPrepared;

  /**
   * Ensures Flixel window notifications, optional user {@link Lwjgl3WindowListener}, and close-absorption wrapping
   * are installed. Idempotent when the configuration already uses {@link FlixelLwjgl3ChainingWindowListener}.
   */
  public static void attachFlixelWindowListener(Lwjgl3ApplicationConfiguration configuration) {
    Lwjgl3WindowListener current = readConfigurationWindowListener(configuration);
    if (current instanceof FlixelLwjgl3ChainingWindowListener existing) {
      FlixelLwjgl3Window.configureCloseHandlingHook(existing);
      return;
    }
    FlixelLwjgl3NotifyWindowListener notify = new FlixelLwjgl3NotifyWindowListener(current);
    FlixelLwjgl3ChainingWindowListener chain = new FlixelLwjgl3ChainingWindowListener(notify);
    configuration.setWindowListener(chain);
    FlixelLwjgl3Window.configureCloseHandlingHook(chain);
  }

  private static Lwjgl3WindowListener readConfigurationWindowListener(Lwjgl3ApplicationConfiguration configuration) {
    try {
      Field field = Lwjgl3WindowConfiguration.class.getDeclaredField("windowListener");
      field.setAccessible(true);
      return (Lwjgl3WindowListener) field.get(configuration);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to read Lwjgl3 windowListener from configuration.", e);
    }
  }

  /**
   * Linux AWT uses GTK for tray icons and dialogs. Prefer GTK3 before any AWT class loads to reduce broken embeddings
   * (orphan windows, wrong icons) on Cinnamon and other GTK3 desktops.
   */
  private static void prepareLinuxAwtCompatibility() {
    if (linuxAwtCompatibilityPrepared) {
      return;
    }
    linuxAwtCompatibilityPrepared = true;
    if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
      System.setProperty("jdk.gtk.version", "3");
    }
  }

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
    Objects.requireNonNull(game, "The game object provided cannot be null!");
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
    if (game.isTransparentFramebufferRequested()) {
      configuration.setTransparentFramebuffer(true);
    }
    attachFlixelWindowListener(configuration);

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
    prepareLinuxAwtCompatibility();
    if (game.isTransparentFramebufferRequested()) {
      configuration.setTransparentFramebuffer(true);
    }
    attachFlixelWindowListener(configuration);
    FlixelRuntimeUtil.setRuntimeProbe(new FlixelJvmRuntimeProbe());
    if (FlixelRuntimeUtil.isRunningFromJar() && !AnsiConsole.isInstalled()) {
      AnsiConsole.systemInstall();
    }

    Flixel.setAlerter(new FlixelLwjgl3Alerter());
    Flixel.setWindow(new FlixelLwjgl3Window());
    Flixel.setHost(new FlixelLwjgl3HostIntegration());
    Flixel.setStackTraceProvider(new FlixelDefaultStackTraceProvider());
    Flixel.setReflection(new FlixelReflectASMHandler());
    Flixel.setLogFileHandler(new FlixelJvmLogFileHandler());
    Flixel.setSoundBackendFactory(new FlixelMiniAudioSoundHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    if (runtimeMode == FlixelRuntimeMode.DEBUG) {
      Flixel.setDebugOverlay(FlixelImGuiDebugOverlay::new);
    }
    Flixel.initialize(game);
    Flixel.mouse.setMouseIconManager(new FlixelLwjgl3MouseIconManager());

    new Lwjgl3Application(game, configuration);

    if (AnsiConsole.isInstalled()) {
      AnsiConsole.systemUninstall();
    }
  }
}
