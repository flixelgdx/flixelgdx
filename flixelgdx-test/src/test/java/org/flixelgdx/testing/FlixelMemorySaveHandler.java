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
package org.flixelgdx.testing;

import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.save.FlixelSaveHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A save handler for unit tests that keeps every save in memory instead of on disk.
 *
 * <p>{@code FlixelSave} only talks to its handler through {@link #read(String)},
 * {@link #write(String, String)} and {@link #delete(String)}, so this is enough to test a full
 * save, flush and reload cycle without a platform backend. Each instance starts empty, so two saves
 * sharing one handler see each other's data, just like two saves on the same disk.
 */
public final class FlixelMemorySaveHandler implements FlixelSaveHandler {

  private final FlixelMap<String, String> entries = new FlixelMap<>();

  @Nullable
  @Override
  public String read(@NotNull String name) {
    return entries.get(name);
  }

  @Override
  public boolean write(@NotNull String name, @NotNull String data) {
    entries.put(name, data);
    return true;
  }

  @Override
  public boolean delete(@NotNull String name) {
    // The contract counts "nothing was stored" as a successful delete, so this never fails.
    entries.remove(name);
    return true;
  }
}
