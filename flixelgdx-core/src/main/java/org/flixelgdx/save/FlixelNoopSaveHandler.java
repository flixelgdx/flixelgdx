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
package org.flixelgdx.save;

import org.flixelgdx.Flixel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link FlixelSaveHandler} that stores nothing.
 *
 * <p>This is the safe default on {@link Flixel#saveHandler} before a backend installs real storage,
 * and on headless sessions. Reads find nothing and writes report failure, so save code never
 * crashes; it simply has nowhere to keep its data.
 */
public enum FlixelNoopSaveHandler implements FlixelSaveHandler {

  /** The single shared no-op save handler. */
  INSTANCE;

  @Override
  @Nullable
  public String read(@NotNull String name) {
    return null;
  }

  @Override
  public boolean write(@NotNull String name, @NotNull String data) {
    return false;
  }

  @Override
  public boolean delete(@NotNull String name) {
    return true;
  }

  @Override
  public boolean exists(@NotNull String name) {
    return false;
  }
}
