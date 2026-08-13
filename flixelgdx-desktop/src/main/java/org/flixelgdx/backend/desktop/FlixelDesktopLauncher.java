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
import org.flixelgdx.backend.desktop.graphics.FlixelBgfxGraphics;
import org.flixelgdx.backend.desktop.graphics.FlixelKtx2Loader;
import org.flixelgdx.backend.desktop.input.FlixelDesktopInputDevice;
import org.flixelgdx.backend.desktop.input.FlixelSdlGamepadProvider;
import org.flixelgdx.backend.desktop.input.FlixelSdlMouseIconManager;
import org.flixelgdx.backend.desktop.text.FlixelStbFontRasterizer;
import org.flixelgdx.backend.jvm.asset.FlixelJvmAssetManager;
import org.flixelgdx.backend.jvm.file.FlixelJvmFiles;
import org.flixelgdx.backend.jvm.logging.FlixelJvmStackTraceProvider;
import org.flixelgdx.backend.jvm.logging.FlixelJvmLogFileHandler;
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
   * Launches the game in {@link FlixelRuntimeMode#RELEASE RELEASE} mode. This is the call almost
   * every game uses.
   *
   * @param game The game instance to run.
   */
  public static void launch(@NotNull FlixelGame game) {
    launch(game, FlixelRuntimeMode.RELEASE);
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
