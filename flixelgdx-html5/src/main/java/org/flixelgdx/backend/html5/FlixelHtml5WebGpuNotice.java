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

import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

/**
 * Tells the player, in the page itself, that their browser cannot run the game because WebGPU is missing.
 *
 * <p>FlixelGDX draws everything with WebGPU and has no fallback, so on a browser without it the only honest thing
 * to do is to explain what is wrong. A blank canvas and a line in the console would leave most players guessing.
 * The notice is a full-page panel built from plain DOM elements with inline styles, so it looks the same whether the
 * game uses the generated page or a hand-written one, and it covers the loading overlay of the generated page.
 *
 * <p>The texts that come from the browser are set through {@code textContent}, never as HTML, so an error message
 * can never inject markup.
 */
public final class FlixelHtml5WebGpuNotice {

  /** The headline of the notice. */
  public static final String TITLE = "WebGPU is not available";

  /** One sentence on why the game needs WebGPU. */
  public static final String EXPLANATION =
      "This game draws with WebGPU, the browser's modern graphics API, and there is no fallback.";

  /** Which browsers have WebGPU, written as short lines. Each line becomes one list item. */
  public static final String SUPPORTED_BROWSERS =
      "Chrome and Edge 113 or newer\nSafari 26 or newer\nFirefox 141 or newer on Windows";

  /** What to try when the browser should support WebGPU but does not. */
  public static final String HINT =
      "Update your browser and your graphics driver. The page must be opened over HTTPS or from localhost. On some "
          + "systems, such as Linux, WebGPU may still be switched off by default and can be enabled in the "
          + "browser's settings.";

  private FlixelHtml5WebGpuNotice() {}

  /**
   * Shows the notice over the page. Calling it again replaces the previous notice.
   *
   * @param reason The technical reason, shown in small print at the bottom. May be empty.
   */
  public static void show(@NotNull String reason) {
    showNotice(TITLE, EXPLANATION, SUPPORTED_BROWSERS, HINT, reason);
  }

  @JSBody(params = { "title", "explanation", "browsers", "hint", "reason" }, script = """
      var old = document.getElementById('flixel-webgpu-notice');
      if (old && old.parentNode) { old.parentNode.removeChild(old); }
      function mk(tag, css, text) {
        var el = document.createElement(tag);
        el.style.cssText = css;
        if (text) { el.textContent = text; }
        return el;
      }
      var root = mk('div', 'position:fixed;left:0;top:0;right:0;bottom:0;z-index:2147483647;background:#000;'
        + 'color:#fff;display:flex;align-items:center;justify-content:center;overflow:auto;'
        + 'font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;');
      root.id = 'flixel-webgpu-notice';
      var card = mk('div', 'box-sizing:border-box;max-width:560px;margin:24px;padding:28px 32px;'
        + 'border:2px solid #fff;line-height:1.5;');
      card.appendChild(mk('h1', 'margin:0 0 12px 0;font-size:26px;', title));
      card.appendChild(mk('p', 'margin:0 0 16px 0;font-size:16px;', explanation));
      card.appendChild(mk('p', 'margin:0 0 6px 0;font-size:16px;font-weight:600;', 'Browsers with WebGPU:'));
      var list = mk('ul', 'margin:0 0 16px 0;padding-left:22px;font-size:16px;');
      browsers.split('\\n').forEach(function (line) { list.appendChild(mk('li', '', line)); });
      card.appendChild(list);
      card.appendChild(mk('p', 'margin:0 0 16px 0;font-size:14px;color:#ccc;', hint));
      if (reason) {
        card.appendChild(mk('p', 'margin:0;font-size:12px;color:#ff6b6b;word-break:break-word;', reason));
      }
      root.appendChild(card);
      document.body.appendChild(root);
      """)
  private static native void showNotice(String title, String explanation, String browsers, String hint,
      String reason);
}
