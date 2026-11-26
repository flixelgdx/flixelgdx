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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.file.FlixelFile;
import org.flixelgdx.file.FlixelFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * A file system for unit tests that keeps every written file in memory instead of on disk.
 *
 * <p>Tests should not depend on a platform backend or leave files behind, so this fake stands in
 * for the real one on {@code Flixel.files}. It splits the roots the same way a real backend does:
 *
 * <ul>
 *   <li>{@link #internal(String)} and {@link #classpath(String)} are read-only and read from the
 *       test classpath, so a test can load a fixture placed under {@code src/test/resources}.</li>
 *   <li>{@link #external(String)}, {@link #local(String)} and {@link #absolute(String)} are writable
 *       and live only in memory. Each root has its own namespace, so {@code external("a.txt")} and
 *       {@code local("a.txt")} are different files.</li>
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelMemoryFiles files = new FlixelMemoryFiles();
 * files.external("saves/slot1.json").writeString("{}");
 * assertTrue(files.external("saves/slot1.json").exists());
 * assertEquals(1, files.external("saves").list().length);
 * }</pre>
 */
public final class FlixelMemoryFiles implements FlixelFiles {

  private final FlixelMap<String, byte[]> entries = new FlixelMap<>();

  @NotNull
  @Override
  public FlixelFile internal(@NotNull String path) {
    return new ResourceFile(path);
  }

  @NotNull
  @Override
  public FlixelFile classpath(@NotNull String path) {
    return new ResourceFile(path);
  }

  @NotNull
  @Override
  public FlixelFile external(@NotNull String path) {
    return new MemoryFile("external", path);
  }

  @NotNull
  @Override
  public FlixelFile local(@NotNull String path) {
    return new MemoryFile("local", path);
  }

  @NotNull
  @Override
  public FlixelFile absolute(@NotNull String path) {
    return new MemoryFile("absolute", path);
  }

  /** Removes every file written so far, so the next test starts with an empty file system. */
  public void clear() {
    entries.clear();
  }

  private static String normalize(@NotNull String path) {
    String p = path.replace('\\', '/');
    while (p.endsWith("/")) {
      p = p.substring(0, p.length() - 1);
    }
    return p;
  }

  private static String nameOf(@NotNull String path) {
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private static String decode(byte @NotNull [] bytes, @Nullable String charset) {
    if (charset == null) {
      return new String(bytes, Charset.defaultCharset());
    }
    try {
      return new String(bytes, charset);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unknown charset: " + charset, e);
    }
  }

  /** A writable file whose contents live in {@link #entries}. */
  private final class MemoryFile implements FlixelFile {

    private final String root;
    private final String path;
    private final String key;

    private MemoryFile(@NotNull String root, @NotNull String path) {
      this.root = root;
      this.path = normalize(path);
      this.key = root + ':' + this.path;
    }

    @NotNull
    @Override
    public String getPath() {
      return path;
    }

    @NotNull
    @Override
    public String getName() {
      return nameOf(path);
    }

    @Override
    public boolean exists() {
      return entries.containsKey(key) || isDirectory();
    }

    @Override
    public boolean isDirectory() {
      String prefix = key + '/';
      for (String k : entries.keys()) {
        if (k.startsWith(prefix)) {
          return true;
        }
      }
      return false;
    }

    @NotNull
    @Override
    public FlixelFile[] list() {
      return list("");
    }

    @NotNull
    @Override
    public FlixelFile[] list(@NotNull String suffix) {
      String prefix = key + '/';
      FlixelArray<String> names = new FlixelArray<>(String[]::new);
      for (String k : entries.keys()) {
        if (!k.startsWith(prefix)) {
          continue;
        }
        // Only direct children count, so a nested file adds its first folder instead of itself.
        String rest = k.substring(prefix.length());
        int slash = rest.indexOf('/');
        String child = slash < 0 ? rest : rest.substring(0, slash);
        if (child.endsWith(suffix) && !names.contains(child, false)) {
          names.add(child);
        }
      }
      FlixelFile[] out = new FlixelFile[names.getSize()];
      for (int i = 0; i < out.length; i++) {
        out[i] = new MemoryFile(root, path.isEmpty() ? names.get(i) : path + '/' + names.get(i));
      }
      return out;
    }

    @NotNull
    @Override
    public String readString() {
      return readString(null);
    }

    @NotNull
    @Override
    public String readString(@Nullable String charset) {
      return decode(readBytes(), charset);
    }

    @Override
    public byte @NotNull [] readBytes() {
      byte[] data = entries.get(key);
      return data == null ? new byte[0] : data.clone();
    }

    @Override
    public long length() {
      byte[] data = entries.get(key);
      return data == null ? 0L : data.length;
    }

    @Override
    public boolean writeString(@NotNull String content) {
      return writeBytes(content.getBytes(Charset.defaultCharset()));
    }

    @Override
    public boolean writeBytes(byte @NotNull [] content) {
      entries.put(key, content.clone());
      return true;
    }

    @Override
    public boolean delete() {
      return entries.remove(key) != null;
    }
  }

  /** A read-only file loaded from the test classpath. */
  private static final class ResourceFile implements FlixelFile {

    private final String path;

    private ResourceFile(@NotNull String path) {
      this.path = normalize(path);
    }

    @NotNull
    @Override
    public String getPath() {
      return path;
    }

    @NotNull
    @Override
    public String getName() {
      return nameOf(path);
    }

    @Override
    public boolean exists() {
      return FlixelMemoryFiles.class.getResource('/' + path) != null;
    }

    @NotNull
    @Override
    public String readString() {
      return readString(null);
    }

    @NotNull
    @Override
    public String readString(@Nullable String charset) {
      return decode(readBytes(), charset);
    }

    @Override
    public byte @NotNull [] readBytes() {
      try (InputStream in = FlixelMemoryFiles.class.getResourceAsStream('/' + path)) {
        return in == null ? new byte[0] : in.readAllBytes();
      } catch (IOException e) {
        throw new IllegalStateException("Could not read test resource: " + path, e);
      }
    }

    @Override
    public long length() {
      return readBytes().length;
    }
  }
}
