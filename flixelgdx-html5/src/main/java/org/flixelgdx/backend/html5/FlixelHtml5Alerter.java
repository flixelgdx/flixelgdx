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

import org.flixelgdx.backend.FlixelAlerter;
import org.teavm.jso.JSBody;

/**
 * Alert implementation for the HTML5 platform.
 *
 * <p>Info and warn alerts emit to the browser console, which is always visible to developers
 * without interrupting the running page. Error alerts show a full-screen DOM crash overlay on top
 * of the canvas, log to {@code console.error}, and save the report to {@code localStorage} under
 * the key {@code flixelgdx_last_crash} so it persists across a page reload.
 *
 * <p>The overlay replaces the old {@code window.alert()} call, which was synchronous, blocking,
 * and showed no structured information.
 */
public class FlixelHtml5Alerter implements FlixelAlerter {

  @Override
  public void info(String title, String message) {
    consoleLog("[FlixelGDX] " + label(title, message));
  }

  @Override
  public void warn(String title, String message) {
    consoleWarn("[FlixelGDX] " + label(title, message));
  }

  /**
   * Shows the DOM crash overlay, logs to {@code console.error}, and persists the report to
   * {@code localStorage}. The overlay renders on top of the canvas with the title, message, a
   * copy-to-clipboard button, and a note pointing to the browser console for the full stack trace.
   */
  @Override
  public void error(String title, String message) {
    String safeTitle = title != null ? title : "Error";
    String safeMessage = message != null ? message : "";
    consoleError("[FlixelGDX] " + label(safeTitle, safeMessage));
    persistCrash(safeTitle, safeMessage);
    showCrashOverlay(safeTitle, safeMessage);
  }

  private static String label(String title, String message) {
    if (title == null && message == null) {
      return "";
    }
    if (title == null) {
      return message;
    }
    if (message == null || message.isEmpty()) {
      return title;
    }
    return title + ": " + message;
  }

  @JSBody(params = { "title", "message" }, script = """
      (function () {
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
          if (navigator.clipboard) {
            navigator.clipboard.writeText(report).catch(function () {});
          }
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
      })();
      """)
  private static native void showCrashOverlay(String title, String message);

  @JSBody(params = { "title", "message" }, script = """
      try {
        localStorage.setItem('flixelgdx_last_crash', JSON.stringify({
          time: new Date().toISOString(),
          title: title,
          message: message,
          ua: navigator.userAgent,
          url: window.location.href
        }));
      } catch (e) {}
      """)
  private static native void persistCrash(String title, String message);

  @JSBody(params = "text", script = "console.log(text);")
  private static native void consoleLog(String text);

  @JSBody(params = "text", script = "console.warn(text);")
  private static native void consoleWarn(String text);

  @JSBody(params = "text", script = "console.error(text);")
  private static native void consoleError(String text);
}
