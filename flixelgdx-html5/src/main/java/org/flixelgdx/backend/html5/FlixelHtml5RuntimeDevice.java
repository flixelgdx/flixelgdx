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
   * self-contained JavaScript functions with no callback into Java; they log to
   * {@code console.error}, show the DOM crash overlay, and persist the report to
   * {@code localStorage}.
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
    this.crashHandler = handler;
    installJsErrorHandlers();
  }

  /** Returns the crash handler installed by {@link #setCrashHandler}, or {@code null} if not set. */
  FlixelCrashHandler getCrashHandler() {
    return crashHandler;
  }

  /**
   * Installs {@code window.onerror} and {@code window.unhandledrejection} as self-contained
   * JavaScript handlers. Both handlers log to {@code console.error}, persist the report to
   * {@code localStorage}, and show the DOM crash overlay without calling back into Java.
   */
  @JSBody(script = """
      function flixelShowJsCrash(title, message) {
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
        overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;background:rgba(0,0,0,0.90);'
          + 'display:flex;align-items:center;justify-content:center;'
          + 'font-family:monospace;box-sizing:border-box;padding:16px;';
        var box = document.createElement('div');
        box.style.cssText = 'background:#161622;border:2px solid #e94560;border-radius:6px;'
          + 'padding:28px 32px;max-width:680px;width:100%;max-height:85vh;overflow-y:auto;'
          + 'color:#eee;box-sizing:border-box;';
        var heading = document.createElement('h2');
        heading.style.cssText = 'margin:0 0 12px 0;color:#e94560;font-size:1.1em;'
          + 'letter-spacing:2px;text-transform:uppercase;';
        heading.textContent = 'GAME CRASHED';
        var titleEl = document.createElement('p');
        titleEl.style.cssText = 'margin:0 0 4px 0;font-weight:bold;color:#f4a261;font-size:0.95em;';
        titleEl.textContent = title;
        var msgEl = document.createElement('p');
        msgEl.style.cssText = 'margin:0 0 14px 0;font-size:0.88em;line-height:1.6;'
          + 'white-space:pre-wrap;word-break:break-word;color:#ddd;';
        msgEl.textContent = message;
        var hint = document.createElement('p');
        hint.style.cssText = 'margin:0 0 16px 0;font-size:0.8em;color:#888;';
        hint.textContent = 'Open the browser console (F12) for the full stack trace and log output.';
        var footer = document.createElement('div');
        footer.style.cssText = 'display:flex;align-items:center;justify-content:space-between;'
          + 'flex-wrap:wrap;gap:10px;border-top:1px solid #333;padding-top:14px;';
        var btn = document.createElement('button');
        btn.style.cssText = 'background:#e94560;color:#fff;border:none;border-radius:4px;'
          + 'padding:7px 18px;cursor:pointer;font-family:monospace;font-size:0.85em;letter-spacing:1px;';
        btn.textContent = 'Copy Report';
        var report = 'FlixelGDX Crash Report\\n'
          + new Date().toISOString() + '\\n'
          + 'Browser: ' + navigator.userAgent + '\\n'
          + 'URL: ' + window.location.href + '\\n\\n'
          + title + '\\n' + message;
        btn.onclick = function () {
          if (navigator.clipboard) { navigator.clipboard.writeText(report).catch(function () {}); }
          btn.textContent = 'Copied!';
          setTimeout(function () { btn.textContent = 'Copy Report'; }, 2000);
        };
        var ua = document.createElement('small');
        ua.style.cssText = 'color:#666;font-size:0.72em;max-width:400px;overflow:hidden;'
          + 'text-overflow:ellipsis;white-space:nowrap;';
        ua.textContent = navigator.userAgent;
        footer.appendChild(btn);
        footer.appendChild(ua);
        box.appendChild(heading);
        box.appendChild(titleEl);
        box.appendChild(msgEl);
        box.appendChild(hint);
        box.appendChild(footer);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
      }
      window.onerror = function(msg, src, line, col, err) {
        var detail = err ? (err.stack || err.message || msg) : (msg || 'Unknown JavaScript error');
        detail += '\\n[' + (src || '?') + ':' + (line || '?') + ']';
        flixelShowJsCrash('JavaScript Error', detail);
        return true;
      };
      window.addEventListener('unhandledrejection', function(event) {
        var r = event.reason;
        var reason = r ? (r.stack || r.message || String(r)) : 'Unknown rejection reason';
        flixelShowJsCrash('Promise Rejection', reason);
      });
      """)
  private static native void installJsErrorHandlers();

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
