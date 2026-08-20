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

import org.flixelgdx.backend.FlixelRunEnvironment;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.teavm.jso.JSBody;

/**
 * Reports what a browser can tell a game about the machine it runs on, which is very little.
 *
 * <p>A desktop runtime device can inspect the JVM's memory, the working directory, and whether it
 * was launched from a JAR. A sandboxed browser tab exposes none of that. The one useful signal some
 * browsers offer is a rough JavaScript heap size through {@code performance.memory}, which this
 * class surfaces for the debug overlay; everything else returns the safe default the interface
 * already provides.
 */
public class FlixelHtml5RuntimeDevice implements FlixelRuntimeDevice {

  @Override
  public long getJavaHeap() {
    return usedHeapBytes();
  }

  @Override
  public long getNativeHeap() {
    return 0L;
  }

  @Override
  public boolean isRunningFromJar() {
    return false;
  }

  @Override
  public boolean isRunningInIDE() {
    return false;
  }

  @Override
  public FlixelRunEnvironment getEnvironment() {
    return FlixelRunEnvironment.UNKNOWN;
  }

  @JSBody(script = "return (window.performance && window.performance.memory)"
      + " ? window.performance.memory.usedJSHeapSize : 0;")
  private static native double usedHeap();

  /**
   * Reads the used JavaScript heap in bytes, or zero on browsers that do not expose it.
   *
   * <p>The value is read as a {@code double} because the browser reports it as a plain JavaScript
   * number, then narrowed to the {@code long} the interface expects.
   *
   * @return The used heap in bytes.
   */
  private static long usedHeapBytes() {
    return (long) usedHeap();
  }
}
