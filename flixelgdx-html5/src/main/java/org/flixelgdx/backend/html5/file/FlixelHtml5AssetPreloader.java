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
 * bridges that gap the same way older web game backends did: at startup it reads an {@code
 * assets.txt} manifest that the build plugin generated, downloads every file it lists into an
 * in-memory cache, and only then lets the game start. From that point on, an asset read is just a
 * lookup in the cache, which is instant and synchronous.
 *
 * <p>The manifest is required. If it is missing, the game was almost certainly built without the
 * {@code org.flixelgdx.html5} plugin, and the preloader reports the failure through its error
 * callback so the runner can fail loudly with a clear message rather than silently rendering a blank
 * page.
 *
 * <p>Image files are a special case. A browser can only decode an image asynchronously, but the
 * framework's texture upload path is synchronous, so this preloader decodes every image to raw RGBA
 * pixels while it downloads and stores that instead of the encoded bytes. The decoded entry starts
 * with a small {@code FLXI} header (the four magic bytes, then the width and height as little-endian
 * 32-bit integers) followed by the pixels, which the web graphics backend unpacks synchronously in
 * {@code decodeImage}. As the assets load, progress is reported through the page's loading overlay
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
      function flixelIsImage(path) { return /\\.(png|jpe?g|gif|bmp|webp)$/i.test(path); }
      function flixelDecode(buffer) {
        return createImageBitmap(new Blob([buffer])).then(function(bitmap) {
          var canvas = document.createElement('canvas');
          canvas.width = bitmap.width; canvas.height = bitmap.height;
          var ctx = canvas.getContext('2d');
          ctx.drawImage(bitmap, 0, 0);
          var pixels = ctx.getImageData(0, 0, bitmap.width, bitmap.height).data;
          var out = new Uint8Array(12 + pixels.length);
          out[0] = 70; out[1] = 76; out[2] = 88; out[3] = 73;
          var view = new DataView(out.buffer);
          view.setUint32(4, bitmap.width, true); view.setUint32(8, bitmap.height, true);
          out.set(pixels, 12);
          if (bitmap.close) { bitmap.close(); }
          return out;
        });
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
            if (flixelIsImage(path)) {
              return flixelDecode(buffer).then(function(decoded) { window.__flixelAssets[path] = decoded; });
            }
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
