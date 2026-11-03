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
package org.flixelgdx.backend.html5.save;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.save.FlixelSaveHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;

/**
 * The web save storage: one {@code localStorage} entry per save.
 *
 * <p>{@code localStorage} is a small key-value store the browser keeps for each website. It
 * survives page reloads and browser restarts, which makes it the web's closest match to a save
 * file. By default each save is stored under the key
 * {@code flixel:Company:Title:saves/Name.json}, using the company name and game title set in
 * {@link FlixelConfig}, so several games hosted on the same site never overwrite each other.
 *
 * <p>Browsers can refuse storage (for example in some private browsing modes, or when the site's
 * quota is full). Every method here catches that and reports failure through its return value
 * instead of crashing the game.
 */
public class FlixelHtml5SaveHandler implements FlixelSaveHandler {

  /** The key prefix put in front of every save name, or {@code null} to build it from the config. */
  @Nullable
  private final String prefix;

  /** Creates a handler that namespaces saves by the company name and game title. */
  public FlixelHtml5SaveHandler() {
    this.prefix = null;
  }

  /**
   * Creates a handler that stores each save under {@code prefix + name}.
   *
   * @param prefix The key prefix, for example {@code "mygame:"}.
   */
  public FlixelHtml5SaveHandler(@NotNull String prefix) {
    this.prefix = prefix;
  }

  @Override
  @Nullable
  public String read(@NotNull String name) {
    return storageGet(keyFor(name));
  }

  @Override
  public boolean write(@NotNull String name, @NotNull String data) {
    return storageSet(keyFor(name), data);
  }

  @Override
  public boolean delete(@NotNull String name) {
    return storageRemove(keyFor(name));
  }

  /**
   * Returns the {@code localStorage} key a save name is stored under.
   *
   * @param name The save name.
   * @return The storage key.
   */
  @NotNull
  public String keyFor(@NotNull String name) {
    if (prefix != null) {
      return prefix + name;
    }
    FlixelConfig config = Flixel.config;
    return "flixel:" + config.getCompany() + ":" + config.getTitle() + ":saves/" + name + ".json";
  }

  @JSBody(params = "key", script = """
      try {
        return window.localStorage.getItem(key);
      } catch (e) {
        console.error('Could not read save "' + key + '".', e);
        return null;
      }
      """)
  private static native String storageGet(String key);

  @JSBody(params = { "key", "value" }, script = """
      try {
        window.localStorage.setItem(key, value);
        return true;
      } catch (e) {
        console.error('Could not write save "' + key + '".', e);
        return false;
      }
      """)
  private static native boolean storageSet(String key, String value);

  @JSBody(params = "key", script = """
      try {
        window.localStorage.removeItem(key);
        return true;
      } catch (e) {
        return false;
      }
      """)
  private static native boolean storageRemove(String key);

  @Nullable
  public String getPrefix() {
    return prefix;
  }
}
