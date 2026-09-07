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

import org.flixelgdx.backend.FlixelCrashHandler;
import org.flixelgdx.backend.FlixelRunEnvironment;
import org.flixelgdx.backend.FlixelRuntimeDevice;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.Objects;

/**
 * Reports what a browser can tell a game about the machine it runs on, which is very little.
 *
 * <p>A desktop runtime device can inspect the JVM's memory, the working directory, and whether it
 * was launched from a JAR. A sandboxed browser tab exposes none of that. The one useful signal some
 * browsers offer is a rough JavaScript heap size through {@code performance.memory}, which this
 * class surfaces for the debug overlay; everything else returns the safe default the interface
 * already provides.
 *
 * <p>On web, {@link #setCrashHandler} wires the supplied handler into two browser-level error
 * signals: {@code window.onerror}, which fires for uncaught JavaScript exceptions and WebAssembly
 * traps that escape the Java exception system, and {@code window.unhandledrejection}, which fires
 * for unhandled Promise rejections. Together these complement the try-catch already wrapped around
 * the game loop in {@link FlixelHtml5Runner}, catching anything that happens outside that boundary.
 */
public class FlixelHtml5RuntimeDevice implements FlixelRuntimeDevice {

  private FlixelRuntimeMode mode = FlixelRuntimeMode.RELEASE;
  private FlixelCrashHandler crashHandler;

  private boolean runtimeModeSet = false;

  @Override
  public long getJavaHeap() {
    return usedHeapBytes();
  }

  @Override
  public FlixelRunEnvironment getEnvironment() {
    return FlixelRunEnvironment.BROWSER;
  }

  @Override
  public @NotNull FlixelRuntimeMode getMode() {
    return mode;
  }

  @Override
  public void setMode(@NotNull FlixelRuntimeMode mode) {
    Objects.requireNonNull(mode, "The provided runtime mode cannot be null.");
    if (!runtimeModeSet) {
      this.mode = mode;
      runtimeModeSet = true;
    } else {
      throw new RuntimeException("The runtime mode has already been set, it cannot be changed.");
    }
  }

  /**
   * Installs the crash handler and wires it into two browser error signals.
   *
   * <p>{@code window.onerror} catches uncaught JavaScript exceptions and WebAssembly traps that
   * escape the Java exception system entirely. {@code window.unhandledrejection} catches unhandled
   * Promise rejections. Both complement the try-catch in the game loop, which handles Java
   * exceptions thrown during a frame.
   *
   * <p>All JavaScript-side error extraction (reading {@code .stack}, {@code .message}, source
   * location) is performed inside the JavaScript wrappers so the {@link JsCrashCallback} interface
   * only ever receives a plain {@link String}. This avoids passing {@code JSObject} references
   * through a {@link JSFunctor} callback, which is not supported in TeaVM's WasmGC code generator.
   *
   * @param handler The crash handler to install.
   */
  @Override
  public void setCrashHandler(@NotNull FlixelCrashHandler handler) {
    Objects.requireNonNull(handler, "handler cannot be null");
    this.crashHandler = handler;

    // window.onerror: the JS wrapper extracts the best available description from the error object
    // and calls back with a plain string so no JSObject crosses the @JSFunctor boundary.
    // Returning true from window.onerror suppresses the browser's own error UI.
    installWindowOnerror(detail -> handler.onCrash(null, new RuntimeException(detail)));

    // window.unhandledrejection: same pattern, reason extracted in JS before the callback.
    installRejectionListener(
        reason -> handler.onCrash(null, new RuntimeException("Unhandled Promise rejection: " + reason)));
  }

  /** Returns the crash handler installed by {@link #setCrashHandler}, or {@code null} if not set. */
  FlixelCrashHandler getCrashHandler() {
    return crashHandler;
  }

  /**
   * Installs a {@code window.onerror} handler that extracts the best available description from
   * the error and passes it as a plain string to {@code callback}. Always returns {@code true} to
   * suppress the browser's own error UI.
   */
  @JSBody(params = "callback", script = """
      window.onerror = function(msg, src, line, col, err) {
        var detail = err ? (err.stack || err.message || msg) : (msg || 'Unknown JavaScript error');
        detail += '\\n[' + (src || '?') + ':' + (line || '?') + ']';
        callback(detail);
        return true;
      };
      """)
  private static native void installWindowOnerror(JsCrashCallback callback);

  /**
   * Adds a {@code window.unhandledrejection} listener that extracts the rejection reason and
   * passes it as a plain string to {@code callback}.
   */
  @JSBody(params = "callback", script = """
      window.addEventListener('unhandledrejection', function(event) {
        var r = event.reason;
        var reason = r ? (r.stack || r.message || String(r)) : 'Unknown rejection reason';
        callback(reason);
      });
      """)
  private static native void installRejectionListener(JsCrashCallback callback);

  @JSBody(script = """
      return (window.performance && window.performance.memory)
        ? window.performance.memory.usedJSHeapSize : 0;
      """)
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

  /**
   * Callback type shared by both browser error signals. Receives an already-formatted string
   * describing the error so no {@code JSObject} references need to cross the JS-to-Java boundary,
   * which is not supported in TeaVM's WasmGC {@link JSFunctor} code generator.
   */
  @JSFunctor
  @FunctionalInterface
  interface JsCrashCallback extends JSObject {
    void onCrash(String detail);
  }
}
