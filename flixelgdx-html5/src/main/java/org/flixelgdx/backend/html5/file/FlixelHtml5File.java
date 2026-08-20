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
import org.teavm.jso.JSBody;

/**
 * A single file handle on the web, backed either by a fetched asset or by browser storage.
 *
 * <p>The browser has no real file system, so a "file" on the web is one of two very different
 * things depending on the root it came from, captured here by {@link Kind}:
 *
 * <ul>
 *   <li>{@link Kind#ASSET} - a read-only resource downloaded from the server, such as a bundled
 *       image or sound. Reads use a synchronous request because the framework's file API is
 *       synchronous; writes are impossible and return {@code false}.</li>
 *   <li>{@link Kind#STORAGE} - a read/write entry in the browser's {@code localStorage}, used for
 *       save data. It persists between sessions on the same origin but is not a real file, so it
 *       has no meaningful directory listing.</li>
 * </ul>
 *
 * <p>Reads for assets use a blocking request. Synchronous requests are discouraged for general web
 * pages because they stall the tab, but a game's asset loader expects a value back immediately and
 * runs during loading screens rather than in the render loop, which is the accepted place to use
 * them.
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
    return kind == Kind.ASSET ? assetExists(resolved) : storageGet(resolved) != null;
  }

  @Override
  public boolean isDirectory() {
    return false;
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
    String binary = kind == Kind.STORAGE ? storageGet(resolved) : assetBinary(resolved);
    if (binary == null) {
      return new byte[0];
    }
    byte[] out = new byte[binary.length()];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) (binary.charAt(i) & 0xFF);
    }
    return out;
  }

  @Override
  public long length() {
    if (kind == Kind.STORAGE) {
      String value = storageGet(resolved);
      return value != null ? value.length() : 0L;
    }
    String binary = assetBinary(resolved);
    return binary != null ? binary.length() : 0L;
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

  @JSBody(params = "url",
      script = "var x = new XMLHttpRequest(); x.open('HEAD', url, false);"
          + "try { x.send(); } catch (e) { return false; }"
          + "return x.status >= 200 && x.status < 300;")
  private static native boolean assetExists(String url);

  @JSBody(params = "url",
      script = "var x = new XMLHttpRequest(); x.open('GET', url, false);"
          + "try { x.send(); } catch (e) { return null; }"
          + "return (x.status >= 200 && x.status < 300) ? x.responseText : null;")
  private static native String assetText(String url);

  @JSBody(params = "url",
      script = "var x = new XMLHttpRequest(); x.open('GET', url, false);"
          + "x.overrideMimeType('text/plain; charset=x-user-defined');"
          + "try { x.send(); } catch (e) { return null; }"
          + "return (x.status >= 200 && x.status < 300) ? x.responseText : null;")
  private static native String assetBinary(String url);

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
