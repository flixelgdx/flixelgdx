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

import org.flixelgdx.backend.html5.FlixelHtml5AssetPreloader;
import org.flixelgdx.file.FlixelFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * A single file handle on the web, backed either by a preloaded asset or by browser storage.
 *
 * <p>The browser has no real file system, so a "file" on the web is one of two very different
 * things depending on the root it came from, captured here by {@link Kind}:
 *
 * <ul>
 *   <li>{@link Kind#ASSET} - a read-only resource bundled with the game. Because the browser cannot
 *       read files synchronously, non-image assets are downloaded up front by
 *       {@link FlixelHtml5AssetPreloader} into an in-memory cache; a read here is then just an
 *       instant lookup in that cache. Image assets are not preloaded and must be requested through
 *       the asset manager ({@code Flixel.assets.load()}) instead. Writes are impossible and return
 *       {@code false}.</li>
 *   <li>{@link Kind#STORAGE} - a read/write entry in the browser's {@code localStorage}, used for
 *       save data. It persists between sessions on the same origin but is not a real file, so it
 *       has no meaningful directory listing.</li>
 * </ul>
 */
public class FlixelHtml5File implements FlixelFile {

  @NotNull
  private final String path;

  @NotNull
  private final String resolved;

  @NotNull
  private final Kind kind;

  /**
   * Creates a file handle.
   *
   * @param path The logical path the game asked for, kept for {@link #getPath()} and {@link #getName()}.
   * @param resolved The concrete locator: a URL for assets, or a storage key for storage entries.
   * @param kind Whether this handle is a fetched asset or a storage entry.
   */
  public FlixelHtml5File(@NotNull String path, @NotNull String resolved, @NotNull Kind kind) {
    this.path = path;
    this.resolved = resolved;
    this.kind = kind;
  }

  @Override
  public boolean exists() {
    if (kind == Kind.STORAGE) {
      return storageGet(resolved) != null;
    }
    return assetCached(resolved) || assetHasPrefix(resolved);
  }

  @Override
  @NotNull
  public String readString() {
    return readString(null);
  }

  @Override
  @NotNull
  public String readString(@Nullable String charset) {
    if (kind == Kind.STORAGE) {
      String value = storageGet(resolved);
      return value != null ? value : "";
    }
    String text = assetText(resolved);
    return text != null ? text : "";
  }

  @Override
  public byte @NotNull [] readBytes() {
    if (kind == Kind.STORAGE) {
      String value = storageGet(resolved);
      if (value == null) {
        return new byte[0];
      }
      byte[] out = new byte[value.length()];
      for (int i = 0; i < out.length; i++) {
        out[i] = (byte) (value.charAt(i) & 0xFF);
      }
      return out;
    }
    Int8Array bytes = assetBytes(resolved);
    return bytes != null ? bytes.copyToJavaArray() : new byte[0];
  }

  @Override
  public long length() {
    if (kind == Kind.STORAGE) {
      String value = storageGet(resolved);
      return value != null ? value.length() : 0L;
    }
    return assetLength(resolved);
  }

  @Override
  public boolean writeString(@NotNull String content) {
    if (kind != Kind.STORAGE) {
      return false;
    }
    storageSet(resolved, content);
    return true;
  }

  @Override
  public boolean writeBytes(byte @NotNull [] content) {
    if (kind != Kind.STORAGE) {
      return false;
    }
    char[] chars = new char[content.length];
    for (int i = 0; i < content.length; i++) {
      chars[i] = (char) (content[i] & 0xFF);
    }
    storageSet(resolved, new String(chars));
    return true;
  }

  @Override
  public boolean delete() {
    if (kind != Kind.STORAGE) {
      return false;
    }
    storageRemove(resolved);
    return true;
  }

  @Override
  @NotNull
  public String getPath() {
    return path;
  }

  @Override
  @NotNull
  public String getName() {
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash + 1) : path;
  }

  @Override
  public boolean isDirectory() {
    return kind == Kind.ASSET && assetHasPrefix(resolved);
  }

  @Override
  @NotNull
  public FlixelFile[] list() {
    if (kind != Kind.ASSET) {
      return new FlixelFile[0];
    }
    String raw = assetListChildNames(resolved);
    if (raw.isEmpty()) {
      return new FlixelFile[0];
    }
    String[] names = raw.split("\n");
    FlixelFile[] children = new FlixelFile[names.length];
    for (int i = 0; i < names.length; i++) {
      String childPath = path + "/" + names[i];
      String childResolved = resolved + "/" + names[i];
      children[i] = new FlixelHtml5File(childPath, childResolved, Kind.ASSET);
    }
    return children;
  }

  @Override
  @NotNull
  public FlixelFile[] list(@NotNull String suffix) {
    FlixelFile[] all = list();
    int count = 0;
    for (int i = 0; i < all.length; i++) {
      if (all[i].getName().endsWith(suffix)) {
        count++;
      }
    }
    if (count == all.length) {
      return all;
    }
    FlixelFile[] out = new FlixelFile[count];
    int idx = 0;
    for (int i = 0; i < all.length; i++) {
      if (all[i].getName().endsWith(suffix)) {
        out[idx++] = all[i];
      }
    }
    return out;
  }

  @JSBody(params = "key", script = """
      return !!(window.__flixelAssetPaths && window.__flixelAssetPaths[key])
          || !!(window.__flixelAssets && window.__flixelAssets[key]);
      """)
  private static native boolean assetCached(String key);

  @JSBody(params = "key", script = """
      var a = window.__flixelAssets && window.__flixelAssets[key];
      return a ? new TextDecoder('utf-8').decode(a) : null;
      """)
  private static native String assetText(String key);

  @JSBody(params = "key", script = """
      var a = window.__flixelAssets && window.__flixelAssets[key];
      return a ? new Int8Array(a.buffer, a.byteOffset, a.byteLength) : null;
      """)
  private static native Int8Array assetBytes(String key);

  @JSBody(params = "key", script = """
      var a = window.__flixelAssets && window.__flixelAssets[key];
      return a ? a.byteLength : 0;
      """)
  private static native int assetLength(String key);

  @JSBody(params = "prefix", script = """
      if (!window.__flixelAssetPaths) { return false; }
      var p = prefix + '/';
      var keys = Object.keys(window.__flixelAssetPaths);
      for (var i = 0; i < keys.length; i++) {
        if (keys[i].startsWith(p)) { return true; }
      }
      return false;
      """)
  private static native boolean assetHasPrefix(String prefix);

  @JSBody(params = "prefix", script = """
      if (!window.__flixelAssetPaths) { return ''; }
      var p = prefix + '/';
      var seen = {};
      var names = [];
      var keys = Object.keys(window.__flixelAssetPaths);
      for (var i = 0; i < keys.length; i++) {
        var k = keys[i];
        if (!k.startsWith(p)) { continue; }
        var rest = k.substring(p.length);
        var slash = rest.indexOf('/');
        var child = slash >= 0 ? rest.substring(0, slash) : rest;
        if (child && !seen[child]) { seen[child] = true; names.push(child); }
      }
      return names.join('\\n');
      """)
  private static native String assetListChildNames(String prefix);

  @JSBody(params = "key", script = "return window.localStorage.getItem(key);")
  private static native String storageGet(String key);

  @JSBody(params = { "key", "value" }, script = "window.localStorage.setItem(key, value);")
  private static native void storageSet(String key, String value);

  @JSBody(params = "key", script = "window.localStorage.removeItem(key);")
  private static native void storageRemove(String key);

  /** Which browser mechanism backs a given handle. */
  public enum Kind {

    /** A read-only resource downloaded from the server. */
    ASSET,

    /** A read/write entry in the browser's {@code localStorage}. */
    STORAGE
  }
}
