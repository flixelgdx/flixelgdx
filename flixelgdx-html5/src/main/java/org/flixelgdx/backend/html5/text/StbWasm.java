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
package org.flixelgdx.backend.html5.text;

import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Bridge to the Emscripten-compiled stb_truetype WebAssembly module loaded by the web build plugin.
 *
 * <p>The plugin extracts {@code META-INF/wasm/stb_truetype_wasm.js} and its companion
 * {@code .wasm} binary from the framework JAR into the web output's {@code native/} directory and
 * injects a {@code <script>} tag for the JS glue file into the generated page. Emscripten's runtime
 * then fetches and instantiates the WebAssembly binary asynchronously. All methods here call into
 * the global {@code Module} object that Emscripten populates once initialization is complete;
 * callers must guard with {@link #isReady()} before making any other call.
 *
 * <p>Memory management follows the WASM heap model: every pointer returned by {@link #malloc} or
 * {@link #stbRasterize} must be freed by the caller via {@link #free} or {@link #stbFreeBitmap}
 * respectively, and both the font context and the raw font data buffer must outlive each other
 * (stb holds an internal pointer into the data).
 */
final class StbWasm {

  private StbWasm() {}

  /**
   * Returns {@code true} once the Emscripten module has finished initializing and all exported
   * stb functions are available on {@code Module}.
   */
  @JSBody(script = "return typeof Module !== 'undefined' && typeof Module._stb_init === 'function';")
  static native boolean isReady();

  /**
   * Allocates {@code size} bytes on the WASM heap. Returns the pointer, or {@code 0} when the
   * allocation fails.
   */
  @JSBody(params = "size", script = "return Module._malloc(size) | 0;")
  static native int malloc(int size);

  /** Frees a pointer previously returned by {@link #malloc}. */
  @JSBody(params = "ptr", script = "Module._free(ptr);")
  static native void free(int ptr);

  /**
   * Bulk-copies {@code src} into the WASM heap at the byte offset {@code ptr}. The destination
   * region must already be allocated with {@link #malloc}.
   *
   * <p>Passing a native JavaScript {@link Int8Array} lets {@code HEAPU8.set()} run without
   * iterating through TeaVM's boxed array representation, so even large font files copy in one
   * JavaScript operation.
   */
  @JSBody(params = { "ptr", "src" }, script = "Module.HEAPU8.set(src, ptr >>> 0);")
  static native void copyToHeap(int ptr, Int8Array src);

  /**
   * Opens a stb_truetype font context from raw font bytes already on the WASM heap at
   * {@code dataPtr}, scaled so one line equals {@code pixelHeight} pixels. Returns the opaque
   * context pointer, or {@code 0} when the data is not a recognized font format.
   *
   * <p>The data at {@code dataPtr} must remain alive in WASM memory for as long as the returned
   * context is in use; stb stores an internal pointer into it rather than copying it.
   */
  @JSBody(params = { "dataPtr", "pixelHeight" }, script = "return Module._stb_init(dataPtr, pixelHeight) | 0;")
  static native int stbInit(int dataPtr, float pixelHeight);

  /**
   * Fills the caller-allocated float buffer at {@code metricsPtr} with three vertical font
   * metrics that have already been converted to pixel units: {@code [ascent, descent, lineGap]}.
   * The buffer must be at least 12 bytes (3 floats).
   */
  @JSBody(params = { "ctxPtr", "metricsPtr" }, script = "Module._stb_metrics(ctxPtr, metricsPtr);")
  static native void stbMetrics(int ctxPtr, int metricsPtr);

  /**
   * Rasterizes {@code codepoint} and writes its four pixel metrics into the int buffer at
   * {@code metricsPtr}: {@code [width, height, xOffset, yOffset]}. Returns a WASM pointer to an
   * 8-bit per-pixel coverage bitmap, or {@code 0} for blank glyphs such as the space character.
   * The returned pointer must be freed with {@link #stbFreeBitmap} when it is no longer needed.
   * The {@code metricsPtr} buffer must be at least 16 bytes (4 ints).
   */
  @JSBody(params = { "ctxPtr", "codepoint", "metricsPtr" },
      script = "return Module._stb_rasterize(ctxPtr, codepoint, metricsPtr) | 0;")
  static native int stbRasterize(int ctxPtr, int codepoint, int metricsPtr);

  /** Frees a coverage bitmap pointer previously returned by {@link #stbRasterize}. */
  @JSBody(params = "bitmapPtr", script = "Module._stb_free_bitmap(bitmapPtr);")
  static native void stbFreeBitmap(int bitmapPtr);

  /** Frees a font context pointer previously returned by {@link #stbInit}. */
  @JSBody(params = "ctxPtr", script = "Module._stb_free_ctx(ctxPtr);")
  static native void stbFreeCtx(int ctxPtr);

  /**
   * Reads a {@code float} from the WASM heap at {@code ptr + index * 4}.
   *
   * @param ptr Base pointer, aligned to a 4-byte boundary.
   * @param index Zero-based float index within the buffer.
   */
  @JSBody(params = { "ptr", "index" }, script = "return Module.HEAPF32[((ptr | 0) >> 2) + (index | 0)];")
  static native float getFloat(int ptr, int index);

  /**
   * Reads a signed {@code int} from the WASM heap at {@code ptr + index * 4}.
   *
   * @param ptr Base pointer, aligned to a 4-byte boundary.
   * @param index Zero-based int index within the buffer.
   */
  @JSBody(params = { "ptr", "index" }, script = "return Module.HEAP32[((ptr | 0) >> 2) + (index | 0)] | 0;")
  static native int getInt(int ptr, int index);

  /**
   * Returns a live view of {@code count} bytes from the WASM heap starting at {@code ptr}.
   *
   * <p>The returned array shares the underlying WASM memory buffer, so callers must read or copy
   * the data before freeing the source pointer; the view becomes undefined after the pointer is
   * freed.
   */
  @JSBody(params = { "ptr", "count" },
      script = "return new Int8Array(Module.HEAPU8.buffer, ptr >>> 0, count);")
  static native Int8Array heapView(int ptr, int count);
}
