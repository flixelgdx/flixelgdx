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
package org.flixelgdx.gradle.teavm;

/**
 * Selects the TeaVM compilation target that the FlixelGDX plugin wires its helper tasks against.
 *
 * <p>Set via {@link FlixelTeaVMExtension#getOutputMode()} in the {@code flixelgdx} block:
 *
 * <pre>{@code
 * flixelgdx {
 *   outputMode = FlixelTeaVMOutputMode.WEBASSEMBLY
 * }
 * }</pre>
 *
 * <p>Defaults to {@link #JAVASCRIPT} when not specified.
 *
 * @see FlixelTeaVMExtension#getOutputMode()
 */
public enum FlixelTeaVMOutputMode {

  /**
   * Compile to JavaScript via TeaVM's {@code js} target.
   *
   * <p>Helper tasks are wired to {@code generateJavaScript} and {@code javaScriptDevServer}.
   * A default {@code index.html} is generated that calls {@code main()} after loading the bundle.
   */
  JAVASCRIPT,

  /**
   * Compile to WebAssembly (GC proposal) via TeaVM's {@code wasmGC} target.
   *
   * <p>Helper tasks are wired to {@code buildWasmGC}. A default {@code index.html} is generated
   * when {@code teavm.wasmGC.copyRuntime = true}; it loads the {@code {targetFileName}-runtime.js}
   * file that TeaVM's {@code copyWasmGCRuntime} task produces.
   */
  WEBASSEMBLY
}
