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
package org.flixelgdx.backend.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.crashinvaders.basisu.gdx.Ktx2TextureLoader;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.common.audio.FlixelMiniAudioSoundHandler;
import org.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import org.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import org.flixelgdx.backend.jvm.runtime.FlixelJvmRuntimeProbe;
import org.flixelgdx.backend.lwjgl3.alert.FlixelLwjgl3Alerter;
import org.flixelgdx.backend.lwjgl3.debug.FlixelImGuiDebugOverlay;
import org.flixelgdx.backend.lwjgl3.input.FlixelLwjgl3MouseIconManager;
import org.flixelgdx.backend.runtime.FlixelRuntimeMode;
import org.flixelgdx.util.FlixelRuntimeUtil;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * Launches the desktop (LWJGL3) version of the Flixel game.
 */
public class FlixelLwjgl3Launcher {

  private static volatile boolean linuxAwtCompatibilityPrepared;

  /**
   * Launches the LWJGL3 version of the Flixel game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode with a default configuration. This should be called from the main method of the libGDX
   * LWJGL3 launcher class.
   *
   * @param game The game instance to launch.
   */
  public static void launch(FlixelGame game) {
    launch(game, FlixelRuntimeMode.RELEASE, (Runnable) null);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game in {@link FlixelRuntimeMode#RELEASE RELEASE}
   * mode with a default configuration and the given window icons.
   *
   * @param game The game instance to launch.
   * @param icons Window icon paths. Make sure your icons actually exist and are valid!
   */
  public static void launch(FlixelGame game, String... icons) {
    launch(game, FlixelRuntimeMode.RELEASE, buildDefaultConfig(game, icons), null);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game with the given runtime mode and a default
   * configuration built from the game's own settings.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param icons Window icon paths. Make sure your icons actually exist and are valid!
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode, String... icons) {
    launch(game, runtimeMode, buildDefaultConfig(game, icons), null);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game with the given runtime mode, a default
   * configuration, and an optional pre-initialization callback.
   *
   * <p>Use this when you want to inject custom services (e.g. a custom debug overlay) without
   * supplying a full configuration object. The configuration is built from the game's own settings
   * exactly as {@link #buildDefaultConfig(FlixelGame, String[])} would produce it.
   *
   * <pre>{@code
   * FlixelLwjgl3Launcher.launch(game, FlixelRuntimeMode.DEBUG, () -> {
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
    launch(game, runtimeMode, buildDefaultConfig(game), onBeforeInitialize);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game using the given configuration. Useful when you
   * have an existing libGDX project with an already-made configuration object.
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param configuration The {@link FlixelLwjgl3ApplicationConfiguration} to use. Use this type
   *     (not a raw {@link Lwjgl3ApplicationConfiguration}) so a custom
   *     {@link com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener} can be preserved without
   *     reflection.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      FlixelLwjgl3ApplicationConfiguration configuration) {
    launch(game, runtimeMode, configuration, null);
  }

  /**
   * Launches the LWJGL3 version of the Flixel game with full control over both the window
   * configuration and the pre-initialization callback.
   *
   * <p>{@code onBeforeInitialize} fires after all default FlixelGDX backend services (alerter,
   * audio, debug overlay, etc.) have been registered but before {@link Flixel#initialize} is
   * called. Use it to override any of those defaults without duplicating the rest of the launcher
   * wiring. Call {@link #buildDefaultConfig(FlixelGame, String[])} to get the same starting
   * configuration the other overloads would use, then customize it before passing it in:
   *
   * <pre>{@code
   * FlixelLwjgl3ApplicationConfiguration config = FlixelLwjgl3Launcher.buildDefaultConfig(game);
   * config.setWindowedMode(1920, 1080);
   * FlixelLwjgl3Launcher.launch(game, FlixelRuntimeMode.DEBUG, config, () -> {
   *   Flixel.setDebugOverlay(MyCustomOverlay::new);
   * });
   * }</pre>
   *
   * @param game The game instance to launch.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param configuration The {@link FlixelLwjgl3ApplicationConfiguration} to use.
   * @param onBeforeInitialize Optional callback invoked just before {@link Flixel#initialize}.
   *     Pass {@code null} to skip.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode,
      FlixelLwjgl3ApplicationConfiguration configuration, @Nullable Runnable onBeforeInitialize) {
    prepareLinuxAwtCompatibility();
    if (game.isTransparentFramebufferRequested()) {
      configuration.setTransparentFramebuffer(true);
    }
    FlixelRuntimeUtil.setRuntimeProbe(new FlixelJvmRuntimeProbe());
    if (FlixelRuntimeUtil.isRunningFromJar() && !AnsiConsole.isInstalled()) {
      AnsiConsole.systemInstall();
    }

    Flixel.setAlerter(new FlixelLwjgl3Alerter());
    Flixel.setWindow(new FlixelLwjgl3Window());
    Flixel.setHost(new FlixelLwjgl3HostIntegration());
    Flixel.setStackTraceProvider(new FlixelDefaultStackTraceProvider());
    Flixel.setLogFileHandler(new FlixelJvmLogFileHandler());
    Flixel.setSoundBackendFactory(new FlixelMiniAudioSoundHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);
    if (runtimeMode == FlixelRuntimeMode.DEBUG) {
      Flixel.setDebugOverlay(FlixelImGuiDebugOverlay::new);
    }
    if (onBeforeInitialize != null) {
      onBeforeInitialize.run();
    }
    Flixel.initialize(game);
    Flixel.assets.setKtx2LoaderInstaller(
        manager -> manager.setLoader(Texture.class, ".ktx2", new Ktx2TextureLoader(manager.getFileHandleResolver())));
    Flixel.gamepads.setHapticsProvider(new FlixelLwjgl3HapticsProvider());
    Flixel.mouse.setMouseIconManager(new FlixelLwjgl3MouseIconManager());

    new Lwjgl3Application(game, configuration);

    if (AnsiConsole.isInstalled()) {
      AnsiConsole.systemUninstall();
    }
  }

  /**
   * Builds the default {@link FlixelLwjgl3ApplicationConfiguration} for a game the same way the
   * simple {@code launch} overloads would. Use this when you need to customize the configuration
   * before passing it to {@link #launch(FlixelGame, FlixelRuntimeMode, FlixelLwjgl3ApplicationConfiguration, Runnable)}.
   *
   * <pre>{@code
   * FlixelLwjgl3ApplicationConfiguration config = FlixelLwjgl3Launcher.buildDefaultConfig(game);
   * config.setWindowedMode(1920, 1080);
   * FlixelLwjgl3Launcher.launch(game, FlixelRuntimeMode.RELEASE, config);
   * }</pre>
   *
   * @param game The game instance the configuration is built for.
   * @param icons Window icon paths (may be empty or omitted). Invalid paths are silently skipped.
   * @return A new {@link FlixelLwjgl3ApplicationConfiguration} ready to pass to {@code launch}.
   */
  public static FlixelLwjgl3ApplicationConfiguration buildDefaultConfig(FlixelGame game,
      String... icons) {
    Objects.requireNonNull(game, "The game object provided cannot be null!");
    FlixelLwjgl3ApplicationConfiguration configuration = new FlixelLwjgl3ApplicationConfiguration();
    configuration.setTitle(game.getTitle());
    configuration.useVsync(game.isVsync());
    configuration.setForegroundFPS(game.getFramerate());
    if (game.isFullscreen()) {
      configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
    } else {
      configuration.setWindowedMode(game.getWidth(), game.getHeight());
    }
    configuration.setWindowIcon(Arrays.stream(icons)
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new));
    if (game.isTransparentFramebufferRequested()) {
      configuration.setTransparentFramebuffer(true);
    }
    configuration.attachFlixelWindowListenerChain();
    return configuration;
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
}
