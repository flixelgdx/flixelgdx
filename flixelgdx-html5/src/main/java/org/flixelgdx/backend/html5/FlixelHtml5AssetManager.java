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
import org.flixelgdx.asset.FlixelAsset;
import org.flixelgdx.asset.FlixelAssetLoader;
import org.flixelgdx.asset.FlixelAssetManager;
import org.flixelgdx.asset.FlixelAssetPaths;
import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelImage;
import org.flixelgdx.graphics.FlixelTexture;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The web asset manager, extending the shared pipeline with lazy asynchronous image decoding.
 *
 * <p>Browsers cannot decode images synchronously: {@code createImageBitmap} is always asynchronous,
 * and there is no way to block the JS main thread while a Promise resolves. The shared
 * {@link FlixelBaseAssetManager} pipeline assumes that {@link FlixelAssetLoader#loadRaw} can
 * return image bytes synchronously, which is true everywhere else. On web the strategy is
 * different:
 *
 * <ol>
 *   <li>When an image path is queued with {@link #load(String)}, a {@code createImageBitmap}
 *     Promise is started in JavaScript and the path is tracked in a pending-decode set. The path is
 *     not yet forwarded to the parent pipeline.</li>
 *   <li>Each call to {@link #update(int)} polls that set. When the browser signals that an image's
 *     Promise has resolved (by populating {@code window.__flixelDecodedImages}), the path is
 *     promoted into the parent's pipeline with a synchronous {@link FlixelBaseAssetManager#load}
 *     call. Because the parent runs single-threaded and the RGBA pixels are now ready, the load
 *     completes immediately within that same {@link #update} call.</li>
 * </ol>
 *
 * <p>Non-image assets (audio, text) are forwarded directly to the parent and use its existing
 * pipeline unchanged.
 *
 * <p><b>Restriction: images require the asset manager on web.</b> Calling
 * {@link #finishLoading()} or {@link #finishLoadingAsset(String)} while images are still being
 * decoded asynchronously will throw {@link UnsupportedOperationException}, because blocking the
 * JS thread prevents the decoding Promises from ever resolving. Use {@link #update()} in a
 * loading-state game loop instead. Games targeting web should always preload images through
 * {@link #load(String)} and {@link #update()}, not through direct file reads or blocking calls.
 *
 * <p>The decoded pixels are stored behind a {@code FLXI} header in
 * {@code window.__flixelDecodedImages}, the same compact format the web graphics backend unpacks
 * via {@link org.flixelgdx.graphics.FlixelGraphicsManager#decodeImage decodeImage}. This keeps the
 * pixel extraction path in one place and means the only web-specific code is the Promise-polling
 * loop in {@link #update}.
 */
public class FlixelHtml5AssetManager extends FlixelBaseAssetManager {

  private static final String[] IMAGE_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".bmp", ".tga" };

  /** Paths of images whose browser decode Promise is still pending, mapped to their persist flag. */
  @NotNull
  private final FlixelMap<String, Boolean> pendingImageDecodes = new FlixelMap<>();

  private int totalImages;
  private int promotedImages;

  /**
   * Creates the manager and replaces the default image loaders with web-aware ones that read
   * pre-decoded pixels from the browser's decode cache rather than calling
   * {@link org.flixelgdx.graphics.FlixelGraphicsManager#decodeImage decodeImage} inline.
   */
  public FlixelHtml5AssetManager() {
    WebImageLoader loader = new WebImageLoader();
    for (String ext : IMAGE_EXTENSIONS) {
      registerLoader(ext, loader);
    }
  }

  /**
   * Queues an asset for loading.
   *
   * <p>For image paths, the asset is not immediately forwarded to the parent pipeline: instead a
   * browser {@code createImageBitmap} Promise is started asynchronously. The path is promoted to
   * the parent during the next {@link #update(int)} call once the Promise resolves.
   *
   * <p>For all other paths (audio, text) the call is forwarded to the parent unchanged.
   *
   * @param path Asset path (e.g. {@code "images/player.png"}).
   * @param persist When {@code true}, the first handle created for this path is persistent.
   */
  @Override
  public void load(@NotNull String path, boolean persist) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    if (!isImageExtension(key)) {
      super.load(key, persist);
      return;
    }

    if (isLoaded(key) || pendingImageDecodes.containsKey(key)) {
      return;
    }

    totalImages++;
    pendingImageDecodes.put(key, persist);
    startImageDecodeJs(key);
  }

  /**
   * Advances the loading pipeline.
   *
   * <p>First polls each pending image decode. When a decode Promise has resolved (detected by the
   * presence of its pixels in {@code window.__flixelDecodedImages}), the path is promoted to the
   * parent pipeline and processed synchronously in the same call. Then the parent's own pipeline
   * is advanced for non-image assets and any newly promoted images.
   *
   * @param millis Maximum time to spend updating.
   * @return {@code true} when all queued loading, including pending image decodes, is finished.
   */
  @Override
  public boolean update(int millis) {
    if (!pendingImageDecodes.isEmpty()) {
      FlixelMap.Entries<String, Boolean> it = pendingImageDecodes.entries();
      while (it.hasNext()) {
        FlixelMap.Entry<String, Boolean> entry = it.next();
        if (isImageDecodeReady(entry.key)) {
          it.remove();
          promotedImages++;
          super.load(entry.key, entry.value);
        }
      }
    }
    boolean parentDone = super.update(millis);
    return parentDone && pendingImageDecodes.isEmpty();
  }

  /**
   * Returns overall loading progress in {@code [0, 1]}, blending pending image decodes with
   * the parent pipeline's progress.
   *
   * @return Progress fraction.
   */
  @Override
  public float getProgress() {
    if (totalImages == 0) {
      return super.getProgress();
    }
    float decodeProgress = promotedImages / (float) totalImages;
    return Math.min(decodeProgress, super.getProgress());
  }

  /**
   * Blocks until all queued assets finish loading.
   *
   * <p>This method cannot be used while images are pending async decode, because blocking the JS
   * thread prevents the decode Promises from ever resolving. Use {@link #update()} in a loading
   * loop instead. If no images are pending, this delegates to the parent implementation.
   *
   * @throws UnsupportedOperationException if images are still being decoded asynchronously.
   */
  @Override
  public void finishLoading() {
    if (!pendingImageDecodes.isEmpty()) {
      throw new UnsupportedOperationException(
          "finishLoading() cannot be used while images are still being decoded on web: blocking "
              + "the JS thread prevents decode Promises from resolving. Use Flixel.assets.update() "
              + "in a loading-state game loop instead.");
    }
    super.finishLoading();
  }

  /**
   * Blocks until the specific asset at {@code path} finishes loading.
   *
   * <p>Like {@link #finishLoading()}, this cannot be used for an image that is still being decoded
   * asynchronously. Use {@link #update()} in a loading loop instead.
   *
   * @param path Asset path.
   * @throws UnsupportedOperationException if the asset is an image pending async decode.
   */
  @Override
  public void finishLoadingAsset(@NotNull String path) {
    String key = FlixelAssetPaths.normalizeAssetPath(path);
    if (pendingImageDecodes.containsKey(key)) {
      throw new UnsupportedOperationException(
          "finishLoadingAsset() cannot be used while the image '" + key + "' is still being "
              + "decoded on web: blocking the JS thread prevents the decode Promise from resolving. "
              + "Use Flixel.assets.update() in a loading-state game loop instead.");
    }
    super.finishLoadingAsset(path);
  }

  private static boolean isImageExtension(@NotNull String path) {
    int dot = path.lastIndexOf('.');
    if (dot < 0) {
      return false;
    }
    String ext = path.substring(dot).toLowerCase();
    for (String imageExt : IMAGE_EXTENSIONS) {
      if (imageExt.equals(ext)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Fetches and asynchronously decodes the image at {@code path} directly from the server.
   *
   * <p>Because image files are not preloaded into {@code window.__flixelAssets} (to avoid enormous
   * up-front memory consumption), this method fetches the image bytes on demand via
   * {@code fetch("assets/" + path)}, creates a {@link Blob} with the correct MIME type so that
   * {@code createImageBitmap} can decode it regardless of the server's {@code Content-Type} header,
   * and stores the decoded RGBA pixels as a compact FLXI-encoded {@code Uint8Array} in
   * {@code window.__flixelDecodedImages[path]}.
   *
   * <p>{@link #isImageDecodeReady(String)} polls that map, and {@link WebImageLoader} reads the
   * encoded result from it once the Promise has resolved.
   *
   * @param path Normalized asset path of the image to fetch and decode.
   */
  @JSBody(params = "path", script = """
      if (!window.__flixelDecodedImages) { window.__flixelDecodedImages = {}; }
      var exts = { 'png': 'image/png', 'jpg': 'image/jpeg', 'jpeg': 'image/jpeg',
                   'bmp': 'image/bmp', 'tga': 'image/x-tga' };
      var ext = path.split('.').pop().toLowerCase();
      var mime = exts[ext] || 'image/png';
      fetch('assets/' + path)
        .then(function(res) {
          if (!res.ok) { throw new Error('HTTP ' + res.status); }
          return res.arrayBuffer();
        })
        .then(function(buffer) {
          return createImageBitmap(new Blob([buffer], { type: mime }));
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
          window.__flixelDecodedImages[path] = out;
        })
        .catch(function(e) {
          console.error('[FlixelGDX] Failed to fetch or decode image "' + path + '":', e && e.message ? e.message : e);
        });
      """)
  private static native void startImageDecodeJs(String path);

  /**
   * Returns whether the browser has finished decoding the image at {@code path}.
   *
   * @param path Normalized asset path to check.
   * @return {@code true} if the FLXI-encoded pixels are ready in {@code window.__flixelDecodedImages}.
   */
  @JSBody(params = "path", script = """
      return !!(window.__flixelDecodedImages && window.__flixelDecodedImages[path]);
      """)
  private static native boolean isImageDecodeReady(String path);

  /**
   * Returns the FLXI-encoded pixel bytes for a decoded image.
   *
   * @param path Normalized asset path of the image.
   * @return The FLXI bytes, or {@code null} if the decode has not completed.
   */
  @JSBody(params = "path", script = """
      var d = window.__flixelDecodedImages && window.__flixelDecodedImages[path];
      return d ? new Int8Array(d.buffer, d.byteOffset, d.byteLength) : null;
      """)
  private static native Int8Array getDecodedImageBytes(String path);

  /**
   * Removes the FLXI-encoded pixel bytes for the image at {@code path} from the browser's decode
   * cache, releasing the CPU-side memory. Call this after the pixels have been uploaded to the GPU.
   *
   * @param path Normalized asset path of the image to free.
   */
  @JSBody(params = "path", script = "if (window.__flixelDecodedImages) { delete window.__flixelDecodedImages[path]; }")
  private static native void freeDecodedImageJs(String path);

  /**
   * Loads images on web by reading pre-decoded FLXI pixels from the browser decode cache.
   *
   * <p>This loader is only invoked after {@link FlixelHtml5AssetManager} has confirmed that
   * the image's {@code createImageBitmap} Promise has resolved and the FLXI-encoded result is
   * stored in {@code window.__flixelDecodedImages}. Calling {@link #loadRaw} directly before the
   * Promise resolves (for example via
   * {@link FlixelAssetManager#loadRawSync(String) loadRawSync}) throws
   * {@link UnsupportedOperationException} with a clear message.
   */
  private static final class WebImageLoader implements FlixelAssetLoader<FlixelGraphic> {

    @NotNull
    @Override
    public Object loadRaw(@NotNull FlixelAssetManager assets, @NotNull String path,
        @NotNull FlixelFile file) {
      Int8Array flxiBytes = getDecodedImageBytes(path);
      if (flxiBytes == null) {
        throw new UnsupportedOperationException(
            "Image '" + path + "' has not been decoded yet. On web, images must be loaded "
                + "through Flixel.assets.load() and Flixel.assets.update(), not through "
                + "direct file reads or blocking load calls.");
      }
      byte[] bytes = flxiBytes.copyToJavaArray();
      ByteBuffer encoded = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
      FlixelImage image = Flixel.graphics.decodeImage(encoded);
      if (image == null) {
        throw new IllegalStateException("Failed to unpack decoded image: '" + path + "'.");
      }
      return image;
    }

    @NotNull
    @Override
    public Object finishRaw(@NotNull FlixelAssetManager assets, @NotNull String path,
        @NotNull Object raw) {
      if (raw instanceof FlixelImage image) {
        FlixelTexture texture = Flixel.graphics.createTexture(image);
        // Pixels are now on the GPU; the CPU-side FLXI bytes are no longer needed.
        freeDecodedImageJs(path);
        return texture;
      }
      return raw;
    }

    @NotNull
    @Override
    public FlixelAsset<FlixelGraphic> createHandle(@NotNull FlixelAssetManager assets,
        @NotNull String path) {
      return new FlixelGraphic(assets, path);
    }
  }
}
