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

import org.jetbrains.annotations.NotNull;

/**
 * Receives the result of a system file picker dialog.
 *
 * <p>Think of it as the "call me back" slip you hand to a clerk: you ask for files, walk away, and
 * the clerk calls you when the user has made a choice. The listener is always invoked on the game's
 * main thread, so it is safe to touch game objects inside it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.host.pickFile(files -> {
 *   if (files.length > 0) {
 *     String text = files[0].readString();
 *   }
 * }, "txt");
 * }</pre>
 */
@FunctionalInterface
public interface FlixelFilePickListener {

  /**
   * Called once when the picker closes.
   *
   * @param files The chosen files. Empty if the user canceled or the pick failed; never
   *     {@code null}. A single-file pick holds at most one entry.
   */
  void onPick(@NotNull FlixelFile @NotNull [] files);
}
