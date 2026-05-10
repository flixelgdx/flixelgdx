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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.common.audio.FlixelMiniAudioSoundHandler;
import me.stringdotjar.flixelgdx.backend.jvm.runtime.FlixelJvmRuntimeProbe;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import me.stringdotjar.flixelgdx.backend.lwjgl3.alert.FlixelLwjgl3Alerter;
import me.stringdotjar.flixelgdx.backend.lwjgl3.debug.FlixelImGuiDebugOverlay;
import me.stringdotjar.flixelgdx.backend.lwjgl3.input.FlixelLwjgl3MouseIconManager;
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
   * Ensures Flixel window notifications, optional user {@link com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener},
   * and close-absorption wrapping are installed. Only {@link FlixelLwjgl3ApplicationConfiguration} is supported so the
   * user listener can be captured without reflection, which allows AOT compilers like GraalVM to not scream at you.
   */
  public static void attachFlixelWindowListener(FlixelLwjgl3ApplicationConfiguration configuration) {
    configuration.attachFlixelWindowListenerChain();
  }

  /**
   * Linux AWT uses GTK for dialogs. Prefer GTK3 before any AWT class loads to reduce broken embeddings
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
    FlixelLwjgl3ApplicationConfiguration configuration = new FlixelLwjgl3ApplicationConfiguration();
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
   * @param configuration The {@link FlixelLwjgl3ApplicationConfiguration} to use. Use this type (not a raw
   * {@link Lwjgl3ApplicationConfiguration}) so a custom {@link com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener}
   * can be preserved without reflection.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode, FlixelLwjgl3ApplicationConfiguration configuration) {
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
