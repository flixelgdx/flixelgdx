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
package org.flixelgdx.backend.jvm.file;

import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A desktop {@link FlixelFile}. Files rooted on disk are read and written through {@code java.io};
 * classpath files are read through the class loader (and are read-only).
 *
 * <p>Instances are created by {@link FlixelJvmFiles}; game code obtains them through
 * {@link org.flixelgdx.Flixel#files Flixel.files} and never constructs them directly.
 */
public final class FlixelJvmFile implements FlixelFile {

  @NotNull
  private final String path;

  /** The backing file on disk, or {@code null} for a classpath resource. */
  @Nullable
  private final File file;

  private final boolean classpath;

  FlixelJvmFile(@NotNull String path, @Nullable File file, boolean classpath) {
    this.path = path;
    this.file = file;
    this.classpath = classpath;
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  @NotNull
  @Override
  public String getName() {
    int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  @Override
  public boolean exists() {
    if (classpath) {
      return openClasspath() != null;
    }
    return file != null && file.exists();
  }

  @Override
  public boolean isDirectory() {
    return !classpath && file != null && file.isDirectory();
  }

  @NotNull
  @Override
  public FlixelFile[] list() {
    File[] children = childFiles();
    if (children == null) {
      return new FlixelFile[0];
    }
    FlixelFile[] out = new FlixelFile[children.length];
    for (int i = 0; i < children.length; i++) {
      out[i] = new FlixelJvmFile(childPath(children[i].getName()), children[i], false);
    }
    return out;
  }

  @NotNull
  @Override
  public FlixelFile[] list(@NotNull String suffix) {
    File[] children = childFiles();
    if (children == null) {
      return new FlixelFile[0];
    }
    int matches = 0;
    for (int i = 0; i < children.length; i++) {
      if (children[i].getName().endsWith(suffix)) {
        matches++;
      }
    }
    FlixelFile[] out = new FlixelFile[matches];
    int idx = 0;
    for (int i = 0; i < children.length; i++) {
      File child = children[i];
      if (child.getName().endsWith(suffix)) {
        out[idx++] = new FlixelJvmFile(childPath(child.getName()), child, false);
      }
    }
    return out;
  }

  @NotNull
  @Override
  public String readString() {
    return readString("UTF-8");
  }

  @NotNull
  @Override
  public String readString(@Nullable String charset) {
    Charset cs = charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8;
    return new String(readBytes(), cs);
  }

  @NotNull
  @Override
  public byte[] readBytes() {
    try {
      if (classpath) {
        try (InputStream in = openClasspath()) {
          if (in == null) {
            return new byte[0];
          }
          return readAll(in);
        }
      }
      if (file == null || !file.exists() || file.isDirectory()) {
        return new byte[0];
      }
      return Files.readAllBytes(file.toPath());
    } catch (IOException e) {
      return new byte[0];
    }
  }

  @Override
  public long length() {
    if (classpath) {
      return readBytes().length;
    }
    return file != null && file.exists() ? file.length() : 0L;
  }

  @Override
  public boolean writeString(@NotNull String content) {
    return writeBytes(content.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public boolean writeBytes(byte @NotNull [] content) {
    if (classpath || file == null) {
      return false;
    }
    try {
      File parent = file.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      Files.write(file.toPath(), content);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean delete() {
    return !classpath && file != null && file.delete();
  }

  @Nullable
  @Override
  public Object getNativeHandle() {
    return file;
  }

  /**
   * Returns the on-disk children of this handle, or {@code null} when it is not a listable
   * directory. Classpath handles cannot be walked, matching the seam's documented behavior.
   */
  @Nullable
  private File[] childFiles() {
    if (classpath || file == null || !file.isDirectory()) {
      return null;
    }
    return file.listFiles();
  }

  @NotNull
  private String childPath(@NotNull String childName) {
    if (path.isEmpty()) {
      return childName;
    }
    return path.endsWith("/") ? path + childName : path + "/" + childName;
  }

  @Nullable
  private InputStream openClasspath() {
    String resource = path.startsWith("/") ? path : "/" + path;
    return FlixelJvmFile.class.getResourceAsStream(resource);
  }

  private static byte @NotNull [] readAll(@NotNull InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
    byte[] chunk = new byte[8192];
    int read;
    while ((read = in.read(chunk)) > 0) {
      out.write(chunk, 0, read);
    }
    return out.toByteArray();
  }
}
