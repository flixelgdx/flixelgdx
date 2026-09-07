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
 * <p>All three severity levels emit to the browser console and show a full-screen DOM overlay on
 * top of the canvas. The title color distinguishes severity: white for info, yellow for warn, and
 * red for error. Every overlay includes a button that copies a structured report (game name,
 * timestamp, browser, URL, and full message) to the clipboard.
 *
 * <p>Crash persistence to {@code localStorage} is intentionally not part of this class. It is
 * handled by the crash handler wrapper in {@link FlixelHtml5RuntimeDevice} so that only actual
 * crashes are stored, not every {@code alert.error()} call.
 */
public class FlixelHtml5Alerter implements FlixelAlerter {

  @Override
  public void info(String title, String message) {
    String safeTitle = title != null ? title : "Info";
    String safeMessage = message != null ? message : "";
    consoleLog("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#ffffff", "Info Report");
  }

  @Override
  public void warn(String title, String message) {
    String safeTitle = title != null ? title : "Warning";
    String safeMessage = message != null ? message : "";
    consoleWarn("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#f5c518", "Warning Report");
  }

  /**
   * Shows the DOM error overlay and logs to {@code console.error}. The overlay renders on top of
   * the canvas with the title, full message, and a button to copy the report to the clipboard.
   *
   * <p>Crash persistence to {@code localStorage} is the crash handler's responsibility, not the
   * alerter's. This method only displays the overlay so it can be used for non-crash errors too.
   */
  @Override
  public void error(String title, String message) {
    String safeTitle = title != null ? title : "Error";
    String safeMessage = message != null ? message : "";
    consoleError("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#e94560", "Crash Report");
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

  @JSBody(params = { "title", "message", "titleColor", "reportLabel" }, script = """
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
      titleEl.style.cssText = 'margin:0 0 12px 0;font-size:0.9em;color:' + titleColor + ';';
      titleEl.textContent = title;
      var msgEl = document.createElement('p');
      msgEl.style.cssText = 'margin:0 0 16px 0;font-size:0.82em;line-height:1.5;'
        + 'white-space:pre-wrap;word-break:break-word;';
      msgEl.textContent = message;
      var btn = document.createElement('button');
      btn.style.cssText = 'background:#222;color:#ccc;border:1px solid #444;'
        + 'padding:5px 12px;cursor:pointer;font-family:monospace;font-size:0.8em;';
      btn.textContent = 'Copy report';
      var report = (document.title || 'Game') + ' ' + reportLabel + '\\n'
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
  private static native void showDomAlert(String title, String message, String titleColor, String reportLabel);

  @JSBody(params = "text", script = "console.log(text);")
  private static native void consoleLog(String text);

  @JSBody(params = "text", script = "console.warn(text);")
  private static native void consoleWarn(String text);

  @JSBody(params = "text", script = "console.error(text);")
  private static native void consoleError(String text);
}
