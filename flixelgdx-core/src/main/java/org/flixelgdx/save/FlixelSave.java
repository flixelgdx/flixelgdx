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
package org.flixelgdx.save;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.functional.FlixelDestroyable;
import org.flixelgdx.json.FlixelJson;
import org.flixelgdx.json.FlixelJsonValue;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A bound, named key-value save store that persists between sessions.
 *
 * <p>Bind to a uniquely identified save slot, manipulate the structured {@link #data} map, and
 * flush changes to storage. Saves serialize to JSON text and hand it to a
 * {@link FlixelSaveHandler}, which decides where the text actually lives. Every platform installs
 * its own handler on {@link Flixel#saveHandler}, so the same code works everywhere: desktop saves
 * to the computer's app data folder, Android saves to the app's private storage, and the web saves
 * to the browser's {@code localStorage}.
 *
 * <p>Supported value types are {@link String}, {@link Number}, {@link Boolean}, nested
 * {@link FlixelMap}{@code <String, Object>} objects, and {@link FlixelArray}{@code <Object>}
 * lists. Anything else is stored via {@code toString()}.
 *
 * <p>Browsers and phones rarely let a game shut down cleanly; they simply stop it in the
 * background. Flush important data as soon as it changes, or listen to
 * {@link Flixel.Signals#gameSuspended}, instead of relying on {@link Flixel.Signals#preGameClose}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.save.bind("MyGame", "slot1");
 * int best = Flixel.save.getInt("highScore", 0);
 * if (score > best) {
 *   Flixel.save.data.put("highScore", score);
 *   Flixel.save.flush();
 * }
 * }</pre>
 */
public class FlixelSave implements FlixelDestroyable {

  private static final FlixelLogger LOG = Flixel.log.tagged("Save");

  /**
   * Root data object. Read and write entries directly, then call {@link #flush()} to persist.
   */
  @NotNull
  public final FlixelMap<String, Object> data = new FlixelMap<>();

  @NotNull
  private String boundName = "";

  @Nullable
  private String boundSlot;

  /** The storage this save reads from and writes to. Set by the {@code bind(...)} methods. */
  @NotNull
  private FlixelSaveHandler handler = FlixelNoopSaveHandler.INSTANCE;

  @NotNull
  private FlixelSaveStatus status = FlixelSaveStatus.EMPTY;

  private boolean bound;

  /**
   * Binds this save object to a named save (and optional slot) in the platform's default storage,
   * then loads any existing data.
   *
   * <p>The platform's {@link Flixel#saveHandler} decides where the save lives. On desktop, save
   * files are written to the OS-specific application data directory, determined by the company
   * name and game title set in {@link FlixelConfig}:
   * <ul>
   *   <li><b>Windows</b>: {@code %APPDATA%\Company\Title\saves\}</li>
   *   <li><b>macOS</b>: {@code ~/Library/Application Support/Company/Title/saves/}</li>
   *   <li><b>Linux</b>: {@code $XDG_DATA_HOME/Company/Title/saves/}</li>
   * </ul>
   *
   * <p>A company name <b>must</b> be set via {@link FlixelConfig.Builder#company(String)}
   * before calling this method. If it is missing, this method logs an error and returns
   * {@code false}. Use {@link #bind(String, String, FlixelSaveHandler)} to supply your own storage
   * as an alternative.
   *
   * @param name The save name, typically your game's name or the player's profile identifier.
   * @param slot An optional slot discriminator (for example {@code "slot1"}), or {@code null}.
   * @return {@code true} when the bind succeeded.
   */
  public boolean bind(@NotNull String name, @Nullable String slot) {
    if (name.isEmpty()) {
      return false;
    }
    FlixelConfig config = Flixel.config;
    if (config.getCompany().isEmpty()) {
      LOG.error("bind() requires a company name to resolve the correct save location. "
          + "Set it via FlixelConfig.Builder.company(...) in your FlixelGame subclass constructor, "
          + "or use bind(name, slot, handler) to supply your own save storage instead.");
      return false;
    }
    return bindInternal(name, slot, Flixel.saveHandler);
  }

  /**
   * Binds this save object to a named save (and optional slot) kept by a specific handler, then
   * loads any existing data.
   *
   * <p>Use this when saves should live somewhere other than the platform's default storage, for
   * example a cloud-sync service, a custom folder, or an in-memory store in tests. No company name
   * check is performed, since the handler decides where the data goes.
   *
   * @param name The save name, typically your game's name or the player's profile identifier.
   * @param slot An optional slot discriminator (for example {@code "slot1"}), or {@code null}.
   * @param handler The storage to read from and write to. Must not be {@code null}.
   * @return {@code true} when the bind succeeded.
   */
  public boolean bind(@NotNull String name, @Nullable String slot, @NotNull FlixelSaveHandler handler) {
    if (name.isEmpty()) {
      return false;
    }
    return bindInternal(name, slot, Objects.requireNonNull(handler, "handler cannot be null"));
  }

  private boolean bindInternal(@NotNull String name, @Nullable String slot, @NotNull FlixelSaveHandler handler) {
    this.boundName = name;
    this.boundSlot = slot;
    this.handler = handler;
    this.bound = true;
    load();
    return true;
  }

  /**
   * Reloads {@link #data} from storage, replacing any unsaved changes.
   */
  public void load() {
    data.clear();
    if (!bound) {
      status = FlixelSaveStatus.EMPTY;
      return;
    }
    String key = getKey();
    String text = handler.read(key);
    if (text == null || text.isEmpty()) {
      status = FlixelSaveStatus.EMPTY;
      return;
    }
    try {
      FlixelJsonValue root = FlixelJson.parse(text);
      readObjectInto(root, data);
      status = data.isEmpty() ? FlixelSaveStatus.EMPTY : FlixelSaveStatus.OK;
    } catch (Exception e) {
      LOG.error("Could not parse save '{}'.", key, e);
      status = FlixelSaveStatus.ERROR;
    }
  }

  /**
   * Writes {@link #data} to storage.
   *
   * @return {@code true} when the write succeeded.
   */
  public boolean flush() {
    if (!bound) {
      return false;
    }
    FlixelString out = new FlixelString(256);
    writeValue(out, data);
    boolean ok = handler.write(getKey(), out.toString());
    if (!ok) {
      LOG.error("Could not write save '{}'.", getKey());
    }
    status = ok ? FlixelSaveStatus.OK : FlixelSaveStatus.ERROR;
    return ok;
  }

  /**
   * Clears {@link #data} and deletes the stored save.
   *
   * @return {@code true} when the save was removed (or never existed).
   */
  public boolean erase() {
    data.clear();
    if (!bound) {
      return false;
    }
    boolean ok = handler.delete(getKey());
    status = FlixelSaveStatus.EMPTY;
    return ok;
  }

  /**
   * Convenience typed read.
   *
   * @param key The entry key.
   * @param defaultValue Returned when the entry is absent or not a number.
   * @return The stored int or the default.
   */
  public int getInt(@NotNull String key, int defaultValue) {
    Object value = data.get(key);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }

  /**
   * Convenience typed read.
   *
   * @param key The entry key.
   * @param defaultValue Returned when the entry is absent or not a number.
   * @return The stored float or the default.
   */
  public float getFloat(@NotNull String key, float defaultValue) {
    Object value = data.get(key);
    return value instanceof Number number ? number.floatValue() : defaultValue;
  }

  /**
   * Convenience typed read.
   *
   * @param key The entry key.
   * @param defaultValue Returned when the entry is absent or not a boolean.
   * @return The stored boolean or the default.
   */
  public boolean getBool(@NotNull String key, boolean defaultValue) {
    Object value = data.get(key);
    return value instanceof Boolean bool ? bool : defaultValue;
  }

  /**
   * Convenience typed read.
   *
   * @param key The entry key.
   * @param defaultValue Returned when the entry is absent.
   * @return The stored string or the default.
   */
  public String getString(@NotNull String key, @Nullable String defaultValue) {
    Object value = data.get(key);
    return value instanceof String string ? string : defaultValue;
  }

  /**
   * Merges entries from another map into {@link #data}.
   *
   * @param source The entries to merge.
   * @param overwrite When {@code true}, existing keys are replaced.
   * @param flushAfter When {@code true}, {@link #flush()} runs after merging.
   * @return {@code true} when a flush was requested and succeeded, or no flush was requested.
   */
  public boolean mergeData(@NotNull FlixelMap<String, Object> source, boolean overwrite, boolean flushAfter) {
    for (FlixelMap.Entry<String, Object> e : source.entries()) {
      if (overwrite || !data.containsKey(e.key)) {
        data.put(e.key, e.value);
      }
    }
    return !flushAfter || flush();
  }

  @Override
  public void destroy() {
    data.clear();
    bound = false;
    boundName = "";
    boundSlot = null;
    handler = FlixelNoopSaveHandler.INSTANCE;
    status = FlixelSaveStatus.EMPTY;
  }

  /**
   * Returns the save name handed to the {@link FlixelSaveHandler}: the bound name, plus
   * {@code "." + slot} when a slot is set.
   *
   * @return The handler key, or an empty string when this save is not bound.
   */
  @NotNull
  public String getKey() {
    return boundSlot != null && !boundSlot.isEmpty() ? boundName + "." + boundSlot : boundName;
  }

  /** Converts a parsed JSON object node into plain map entries. */
  private static void readObjectInto(@NotNull FlixelJsonValue object, @NotNull FlixelMap<String, Object> out) {
    for (int i = 0; i < object.getSize(); i++) {
      FlixelJsonValue child = object.get(i);
      if (child != null && child.getName() != null) {
        out.put(child.getName(), toPlainValue(child));
      }
    }
  }

  /** Converts one parsed JSON node into a plain Java value. */
  @Nullable
  private static Object toPlainValue(@NotNull FlixelJsonValue value) {
    return switch (value.getKind()) {
      case STRING -> value.asString();
      case NUMBER -> value.asDouble();
      case BOOL -> value.asBool();
      case NULL -> null;
      case OBJECT -> {
        FlixelMap<String, Object> map = new FlixelMap<>();
        readObjectInto(value, map);
        yield map;
      }
      case ARRAY -> {
        FlixelArray<Object> list = new FlixelArray<>(value.getSize());
        for (int i = 0; i < value.getSize(); i++) {
          FlixelJsonValue child = value.get(i);
          list.add(child != null ? toPlainValue(child) : null);
        }
        yield list;
      }
    };
  }

  /** Serializes one supported value into JSON text. */
  private static void writeValue(@NotNull FlixelString out, @Nullable Object value) {
    if (value == null) {
      out.concat("null");
    } else if (value instanceof String string) {
      writeString(out, string);
    } else if (value instanceof Boolean bool) {
      out.concat(bool.booleanValue());
    } else if (value instanceof Number number) {
      double d = number.doubleValue();
      // Integers write without a decimal point so they read back naturally.
      if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
        out.concat((long) d);
      } else {
        out.concat(String.valueOf(d));
      }
    } else if (value instanceof FlixelMap<?, ?> map) {
      out.concat('{');
      boolean first = true;
      for (FlixelMap.Entry<?, ?> e : map.entries()) {
        if (!first) {
          out.concat(',');
        }
        first = false;
        writeString(out, String.valueOf(e.key));
        out.concat(':');
        writeValue(out, e.value);
      }
      out.concat('}');
    } else if (value instanceof FlixelArray<?> list) {
      out.concat('[');
      for (int i = 0; i < list.getSize(); i++) {
        if (i > 0) {
          out.concat(',');
        }
        writeValue(out, list.get(i));
      }
      out.concat(']');
    } else {
      writeString(out, String.valueOf(value));
    }
  }

  /** Writes a JSON string literal with escaping. */
  private static void writeString(@NotNull FlixelString out, @NotNull String value) {
    out.concat('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.concat("\\\"");
        case '\\' -> out.concat("\\\\");
        case '\n' -> out.concat("\\n");
        case '\r' -> out.concat("\\r");
        case '\t' -> out.concat("\\t");
        default -> {
          if (c < 0x20) {
            out.concat("\\u").concat(String.format("%04x", (int) c));
          } else {
            out.concat(c);
          }
        }
      }
    }
    out.concat('"');
  }

  public boolean isBound() {
    return bound;
  }

  /**
   * Returns whether this save object is bound to a save.
   *
   * @return {@code true} if this save is bound, {@code false} otherwise.
   */
  public boolean getBound() {
    return bound;
  }

  @NotNull
  public String getName() {
    return boundName;
  }

  @Nullable
  public String getSlot() {
    return boundSlot;
  }

  @NotNull
  public FlixelSaveHandler getHandler() {
    return handler;
  }

  @NotNull
  public FlixelSaveStatus getStatus() {
    return status;
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  /**
   * Returns whether the bound save currently holds no data.
   *
   * @return {@code true} if the save holds no data, {@code false} otherwise.
   */
  public boolean getEmpty() {
    return data.isEmpty();
  }
}
