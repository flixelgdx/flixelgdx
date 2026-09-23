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
package org.flixelgdx.backend.html5.asset;

import org.flixelgdx.backend.html5.file.FlixelHtml5File;
import org.flixelgdx.graphics.FlixelGraphicsManager;
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
 * and decoded on demand by {@link FlixelHtml5AssetManager} when a game
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
   * Downloads all non-image assets listed in the manifest, then invokes a callback.
   *
   * <p>Downloads run through a pool of at most 6 concurrent connections so the browser is not
   * overwhelmed when the manifest lists hundreds of files. {@code onComplete} fires once every
   * non-image file is cached; {@code onError} fires if the manifest cannot be fetched or any
   * individual download fails.
   *
   * @param manifestUrl The URL of the {@code assets.txt} manifest (for example {@code "assets/assets.txt"}).
   * @param assetRoot The URL prefix each manifest entry is resolved against (for example {@code "assets/"}).
   * @param onComplete Invoked once all non-image assets are cached.
   * @param onError Invoked when the manifest is missing or a download fails.
   */
  public static void preload(String manifestUrl, String assetRoot, PreloadCallback onComplete,
      PreloadCallback onError) {
    preloadJs(manifestUrl, assetRoot, onComplete, onError);
  }

  /**
   * Decodes the packaged framework images and stores them in the asset cache as FLXI-encoded pixels.
   *
   * <p>Certain framework resources are PNG images that the runtime reads through
   * {@code Flixel.files.classpath(...).readBytes()} at startup and then decodes synchronously via
   * {@link FlixelGraphicsManager#decodeImage decodeImage}. The regular
   * asset preloader skips PNG files to avoid excessive memory use, and the browser cannot decode
   * images synchronously. This method bridges that gap by fetching and decoding only the small set
   * of framework-owned images the runtime needs before the game's {@code create()} is called,
   * storing the decoded pixels in {@code window.__flixelAssets} under the same path key that
   * {@code readBytes()} looks up. The result is a compact FLXI-encoded buffer that the web graphics
   * backend can unpack synchronously without any further async work.
   *
   * <p>This must be called after the main preloader finishes but before {@code startGame()} is
   * invoked. Any fetch or decode failure is non-fatal: the callback fires regardless so the game
   * still starts; text using the packaged default font simply will not render.
   *
   * @param assetRoot The URL prefix the web assets are served from (for example {@code "assets/"}).
   * @param onComplete Invoked once all framework images have been decoded (or failed).
   */
  public static void preloadFrameworkImages(String assetRoot, PreloadCallback onComplete) {
    preloadFrameworkImagesJs(assetRoot, onComplete);
  }

  @JSBody(params = { "assetRoot", "onComplete" }, script = """
      if (!window.__flixelAssets) { window.__flixelAssets = {}; }
      var path = 'org/flixelgdx/bitmap/lsans-15.png';
      fetch(assetRoot + path)
        .then(function(res) {
          if (!res.ok) { throw new Error('HTTP ' + res.status); }
          return res.arrayBuffer();
        })
        .then(function(buffer) {
          return createImageBitmap(new Blob([buffer], { type: 'image/png' }));
        })
        .then(function(bitmap) {
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
          window.__flixelAssets[path] = out;
          onComplete();
        })
        .catch(function(e) {
          console.warn('[FlixelGDX] Packaged font page unavailable:', e && e.message ? e.message : e);
          onComplete();
        });
      """)
  private static native void preloadFrameworkImagesJs(String assetRoot, PreloadCallback onComplete);

  @JSBody(params = { "manifestUrl", "assetRoot", "onComplete", "onError" },
      script = """
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
            var allPaths = text.split('\\n').map(function(l) { return l.trim(); }).filter(function(l) { return l.length > 0; });
            allPaths.forEach(function(p) { window.__flixelAssetPaths[p] = true; });
            var paths = allPaths.filter(function(p) { return !flixelIsImage(p); });
            var total = paths.length; var done = 0;
            flixelProgress(0, total);
            if (total === 0) { onComplete(); return; }
            var dlIdx = 0;
            return new Promise(function(resolve, reject) {
              function startNext() {
                if (dlIdx >= paths.length) { return; }
                var path = paths[dlIdx++];
                fetch(assetRoot + path).then(function(res) {
                  if (!res.ok) { throw new Error('Failed to download "' + path + '" (HTTP ' + res.status + ')'); }
                  return res.arrayBuffer();
                }).then(function(buffer) {
                  window.__flixelAssets[path] = new Uint8Array(buffer);
                  done++;
                  flixelProgress(done, total);
                  if (done === total) { resolve(); } else { startNext(); }
                }).catch(reject);
              }
              var slots = Math.min(6, total);
              for (var i = 0; i < slots; i++) { startNext(); }
            });
          }).then(function() { onComplete(); }).catch(function(e) {
            console.error('[FlixelGDX] Asset preload failed:', e && e.message ? e.message : e);
            onError();
          });
          """)
  private static native void preloadJs(String manifestUrl, String assetRoot, PreloadCallback onComplete,
      PreloadCallback onError);

  /** A zero-argument callback the preloader invokes when it finishes or fails. */
  @JSFunctor
  public interface PreloadCallback extends JSObject {
    /** Invoked by the preloader when all assets have finished loading or an error occurred. */
    void run();
  }
}
