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
package org.flixelgdx.backend.html5;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelGame;
import org.flixelgdx.backend.FlixelGameRunner;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.backend.html5.audio.FlixelWebAudioFactory;
import org.flixelgdx.backend.html5.file.FlixelHtml5Files;
import org.flixelgdx.backend.html5.graphics.FlixelHtml5Graphics;
import org.flixelgdx.backend.html5.input.FlixelHtml5InputDevice;
import org.flixelgdx.backend.html5.input.FlixelWebGamepadProvider;
import org.teavm.jso.JSBody;

/**
 * The one-line entry point for a FlixelGDX game running in the browser through TeaVM.
 *
 * <p>Like the desktop launcher, this installs every web backend piece (canvas window, DOM input,
 * WebGL2 graphics, Web Audio, browser files and host integration) and then starts the game, so a
 * game's TeaVM entry point is a single call:
 *
 * <pre>{@code
 * public final class WebLauncher {
 *   public static void main(String[] args) {
 *     FlixelHtml5Launcher.launch(new MyGame());
 *   }
 *   private WebLauncher() {}
 * }
 * }</pre>
 *
 * <p>The runtime mode is chosen from the page URL rather than a command-line flag, since a browser
 * has no launch arguments. Adding {@code ?flixel.mode=debug} (or the shorthand {@code ?debug}) to
 * the address starts the game in {@link FlixelRuntimeMode#DEBUG DEBUG} without a rebuild, and
 * {@code ?flixel.mode=test} selects {@link FlixelRuntimeMode#TEST TEST}. With no such parameter the
 * game runs in {@link FlixelRuntimeMode#RELEASE RELEASE}. The web build plugin can bake a default
 * into the generated page, which the URL parameter then overrides.
 */
public final class FlixelHtml5Launcher {

  /** The canvas element id the runner looks for, matching the build plugin's generated page. */
  private static final String CANVAS_ID = "flixelgdx-canvas";

  private FlixelHtml5Launcher() {}

  /**
   * Launches the game, choosing the runtime mode from the page URL.
   *
   * @param game The game instance to run.
   */
  public static void launch(FlixelGame game) {
    launch(game, resolveRuntimeMode());
  }

  /**
   * Launches the game with an explicit runtime mode, ignoring the URL parameter.
   *
   * @param game The game instance to run.
   * @param runtimeMode The runtime mode for this session.
   */
  public static void launch(FlixelGame game, FlixelRuntimeMode runtimeMode) {
    FlixelHtml5Graphics graphics = new FlixelHtml5Graphics();
    FlixelHtml5Window window = new FlixelHtml5Window();
    FlixelHtml5InputDevice input = new FlixelHtml5InputDevice();
    FlixelWebGamepadProvider gamepads = new FlixelWebGamepadProvider();

    Flixel.alert = new FlixelHtml5Alerter();
    Flixel.window = window;
    Flixel.host = new FlixelHtml5HostIntegration();
    Flixel.runtime = new FlixelHtml5RuntimeDevice();
    Flixel.files = new FlixelHtml5Files();
    Flixel.input = input;
    Flixel.graphics = graphics;
    Flixel.assets = new FlixelHtml5AssetManager();
    Flixel.soundFactory = FlixelWebAudioFactory.create();

    window.setTitle(game.getTitle());
    gamepads.attach();

    // Flixel.gamepads and Flixel.mouse are created inside Flixel.start, so wire the web gamepad
    // provider once those systems exist, just before the runner takes over the loop.
    Flixel.afterStart.add(() -> {
      Flixel.gamepads.setGamepadProvider(gamepads);
      Flixel.gamepads.addMappingResolver(gamepads);
    });

    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);

    FlixelGameRunner runner = new FlixelHtml5Runner(CANVAS_ID, game.getInitialWidth(), game.getInitialHeight(),
        graphics, window, input);
    Flixel.start(game, runner);
  }

  /**
   * Resolves the runtime mode from the page URL, defaulting to {@link FlixelRuntimeMode#RELEASE}.
   *
   * @return The runtime mode requested in the URL, or {@code RELEASE} when none was.
   */
  private static FlixelRuntimeMode resolveRuntimeMode() {
    String mode = urlMode();
    if (mode == null) {
      return FlixelRuntimeMode.RELEASE;
    }
    return switch (mode.trim().toLowerCase()) {
      case "debug" -> FlixelRuntimeMode.DEBUG;
      case "test" -> FlixelRuntimeMode.TEST;
      default -> FlixelRuntimeMode.RELEASE;
    };
  }

  /**
   * Reads the requested mode from the page URL query string.
   *
   * <p>Prefers the explicit {@code flixel.mode} parameter, then falls back to the {@code debug}
   * shorthand flag, then a {@code window.flixelMode} default the build plugin may have injected.
   *
   * @return The requested mode string, or {@code null} when none is present.
   */
  @JSBody(script = """
      var params = new URLSearchParams(window.location.search);
      if (params.has('flixel.mode')) { return params.get('flixel.mode'); }
      if (params.has('debug')) { return 'debug'; }
      return window.flixelMode || null;
      """)
  private static native String urlMode();
}
