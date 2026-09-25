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
package org.flixelgdx.backend.android;

/**
 * Listener for GL context loss and restoration events on Android.
 *
 * <p>Android may destroy the EGL context when the app goes to the background
 * ({@code GLSurfaceView} with {@code setPreserveEGLContextOnPause(false)}, or when the OS
 * reclaims GPU resources). When a new surface is created after the context was lost,
 * {@link FlixelAndroidRunner} calls {@link #onContextRestored()} so the graphics manager can
 * re-upload textures and recreate GPU resources.
 *
 * <p>The Android GLES graphics manager implements this interface and registers itself on the runner
 * via {@link FlixelAndroidRunner#setContextListener}. Keep this interface package-level; it is an
 * internal contract between the runner and the graphics manager and is not part of the public API.
 */
interface FlixelAndroidContextListener {

  /**
   * Called on the GL thread when the EGL context has been lost, just before the surface is
   * destroyed. GPU resources (textures, buffers, shaders) are no longer valid after this point.
   */
  void onContextLost();

  /**
   * Called on the GL thread when a new EGL context has been created after a prior loss. The
   * graphics manager should re-upload all GPU resources here.
   */
  void onContextRestored();
}
