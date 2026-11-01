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
package org.flixelgdx.backend.android.graphics;

import org.flixelgdx.backend.gpu.FlixelGpuGraphics;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.util.DisplayMetrics;

/**
 * The Android graphics manager: the shared WebGPU renderer plus the facts only an Android screen knows.
 *
 * <p>All drawing, textures, render targets and shaders come from {@link FlixelGpuGraphics}; this class adds the
 * screen density and the pixels-per-inch of the display, which games use to size touch controls and text. The
 * Android runner creates the GPU device and the surface (see {@code FlixelAndroidRunner}), because Android decides
 * when a surface exists, and hands them to {@link #initialize}.
 */
public class FlixelAndroidGraphics extends FlixelGpuGraphics {

  @NotNull
  private final Activity activity;

  private float density = 1f;
  private float ppi;

  /**
   * Creates a graphics manager for the given activity.
   *
   * @param activity The host activity, used to read the display metrics.
   */
  public FlixelAndroidGraphics(@NotNull Activity activity) {
    this.activity = activity;
    refreshMetrics();
  }

  /**
   * Reads the display density and pixels-per-inch again. Call it after the screen configuration changes (for
   * example when the user moves the app to another display).
   */
  public void refreshMetrics() {
    DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
    density = metrics.density;
    ppi = metrics.xdpi;
  }

  /**
   * Does nothing: on Android the screen always decides the resolution.
   *
   * <p>A game covers the whole display, and phones come in many shapes, so a fixed resolution would only
   * stretch or squash the picture. Use {@link #setRenderScale(float)} to trade sharpness for speed instead.
   *
   * @param width Ignored.
   * @param height Ignored.
   */
  @Override
  public void setRenderResolution(int width, int height) {}

  @Override
  public float getDensity() {
    return density;
  }

  @Override
  public float getPpi() {
    return ppi;
  }

  @NotNull
  public Activity getActivity() {
    return activity;
  }
}
