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
package org.flixelgdx.backend.android.file;

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * A read-only {@link FlixelFile} backed by a {@code content://} URI from the system file picker.
 *
 * <p>Android does not give apps real paths for files the user picks, only a URI that must be read
 * through a {@link ContentResolver}. This class hides that detail: {@link #readBytes()} and
 * {@link #readString()} open the URI on demand. Writes and deletes are not supported and return
 * {@code false}.
 */
public class FlixelAndroidUriFile implements FlixelFile {

  @NotNull
  private final Uri uri;

  @NotNull
  private final ContentResolver resolver;

  @NotNull
  private final String name;

  private final long size;

  /**
   * Creates a file handle for a picked URI.
   *
   * @param resolver The resolver used to open the URI.
   * @param uri The {@code content://} URI of the file.
   * @param name The display name reported by the provider.
   * @param size The size in bytes, or {@code -1} if the provider did not report one.
   */
  public FlixelAndroidUriFile(@NotNull ContentResolver resolver, @NotNull Uri uri, @NotNull String name, long size) {
    this.resolver = resolver;
    this.uri = uri;
    this.name = name;
    this.size = size;
  }

  @Override
  public boolean exists() {
    try (InputStream in = resolver.openInputStream(uri)) {
      return in != null;
    } catch (IOException | RuntimeException e) {
      return false;
    }
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
      } catch (IllegalArgumentException ignored) {
        // Unknown charset names fall back to UTF-8.
      }
    }
    return new String(readBytes(), cs);
  }

  @Override
  public byte @NotNull [] readBytes() {
    try (InputStream in = resolver.openInputStream(uri)) {
      if (in == null) {
        return new byte[0];
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream(size > 0L ? (int) Math.min(size, Integer.MAX_VALUE - 8) : 8192);
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    } catch (IOException | RuntimeException e) {
      return new byte[0];
    }
  }

  @Override
  public long length() {
    return size >= 0L ? size : readBytes().length;
  }

  @Override
  @NotNull
  public String getPath() {
    return name;
  }

  @Override
  @NotNull
  public String getAbsolutePath() {
    return uri.toString();
  }

  @Override
  @NotNull
  public String getName() {
    return name;
  }

  @Override
  @NotNull
  public Object getNativeHandle() {
    return uri;
  }
}
