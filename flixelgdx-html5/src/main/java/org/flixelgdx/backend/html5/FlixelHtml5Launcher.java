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
import org.flixelgdx.backend.html5.graphics.FlixelHtml5Graphics;

/**
 * The main entry point for a FlixelGDX game to launch inside a browser.
 *
 * <p>FlixelGDX programmatically creates a new HTML canvas under the simple ID {@code "canvas"}.
 * The framework will natively select either WebGPU or WebGL as the renderer, depending on whether
 * {@code navigator.gpu} returns {@code null} on TeaVM's side when the game is compiled to
 * JavaScript or WebAssembly.
 */
public final class FlixelHtml5Launcher {

  public static void launch(FlixelGame game) {
    var graphics = new FlixelHtml5Graphics();
    var canvas = new FlixelHtml5Canvas();

    Flixel.alert = new FlixelHtml5Alerter();
    Flixel.graphics = graphics;
    Flixel.window = canvas;

    var runner = new FlixelHtml5Runner(game.getTitle(),
        game.getInitialWidth(), game.getInitialHeight(), graphics, canvas);

    try {
      Flixel.start(game, runner);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private FlixelHtml5Launcher() {}
}
