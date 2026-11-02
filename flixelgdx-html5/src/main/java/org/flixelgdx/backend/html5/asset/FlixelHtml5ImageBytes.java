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

import org.teavm.jso.JSBody;

/**
 * Installs the browser-side image decoder that every web image load goes through.
 *
 * <p>Images must reach the renderer as the exact, straight (non-premultiplied) RGBA values stored in the file,
 * like stb_image gives on the desktop and {@code BitmapFactory} gives on Android. The usual browser route, drawing
 * an {@code ImageBitmap} onto a 2D canvas and reading it back with {@code getImageData}, cannot do that: a canvas
 * stores premultiplied color, so soft edges lose precision and fully transparent pixels turn black, and the browser
 * may also convert colors. Blend modes and smooth edges then look different on the web than everywhere else.
 *
 * <p>The decoder installed here asks the browser to decode the file with no premultiplication and no color
 * conversion, copies the result into a texture of a small WebGPU device of its own, and reads the texture back, so
 * every byte arrives unchanged. Any browser that can run the game has WebGPU. If the copy fails anyway (for example
 * an image larger than the GPU's texture limit), it falls back to the canvas route.
 *
 * <p>After {@link #install()}, JavaScript calls {@code window.__flixelDecodeImage(arrayBuffer, mimeType)}, which
 * returns a promise of the pixels in the FLXI layout the web graphics backend unpacks: the bytes {@code FLXI}, the
 * width and height as little-endian 32-bit integers, then the RGBA rows.
 */
public final class FlixelHtml5ImageBytes {

  private FlixelHtml5ImageBytes() {}

  /** Defines {@code window.__flixelDecodeImage} if it does not exist yet. Calling it again does nothing. */
  @JSBody(script = """
      if (window.__flixelDecodeImage) { return; }
      var gpu = null;
      function device() {
        if (!gpu) {
          gpu = (navigator.gpu ? navigator.gpu.requestAdapter() : Promise.resolve(null))
            .then(function(adapter) { return adapter ? adapter.requestDevice() : null; })
            .catch(function() { return null; });
        }
        return gpu;
      }
      function flxi(w, h) {
        var out = new Uint8Array(12 + w * h * 4);
        out[0] = 70; out[1] = 76; out[2] = 88; out[3] = 73;
        var view = new DataView(out.buffer);
        view.setUint32(4, w, true); view.setUint32(8, h, true);
        return out;
      }
      function viaCanvas(bitmap) {
        var canvas = document.createElement('canvas');
        canvas.width = bitmap.width; canvas.height = bitmap.height;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(bitmap, 0, 0);
        var out = flxi(bitmap.width, bitmap.height);
        out.set(ctx.getImageData(0, 0, bitmap.width, bitmap.height).data, 12);
        return out;
      }
      function viaGpu(dev, bitmap) {
        var w = bitmap.width, h = bitmap.height;
        var max = dev.limits.maxTextureDimension2D;
        if (w > max || h > max) { return Promise.resolve(viaCanvas(bitmap)); }
        var texture = dev.createTexture({ size: [w, h], format: 'rgba8unorm',
          usage: GPUTextureUsage.COPY_DST | GPUTextureUsage.COPY_SRC | GPUTextureUsage.RENDER_ATTACHMENT });
        dev.queue.copyExternalImageToTexture({ source: bitmap }, { texture: texture, premultipliedAlpha: false },
          [w, h]);
        // Buffer copies need rows aligned to 256 bytes; the padding is dropped when the rows are packed below.
        var stride = Math.ceil(w * 4 / 256) * 256;
        var buffer = dev.createBuffer({ size: stride * h, usage: GPUBufferUsage.COPY_DST | GPUBufferUsage.MAP_READ });
        var encoder = dev.createCommandEncoder();
        encoder.copyTextureToBuffer({ texture: texture }, { buffer: buffer, bytesPerRow: stride }, [w, h]);
        dev.queue.submit([encoder.finish()]);
        return buffer.mapAsync(GPUMapMode.READ).then(function() {
          var src = new Uint8Array(buffer.getMappedRange());
          var out = flxi(w, h);
          var row = w * 4;
          for (var y = 0; y < h; y++) {
            out.set(src.subarray(y * stride, y * stride + row), 12 + y * row);
          }
          buffer.unmap(); buffer.destroy(); texture.destroy();
          return out;
        }, function(e) {
          buffer.destroy(); texture.destroy();
          throw e;
        });
      }
      window.__flixelDecodeImage = function(data, mime) {
        return createImageBitmap(new Blob([data], { type: mime }),
            { premultiplyAlpha: 'none', colorSpaceConversion: 'none' })
          .then(function(bitmap) {
            return device().then(function(dev) {
              var pixels = dev
                ? viaGpu(dev, bitmap).catch(function() { return viaCanvas(bitmap); })
                : Promise.resolve(viaCanvas(bitmap));
              return pixels.then(function(out) {
                if (bitmap.close) { bitmap.close(); }
                return out;
              });
            });
          });
      };
      """)
  public static native void install();
}
