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
package org.flixelgdx.backend.html5.file;

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A read-only {@link FlixelFile} whose bytes already live in memory.
 *
 * <p>A browser never reveals the real location of a file the user picks, it only hands over the
 * contents. This class wraps those contents so the file can be used like any other
 * {@link FlixelFile}: the name comes from the browser, and reads return the stored bytes. Writes
 * and deletes are not supported and return {@code false}.
 */
public class FlixelHtml5MemoryFile implements FlixelFile {

  @NotNull
  private final String name;

  private final byte @NotNull [] data;

  /**
   * Creates an in-memory file.
   *
   * @param name The file name reported by the browser.
   * @param data The file contents; the array is kept, not copied.
   */
  public FlixelHtml5MemoryFile(@NotNull String name, byte @NotNull [] data) {
    this.name = name;
    this.data = data;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  @NotNull
  public String readString() {
    return readString(null);
  }

  @Override
  @NotNull
  public String readString(@Nullable String charset) {
    Charset cs = StandardCharsets.UTF_8;
    if (charset != null) {
      try {
        cs = Charset.forName(charset);
      } catch (RuntimeException ignored) {
        // Unknown charset names fall back to UTF-8.
      }
    }
    return new String(data, cs);
  }

  @Override
  public byte @NotNull [] readBytes() {
    return data.clone();
  }

  @Override
  public long length() {
    return data.length;
  }

  @Override
  @NotNull
  public String getPath() {
    return name;
  }

  @Override
  @NotNull
  public String getName() {
    return name;
  }
}
