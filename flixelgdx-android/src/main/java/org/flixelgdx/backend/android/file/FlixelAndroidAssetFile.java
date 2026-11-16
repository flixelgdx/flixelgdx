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

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.jvm.file.FlixelJvmFile;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import android.content.res.AssetManager;

/**
 * An Android {@link FlixelFile} that reads an asset bundled inside the APK.
 *
 * <p>This is what {@link FlixelAndroidFiles#internal(String)} returns. Reads go to the APK's
 * {@link AssetManager} first, then fall back to the classpath, so framework-bundled resources
 * (such as default fonts and shaders) resolve the same way they do on desktop. APK assets are
 * read-only, so every write and delete returns {@code false}.
 *
 * <p>APK folders behave like normal folders: {@link #isDirectory()} reports them, and
 * {@link #list()} returns their entries through {@link AssetManager#list(String)}.
 *
 * <p>Files on disk (the local, external and absolute roots) are not handled here; they use the
 * shared {@link FlixelJvmFile}. Game code obtains every file through {@link Flixel#files} and
 * does not construct this class directly.
 */
public class FlixelAndroidAssetFile implements FlixelFile {

  @NotNull
  private final AssetManager assets;

  @NotNull
  private final String path;

  /**
   * Creates a handle to an APK asset.
   *
   * @param assets The asset manager of the APK the asset lives in.
   * @param path The asset path, relative to the APK's {@code assets/} folder.
   */
  public FlixelAndroidAssetFile(@NotNull AssetManager assets, @NotNull String path) {
    this.assets = assets;
    this.path = path;
  }

  @NotNull
  @Override
  public String getPath() {
    return path;
  }

  @NotNull
  @Override
  public String getAbsolutePath() {
    return path;
  }

  @NotNull
  @Override
  public String getName() {
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  @Override
  public boolean exists() {
    if (fileExists()) {
      return true;
    }
    String dir = dirPath();
    if (dir.isEmpty()) {
      return true;
    }
    String[] children = children(dir);
    if (children != null && children.length > 0) {
      return true;
    }
    // Check the classpath for framework-bundled resources.
    try (InputStream in = FlixelJvmFile.openResource(path)) {
      return in != null;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean isDirectory() {
    String dir = dirPath();
    if (dir.isEmpty()) {
      return true;
    }
    if (fileExists()) {
      return false;
    }
    String[] children = children(dir);
    return children != null && children.length > 0;
  }

  @NotNull
  @Override
  public FlixelFile[] list() {
    return listChildren(null);
  }

  @NotNull
  @Override
  public FlixelFile[] list(@NotNull String suffix) {
    return listChildren(suffix);
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
    try (InputStream in = openStream()) {
      if (in == null) {
        return new byte[0];
      }
      return FlixelJvmFile.readAll(in);
    } catch (IOException e) {
      return new byte[0];
    }
  }

  @Override
  public long length() {
    return readBytes().length;
  }

  @Override
  public boolean writeString(@NotNull String content) {
    return false;
  }

  @Override
  public boolean writeBytes(byte @NotNull [] content) {
    return false;
  }

  @Override
  public boolean delete() {
    return false;
  }

  @Nullable
  @Override
  public Object getNativeHandle() {
    return null;
  }

  @NotNull
  private FlixelFile[] listChildren(@Nullable String suffix) {
    String[] children = children(dirPath());
    if (children == null || children.length == 0) {
      return new FlixelFile[0];
    }
    int matches = 0;
    for (String child : children) {
      if (suffix == null || child.endsWith(suffix)) {
        matches++;
      }
    }
    FlixelFile[] out = new FlixelFile[matches];
    int idx = 0;
    for (String child : children) {
      if (suffix == null || child.endsWith(suffix)) {
        out[idx++] = new FlixelAndroidAssetFile(assets, childPath(child));
      }
    }
    return out;
  }

  @Nullable
  private InputStream openStream() {
    try {
      return assets.open(path);
    } catch (IOException e) {
      // Not in the APK, so fall through to the classpath.
      return FlixelJvmFile.openResource(path);
    }
  }

  private boolean fileExists() {
    try (InputStream in = assets.open(path)) {
      return in != null;
    } catch (IOException e) {
      return false;
    }
  }

  @Nullable
  private String[] children(@NotNull String dir) {
    try {
      return assets.list(dir);
    } catch (IOException e) {
      return null;
    }
  }

  @NotNull
  private String dirPath() {
    int start = 0;
    int end = path.length();
    while (start < end && path.charAt(start) == '/') {
      start++;
    }
    while (end > start && path.charAt(end - 1) == '/') {
      end--;
    }
    return path.substring(start, end);
  }

  @NotNull
  private String childPath(@NotNull String childName) {
    if (path.isEmpty()) {
      return childName;
    }
    return path.endsWith("/") ? path + childName : path + "/" + childName;
  }

  @NotNull
  public AssetManager getAssets() {
    return assets;
  }
}
