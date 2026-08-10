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
package org.flixelgdx.backend.teavm;

/**
 * Placeholder entry point for the web (TeaVM) backend.
 *
 * <p>The web backend is not implemented on the framework's current rendering and platform stack. On
 * the web the destination is WebGPU (with a WebGL fallback) driven through the framework's own
 * TeaVM bindings, and that work happens in a later phase. Until then this class exists only so the
 * module compiles and names a TeaVM entry point; running it fails fast with a clear message.
 *
 * <p>Use {@code flixelgdx-desktop} to run games today; it is the reference backend (bgfx + SDL3)
 * every platform is validated against first.
 */
public final class FlixelTeaVMLauncher {

  private FlixelTeaVMLauncher() {}

  /**
   * TeaVM entry point. Fails fast because the web backend is not available yet.
   *
   * @param args Ignored.
   */
  public static void main(String[] args) {
    throw new UnsupportedOperationException(
        "The web (TeaVM) backend is not available yet. Run games on flixelgdx-desktop, which is "
            + "the reference backend (bgfx + SDL3); web support (WebGPU + WebGL fallback) lands in "
            + "a later phase.");
  }
}
