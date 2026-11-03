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
package org.flixelgdx.file;

/**
 * Receives the result of a system save dialog.
 *
 * <p>It works like a delivery receipt: you hand your data to the courier, walk away, and later the
 * receipt tells you whether the package arrived. The listener is always invoked on the game's main
 * thread, so it is safe to touch game objects inside it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.host.saveFile(levelJson.getBytes(StandardCharsets.UTF_8), "level.json", saved -> {
 *   statusText.setText(saved ? "Level saved!" : "Save canceled.");
 * }, "json");
 * }</pre>
 */
@FunctionalInterface
public interface FlixelFileSaveListener {

  /** Shared listener that ignores the result, for callers that do not care whether it saved. */
  FlixelFileSaveListener NONE = saved -> {
  };

  /**
   * Called once when the save dialog closes.
   *
   * @param saved {@code true} if the data was written, {@code false} if the user canceled or the
   *     write failed.
   */
  void onSave(boolean saved);
}
