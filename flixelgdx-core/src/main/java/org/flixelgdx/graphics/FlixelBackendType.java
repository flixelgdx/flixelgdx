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
package org.flixelgdx.graphics;

/**
 * Identifies which graphics backend was selected when the game started.
 *
 * <p>Game code never chooses a backend, and it should almost never need to branch on one either.
 * This enum exists mostly for introspection: logging what is running, showing it on a debug
 * overlay, or guarding a rare, backend-specific workaround. Read it from
 * {@link FlixelGraphicsManager#getBackendType() Flixel.graphics.getBackendType()}.
 *
 * <p>The actual drawing library sits behind the internal {@link FlixelGraphicsBackend} seam and is
 * never exposed to game code directly. This enum only names it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * if (Flixel.graphics.getBackendType() == FlixelBackendType.WEBGL) {
 *   // Fall back to a simpler effect on the WebGL path.
 * }
 * }</pre>
 *
 * @see FlixelGraphicsManager#getBackendType()
 */
public enum FlixelBackendType {

  /**
   * The transitional libGDX-backed implementation used while the framework migrates off libGDX.
   *
   * <p>This is what runs today. It exists so the abstraction seam can be introduced and tested
   * before any native backend is written, and it is expected to be removed once the native and web
   * backends reach parity.
   */
  LIBGDX,

  /** The native backend built on bgfx (desktop, Android, and eventually iOS via Metal). */
  BGFX,

  /** The web backend built on the browser's native WebGPU, through the framework's own bindings. */
  WEBGPU,

  /** The web fallback backend built on WebGL, used when the browser has no WebGPU support. */
  WEBGL,

  /**
   * No real backend is present.
   *
   * <p>This is reported by the safe default manager on headless, server, or not-yet-initialized
   * sessions, where drawing is a no-op.
   */
  NOOP
}
