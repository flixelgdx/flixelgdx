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
 * and provided no structured crash information.
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
   * {@code localStorage}. The overlay renders on top of the canvas with the title, full message,
   * and a button to copy the crash report to the clipboard.
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
      if (!document.body || document.getElementById('flixel-crash-overlay')) { return; }

      var overlay = document.createElement('div');
      overlay.id = 'flixel-crash-overlay';
      overlay.style.cssText = 'position:fixed;inset:0;z-index:99999;background:rgba(0,0,0,0.85);'
        + 'display:flex;align-items:center;justify-content:center;'
        + 'font-family:monospace;padding:16px;box-sizing:border-box;';

      var box = document.createElement('div');
      box.style.cssText = 'background:#111;border:1px solid #333;'
        + 'padding:20px 24px;max-width:600px;width:100%;max-height:80vh;overflow-y:auto;'
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
      var report = 'FlixelGDX Crash Report\\n'
        + new Date().toISOString() + '\\n'
        + 'Browser: ' + navigator.userAgent + '\\n'
        + 'URL: ' + window.location.href + '\\n\\n'
        + title + '\\n' + message;
      btn.onclick = function () {
        if (navigator.clipboard) {
          navigator.clipboard.writeText(report).catch(function () {});
        }
        btn.textContent = 'Copied';
        setTimeout(function () { btn.textContent = 'Copy report'; }, 2000);
      };

      box.appendChild(titleEl);
      box.appendChild(msgEl);
      box.appendChild(btn);
      overlay.appendChild(box);
      document.body.appendChild(overlay);
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
