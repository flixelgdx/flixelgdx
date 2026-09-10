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
    showDomAlert(safeTitle, safeMessage, "#ffffff");
  }

  @Override
  public void warn(String title, String message) {
    String safeTitle = title != null ? title : "Warning";
    String safeMessage = message != null ? message : "";
    showDomAlert(safeTitle, safeMessage, "#f5c518");
  }

  @Override
  public void error(String title, String message) {
    String safeTitle = title != null ? title : "Error";
    String safeMessage = message != null ? message : "";
    showDomAlert(safeTitle, safeMessage, "#e94560");
  }

  /**
   * Shows a full-screen DOM alert overlay with an OK button that dismisses it.
   *
   * <p>Delegates to {@code window.__flixelOverlay}, which is defined by
   * {@code FlixelHtml5RuntimeDevice.installJsErrorHandlers} and holds the single shared
   * overlay-building implementation for both alert dialogs and crash reports.
   */
  @JSBody(params = { "title", "message", "titleColor" }, script = """
      if (typeof window.__flixelOverlay === 'function') {
        window.__flixelOverlay(title, message, titleColor, false);
      }
      """)
  private static native void showDomAlert(String title, String message, String titleColor);
}
