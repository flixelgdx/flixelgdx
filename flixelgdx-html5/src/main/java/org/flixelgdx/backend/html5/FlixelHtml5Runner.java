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
import org.flixelgdx.backend.html5.asset.FlixelHtml5AssetPreloader;
import org.flixelgdx.backend.html5.file.FlixelHtml5Files;
import org.flixelgdx.backend.html5.graphics.FlixelHtml5Graphics;
import org.flixelgdx.backend.html5.input.FlixelHtml5InputDevice;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * The web platform's game loop, driven by the browser's {@code requestAnimationFrame} callback.
 *
 * <p>A desktop runner owns a tight {@code while} loop it spins until the window closes. A browser
 * gives up no such loop: the page's single thread belongs to the browser, and code that blocked it
 * would freeze the tab. Instead, the browser hands time back one frame at a time through
 * {@code requestAnimationFrame}, which calls a supplied function right before the next repaint.
 * This runner schedules itself that way, so each callback advances the game by exactly one frame
 * and then asks for the next one. That is why {@link #run(FlixelGame)} returns immediately after
 * kicking off the first frame, unlike the blocking desktop runner.
 *
 * <p>The runner also owns the pieces of browser lifecycle the game needs to react to: it resizes
 * the canvas when the page changes size and forwards tab visibility changes to the game's focus
 * hooks so audio and updates can pause when the tab is hidden.
 */
public class FlixelHtml5Runner implements FlixelGameRunner {

  private double lastTimestamp = -1.0;

  @NotNull
  private final FlixelHtml5Graphics graphics;

  @NotNull
  private final FlixelHtml5Window window;

  @NotNull
  private final FlixelHtml5InputDevice input;

  @NotNull
  private final FlixelHtml5HostIntegration host;

  @NotNull
  private final String canvasId;

  private final int width;
  private final int height;

  private HTMLCanvasElement canvas;
  private FlixelGame game;

  public FlixelHtml5Runner(@NotNull String canvasId, int width, int height, @NotNull FlixelHtml5Graphics graphics,
      @NotNull FlixelHtml5Window window, @NotNull FlixelHtml5HostIntegration host, @NotNull FlixelHtml5InputDevice input) {
    this.canvasId = canvasId;
    this.width = width;
    this.height = height;
    this.graphics = graphics;
    this.window = window;
    this.host = host;
    this.input = input;
  }

  @Override
  public void run(@NotNull FlixelGame game) {
    this.game = game;

    HTMLDocument document = HTMLDocument.current();
    HTMLCanvasElement element = resolveCanvas(document);
    element.setWidth(width);
    element.setHeight(height);
    this.canvas = element;

    window.bind(element, width, height);
    input.attach(element);
    graphics.initialize(element);

    registerLifecycleListeners(document);

    if (FlixelHtml5MonitorHelper.isWindowManagementSupported()) {
      FlixelHtml5MonitorHelper.requestScreenDetails(host::updateMonitors);
    }

    // The browser cannot read files synchronously, so every bundled asset is downloaded first and
    // the game only starts once the cache is warm. See FlixelHtml5AssetPreloader.
    FlixelHtml5AssetPreloader.preload(FlixelHtml5Files.ASSET_MANIFEST, FlixelHtml5Files.ASSET_ROOT,
        this::startGame, FlixelHtml5Runner::onPreloadFailed);
  }

  /** Runs once every asset is cached: dismisses the loading overlay, creates the first state, and starts the loop. */
  private void startGame() {
    hideLoadingOverlay();
    game.create();
    Window.requestAnimationFrame(this::onAnimationFrame);
  }

  /**
   * Runs when the asset preloader fails. The game is not started, a clear error is logged, and the
   * loading overlay shows the failure so the cause is obvious rather than a silent blank page.
   *
   * <p>Common causes: the manifest ({@code assets/assets.txt}) is missing because the game was
   * built without the {@code org.flixelgdx.html5} plugin, or an individual non-image asset could
   * not be downloaded. The browser console contains the specific error logged by the preloader.
   */
  private static void onPreloadFailed() {
    Flixel.error("Html5",
        "Asset preloading failed. Common causes: the 'assets/assets.txt' manifest is missing "
            + "(make sure the org.flixelgdx.html5 plugin is applied), or a non-image asset could not be "
            + "downloaded. The game will not start.");
    showLoadingError("Failed to load game assets. See the console for details.");
  }

  @JSBody(script = "if (window.__flixelLoading) { window.__flixelLoading.done(); }")
  private static native void hideLoadingOverlay();

  @JSBody(params = "message", script = "if (window.__flixelLoading) { window.__flixelLoading.error(message); }")
  private static native void showLoadingError(String message);

  /**
   * Advances the game by one frame, then schedules the next one.
   *
   * <p>The browser passes a high-resolution timestamp in milliseconds. The first frame has no
   * previous timestamp to subtract from, so it reports a zero delta and lets {@link FlixelGame}
   * clamp it; every later frame reports the real time elapsed since the previous callback.
   *
   * @param timestamp The browser-supplied frame time in milliseconds.
   */
  private void onAnimationFrame(double timestamp) {
    float deltaSeconds = lastTimestamp < 0.0 ? 0f : (float) ((timestamp - lastTimestamp) / 1000.0);
    lastTimestamp = timestamp;

    graphics.beginFrame();
    game.render(deltaSeconds);
    graphics.endFrame();

    Window.requestAnimationFrame(this::onAnimationFrame);
  }

  /**
   * Locates the canvas the game draws into, creating one under {@link #canvasId} if the page did
   * not already supply it. Reusing a page-provided canvas lets developers style and position the
   * canvas from their own HTML instead of taking whatever the framework appends.
   *
   * @param document The current page document.
   * @return The canvas element to render into.
   */
  private HTMLCanvasElement resolveCanvas(HTMLDocument document) {
    HTMLCanvasElement existing = (HTMLCanvasElement) document.getElementById(canvasId);
    if (existing != null) {
      return existing;
    }
    HTMLCanvasElement created = (HTMLCanvasElement) document.createElement("canvas");
    created.setAttribute("id", canvasId);
    document.getBody().appendChild(created);
    return created;
  }

  /**
   * Wires the browser resize, fullscreen-change, and visibility events to the framework so the
   * canvas drawing buffer tracks the page and the game pauses when its tab is hidden.
   *
   * <p>Both the {@code resize} and {@code fullscreenchange} events call {@link #onViewportChanged}
   * because the browser does not guarantee that {@code resize} fires around every fullscreen
   * transition. Handling both events ensures the canvas drawing buffer and WebGL viewport are
   * always kept in sync with the actual rendered size.
   *
   * @param document The current page document.
   */
  private void registerLifecycleListeners(HTMLDocument document) {
    Window.current().addEventListener("resize", event -> onViewportChanged());
    document.addEventListener("fullscreenchange", event -> onViewportChanged());
    document.addEventListener("visibilitychange", event -> {
      if (isDocumentHidden()) {
        game.onFocusLost();
      } else {
        // Reset the delta so the first frame back does not report the whole hidden duration.
        lastTimestamp = -1.0;
        game.onFocusGained();
      }
    });
  }

  /**
   * Synchronizes the canvas drawing buffer, WebGL viewport, and game cameras to the current
   * browser viewport size.
   *
   * <p>The canvas {@code width} and {@code height} attributes control the WebGL drawing-buffer
   * resolution. When they do not match the dimensions passed to {@code gl.viewport}, WebGL renders
   * into a buffer that is a different size than the projection matrix expects, which causes the
   * view to appear zoomed in or out. Updating the attributes here keeps the drawing buffer in sync
   * whenever the viewport changes, including during fullscreen transitions.
   */
  private void onViewportChanged() {
    int newWidth = browserInnerWidth();
    int newHeight = browserInnerHeight();
    canvas.setWidth(newWidth);
    canvas.setHeight(newHeight);
    window.onResized(newWidth, newHeight);
    graphics.onResized(newWidth, newHeight);
    game.resize(newWidth, newHeight);
  }

  @JSBody(script = "return document.hidden;")
  private static native boolean isDocumentHidden();

  @JSBody(script = "return window.innerWidth;")
  private static native int browserInnerWidth();

  @JSBody(script = "return window.innerHeight;")
  private static native int browserInnerHeight();
}
