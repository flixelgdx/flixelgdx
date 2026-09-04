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
package org.flixelgdx.backend;

import org.flixelgdx.Flixel;

/**
 * Platform abstraction for displaying modal alert dialogs to the user.
 *
 * <p>Different platforms surface alerts in very different ways: a desktop app pops up a native OS
 * dialog, a web page might show a browser {@code alert()} call or an overlay, and a mobile game
 * might draw its own in-engine panel. This interface hides all of that detail. Core framework code
 * (and game code) can call {@link Flixel#alert} without knowing which platform it is running on,
 * and the platform backend supplies the right implementation.
 *
 * <p>Think of it like the "notification light" on a device: the hardware is different on every
 * phone, but the software just calls "turn on the light" and the driver figures out the rest.
 *
 * <p>The three severity levels map to common alert conventions:
 * <ul>
 *   <li>{@link #info} - neutral information the user may find useful but does not need to act on
 *   <li>{@link #warn} - something unexpected happened, but the game can continue
 *   <li>{@link #error} - a serious problem that the user or developer should investigate
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * // Show a save-failed notice without caring whether the game is running on desktop or web.
 * Flixel.alert.warn("Save Failed", "Your progress could not be saved. Check your storage space.");
 * }</pre>
 *
 * <p>Implementations must be safe to call from the game thread. Whether alerts are queued,
 * displayed immediately, or logged instead (for platforms with no UI) is left to the backend.
 */
public interface FlixelAlerter {

  /**
   * Displays an informational alert to the user.
   *
   * @param title The alert title.
   * @param message The alert body text.
   */
  void info(String title, String message);

  /**
   * Displays a warning alert to the user.
   *
   * @param title The alert title.
   * @param message The alert body text.
   */
  void warn(String title, String message);

  /**
   * Displays an error alert to the user.
   *
   * @param title The alert title.
   * @param message The alert body text.
   */
  void error(String title, String message);
}
