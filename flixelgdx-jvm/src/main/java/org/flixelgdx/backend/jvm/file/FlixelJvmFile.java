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

import org.flixelgdx.Flixel;
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
 * A {@link FlixelFile} backed by a real file on disk or by a resource on the Java classpath.
 *
 * <p>Files on disk are read and written through {@code java.io}. Classpath files are read through
 * the class loader and are read-only, because a resource inside a JAR or APK cannot be changed
 * while the game runs.
 *
 * <p>Every JVM backend (desktop and Android) uses this class for its classpath, local, external
 * and absolute roots, so those roots behave the same on both. Instances are normally created by a
 * {@link FlixelJvmFiles} subclass; game code obtains them through {@link Flixel#files} and does
 * not construct them directly.
 */
public class FlixelJvmFile implements FlixelFile {

  @NotNull
  private final String path;

  /** The backing file on disk, or {@code null} for a classpath resource. */
  @Nullable
  private final File file;

  private final boolean classpath;

  /**
   * Creates a file handle.
   *
   * @param path The path the handle was requested with, relative to its root.
   * @param file The backing file on disk, or {@code null} for a classpath resource.
   * @param classpath {@code true} when the handle reads from the classpath instead of disk.
   */
  public FlixelJvmFile(@NotNull String path, @Nullable File file, boolean classpath) {
    this.path = path;
    this.file = file;
    this.classpath = classpath;
  }

  /**
   * Opens a resource on the Java classpath.
   *
   * <p>The lookup first goes through this class, then through the current thread's context class
   * loader, because some platforms (such as Android) only see app resources through the latter.
   *
   * @param path The resource path, with or without a leading slash.
   * @return A stream over the resource, or {@code null} if no resource has that path. The caller
   *     must close it.
   */
  @Nullable
  public static InputStream openResource(@NotNull String path) {
    String resource = path.startsWith("/") ? path : "/" + path;
    InputStream in = FlixelJvmFile.class.getResourceAsStream(resource);
    if (in != null) {
      return in;
    }
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return loader != null ? loader.getResourceAsStream(resource.substring(1)) : null;
  }

  /**
   * Reads every remaining byte from a stream.
   *
   * <p>The stream is not closed, so the caller stays in charge of it.
   *
   * @param in The stream to read.
   * @return The bytes that were read.
   * @throws IOException If the stream fails while reading.
   */
  public static byte @NotNull [] readAll(@NotNull InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(32, in.available()));
    byte[] chunk = new byte[8192];
    int read;
    while ((read = in.read(chunk)) > 0) {
      out.write(chunk, 0, read);
    }
    return out.toByteArray();
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  @NotNull
  @Override
  public String getAbsolutePath() {
    return file != null ? file.getAbsolutePath() : path;
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
      try (InputStream in = openResource(path)) {
        return in != null;
      } catch (IOException e) {
        return false;
      }
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
    for (File value : children) {
      if (value.getName().endsWith(suffix)) {
        matches++;
      }
    }
    FlixelFile[] out = new FlixelFile[matches];
    int idx = 0;
    for (File child : children) {
      if (child.getName().endsWith(suffix)) {
        out[idx++] = new FlixelJvmFile(childPath(child.getName()), child, false);
      }
    }
    return out;
  }

  @NotNull
  @Override
  public String readString() {
    return new String(readBytes(), StandardCharsets.UTF_8);
  }

  @NotNull
  @Override
  public String readString(@Nullable String charset) {
    Charset cs = charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8;
    return new String(readBytes(), cs);
  }

  @Override
  public byte @NotNull [] readBytes() {
    try {
      if (classpath) {
        try (InputStream in = openResource(path)) {
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
  public File getFile() {
    return file;
  }

  public boolean isClasspath() {
    return classpath;
  }
}
