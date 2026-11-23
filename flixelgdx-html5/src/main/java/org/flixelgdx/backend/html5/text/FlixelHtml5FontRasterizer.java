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

import org.flixelgdx.collections.FlixelIdentityMap;
import org.flixelgdx.text.FlixelFontRasterizer;
import org.flixelgdx.text.FlixelRasterizedFont;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * The browser font rasterizer, backed by the Emscripten-compiled stb_truetype WebAssembly module.
 *
 * <p>The web build plugin extracts the WASM module from the framework JAR, places it under
 * {@code native/} in the web output, and injects a {@code <script>} tag for the Emscripten glue
 * file into the generated {@code index.html} before the game bundle loads. Once the module is
 * ready, this rasterizer copies the raw font bytes into the WASM heap, opens a stb_truetype
 * context, reads the vertical font metrics, and hands the live context to
 * {@link FlixelHtml5RasterizedFont} for per-glyph rasterization on demand.
 *
 * <p>Each font file is copied into WASM memory only once. The font registry passes the same
 * {@code byte[]} every time it opens another size of a font, so every size opened from one array
 * shares one copy, which is freed when the last of those sizes is destroyed. WASM memory starts
 * at 16 MB and grows as needed, so large fonts such as full Japanese or Chinese ones fit.
 *
 * <p>If the module has not finished initializing when {@link #open} is called, the method returns
 * {@code null} gracefully and the font system falls back to the packaged bitmap font.
 *
 * @see StbWasm
 * @see FlixelHtml5RasterizedFont
 */
public class FlixelHtml5FontRasterizer implements FlixelFontRasterizer {

  /** Font files already copied into WASM memory, keyed by the exact array they came from. */
  private final FlixelIdentityMap<byte[], SharedFontData> shared = new FlixelIdentityMap<>();

  @Override
  public @Nullable FlixelRasterizedFont open(byte @NotNull [] data, float pixelHeight) {
    if (!StbWasm.isReady()) {
      return null;
    }
    SharedFontData fontData = acquire(data);
    if (fontData == null) {
      return null;
    }

    // stb_init stores a pointer into the font data rather than copying it, so the data must stay
    // alive for the entire lifetime of ctxPtr. The shared copy is released when the font is destroyed.
    int ctxPtr = StbWasm.stbInit(fontData.pointer, pixelHeight);
    if (ctxPtr == 0) {
      release(fontData);
      return null;
    }

    // Read the three vertical metrics: [ascent, descent, lineGap] in pixel units.
    int metricsPtr = StbWasm.malloc(12);
    StbWasm.stbMetrics(ctxPtr, metricsPtr);
    float ascent = StbWasm.getFloat(metricsPtr, 0);
    // stb's raw descent is negative (below the baseline); flip it to the positive convention
    // that FlixelRasterizedFont.getDescent() documents.
    float descent = -StbWasm.getFloat(metricsPtr, 1);
    float lineGap = StbWasm.getFloat(metricsPtr, 2);
    StbWasm.free(metricsPtr);

    float lineHeight = ascent + descent + lineGap;
    return new FlixelHtml5RasterizedFont(ctxPtr, fontData, this, pixelHeight, ascent, descent, lineHeight);
  }

  /**
   * Returns the WASM copy of {@code data}, copying it in first if no open font uses it yet.
   *
   * @return The shared copy with one more user counted, or {@code null} when WASM memory is full.
   */
  @Nullable
  private SharedFontData acquire(byte @NotNull [] data) {
    SharedFontData fontData = shared.get(data);
    if (fontData == null) {
      int pointer = StbWasm.malloc(data.length);
      if (pointer == 0) {
        return null;
      }
      // Wrap the Java byte[] as a native Int8Array so HEAPU8.set() can bulk-copy it into
      // WASM memory in a single JavaScript operation rather than iterating byte by byte.
      StbWasm.copyToHeap(pointer, Int8Array.copyFromJavaArray(data));
      fontData = new SharedFontData(data, pointer);
      shared.put(data, fontData);
    }
    fontData.users++;
    return fontData;
  }

  /**
   * Counts one user of a shared font copy as gone, freeing the WASM memory when it was the last.
   *
   * @param fontData The copy that a destroyed font was using.
   */
  void release(@NotNull SharedFontData fontData) {
    fontData.users--;
    if (fontData.users <= 0 && shared.remove(fontData.source) != null) {
      StbWasm.free(fontData.pointer);
    }
  }

  /**
   * Returns how many distinct font files are currently copied into WASM memory.
   *
   * @return The number of shared font copies.
   */
  public int getSharedFontCount() {
    return shared.getSize();
  }

  /** One font file copied into WASM memory, and how many open fonts are using it. */
  static final class SharedFontData {

    /** The array the copy was made from, used as the cache key. */
    final byte[] source;

    /** Where the copy starts in WASM memory. */
    final int pointer;

    /** How many open fonts use this copy. */
    int users;

    SharedFontData(byte[] source, int pointer) {
      this.source = source;
      this.pointer = pointer;
    }
  }
}
