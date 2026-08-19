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
package org.flixelgdx.backend.html5.graphics;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.graphics.FlixelDisplayMode;
import org.flixelgdx.graphics.FlixelGraphicsApi;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class FlixelHtml5Graphics implements FlixelGraphicsManager {

  private final FlixelArray<FlixelDisplayMode> displayModes;

  public FlixelHtml5Graphics() {
    displayModes = new FlixelArray<>();
  }

  public void initialize(HTMLCanvasElement canvas) {
    jsInitialize(canvas);
  }

  @Override
  public @NotNull FlixelList<FlixelDisplayMode> getDisplayModes() {
    return displayModes;
  }

  @Override
  public @NotNull FlixelGraphicsApi getApi() {
    return jsWebGpuSupported() ? FlixelGraphicsApi.WebGPU : FlixelGraphicsApi.WebGL;
  }

  @JSBody(params = { "canvas" }, script = """
    if (!navigator.gpu) {
      alert("WebGPU isn't supported.");
      return;
    }
    navigator.gpu.requestAdapter().then(adapter => {
      return adapter.requestDevice();
    }).then(device => {
      // TODO: Fill this in when I get the chance.
    }).catch(err => console.error(err));
    """)
  private static native void jsInitialize(HTMLCanvasElement canvas);

  @JSBody(script = "return navigator.gpu;")
  private static native boolean jsWebGpuSupported();
}
