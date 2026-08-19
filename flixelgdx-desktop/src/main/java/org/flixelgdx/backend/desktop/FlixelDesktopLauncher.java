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
import org.flixelgdx.FlixelGame;
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

/**
 * The one-line entry point for a desktop FlixelGDX game.
 *
 * <p>Call {@link #launch(FlixelGame)} from your {@code main} method and nothing else is required:
 * the launcher installs every desktop backend piece (window, input, graphics, audio, files,
 * logging) and then starts the game. Developers do not need to call {@link Flixel#start(FlixelGame, FlixelGameRunner)}
 * themselves; it is the internal step this launcher performs once the backend is wired.
 *
 * <pre>{@code
 * public final class DesktopLauncher {
 *   public static void main(String[] args) {
 *     FlixelDesktopLauncher.launch(new MyGame());
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
    launch(game, resolveRuntimeMode());
  }

  /**
   * Resolves the runtime mode from the {@code flixel.mode} (or legacy {@code flixel.debug}) system
   * property, defaulting to {@link FlixelRuntimeMode#RELEASE RELEASE}.
   *
   * @return The runtime mode requested on the command line, or {@code RELEASE} when none was.
   */
  private static FlixelRuntimeMode resolveRuntimeMode() {
    String mode = System.getProperty("flixel.mode", "").trim().toLowerCase();
    if (mode.isEmpty() && "true".equalsIgnoreCase(System.getProperty("flixel.debug", ""))) {
      return FlixelRuntimeMode.DEBUG;
    }
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

  /**
   * Launches the game with full control over the runtime mode and a pre-start callback.
   *
   * <p>{@code onBeforeStart} runs after every default backend service has been installed but before
   * the game starts, so it is the right place to replace a default without duplicating the rest of
   * the launcher wiring.
   *
   * @param game The game instance to run.
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(@NotNull FlixelGame game, @NotNull FlixelRuntimeMode runtimeMode) {
    Flixel.runtime = new FlixelJvmRuntimeDevice();
    if (Flixel.runtime.isRunningFromJar() && !AnsiConsole.isInstalled()) {
      AnsiConsole.systemInstall();
    }

    FlixelSdlWindow window = new FlixelSdlWindow();
    FlixelDesktopInputDevice input = new FlixelDesktopInputDevice();
    FlixelBgfxGraphics graphics = new FlixelBgfxGraphics();
    FlixelSdlGamepadProvider gamepads = new FlixelSdlGamepadProvider();
    FlixelSdlMouseIconManager iconManager = new FlixelSdlMouseIconManager();
    int width = game.getInitialWidth();
    int height = game.getInitialHeight();

    Flixel.alert = new FlixelDesktopAlerter();
    Flixel.window = window;
    Flixel.host = new FlixelDesktopHostIntegration();
    Flixel.files = new FlixelJvmFiles();
    Flixel.input = input;
    Flixel.graphics = graphics;
    Flixel.stackTraceProvider = new FlixelJvmStackTraceProvider();
    Flixel.logFileHandler = new FlixelJvmLogFileHandler();
    Flixel.soundFactory = FlixelMiniAudioFactory.create();
    FlixelGameRunner runner = new FlixelDesktopRunner(window, input, graphics, gamepads, iconManager, width, height);

    FlixelJvmAssetManager assets = new FlixelJvmAssetManager();
    assets.registerLoader(".ktx2", new FlixelKtx2Loader());
    assets.setCompressedTexturesEnabled(true);
    Flixel.assets = assets;

    FlixelFontRegistry.setRasterizer(new FlixelStbFontRasterizer());

    // Flixel.gamepads and Flixel.mouse are created inside Flixel.start, so wire their desktop
    // implementations once they exist, just before the runner takes over the loop.
    Flixel.afterStart.add(() -> {
      Flixel.gamepads.setGamepadProvider(gamepads);
      Flixel.gamepads.addMappingResolver(gamepads);
      Flixel.mouse.setMouseIconManager(iconManager);
    });

    // Install the Dear ImGui debug overlay factory. It is only instantiated when debug mode is on
    // (FlixelGame.create gates on it), so this is inert in release builds while still letting a game
    // flip debug mode on at runtime and get the full overlay.
    Flixel.setDebugOverlay(FlixelImGuiDebugOverlay::new);

    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);

    try {
      Flixel.start(game, runner);
    } finally {
      if (AnsiConsole.isInstalled()) {
        AnsiConsole.systemUninstall();
      }
    }
  }
}
