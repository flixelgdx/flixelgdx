/**
 * Persistent, JSON-backed save data for storing game progress between sessions.
 *
 * <p>Saving in FlixelGDX works like a simple key-value notebook. You open a named notebook
 * (called "binding"), write whatever you want into it, and close it (called "flushing") to
 * persist the data to disk. The next time the game starts, you open the same notebook and
 * your data is there waiting. The only class you need for this is
 * {@link org.flixelgdx.util.save.FlixelSave FlixelSave}, which the framework makes available
 * globally as {@link org.flixelgdx.Flixel#save Flixel.save}.
 *
 * <h2>Core workflow</h2>
 * <p>Every save session follows the same three steps: bind, read/write, then flush.
 *
 * <ol>
 *   <li><b>Bind</b> - call {@link org.flixelgdx.util.save.FlixelSave#bind(String, String) FlixelSave.bind(...)}
 *     to open a named save file. Binding also loads any previously saved data automatically, so you can
 *     start reading immediately after binding.</li>
 *   <li><b>Read and write</b> - access entries through the public
 *     {@link org.flixelgdx.util.save.FlixelSave#data FlixelSave.data} map, or use the typed convenience
 *     methods ({@link org.flixelgdx.util.save.FlixelSave#getInt(String, int) getInt(...)},
 *     {@link org.flixelgdx.util.save.FlixelSave#getFloat(String, float) getFloat(...)},
 *     {@link org.flixelgdx.util.save.FlixelSave#getBool(String, boolean) getBool(...)},
 *     {@link org.flixelgdx.util.save.FlixelSave#getString(String, String) getString(...)})
 *     when reading values back.</li>
 *   <li><b>Flush</b> - call {@link org.flixelgdx.util.save.FlixelSave#flush() FlixelSave.flush()} to
 *     write the current state of {@code data} to disk. Nothing is saved until you flush.</li>
 * </ol>
 *
 * <pre>{@code
 * // In your FlixelState.create():
 * Flixel.save.bind("AwesomeGame", "slot1");
 *
 * int highScore = Flixel.save.getInt("highScore", 0);
 * if (score > highScore) {
 *   Flixel.save.data.put("highScore", score);
 *   Flixel.save.flush(); // Persist immediately.
 * }
 * }</pre>
 *
 * <h2>Save slots</h2>
 * <p>The second argument to {@code bind(...)} is an optional slot discriminator. Slots let you
 * maintain several independent save files under the same game name (for example, one per player
 * profile or difficulty mode). Each {@code (name, slot)} pair maps to a separate file on disk.
 *
 * <pre>{@code
 * // Three independent save files for three player profiles.
 * Flixel.save.bind("AwesomeGame", "profile1");
 * Flixel.save.bind("AwesomeGame", "profile2");
 * Flixel.save.bind("AwesomeGame", "profile3");
 * }</pre>
 *
 * <p>Pass {@code null} as the slot when you only need a single save file:
 *
 * <pre>{@code
 * Flixel.save.bind("AwesomeGame", null);
 * }</pre>
 *
 * <h2>Save file locations</h2>
 * <p>FlixelGDX writes save files through the platform file seam using the OS-specific preferences
 * directory. The company name set in {@link org.flixelgdx.FlixelGame.Config FlixelGame.Config}
 * is required so the framework can find the correct folder. The bind method will log an error and return
 * {@code false} if the company name has not been set.
 *
 * <ul>
 *   <li><b>Windows</b>: {@code %APPDATA%\Company\Title\saves\}</li>
 *   <li><b>macOS</b>: {@code ~/Library/Application Support/Company/Title/saves/}</li>
 *   <li><b>Linux</b>: {@code $XDG_DATA_HOME/Company/Title/saves/}</li>
 * </ul>
 *
 * <p>To save somewhere else entirely (for example a cloud-sync folder during development or
 * a temporary path in tests) use the three-argument overload
 * {@link org.flixelgdx.util.save.FlixelSave#bind(String, String, org.flixelgdx.file.FlixelFile) FlixelSave.bind(name, slot, directory)}.
 * When a custom directory is provided, no company name check is performed.
 *
 * <h2>Checking whether data was loaded</h2>
 * <p>After binding, inspect *
 * @Override
 * public void update(float elapsed) {
 *   super.update(elapsed);
 *   timers.update(elapsed);
 * }
 *
 * @Override
 * public void destroy() {
 *   timers.destroy(); // Cancels and frees all pending timers.
 *   super.destroy();
 * }
 * {@link org.flixelgdx.util.save.FlixelSave#getStatus() FlixelSave.getStatus()} to understand
 * what happened:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.util.save.FlixelSaveStatus#EMPTY FlixelSaveStatus.EMPTY} - no file
 *     existed yet (for example on a first launch), or the file was empty.</li>
 *   <li>{@link org.flixelgdx.util.save.FlixelSaveStatus#OK FlixelSaveStatus.OK} - data was
 *     loaded or last flushed successfully.</li>
 *   <li>{@link org.flixelgdx.util.save.FlixelSaveStatus#ERROR FlixelSaveStatus.ERROR} - the file
 *     existed but could not be parsed, or the last flush failed.</li>
 * </ul>
 *
 * <pre>{@code
 * Flixel.save.bind("AwesomeGame", null);
 * if (Flixel.save.getStatus() == FlixelSaveStatus.EMPTY) {
 *   // First launch: write initial defaults.
 *   Flixel.save.data.put("highScore", 0);
 *   Flixel.save.data.put("musicVolume", 1.0f);
 *   Flixel.save.flush();
 * }
 * }</pre>
 *
 * <h2>Supported value types</h2>
 * <p>The {@code data} map accepts the following types as values:
 *
 * <ul>
 *   <li>{@code String}</li>
 *   <li>Any {@code Number} subclass ({@code int}, {@code float}, {@code double}, etc.)</li>
 *   <li>{@code Boolean}</li>
 *   <li>Nested {@link org.flixelgdx.collections.FlixelMap FlixelMap}{@code <String, Object>}
 *     objects for grouped data</li>
 *   <li>{@link org.flixelgdx.collections.FlixelArray FlixelArray}{@code <Object>} lists</li>
 * </ul>
 *
 * <p>Anything else is converted with {@code toString()} before writing, which means it will read
 * back as a {@code String}. Store structured data in nested maps instead.
 *
 * <h2>Merging data from another source</h2>
 * <p>Use {@link org.flixelgdx.util.save.FlixelSave#mergeData(org.flixelgdx.collections.FlixelMap, boolean, boolean) FlixelSave.mergeData(...)}
 * to copy entries from another map into the save without overwriting the whole thing. This is
 * useful when applying defaults: merge a defaults map with {@code overwrite = false} so existing
 * player progress is never replaced.
 *
 * <pre>{@code
 * FlixelMap<String, Object> defaults = new FlixelMap<>();
 * defaults.put("highScore", 0);
 * defaults.put("musicVolume", 1.0f);
 *
 * // Only adds keys that the player does not already have.
 * Flixel.save.mergeData(defaults, false, true);
 * }</pre>
 *
 * <h2>Erasing a save</h2>
 * <p>Call {@link org.flixelgdx.util.save.FlixelSave#erase() FlixelSave.erase()} to clear the
 * in-memory {@code data} map and delete the file from disk. The save remains bound afterward,
 * so you can immediately start writing new data and flush again.
 *
 * <h2>Things to keep in mind</h2>
 * <ul>
 *   <li>Use {@code FlixelSave} from the main game thread only. There is no thread safety.</li>
 *   <li>This system is designed for small structured state: scores, settings, unlocks, and
 *     similar compact data. Do not store large binary blobs here; load those through
 *     {@link org.flixelgdx.Flixel#files Flixel.files} instead.</li>
 *   <li>Nothing is written to disk until you call {@link org.flixelgdx.util.save.FlixelSave#flush() flush()}.
 *     Forgetting to flush after writing is the most common save bug.</li>
 * </ul>
 *
 * @see org.flixelgdx.util.save.FlixelSave
 * @see org.flixelgdx.util.save.FlixelSaveStatus
 */
package org.flixelgdx.util.save;
