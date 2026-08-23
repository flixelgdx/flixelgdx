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
 * Downloads non-image bundled assets up front so the rest of the framework can read them
 * synchronously, while deferring image loading to save memory.
 *
 * <p>A browser has no file system and every network read is asynchronous, but the framework's file
 * API expects to read text and binary assets and get bytes back immediately. This preloader bridges
 * that gap for non-image assets (configs, audio, fonts, shaders): at startup it reads an
 * {@code assets.txt} manifest that the build plugin generated, downloads every non-image file into
 * an in-memory cache ({@code window.__flixelAssets}), and only then lets the game start. From that
 * point on, a text or audio read is just a lookup in the cache, which is instant and synchronous.
 *
 * <p>Image files ({@code .png}, {@code .jpg}, {@code .jpeg}, {@code .bmp}, {@code .tga}) are
 * intentionally excluded from the upfront download. Preloading all images at startup would consume
 * enormous amounts of memory (each uncompressed RGBA image is {@code width * height * 4} bytes in
 * the browser heap, on top of the compressed bytes already downloaded). Instead, images are fetched
 * and decoded on demand by {@link org.flixelgdx.backend.html5.FlixelHtml5AssetManager} when a game
 * calls {@code Flixel.assets.load()}, so only images the game actually requests are ever decoded,
 * and each decoded image is freed from CPU memory once it is uploaded to the GPU.
 *
 * <p>All paths from the manifest (including image paths) are recorded in
 * {@code window.__flixelAssetPaths} so that {@link FlixelHtml5File#exists()} can report correctly
 * for image paths even though their content is not in {@code window.__flixelAssets}.
 *
 * <p>The manifest is required. If it is missing or any non-image download fails, the preloader
 * reports the failure through its error callback (and logs detail to the console) so the runner can
 * fail loudly with a clear message rather than silently rendering a blank page.
 *
 * <p>As non-image files download, progress is reported through the page's loading overlay
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
      if (!window.__flixelAssetPaths) { window.__flixelAssetPaths = {}; }
      var flixelImageExts = ['.png', '.jpg', '.jpeg', '.bmp', '.tga'];
      function flixelIsImage(path) {
        var lower = path.toLowerCase();
        for (var i = 0; i < flixelImageExts.length; i++) {
          if (lower.endsWith(flixelImageExts[i])) { return true; }
        }
        return false;
      }
      function flixelProgress(done, total) {
        if (window.__flixelLoading) { window.__flixelLoading.set(total ? done / total : 1); }
      }
      fetch(manifestUrl).then(function(response) {
        if (!response.ok) { throw new Error('Manifest fetch failed (HTTP ' + response.status + ')'); }
        return response.text();
      }).then(function(text) {
        window.__flixelAssets['assets.txt'] = new TextEncoder().encode(text);
        var allPaths = text.split(/\\r?\\n/).filter(function(line) { return line.trim().length > 0; });
        allPaths.forEach(function(p) { window.__flixelAssetPaths[p] = true; });
        var paths = allPaths.filter(function(p) { return !flixelIsImage(p); });
        var total = paths.length; var done = 0;
        flixelProgress(0, total);
        if (total === 0) { onComplete(); return; }
        return Promise.all(paths.map(function(path) {
          return fetch(assetRoot + path).then(function(res) {
            if (!res.ok) { throw new Error('Failed to download "' + path + '" (HTTP ' + res.status + ')'); }
            return res.arrayBuffer();
          }).then(function(buffer) {
            window.__flixelAssets[path] = new Uint8Array(buffer);
            done++; flixelProgress(done, total);
          });
        })).then(function() { onComplete(); });
      }).catch(function(e) {
        console.error('[FlixelGDX] Asset preload failed:', e && e.message ? e.message : e);
        onError();
      });
      """)
  private static native void preloadJs(String manifestUrl, String assetRoot, PreloadCallback onComplete,
      PreloadCallback onError);

  /** A zero-argument callback the preloader invokes when it finishes or fails. */
  @JSFunctor
  public interface PreloadCallback extends JSObject {
    void run();
  }
}
