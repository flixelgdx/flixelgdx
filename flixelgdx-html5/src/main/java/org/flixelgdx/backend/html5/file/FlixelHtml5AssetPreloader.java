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
package org.flixelgdx.backend.html5.file;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Downloads every bundled asset up front so the rest of the framework can read files synchronously.
 *
 * <p>A browser has no file system and every network read is asynchronous, but the framework's file
 * API (and its asset manager) expect to read bytes and get them back immediately. This preloader
 * bridges that gap at the network level: at startup it reads an {@code assets.txt} manifest that the
 * build plugin generated, downloads every file it lists into an in-memory cache
 * ({@code window.__flixelAssets}), and only then lets the game start. From that point on, an asset
 * read is just a lookup in the cache, which is instant and synchronous.
 *
 * <p>The manifest is required. If it is missing, the game was almost certainly built without the
 * {@code org.flixelgdx.html5} plugin, and the preloader reports the failure through its error
 * callback so the runner can fail loudly with a clear message rather than silently rendering a blank
 * page.
 *
 * <p>All assets, including images, are stored as their raw encoded bytes. Image decoding is
 * intentionally deferred: browsers can only decode images asynchronously (via
 * {@code createImageBitmap}), and doing it here would block the game from starting until every image
 * was decoded. Instead, {@link org.flixelgdx.backend.html5.FlixelHtml5AssetManager} starts
 * individual image decodes when they are queued via {@code Flixel.assets.load()}, so decoding runs
 * lazily, in parallel with the game's own loading state, and only for images the game actually
 * requests.
 *
 * <p>As files download, progress is reported through the page's loading overlay
 * ({@code window.__flixelLoading}) so the player sees a bar fill rather than a frozen screen.
 */
public final class FlixelHtml5AssetPreloader {

  private FlixelHtml5AssetPreloader() {}

  /**
   * Downloads all assets listed in the manifest, then invokes a callback.
   *
   * <p>Downloads run concurrently; {@code onComplete} fires once every file is cached, and
   * {@code onError} fires if the manifest cannot be fetched or a download fails.
   *
   * @param manifestUrl The URL of the {@code assets.txt} manifest (for example {@code "assets/assets.txt"}).
   * @param assetRoot The URL prefix each manifest entry is resolved against (for example {@code "assets/"}).
   * @param onComplete Invoked once all assets are cached.
   * @param onError Invoked when the manifest is missing or a download fails.
   */
  public static void preload(String manifestUrl, String assetRoot, PreloadCallback onComplete,
      PreloadCallback onError) {
    preloadJs(manifestUrl, assetRoot, onComplete, onError);
  }

  @JSBody(params = { "manifestUrl", "assetRoot", "onComplete", "onError" }, script = """
      if (!window.__flixelAssets) { window.__flixelAssets = {}; }
      function flixelProgress(done, total) {
        if (window.__flixelLoading) { window.__flixelLoading.set(total ? done / total : 1); }
      }
      fetch(manifestUrl).then(function(response) {
        if (!response.ok) { onError(); return null; }
        return response.text();
      }).then(function(text) {
        if (text === null) { return; }
        var paths = text.split(/\\r?\\n/).filter(function(line) { return line.trim().length > 0; });
        var total = paths.length; var done = 0;
        flixelProgress(0, total);
        if (total === 0) { onComplete(); return; }
        return Promise.all(paths.map(function(path) {
          return fetch(assetRoot + path).then(function(res) {
            if (!res.ok) { throw new Error(path); }
            return res.arrayBuffer();
          }).then(function(buffer) {
            window.__flixelAssets[path] = new Uint8Array(buffer);
          }).then(function() { done++; flixelProgress(done, total); });
        })).then(function() { onComplete(); });
      }).catch(function() { onError(); });
      """)
  private static native void preloadJs(String manifestUrl, String assetRoot, PreloadCallback onComplete,
      PreloadCallback onError);

  /** A zero-argument callback the preloader invokes when it finishes or fails. */
  @JSFunctor
  public interface PreloadCallback extends JSObject {
    void run();
  }
}
