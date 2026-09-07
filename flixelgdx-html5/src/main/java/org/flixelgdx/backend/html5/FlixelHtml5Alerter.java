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
 * red for error. Every overlay pauses the game loop (via {@code window.__flixelAlertPaused}) and
 * shows an OK button that dismisses it and resumes the loop.
 *
 * <p>The Copy report button and crash persistence to {@code localStorage} are handled exclusively
 * by the crash overlay path in {@link FlixelHtml5RuntimeDevice}, not by this alerter, so that
 * only actual crashes carry those behaviors.
 */
public class FlixelHtml5Alerter implements FlixelAlerter {

  @Override
  public void info(String title, String message) {
    String safeTitle = title != null ? title : "Info";
    String safeMessage = message != null ? message : "";
    consoleLog("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#ffffff");
  }

  @Override
  public void warn(String title, String message) {
    String safeTitle = title != null ? title : "Warning";
    String safeMessage = message != null ? message : "";
    consoleWarn("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#f5c518");
  }

  /**
   * Shows the DOM error overlay, logs to {@code console.error}, and pauses the game loop. The
   * overlay shows an OK button that dismisses it and resumes the loop.
   *
   * <p>Crash overlays (with a Copy report button) are shown separately by
   * {@link FlixelHtml5RuntimeDevice} before the crash handler runs, so the crash-specific overlay
   * is already in place when {@code alert.error()} is called from the crash path. The duplicate
   * check at the top of {@code showDomAlert} prevents a second overlay from appearing.
   */
  @Override
  public void error(String title, String message) {
    String safeTitle = title != null ? title : "Error";
    String safeMessage = message != null ? message : "";
    consoleError("[FlixelGDX] " + label(safeTitle, safeMessage));
    showDomAlert(safeTitle, safeMessage, "#e94560");
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

  /**
   * Shows a full-screen DOM alert overlay with an OK button that dismisses it.
   *
   * <p>Sets {@code window.__flixelAlertPaused = true} before adding the overlay so the game
   * loop's {@code requestAnimationFrame} callback stops updating while the alert is visible.
   * Clicking OK clears the flag and removes the overlay, resuming the loop.
   *
   * <p>If an overlay with id {@code flixel-crash-overlay} is already present (placed by the crash
   * handler before calling {@code alert.error()}), this method returns immediately so the crash
   * overlay is not replaced by a generic error box.
   */
  @JSBody(params = { "title", "message", "titleColor" }, script = """
      if (!document.body || document.getElementById('flixel-crash-overlay')) { return; }
      window.__flixelAlertPaused = true;
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
      btn.textContent = 'OK';
      btn.onclick = function () {
        window.__flixelAlertPaused = false;
        if (overlay.parentNode) { overlay.parentNode.removeChild(overlay); }
      };
      box.appendChild(titleEl);
      box.appendChild(msgEl);
      box.appendChild(btn);
      overlay.appendChild(box);
      document.body.appendChild(overlay);
      """)
  private static native void showDomAlert(String title, String message, String titleColor);

  @JSBody(params = "text", script = "console.log(text);")
  private static native void consoleLog(String text);

  @JSBody(params = "text", script = "console.warn(text);")
  private static native void consoleWarn(String text);

  @JSBody(params = "text", script = "console.error(text);")
  private static native void consoleError(String text);
}
