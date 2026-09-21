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
package org.flixelgdx.backend.desktop;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.audio.FlixelSoundManager;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.backend.desktop.audio.FlixelMiniAudioFactory;
import org.flixelgdx.backend.desktop.debug.FlixelImGuiDebugOverlay;
import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.backend.desktop.graphics.FlixelKtx2Loader;
import org.flixelgdx.backend.desktop.input.FlixelDesktopInputDevice;
import org.flixelgdx.backend.desktop.input.FlixelSdlGamepadProvider;
import org.flixelgdx.backend.desktop.input.FlixelSdlMouseIconManager;
import org.flixelgdx.backend.desktop.text.FlixelStbFontRasterizer;
import org.flixelgdx.backend.jvm.asset.FlixelJvmAssetManager;
import org.flixelgdx.backend.jvm.file.FlixelJvmFiles;
import org.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
import org.flixelgdx.backend.jvm.logging.FlixelJvmStackTraceProvider;
import org.flixelgdx.backend.jvm.runtime.FlixelJvmRuntimeDevice;
import org.flixelgdx.text.FlixelFontRegistry;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The one-line entry point for a desktop FlixelGDX game.
 *
 * <p>Call {@link #launch(FlixelGame)} from your {@code main} method and nothing else is required:
 * the launcher installs every desktop backend piece (window, input, graphics, audio, files,
 * logging) and then starts the game. Developers do not need to call {@link Flixel#start(FlixelGame, FlixelGameRunner)}
 * themselves; it is the internal step this launcher performs once the backend is wired.
 *
 * <p>To set a custom window icon (shown in the taskbar and title bar), pass the icon resource paths
 * to {@link #launch(FlixelGame, String...)}. Icons are resolved from the Java resources folder, not
 * the game's assets folder, and should be listed from smallest to largest so the OS can pick the
 * best fit. An exception is thrown immediately if any path cannot be found, so missing icons are
 * caught at startup rather than silently ignored at runtime.
 *
 * <pre>{@code
 * public final class DesktopLauncher {
 *   public static void main(String[] args) {
 *     FlixelDesktopLauncher.launch(new MyGame(),
 *         "icons/icon16.png", "icons/icon32.png", "icons/icon256.png");
 *   }
 *   private DesktopLauncher() {}
 * }
 * }</pre>
 */
public final class FlixelDesktopLauncher {

  private FlixelDesktopLauncher() {}

  /**
   * Launches the game, choosing the runtime mode from the {@code flixel.mode} system property so the
   * same code path serves development and release without a code change.
   *
   * <p>This is the call almost every game uses. The mode is resolved as follows:
   *
   * <ul>
   *   <li>{@code -Dflixel.mode=debug} (or {@code -Dflixel.debug=true}) starts in
   *       {@link FlixelRuntimeMode#DEBUG DEBUG}, which enables the debug overlay and diagnostics.</li>
   *   <li>{@code -Dflixel.mode=test} starts in {@link FlixelRuntimeMode#TEST TEST}.</li>
   *   <li>No property (the default, and how packaged/published builds run) starts in
   *       {@link FlixelRuntimeMode#RELEASE RELEASE}.</li>
   * </ul>
   *
   * <p>Because the flag lives in the launch command (for example a Gradle {@code debug} run task) and
   * not in code, there is nothing to remember to remove before publishing: a shipped build simply runs
   * without the property and lands in release mode. To force a specific mode regardless of the
   * property, call {@link #launch(FlixelGame, FlixelRuntimeMode)} directly.
   *
   * @param game The game instance to run.
   */
  public static void launch(@NotNull FlixelGame game) {
    launch(game, resolveRuntimeMode(), (String[]) null);
  }

  /**
   * Launches the game with a set of window icons, using the runtime mode from the
   * {@code flixel.mode} system property.
   *
   * <p>Icons are loaded from the Java resources folder (not the game's assets folder). Provide
   * them in order from smallest to largest so the OS can pick the best fit for each context (for
   * example the taskbar may use 16x16 and the alt-tab switcher may use 48x48). A
   * {@link RuntimeException} is thrown immediately if any icon path cannot be found in the
   * classpath, so missing icons are caught at startup rather than silently ignored.
   *
   * <p>Example:
   *
   * <pre>{@code
   * FlixelDesktopLauncher.launch(new MyGame(),
   *     "icons/icon16.png",
   *     "icons/icon32.png",
   *     "icons/icon256.png");
   * }</pre>
   *
   * @param game The game instance to run.
   * @param icons Resource paths for the window icons, ordered from smallest to largest. Each path
   *              is resolved from the classpath root (for example {@code "icons/icon16.png"} loads
   *              {@code /icons/icon16.png}). May be empty to set no icon.
   * @throws RuntimeException if any icon path cannot be found in the classpath.
   */
  public static void launch(@NotNull FlixelGame game, @NotNull String... icons) {
    launch(game, resolveRuntimeMode(), icons);
  }

  /**
   * Launches the game with full control over the runtime mode.
   *
   * @param game The game instance to run.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(@NotNull FlixelGame game, @NotNull FlixelRuntimeMode runtimeMode) {
    launch(game, runtimeMode, (String[]) null);
  }

  /**
   * Launches the game with full control over the runtime mode and a set of window icons.
   *
   * <p>Icons are loaded from the Java resources folder (not the game's assets folder). Provide
   * them in order from smallest to largest so the OS can pick the best fit for each context. A
   * {@link RuntimeException} is thrown immediately if any icon path cannot be found in the
   * classpath.
   *
   * @param game The game instance to run.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   * @param icons Resource paths for the window icons, ordered from smallest to largest. Each path
   *              is resolved from the classpath root (for example {@code "icons/icon16.png"} loads
   *              {@code /icons/icon16.png}). May be {@code null} or empty to set no icon.
   * @throws RuntimeException if any icon path cannot be found in the classpath.
   */
  public static void launch(@NotNull FlixelGame game, @NotNull FlixelRuntimeMode runtimeMode,
      @Nullable String... icons) {
    if (icons != null) {
      for (String path : icons) {
        if (FlixelDesktopLauncher.class.getResource("/" + path) == null) {
          throw new RuntimeException("Icon not found in resources: " + path);
        }
      }
    }

    FlixelConfig config = Flixel.config;
    Flixel.runtime = new FlixelJvmRuntimeDevice();
    if (Flixel.runtime.isRunningFromJar() && !AnsiConsole.isInstalled()) {
      AnsiConsole.systemInstall();
    }

    FlixelSdlWindow window = new FlixelSdlWindow();
    FlixelDesktopHostIntegration host = new FlixelDesktopHostIntegration();
    FlixelDesktopInputDevice input = new FlixelDesktopInputDevice();
    FlixelBgfxGraphics graphics = new FlixelBgfxGraphics();
    FlixelSdlGamepadProvider gamepads = new FlixelSdlGamepadProvider();
    FlixelSdlMouseIconManager iconManager = new FlixelSdlMouseIconManager();
    int width = config.getWidth();
    int height = config.getHeight();

    Flixel.alert = new FlixelDesktopAlerter();
    Flixel.window = window;
    Flixel.host = host;
    Flixel.files = new FlixelJvmFiles();
    Flixel.input = input;
    Flixel.graphics = graphics;
    Flixel.runtime.setStackTraceProvider(new FlixelJvmStackTraceProvider());
    Flixel.log.logFileHandler = new FlixelJvmLogFileHandler();
    FlixelSoundManager.defaultFactory = FlixelMiniAudioFactory.create();
    FlixelGameRunner runner = new FlixelDesktopRunner(window, input, graphics, gamepads,
        iconManager, host, width, height, icons);

    FlixelJvmAssetManager assets = new FlixelJvmAssetManager();
    assets.registerLoader(".ktx2", new FlixelKtx2Loader());
    assets.setCompressedTexturesEnabled(true);
    Flixel.assets = assets;

    FlixelFontRegistry.setRasterizer(new FlixelStbFontRasterizer());

    Flixel.boot.afterStart(() -> {
      Flixel.debug.setOverlayFactory(FlixelImGuiDebugOverlay::new);
      Flixel.gamepads.setGamepadProvider(gamepads);
      Flixel.gamepads.addMappingResolver(gamepads);
      Flixel.mouse.setMouseIconManager(iconManager);
    });

    Flixel.runtime.setMode(runtimeMode);

    Flixel.start(game, runner);
    if (AnsiConsole.isInstalled()) {
      AnsiConsole.systemUninstall();
    }
  }

  /**
   * Resolves the runtime mode from the {@code flixel.mode} (or legacy {@code flixel.debug}) system
   * property, defaulting to {@link FlixelRuntimeMode#RELEASE RELEASE}.
   *
   * @return The runtime mode requested on the command line, or {@code RELEASE} when none was.
   */
  private static FlixelRuntimeMode resolveRuntimeMode() {
    String mode = System.getProperty("flixel.mode", "").trim().toLowerCase();
    return switch (mode) {
      case "debug" -> FlixelRuntimeMode.DEBUG;
      case "test" -> FlixelRuntimeMode.TEST;
      case "", "release" -> FlixelRuntimeMode.RELEASE;
      default -> {
        Flixel.warn("Desktop", "Unknown flixel.mode '" + mode + "'; defaulting to RELEASE.");
        yield FlixelRuntimeMode.RELEASE;
      }
    };
  }
}
