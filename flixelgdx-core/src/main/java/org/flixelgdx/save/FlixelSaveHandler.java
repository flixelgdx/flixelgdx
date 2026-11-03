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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The storage contract behind {@link FlixelSave}: where save text is kept and how it is read back.
 *
 * <p>Think of {@link FlixelSave} as the notebook you write in and a save handler as the shelf the
 * notebook is kept on. The notebook never cares whether the shelf is a folder on a hard drive, the
 * app's private storage on a phone, or the browser's {@code localStorage}. It only asks the shelf
 * four things: give me the text stored under this name, store this text under this name, throw
 * this name away, and is anything stored under this name at all.
 *
 * <p>Every platform installs its own handler on {@link FlixelSave#defaultHandler} before the game
 * starts, so {@code Flixel.save.bind(name, slot)} works the same everywhere. Games can also write
 * their own handler (for example one that syncs to a cloud service) and give it to a single save
 * object through {@link FlixelSave#FlixelSave(FlixelSaveHandler)} or
 * {@link FlixelSave#setHandler(FlixelSaveHandler)}.
 *
 * <p>A save name is a plain identifier such as {@code "MyGame.slot1"}. It never contains a folder
 * or a file extension; each handler decides how to turn it into a real location. Handlers must
 * never throw from these methods. They report failure through their return values instead, so a
 * broken disk or a full browser quota can never crash the game.
 *
 * <p>Example of a custom handler that keeps saves in memory, which is handy in tests:
 *
 * <pre>{@code
 * public class MemorySaveHandler implements FlixelSaveHandler {
 *
 *   private final FlixelMap<String, String> entries = new FlixelMap<>();
 *
 *   public String read(String name) {
 *     return entries.get(name);
 *   }
 *
 *   public boolean write(String name, String data) {
 *     entries.put(name, data);
 *     return true;
 *   }
 *
 *   public boolean delete(String name) {
 *     entries.remove(name);
 *     return true;
 *   }
 * }
 *
 * FlixelSave save = new FlixelSave(new MemorySaveHandler());
 * save.bind("MyGame", "slot1");
 * }</pre>
 *
 * @see FlixelSave
 * @see FlixelNoopSaveHandler
 */
public interface FlixelSaveHandler {

  /**
   * Reads the text stored under a save name.
   *
   * @param name The save name, for example {@code "MyGame.slot1"}.
   * @return The stored text, or {@code null} when nothing is stored under that name or it could
   *     not be read.
   */
  @Nullable
  String read(@NotNull String name);

  /**
   * Stores text under a save name, replacing anything stored there before.
   *
   * <p>Implementations should make the write all-or-nothing where the platform allows it, so a
   * crash or power loss halfway through never leaves a half-written save behind.
   *
   * @param name The save name, for example {@code "MyGame.slot1"}.
   * @param data The text to store.
   * @return {@code true} when the text was stored.
   */
  boolean write(@NotNull String name, @NotNull String data);

  /**
   * Removes whatever is stored under a save name.
   *
   * @param name The save name, for example {@code "MyGame.slot1"}.
   * @return {@code true} when nothing is stored under that name anymore, including when nothing was
   *     stored there to begin with.
   */
  boolean delete(@NotNull String name);

  /**
   * Returns whether anything is stored under a save name.
   *
   * <p>The default reads the whole entry; handlers that can check cheaply should override it.
   *
   * @param name The save name, for example {@code "MyGame.slot1"}.
   * @return {@code true} if something is stored under that name, {@code false} otherwise.
   */
  default boolean exists(@NotNull String name) {
    return read(name) != null;
  }
}
