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
import org.flixelgdx.util.FlixelExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.JSBody;

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
 * <p>On web, {@link #setCrashHandler} wires crash detection into two browser-level error signals:
 * {@code window.onerror}, which fires for uncaught JavaScript exceptions and WebAssembly traps that
 * escape the Java exception system, and {@code window.unhandledrejection}, which fires for unhandled
 * Promise rejections. Both are installed as pure JavaScript handlers (no {@code @JSFunctor} callback)
 * so they work on both the JavaScript and WebAssembly GC TeaVM targets. They log to
 * {@code console.error}, show the crash overlay, and persist the report to {@code localStorage}.
 * Java exceptions thrown inside the game loop are handled separately by the try-catch in
 * {@link FlixelHtml5Runner}, which routes them through the stored {@link FlixelCrashHandler}.
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
   * Stores the crash handler for use by the game loop and installs two pure-JavaScript browser
   * error listeners.
   *
   * <p>{@code window.onerror} handles uncaught JavaScript exceptions and WebAssembly traps.
   * {@code window.unhandledrejection} handles bare Promise rejections. Both are installed as
   * self-contained JavaScript functions with no callback into Java via
   * {@code window.__flixelShowCrash}, which also handles crash persistence and the Copy report
   * overlay.
   *
   * <p>The handler is wrapped so that Java-side crashes also go through
   * {@code window.__flixelShowCrash} before the platform-agnostic handler runs. Calling it first
   * ensures the crash overlay (with its Copy report button) is in place before
   * {@code alert.error()} is called inside the handler; the duplicate-overlay guard in
   * {@link FlixelHtml5Alerter} then skips creating a second one.
   *
   * <p>A {@code @JSFunctor} callback is intentionally not used here. TeaVM 0.13.0's WasmGC code
   * generator produces a nameless function statement ({@code function() {}}) instead of an
   * expression when wrapping any {@code @JSFunctor} method that takes parameters, causing a
   * {@code SyntaxError} at module load time. The only safe {@code @JSFunctor} shape is
   * {@code void} return with zero parameters (see
   * {@link org.flixelgdx.backend.html5.asset.FlixelHtml5AssetPreloader.PreloadCallback}).
   *
   * @param handler The crash handler to install.
   */
  @Override
  public void setCrashHandler(@NotNull FlixelCrashHandler handler) {
    Objects.requireNonNull(handler, "handler cannot be null");
    this.crashHandler = (thread, throwable) -> {
      String threadName = thread != null ? thread.getName() : "main";
      String msg = "There was an uncaught exception on thread \"" + threadName + "\"!\n"
          + FlixelExceptionUtil.getFullExceptionMessage(throwable);
      // Show the crash overlay (Copy report) and persist before delegating. alert.error() inside
      // the handler skips creating a second overlay because the id is already present.
      showJavaCrashOverlay("Uncaught Exception", msg);
      handler.onCrash(thread, throwable);
    };
    installJsErrorHandlers();
  }

  /** Returns the crash handler installed by {@link #setCrashHandler}, or {@code null} if not set. */
  FlixelCrashHandler getCrashHandler() {
    return crashHandler;
  }

  /**
   * Installs {@code window.onerror} and {@code window.unhandledrejection} as self-contained
   * JavaScript handlers and defines {@code window.__flixelShowCrash} as the shared crash overlay
   * function. Both browser error events call through it, and Java-side crashes call it via
   * {@link #showJavaCrashOverlay}. It logs to {@code console.error}, persists the report to
   * {@code localStorage}, and shows the crash overlay with a Copy report button.
   */
  @JSBody(script = """
      window.__flixelShowCrash = function(title, message) {
        console.error('[FlixelGDX] ' + title + ': ' + message);
        try {
          localStorage.setItem('flixelgdx_last_crash', JSON.stringify({
            time: new Date().toISOString(),
            title: title,
            message: message,
            ua: navigator.userAgent,
            url: window.location.href
          }));
        } catch (e) {}
        if (!document.body || document.getElementById('flixel-crash-overlay')) { return; }
        var overlay = document.createElement('div');
        overlay.id = 'flixel-crash-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;background:rgba(0,0,0,0.85);'
          + 'display:flex;align-items:center;justify-content:center;'
          + 'font-family:monospace;padding:16px;box-sizing:border-box;';
        var box = document.createElement('div');
        box.style.cssText = 'background:#111;border:1px solid #333;'
          + 'padding:20px 24px;width:max-content;max-width:80vw;max-height:80vh;overflow-y:auto;'
          + 'color:#ccc;box-sizing:border-box;';
        var titleEl = document.createElement('p');
        titleEl.style.cssText = 'margin:0 0 12px 0;color:#e94560;font-size:0.9em;';
        titleEl.textContent = title;
        var msgEl = document.createElement('p');
        msgEl.style.cssText = 'margin:0 0 16px 0;font-size:0.82em;line-height:1.5;'
          + 'white-space:pre-wrap;word-break:break-word;';
        msgEl.textContent = message;
        var btn = document.createElement('button');
        btn.style.cssText = 'background:#222;color:#ccc;border:1px solid #444;'
          + 'padding:5px 12px;cursor:pointer;font-family:monospace;font-size:0.8em;';
        btn.textContent = 'Copy report';
        var report = (document.title || 'Game') + ' Crash Report\\n'
          + new Date().toISOString() + '\\n'
          + 'Browser: ' + navigator.userAgent + '\\n'
          + 'URL: ' + window.location.href + '\\n\\n'
          + title + '\\n' + message;
        btn.onclick = function () {
          if (navigator.clipboard) { navigator.clipboard.writeText(report).catch(function () {}); }
          btn.textContent = 'Copied';
          setTimeout(function () { btn.textContent = 'Copy report'; }, 2000);
        };
        box.appendChild(titleEl);
        box.appendChild(msgEl);
        box.appendChild(btn);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
      };
      window.onerror = function(msg, src, line, col, err) {
        var detail = err ? (err.stack || err.message || msg) : (msg || 'Unknown JavaScript error');
        detail += '\\n[' + (src || '?') + ':' + (line || '?') + ']';
        window.__flixelShowCrash('JavaScript Error', detail);
        return true;
      };
      window.addEventListener('unhandledrejection', function(event) {
        var r = event.reason;
        var reason = r ? (r.stack || r.message || String(r)) : 'Unknown rejection reason';
        window.__flixelShowCrash('Promise Rejection', reason);
      });
      """)
  private static native void installJsErrorHandlers();

  @JSBody(params = { "title", "message" }, script = """
      if (typeof window.__flixelShowCrash === 'function') {
        window.__flixelShowCrash(title, message);
      }
      """)
  private static native void showJavaCrashOverlay(String title, String message);

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
}
