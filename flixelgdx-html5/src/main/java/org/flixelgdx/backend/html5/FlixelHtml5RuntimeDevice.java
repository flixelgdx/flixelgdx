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
   * @param handler The crash handler to install.
   */
  @Override
  public void setCrashHandler(@NotNull FlixelCrashHandler handler) {
    Objects.requireNonNull(handler, "handler cannot be null");
    this.crashHandler = handler;

    // window.onerror: catches uncaught JS exceptions and WASM traps that escape the Java layer.
    // Returning true suppresses the browser's own built-in error UI so the framework overlay takes over.
    installWindowOnerror((msg, src, line, col, jsErr) -> {
      String detail = extractJsError(jsErr, msg);
      handler.onCrash(null, new RuntimeException(detail));
      return true;
    });

    // window.unhandledrejection: catches Promises that rejected without a .catch() handler.
    installRejectionListener(event -> {
      String reason = extractRejectionReason(event);
      handler.onCrash(null, new RuntimeException("Unhandled Promise rejection: " + reason));
    });
  }

  /** Returns the crash handler installed by {@link #setCrashHandler}, or {@code null} if not set. */
  FlixelCrashHandler getCrashHandler() {
    return crashHandler;
  }

  @JSBody(params = { "err", "fallback" }, script = """
      if (!err) { return fallback || 'Unknown JavaScript error'; }
      return err.stack || err.message || String(err);
      """)
  private static native String extractJsError(JSObject err, String fallback);

  @JSBody(params = "event", script = """
      var r = event.reason;
      if (!r) { return 'Unknown rejection reason'; }
      return r.stack || r.message || String(r);
      """)
  private static native String extractRejectionReason(JSObject event);

  @JSBody(params = "handler", script = "window.onerror = handler;")
  private static native void installWindowOnerror(JsErrorHandler handler);

  @JSBody(params = "handler", script = "window.addEventListener('unhandledrejection', handler);")
  private static native void installRejectionListener(JsRejectionHandler handler);

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

  /** Callback type for {@code window.onerror}. Returns {@code true} to suppress the browser's default error UI. */
  @JSFunctor
  @FunctionalInterface
  interface JsErrorHandler extends JSObject {
    boolean onError(String message, String source, int line, int col, JSObject error);
  }

  /** Callback type for {@code window.unhandledrejection}. */
  @JSFunctor
  @FunctionalInterface
  interface JsRejectionHandler extends JSObject {
    void onRejection(JSObject event);
  }
}
