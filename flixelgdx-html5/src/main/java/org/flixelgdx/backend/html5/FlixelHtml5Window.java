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
import org.flixelgdx.backend.FlixelWindow;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * The web platform's {@link FlixelWindow}, backed by an HTML canvas inside the browser page.
 *
 * <p>A desktop game owns a real operating-system window it can move, resize, and decorate. A web
 * game has none of that: it lives inside a single {@code <canvas>} element on a page the browser
 * controls. This class therefore maps the {@link FlixelWindow} contract onto what a browser can
 * actually do, which is a much smaller set. Anything the browser has no equivalent for (moving the
 * window, opacity, decorations, always-on-top) is left as the safe no-op the interface already
 * provides, so game code that toggles those simply has no visible effect on the web.
 *
 * <p>The handful of operations that do translate are honored here:
 * <ul>
 *   <li>{@link #setTitle(String)} updates the browser tab title through {@code document.title}.</li>
 *   <li>{@link #getWidth()} and {@link #getHeight()} report the canvas size in CSS pixels.</li>
 *   <li>{@link #setFullscreen} and {@link #setWindowed} use the browser Fullscreen API. A browser only
 *       allows fullscreen right after the player presses a key or clicks, so a request made at any other
 *       time (for example when a game applies a saved fullscreen setting at startup) is held back and
 *       retried on the player's next click, tap, or key press. {@link #isFullscreen()} reports the
 *       browser's real state, so it stays {@code false} until fullscreen has actually started.</li>
 *   <li>{@link #isFocused()} reports whether the page currently has focus.</li>
 *   <li>{@link #close()} stops the game loop and asks the browser to close the tab.</li>
 * </ul>
 */
public class FlixelHtml5Window implements FlixelWindow {

  @Nullable
  private HTMLCanvasElement canvas;
  @Nullable
  private FlixelHtml5Runner runner;

  private final FullscreenDeferred onDeferred = () -> Flixel.log.tagged("Html5/Window").debug(
      "Fullscreen needs a user gesture, so it will start on the next click, tap, or key press.");

  private int width;
  private int height;

  /**
   * Binds this window to the canvas element the runner created. Called once during startup before
   * the game loop begins.
   *
   * @param canvas The canvas the game renders into.
   * @param width The initial canvas width in CSS pixels.
   * @param height The initial canvas height in CSS pixels.
   */
  public void bind(HTMLCanvasElement canvas, int width, int height) {
    this.canvas = canvas;
    this.width = width;
    this.height = height;
  }

  /**
   * Registers the runner so {@link #close()} can stop the game loop before asking the browser to
   * close the tab.
   *
   * @param runner The runner driving this session's game loop.
   */
  void setRunner(FlixelHtml5Runner runner) {
    this.runner = runner;
  }

  /**
   * Records the current canvas size after a browser resize so the getters stay accurate.
   *
   * @param width The new width in CSS pixels.
   * @param height The new height in CSS pixels.
   */
  public void onResized(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getBackBufferWidth() {
    return canvas != null ? canvas.getWidth() : width;
  }

  @Override
  public int getBackBufferHeight() {
    return canvas != null ? canvas.getHeight() : height;
  }

  @Override
  public boolean isFocused() {
    return hasFocus();
  }

  @Override
  public boolean isFullscreen() {
    return isFullscreenJs();
  }

  /**
   * Returns whether a fullscreen request is waiting for the player's next click, tap, or key press.
   *
   * <p>This is {@code true} only after the browser refused a request because it did not come from a
   * user gesture. It turns {@code false} again once the retry is made, or when {@link #setWindowed}
   * or a newer {@link #setFullscreen} call cancels the wait.
   *
   * @return {@code true} while a deferred fullscreen request is pending.
   */
  public boolean isFullscreenPending() {
    return isFullscreenPendingJs();
  }

  @Override
  public boolean supportsFullscreen() {
    return true;
  }

  @Override
  public void setFullscreen(FlixelDisplayMode mode) {
    // Request fullscreen on the document element rather than the canvas. The browser's Fullscreen
    // API promotes the target element into a top layer that sits above all normal page content.
    // The browser may refuse until a user gesture happens; the script then retries on the next one.
    requestFullscreen(onDeferred);
  }

  @Override
  public void setWindowed(int width, int height) {
    exitFullscreen();
  }

  @Override
  public String getTitle() {
    return HTMLDocument.current().getTitle();
  }

  @Override
  public void setTitle(String title) {
    HTMLDocument.current().setTitle(title);
  }

  @Override
  public void close() {
    if (runner != null) {
      runner.stop();
    }
    closeWindow();
  }

  @JSBody(params = "onDeferred", script = """
      var fs = window.__flixelFs;
      if (!fs) {
        fs = window.__flixelFs = { id: 0, tries: 0, handler: null, cancel: null };
        fs.cancel = function() {
          fs.id++;
          if (!fs.handler) { return; }
          document.removeEventListener('pointerdown', fs.handler, true);
          document.removeEventListener('pointerup', fs.handler, true);
          document.removeEventListener('keydown', fs.handler, true);
          fs.handler = null;
        };
      }
      var el = document.documentElement;
      fs.cancel();
      fs.tries = 0;
      if (!el.requestFullscreen) { return; }
      function defer(id, retry) {
        if (id !== fs.id || fs.handler) { return; }
        if (retry && ++fs.tries > 3) { return; }
        fs.handler = function(e) {
          if (e.type === 'keydown' && e.key === 'Escape') { return; }
          fs.cancel();
          attempt(true);
        };
        document.addEventListener('pointerdown', fs.handler, true);
        document.addEventListener('pointerup', fs.handler, true);
        document.addEventListener('keydown', fs.handler, true);
        if (!retry) { onDeferred(); }
      }
      function attempt(inGesture) {
        var id = fs.id;
        var p;
        try { p = el.requestFullscreen(); } catch (e) { return; }
        if (p && typeof p.catch === 'function') {
          p.catch(function() { defer(id, inGesture); });
        }
      }
      attempt(false);
      """)
  private static native void requestFullscreen(FullscreenDeferred onDeferred);

  @JSBody(script = """
      if (window.__flixelFs) { window.__flixelFs.cancel(); }
      if (document.exitFullscreen && document.fullscreenElement) {
        var p = document.exitFullscreen();
        if (p && typeof p.catch === 'function') { p.catch(function() {}); }
      }
      """)
  private static native void exitFullscreen();

  @JSBody(script = "return !!document.fullscreenElement;")
  private static native boolean isFullscreenJs();

  @JSBody(script = "return !!(window.__flixelFs && window.__flixelFs.handler);")
  private static native boolean isFullscreenPendingJs();

  @JSBody(script = "return document.hasFocus();")
  private static native boolean hasFocus();

  @JSBody(script = "if (window.close) { window.close(); }")
  private static native void closeWindow();

  @Nullable
  public HTMLCanvasElement getCanvas() {
    return canvas;
  }

  @Nullable
  public FlixelHtml5Runner getRunner() {
    return runner;
  }

  /** Told by the fullscreen script when the browser refused a request and it was held for a user gesture. */
  @JSFunctor
  private interface FullscreenDeferred extends JSObject {
    void run();
  }
}
