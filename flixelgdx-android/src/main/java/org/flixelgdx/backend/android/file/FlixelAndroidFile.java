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

import android.content.res.AssetManager;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Android {@link FlixelFile} implementation that reads APK assets and disk files.
 *
 * <p>Instances are created by {@link FlixelAndroidFiles}; game code obtains them through
 * {@link org.flixelgdx.Flixel#files} and never constructs them directly.
 *
 * <p>Internal files are read first from the APK {@link AssetManager}, then from the classpath
 * (for framework-bundled resources such as default fonts and shaders). Disk-backed files use
 * standard Java IO available at API 24.
 */
public class FlixelAndroidFile implements FlixelFile {

  /** Asset manager used for APK asset reads, or {@code null} for disk-only handles. */
  @Nullable
  private final AssetManager assets;

  @NotNull
  private final String path;

  /** Backing file on disk, or {@code null} for APK/classpath handles. */
  @Nullable
  private final File file;

  private final boolean classpath;
  private final boolean apk;

  FlixelAndroidFile(@Nullable AssetManager assets, @NotNull String path,
      @Nullable File file, boolean classpath, boolean apk) {
    this.assets = assets;
    this.path = path;
    this.file = file;
    this.classpath = classpath;
    this.apk = apk;
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
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  @Override
  public boolean exists() {
    if (apk && assets != null) {
      return apkExists();
    }
    if (classpath) {
      return classpathStream() != null;
    }
    return file != null && file.exists();
  }

  @Override
  public boolean isDirectory() {
    if (apk || classpath) {
      return false;
    }
    return file != null && file.isDirectory();
  }

  @NotNull
  @Override
  public FlixelFile[] list() {
    if (apk || classpath || file == null || !file.isDirectory()) {
      return new FlixelFile[0];
    }
    File[] children = file.listFiles();
    if (children == null) {
      return new FlixelFile[0];
    }
    FlixelFile[] out = new FlixelFile[children.length];
    for (int i = 0; i < children.length; i++) {
      String childPath = childPath(children[i].getName());
      out[i] = new FlixelAndroidFile(null, childPath, children[i], false, false);
    }
    return out;
  }

  @NotNull
  @Override
  public FlixelFile[] list(@NotNull String suffix) {
    if (apk || classpath || file == null || !file.isDirectory()) {
      return new FlixelFile[0];
    }
    File[] children = file.listFiles();
    if (children == null) {
      return new FlixelFile[0];
    }
    int matches = 0;
    for (File child : children) {
      if (child.getName().endsWith(suffix)) {
        matches++;
      }
    }
    FlixelFile[] out = new FlixelFile[matches];
    int idx = 0;
    for (File child : children) {
      if (child.getName().endsWith(suffix)) {
        out[idx++] = new FlixelAndroidFile(null, childPath(child.getName()), child, false, false);
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
      InputStream in = openStream();
      try (in) {
        if (in == null) {
          return new byte[0];
        }
        return readAll(in);
      }
    } catch (IOException e) {
      return new byte[0];
    }
  }

  @Override
  public long length() {
    if (apk || classpath) {
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
    if (apk || classpath || file == null) {
      return false;
    }
    try {
      File parent = file.getParentFile();
      if (parent != null) {
        parent.mkdirs();
      }
      try (FileOutputStream out = new FileOutputStream(file)) {
        out.write(content);
      }
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean delete() {
    return !apk && !classpath && file != null && file.delete();
  }

  @Nullable
  @Override
  public Object getNativeHandle() {
    return file;
  }

  /**
   * Opens a stream for this file, trying APK assets first, then classpath, then disk.
   *
   * @return An open {@link InputStream}, or {@code null} if the file cannot be found.
   * @throws IOException if the file exists but cannot be opened.
   */
  @Nullable
  private InputStream openStream() throws IOException {
    if (apk && assets != null) {
      try {
        return assets.open(path);
      } catch (IOException e) {
        // Not in APK - fall through to classpath.
      }
      return classpathStream();
    }
    if (classpath) {
      return classpathStream();
    }
    if (file != null && file.exists() && !file.isDirectory()) {
      return new FileInputStream(file);
    }
    return null;
  }

  private boolean apkExists() {
    if (assets == null) {
      return false;
    }
    try {
      InputStream in = assets.open(path);
      in.close();
      return true;
    } catch (IOException e) {
      // Fall through to classpath check.
    }
    // Check classpath for framework-bundled resources.
    return classpathStream() != null;
  }

  @Nullable
  private InputStream classpathStream() {
    String resource = path.startsWith("/") ? path : "/" + path;
    InputStream in = FlixelAndroidFile.class.getResourceAsStream(resource);
    if (in != null) {
      return in;
    }
    // Also check without leading slash for some class loaders.
    String noSlash = path.startsWith("/") ? path.substring(1) : path;
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null) {
      return cl.getResourceAsStream(noSlash);
    }
    return null;
  }

  @NotNull
  private String childPath(@NotNull String childName) {
    if (path.isEmpty()) {
      return childName;
    }
    return path.endsWith("/") ? path + childName : path + "/" + childName;
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
