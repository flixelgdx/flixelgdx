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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.save.FlixelNoopSaveHandler;
import org.flixelgdx.save.FlixelSave;
import org.flixelgdx.save.FlixelSaveStatus;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The save editor of the remote debugger: see every value in the game's save, change one, undo one, save, or start
 * over.
 *
 * <p>Think of a spreadsheet that sits next to the game. Every cell is one value of the save ({@code player.coins},
 * {@code settings.volume}), and a cell turns yellow when it no longer matches what is stored on disk. You can type a
 * new value into a cell and watch the game react, throw one cell away to see how the game behaves with its default,
 * and press "save" when you are happy.
 *
 * <h2>What the panel edits</h2>
 *
 * <p>It works on {@link FlixelSave#data} of one save at a time, picked from a short list. Entry 0 of the list is
 * always {@link Flixel#save}, looked up again on every refresh, so the panel follows the game when it replaces that
 * save. Games that keep more than one save (a settings file and a few profiles, for example) {@link #add(FlixelSave)
 * add} the others, and the debugger app can then switch between them with {@link #select(int)}. Each entry is shown
 * as {@code name/slot} (or just {@code name} without a slot), as {@code (not bound)} while the save has no name (a
 * destroyed save has none), or as {@code (no save)} for entry 0 while {@link Flixel#save} is {@code null}.
 * {@link #setSave(FlixelSave)} adds a save and switches to it in one call.
 *
 * <p>The data is a tree of maps, lists, and plain values. The panel flattens it into one row per plain value with a
 * dotted path, such as {@code player.coins} or {@code inventory.2.name} (list elements are named by their index).
 * Keys that are numbers sort by number, other keys sort alphabetically without regard to case, and numbers come
 * first. An empty map or list is a row of its own, shown as {@code {}} or {@code []}. At most {@value #MAX_ROWS} rows
 * are sent.
 *
 * <h2>Unsaved changes</h2>
 *
 * <p>{@link FlixelSave} does not track changes, so the panel compares each value with what the save's storage holds:
 * it loads a second copy of the stored save and checks every row against it. A row differs when it is new, or its
 * type or value is not the same (a whole number {@code 5} and the decimal {@code 5.0} count as the same, because
 * saved files do not tell them apart). Rows that are in the stored save but no longer in the data are reported as
 * <em>removed</em>. The stored copy is only loaded again when the data changed or something is unsaved, so an idle save
 * costs nothing after the first read.
 *
 * <h2>Actions</h2>
 *
 * <ul>
 *   <li>{@link #set(CharSequence, CharSequence) set} changes a value that already exists and keeps its type.
 *       Strings can be typed with or without quotes, booleans are {@code true} or {@code false}, and numbers are
 *       parsed as the type the value already has.</li>
 *   <li>{@link #resetKey(CharSequence) resetKey} removes a value. {@link FlixelSave} has no default values, so the game
 *       falls back to the default it passes to calls such as {@code getInt("coins", 0)}.</li>
 *   <li>{@link #saveNow()} writes the data to storage.</li>
 *   <li>{@link #resetAll()} erases the data and the stored save.</li>
 *   <li>{@link #select(int) select} switches to another save of the list.</li>
 * </ul>
 *
 * <p>Changes are made in memory only; nothing reaches storage until {@link #saveNow()} (or the game's own flush).
 *
 * <p>The panel refreshes once a second while it is enabled, reusing its rows, and again right after every action.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelSave profile = new FlixelSave();
 * profile.bind("Game", "profile1");
 * Flixel.debug.save.add(profile);                  // The app now lists "Game/profile1" next to Flixel.save.
 *
 * Flixel.debug.save.set("player.coins", "999");    // The coins row turns "unsaved".
 * Flixel.debug.save.saveNow();                     // Now it matches the stored save again.
 * Flixel.debug.save.resetKey("settings.volume");   // The game uses its own default from now on.
 * }</pre>
 *
 * @see FlixelSave
 * @see FlixelDebugProtocol#KIND_SAVE
 */
public class FlixelDebugSavePanel extends FlixelDebugPanelBase {

  /** The most rows the panel flattens and sends. */
  public static final int MAX_ROWS = 300;

  /** The longest path or value text an action may carry, in characters. */
  public static final int MAX_TEXT = 200;

  /** Seconds between refreshes while the panel is enabled. */
  public static final float REFRESH_INTERVAL = 1f;

  /** Seconds an action's message stays in the status line before the summary comes back. */
  public static final float STATUS_SECONDS = 4f;

  private static final int MAX_DEPTH = 16;

  private static final int TYPE_NULL = 0;
  private static final int TYPE_BOOL = 1;
  private static final int TYPE_NUMBER = 2;
  private static final int TYPE_STRING = 3;
  private static final int TYPE_EMPTY_MAP = 4;
  private static final int TYPE_EMPTY_LIST = 5;

  private long signature;
  private long lastLiveHash;

  private float timer = REFRESH_INTERVAL;
  private float statusTimer;

  private final FlixelString path = new FlixelString(64);
  private final FlixelString pathIn = new FlixelString(64);
  private final FlixelString textIn = new FlixelString(64);
  private final FlixelString status = new FlixelString(64);
  private final RowList rows = new RowList();
  private final RowList stored = new RowList();
  private final RowList removed = new RowList();
  private final FlixelArray<FlixelArray<Object>> keyScratch = new FlixelArray<>();
  private final FlixelArray<FlixelSave> extras = new FlixelArray<>();
  private final FlixelArray<FlixelString> labels = new FlixelArray<>();

  @Nullable
  private FlixelSave save;

  @Nullable
  private FlixelSave lastTarget;

  @Nullable
  private Object targetParent;

  @Nullable
  private Object targetKey;

  private int targetIndex;
  private int unsavedCount;
  private int lastUnsaved;
  private int labelCount;
  private int labelSelected;

  private boolean statusError;
  private boolean statusFromAction;
  private boolean storedValid;

  /** Creates the save panel. It edits {@link Flixel#save}. */
  public FlixelDebugSavePanel() {
    super("save", "Save", FlixelDebugProtocol.KIND_SAVE);
  }

  @Override
  public void update(float elapsed) {
    if (statusFromAction) {
      statusTimer += elapsed;
      if (statusTimer >= STATUS_SECONDS) {
        statusFromAction = false;
        timer = REFRESH_INTERVAL;
      }
    }
    timer += elapsed;
    if (timer >= REFRESH_INTERVAL) {
      timer = 0f;
      refresh();
    }
  }

  /**
   * Flattens the save, compares it with the stored copy, and bumps the revision when anything changed.
   *
   * <p>The panel does this about once a second on its own. Call it to see the effect of a change at once.
   */
  public void refresh() {
    FlixelSave target = getSave();
    if (target != lastTarget) {
      // Another save (or a new Flixel.save) is edited now, so the stored copy belongs to the old one.
      lastTarget = target;
      storedValid = false;
    }
    rebuildLabels();
    rows.clear();
    removed.clear();
    unsavedCount = 0;
    if (target != null) {
      path.clear();
      flatten(target.data, 0, rows);
      compareWithStorage(target);
    } else {
      storedValid = false;
    }
    if (!statusFromAction) {
      summarize(target);
    }
    long next = computeSignature();
    if (next != signature) {
      signature = next;
      markChanged();
    }
  }

  /**
   * Changes the value at a path to the value a person typed, keeping its type.
   *
   * <p>The path must already exist and lead to a string, a boolean, or a number. A string may be typed with or
   * without double quotes (inside quotes, {@code \"}, {@code \\}, and {@code \n} stand for a quote, a backslash, and
   * a line break). A boolean is {@code true} or {@code false} in any case. A number is parsed as the type the value
   * has, so a whole number rejects {@code 1.5}, and a number that does not fit is rejected. The outcome is shown in the
   * status line.
   *
   * @param path The dotted path of the value, as shown in the rows.
   * @param text The new value as text. At most {@value #MAX_TEXT} characters.
   * @return {@code true} when the value was changed.
   */
  public boolean set(@NotNull CharSequence path, @NotNull CharSequence text) {
    FlixelSave target = getSave();
    if (target == null) {
      return fail("There is no save to edit.");
    }
    if (path.length() > MAX_TEXT || text.length() > MAX_TEXT) {
      return fail("The path and the value must each be at most " + MAX_TEXT + " characters.");
    }
    if (!locate(target.data, path, 0, 0)) {
      return fail("There is no value at \"" + path + "\".");
    }
    Object current = current();
    Object replacement;
    try {
      replacement = convert(current, text);
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage() + " (" + path + ")");
    }
    store(replacement);
    refresh();
    return succeed("Set " + path + " to " + text.toString().trim());
  }

  /**
   * Removes a value from the save, so the game falls back to its own default for it.
   *
   * <p>{@link FlixelSave} has no table of default values, so "back to the default" means "gone". Removing a map
   * entry never changes the other entries. Removing a list element moves the later elements down by one.
   *
   * @param path The dotted path of the value (or of a whole map or list), as shown in the rows.
   * @return {@code true} when something was removed.
   */
  public boolean resetKey(@NotNull CharSequence path) {
    FlixelSave target = getSave();
    if (target == null) {
      return fail("There is no save to edit.");
    }
    if (path.length() > MAX_TEXT) {
      return fail("The path must be at most " + MAX_TEXT + " characters.");
    }
    if (!locate(target.data, path, 0, 0)) {
      return fail("There is no value at \"" + path + "\".");
    }
    removeTarget();
    refresh();
    return succeed("Removed " + path + ". The game uses its default.");
  }

  /**
   * Writes the data to storage with {@link FlixelSave#flush()}.
   *
   * @return {@code true} when the write succeeded.
   */
  public boolean saveNow() {
    FlixelSave target = getSave();
    if (target == null) {
      return fail("There is no save to write.");
    }
    if (!target.isBound()) {
      return fail("The save is not bound to a name, so it cannot be written.");
    }
    boolean ok = target.flush();
    storedValid = false;
    refresh();
    return ok ? succeed("Saved " + target.getKey() + ".") : fail("Could not write " + target.getKey() + ".");
  }

  /**
   * Erases the data and the stored save with {@link FlixelSave#erase()}.
   *
   * @return {@code true} when the stored save was removed (or never existed).
   */
  public boolean resetAll() {
    FlixelSave target = getSave();
    if (target == null) {
      return fail("There is no save to erase.");
    }
    boolean ok = target.erase();
    storedValid = false;
    refresh();
    return ok ? succeed("Erased the whole save.") : fail("Erased the data, but the stored save could not be removed.");
  }

  /**
   * Adds a save to the list the debugger app can pick from, without switching to it.
   *
   * <p>Adding a save that is already listed, or {@link Flixel#save} itself (which is always entry 0), does nothing.
   * The save is kept until {@link #remove(FlixelSave)} is called, even after it is destroyed; a destroyed save is
   * simply shown as {@code (not bound)}.
   *
   * <pre>{@code
   * FlixelSave settings = new FlixelSave();
   * settings.bind("Game", "settings");
   * Flixel.debug.save.add(settings);
   * }</pre>
   *
   * @param save The save to list.
   */
  public void add(@NotNull FlixelSave save) {
    if (save == null || save == Flixel.save || extras.contains(save, true)) {
      return;
    }
    extras.add(save);
    rebuildLabels();
    refreshSignature();
  }

  /**
   * Takes a save off the list.
   *
   * <p>When it was the save being edited, the panel goes back to entry 0 ({@link Flixel#save}). {@link Flixel#save}
   * itself always stays as entry 0.
   *
   * @param save The save to take off.
   * @return {@code true} when the save was listed by {@link #add(FlixelSave)} or {@link #setSave(FlixelSave)}.
   */
  public boolean remove(@NotNull FlixelSave save) {
    if (save == null || !extras.removeValue(save, true)) {
      return false;
    }
    if (this.save == save) {
      choose(null);
    } else {
      rebuildLabels();
      refreshSignature();
    }
    return true;
  }

  /**
   * Switches the panel to another save of the list and refreshes at once.
   *
   * <p>Index 0 is {@link Flixel#save}; the others are the saves given to {@link #add(FlixelSave)}, in the order they
   * were added.
   *
   * @param index The position in the list, from 0 to {@link #getSaveCount()} minus one.
   * @return {@code true} when the index was valid.
   */
  public boolean select(int index) {
    if (index < 0 || index >= getSaveCount()) {
      return false;
    }
    choose(index == 0 ? null : getSaveAt(index));
    return true;
  }

  /**
   * Writes the rows, the removed paths, the status line, the error flag, the list of saves, and the selected index.
   *
   * @param writer The writer positioned after the panel index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.beginArray();
    for (int i = 0; i < rows.count; i++) {
      Row row = rows.items.get(i);
      writer.writeString(row.path)
          .writeString(row.display)
          .writeU8(row.kind)
          .writeBool(row.unsaved);
    }
    writer.endArray(rows.count);
    writer.beginArray();
    for (int i = 0; i < removed.count; i++) {
      writer.writeString(removed.items.get(i).path);
    }
    writer.endArray(removed.count);
    writer.writeString(status).writeBool(statusError);
    writer.beginArray();
    for (int i = 0; i < labelCount; i++) {
      writer.writeString(labels.get(i));
    }
    writer.endArray(labelCount);
    writer.writeU16(labelSelected);
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    switch (action) {
      case FlixelDebugProtocol.SAVE_SET -> {
        reader.readString(pathIn);
        reader.readString(textIn);
        set(pathIn, textIn);
      }
      case FlixelDebugProtocol.SAVE_RESET_KEY -> {
        reader.readString(pathIn);
        resetKey(pathIn);
      }
      case FlixelDebugProtocol.SAVE_SAVE_NOW -> saveNow();
      case FlixelDebugProtocol.SAVE_RESET_ALL -> resetAll();
      case FlixelDebugProtocol.SAVE_SELECT -> select(reader.readU16());
      default -> {
      }
    }
  }

  @Override
  public void reset() {
    rows.clear();
    removed.clear();
    stored.clear();
    storedValid = false;
    statusFromAction = false;
    status.clear();
    statusError = false;
    unsavedCount = 0;
    lastTarget = null;
    rebuildLabels();
    signature = 0L;
    timer = REFRESH_INTERVAL;
    markChanged();
  }

  @SuppressWarnings("unchecked")
  private void flatten(@Nullable Object value, int depth, RowList out) {
    if (value instanceof FlixelMap<?, ?> map) {
      if (map.getSize() > 0 && depth < MAX_DEPTH) {
        FlixelArray<Object> keys = keysOf(map, depth);
        int n = keys.getSize();
        int i = 0;
        for (; i < n && out.count < MAX_ROWS; i++) {
          Object key = keys.get(i);
          int mark = path.length();
          if (mark > 0) {
            path.concat('.');
          }
          path.concat(key instanceof String s ? s : String.valueOf(key));
          flatten(((FlixelMap<Object, Object>) map).get(key), depth + 1, out);
          path.setLength(mark);
        }
        out.truncated |= i < n;
        return;
      }
      if (path.length() > 0) {
        leaf(null, TYPE_EMPTY_MAP, out);
      }
      return;
    }
    if (value instanceof FlixelArray<?> list) {
      if (list.getSize() > 0 && depth < MAX_DEPTH) {
        int n = list.getSize();
        int i = 0;
        for (; i < n && out.count < MAX_ROWS; i++) {
          int mark = path.length();
          if (mark > 0) {
            path.concat('.');
          }
          path.concat(i);
          flatten(list.get(i), depth + 1, out);
          path.setLength(mark);
        }
        out.truncated |= i < n;
        return;
      }
      leaf(null, TYPE_EMPTY_LIST, out);
      return;
    }
    leaf(value, TYPE_NULL, out);
  }

  /** Adds one row for a plain value (or an empty container) at the current path. */
  private void leaf(@Nullable Object value, int containerType, RowList out) {
    if (out.count >= MAX_ROWS) {
      out.truncated = true;
      return;
    }
    Row row = out.next();
    row.path.set(path);
    row.unsaved = false;
    row.display.clear();
    if (containerType == TYPE_EMPTY_MAP) {
      row.type = TYPE_EMPTY_MAP;
      row.kind = FlixelDebugProtocol.VALUE_STRING;
      row.hash = 0L;
      row.display.concat("{}");
    } else if (containerType == TYPE_EMPTY_LIST) {
      row.type = TYPE_EMPTY_LIST;
      row.kind = FlixelDebugProtocol.VALUE_STRING;
      row.hash = 0L;
      row.display.concat("[]");
    } else if (value == null) {
      row.type = TYPE_NULL;
      row.kind = FlixelDebugProtocol.VALUE_STRING;
      row.hash = 0L;
      row.display.concat("null");
    } else if (value instanceof Boolean bool) {
      row.type = TYPE_BOOL;
      row.kind = FlixelDebugProtocol.VALUE_BOOL;
      row.hash = bool ? 1L : 0L;
      row.display.concat(bool.booleanValue());
    } else if (value instanceof Number number) {
      row.type = TYPE_NUMBER;
      double d = number.doubleValue();
      row.hash = Double.doubleToLongBits(d == 0d ? 0d : d);
      if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
        row.kind = FlixelDebugProtocol.VALUE_INT;
        row.display.concat(number.longValue());
      } else if (value instanceof Long) {
        row.kind = FlixelDebugProtocol.VALUE_LONG;
        row.display.concat(number.longValue());
      } else if (value instanceof Float) {
        row.kind = FlixelDebugProtocol.VALUE_FLOAT;
        row.display.concat(number.floatValue());
      } else {
        row.kind = FlixelDebugProtocol.VALUE_DOUBLE;
        row.display.concat(d);
      }
    } else {
      row.type = TYPE_STRING;
      row.kind = FlixelDebugProtocol.VALUE_STRING;
      String text = value instanceof String s ? s : String.valueOf(value);
      row.hash = hashOf(text);
      quote(text, row.display);
    }
  }

  private FlixelArray<Object> keysOf(FlixelMap<?, ?> map, int depth) {
    while (keyScratch.getSize() <= depth) {
      keyScratch.add(new FlixelArray<>());
    }
    FlixelArray<Object> keys = keyScratch.get(depth);
    keys.clear();
    for (FlixelMap.Entry<?, ?> entry : map.entries()) {
      Object key = entry.key;
      int at = keys.getSize();
      keys.add(key);
      // Insertion sort: save maps are small, and this needs no comparator object.
      while (at > 0 && compareKeys(keys.get(at - 1), key) > 0) {
        keys.set(at, keys.get(at - 1));
        at--;
      }
      keys.set(at, key);
    }
    return keys;
  }

  /** Numbers sort by value and come before everything else; other keys sort alphabetically, ignoring case. */
  private static int compareKeys(Object a, Object b) {
    String x = a instanceof String s ? s : String.valueOf(a);
    String y = b instanceof String s ? s : String.valueOf(b);
    boolean numericX = isNumber(x);
    boolean numericY = isNumber(y);
    if (numericX && numericY) {
      int byValue = Long.compare(Long.parseLong(x), Long.parseLong(y));
      return byValue != 0 ? byValue : x.compareTo(y);
    }
    if (numericX != numericY) {
      return numericX ? -1 : 1;
    }
    int byText = x.compareToIgnoreCase(y);
    return byText != 0 ? byText : x.compareTo(y);
  }

  private static boolean isNumber(String text) {
    int n = text.length();
    if (n == 0 || n > 18) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      char c = text.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  private void compareWithStorage(FlixelSave target) {
    long liveHash = rowsHash(rows);
    if (!storedValid || liveHash != lastLiveHash || lastUnsaved > 0) {
      loadStored(target);
    }
    lastLiveHash = liveHash;
    int hint = 0;
    for (int i = 0; i < rows.count; i++) {
      Row row = rows.items.get(i);
      int match = findPath(stored, row.path, hint);
      if (match < 0) {
        row.unsaved = true;
      } else {
        hint = match + 1;
        Row old = stored.items.get(match);
        row.unsaved = old.type != row.type || old.hash != row.hash;
      }
      if (row.unsaved) {
        unsavedCount++;
      }
    }
    hint = 0;
    for (int i = 0; i < stored.count && removed.count < MAX_ROWS; i++) {
      Row old = stored.items.get(i);
      int match = findPath(rows, old.path, hint);
      if (match < 0) {
        removed.next().path.set(old.path);
      } else {
        hint = match + 1;
      }
    }
    lastUnsaved = unsavedCount;
  }

  /** Loads a second copy of the stored save and flattens it. */
  private void loadStored(FlixelSave target) {
    stored.clear();
    storedValid = true;
    if (!target.isBound()) {
      return;
    }
    FlixelSave probe = new FlixelSave(target.getHandler());
    if (probe.bind(target.getName(), target.getSlot())) {
      path.clear();
      flatten(probe.data, 0, stored);
    }
  }

  /** Finds a row by path, searching forward from {@code hint} first because both lists are in the same order. */
  private static int findPath(RowList list, FlixelString wanted, int hint) {
    int start = Math.min(hint, list.count);
    for (int i = start; i < list.count; i++) {
      if (sameText(list.items.get(i).path, wanted)) {
        return i;
      }
    }
    for (int i = 0; i < start; i++) {
      if (sameText(list.items.get(i).path, wanted)) {
        return i;
      }
    }
    return -1;
  }

  private static boolean sameText(FlixelString a, FlixelString b) {
    int n = a.length();
    if (n != b.length()) {
      return false;
    }
    for (int i = 0; i < n; i++) {
      if (a.charAt(i) != b.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private void summarize(@Nullable FlixelSave target) {
    status.clear();
    statusError = false;
    if (target == null) {
      status.concat("There is no save. Flixel.save has not been created.");
      return;
    }
    if (!target.isBound()) {
      status.concat("The save is not bound to a name yet, so it cannot be written.");
      return;
    }
    status.concat(target.getKey()).concat(": ").concat(rows.count).concat(rows.truncated ? "+" : "")
        .concat(" values, ").concat(unsavedCount).concat(" unsaved, ").concat(removed.count).concat(" removed");
    if (target.getStatus() == FlixelSaveStatus.ERROR) {
      status.concat(". The last load or write failed.");
      statusError = true;
    } else if (target.getHandler() instanceof FlixelNoopSaveHandler) {
      status.concat(". This platform keeps no storage, so nothing can be saved.");
    }
  }

  private boolean succeed(String message) {
    status.set(message);
    statusError = false;
    statusFromAction = true;
    statusTimer = 0f;
    refreshSignature();
    return true;
  }

  private boolean fail(String message) {
    status.set(message);
    statusError = true;
    statusFromAction = true;
    statusTimer = 0f;
    refreshSignature();
    return false;
  }

  private void refreshSignature() {
    long next = computeSignature();
    if (next != signature) {
      signature = next;
      markChanged();
    }
  }

  private long computeSignature() {
    long hash = rowsHash(rows) * 31L + rowsHash(removed);
    for (int i = 0; i < rows.count; i++) {
      hash = hash * 31L + (rows.items.get(i).unsaved ? 1L : 0L);
    }
    for (int i = 0; i < status.length(); i++) {
      hash = hash * 31L + status.charAt(i);
    }
    hash = hash * 31L + (statusError ? 1L : 0L);
    for (int i = 0; i < labelCount; i++) {
      FlixelString label = labels.get(i);
      // The length keeps "ab" + "c" apart from "a" + "bc".
      hash = hash * 31L + label.length();
      for (int c = 0; c < label.length(); c++) {
        hash = hash * 31L + label.charAt(c);
      }
    }
    return (hash * 31L + labelCount) * 31L + labelSelected;
  }

  /** Switches to a save (or to {@link Flixel#save} for {@code null}) and refreshes with the new save's summary. */
  private void choose(@Nullable FlixelSave next) {
    save = next;
    storedValid = false;
    statusFromAction = false;
    refresh();
  }

  /** Builds the label of every listed save into the reused strings and remembers which one is selected. */
  private void rebuildLabels() {
    FlixelSave main = Flixel.save;
    labelCount = 0;
    addLabel(main);
    for (int i = 0; i < extras.getSize(); i++) {
      FlixelSave entry = extras.get(i);
      if (entry != main) {
        addLabel(entry);
      }
    }
    labelSelected = getSelectedIndex();
  }

  private void addLabel(@Nullable FlixelSave entry) {
    if (labelCount == labels.getSize()) {
      labels.add(new FlixelString(16));
    }
    FlixelString label = labels.get(labelCount++);
    label.clear();
    if (entry == null) {
      label.concat("(no save)");
    } else if (!entry.isBound()) {
      label.concat("(not bound)");
    } else {
      label.concat(entry.getName());
      String slot = entry.getSlot();
      if (slot != null && !slot.isEmpty()) {
        label.concat('/').concat(slot);
      }
    }
  }

  private static long rowsHash(RowList list) {
    long hash = 1125899906842597L;
    for (int i = 0; i < list.count; i++) {
      Row row = list.items.get(i);
      for (int c = 0; c < row.path.length(); c++) {
        hash = hash * 31L + row.path.charAt(c);
      }
      hash = hash * 31L + row.type;
      hash = hash * 31L + row.hash;
    }
    return hash;
  }

  private static long hashOf(String text) {
    long hash = 0xcbf29ce484222325L;
    for (int i = 0; i < text.length(); i++) {
      hash = (hash ^ text.charAt(i)) * 0x100000001b3L;
    }
    return hash;
  }

  /** Writes a string between quotes, escaping what would break the quoting, and cuts it at {@value #MAX_TEXT}. */
  private static void quote(String text, FlixelString out) {
    out.concat('"');
    int limit = Math.min(text.length(), MAX_TEXT);
    for (int i = 0; i < limit; i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.concat("\\\"");
        case '\\' -> out.concat("\\\\");
        case '\n' -> out.concat("\\n");
        case '\r' -> out.concat("\\r");
        case '\t' -> out.concat("\\t");
        default -> out.concat(c);
      }
    }
    if (text.length() > limit) {
      out.concat("...");
    }
    out.concat('"');
  }

  /**
   * Finds the container and key that a path points to. The result is left in {@link #targetParent},
   * {@link #targetKey}, and {@link #targetIndex}. Map keys may contain dots, so every way to cut the path is tried.
   */
  @SuppressWarnings("unchecked")
  private boolean locate(@Nullable Object container, CharSequence wanted, int from, int depth) {
    if (depth > MAX_DEPTH || from > wanted.length()) {
      return false;
    }
    if (container instanceof FlixelArray<?> list) {
      int end = from;
      int index = 0;
      while (end < wanted.length() && wanted.charAt(end) >= '0' && wanted.charAt(end) <= '9' && end - from < 9) {
        index = index * 10 + (wanted.charAt(end) - '0');
        end++;
      }
      if (end == from || index >= list.getSize() || (end < wanted.length() && wanted.charAt(end) != '.')) {
        return false;
      }
      if (end == wanted.length()) {
        targetParent = list;
        targetKey = null;
        targetIndex = index;
        return true;
      }
      return locate(list.get(index), wanted, end + 1, depth + 1);
    }
    if (container instanceof FlixelMap<?, ?> map) {
      for (int end = from; end <= wanted.length(); end++) {
        if (end != wanted.length() && wanted.charAt(end) != '.') {
          continue;
        }
        Object key = findKey(map, wanted.subSequence(from, end).toString());
        if (key == null) {
          continue;
        }
        if (end == wanted.length()) {
          targetParent = map;
          targetKey = key;
          return true;
        }
        if (locate(((FlixelMap<Object, Object>) map).get(key), wanted, end + 1, depth + 1)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private static Object findKey(FlixelMap<?, ?> map, String name) {
    for (FlixelMap.Entry<?, ?> entry : map.entries()) {
      Object key = entry.key;
      if ((key instanceof String s ? s : String.valueOf(key)).equals(name)) {
        return key;
      }
    }
    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private Object current() {
    if (targetParent instanceof FlixelArray<?> list) {
      return list.get(targetIndex);
    }
    return targetParent instanceof FlixelMap<?, ?> map ? ((FlixelMap<Object, Object>) map).get(targetKey) : null;
  }

  @SuppressWarnings("unchecked")
  private void store(Object value) {
    if (targetParent instanceof FlixelArray<?> list) {
      ((FlixelArray<Object>) list).set(targetIndex, value);
    } else if (targetParent instanceof FlixelMap<?, ?> map) {
      ((FlixelMap<Object, Object>) map).put(targetKey, value);
    }
  }

  @SuppressWarnings("unchecked")
  private void removeTarget() {
    if (targetParent instanceof FlixelArray<?> list) {
      list.removeIndex(targetIndex);
    } else if (targetParent instanceof FlixelMap<?, ?> map) {
      ((FlixelMap<Object, Object>) map).remove(targetKey);
    }
  }

  /**
   * Parses typed text as the type of the value it replaces.
   *
   * @throws IllegalArgumentException With a message for the person when the text does not fit.
   */
  private static Object convert(@Nullable Object current, CharSequence typed) {
    String text = typed.toString().trim();
    if (current instanceof String) {
      return unquote(typed.toString());
    }
    if (current instanceof Boolean) {
      if (text.equalsIgnoreCase("true")) {
        return Boolean.TRUE;
      }
      if (text.equalsIgnoreCase("false")) {
        return Boolean.FALSE;
      }
      throw new IllegalArgumentException("Type true or false");
    }
    if (current instanceof Number) {
      try {
        return parseNumber((Number) current, text);
      } catch (NumberFormatException e) {
        String type = current instanceof Float || current instanceof Double ? "a number" : "a whole number";
        throw new IllegalArgumentException("\"" + text + "\" is not " + type + " that fits here");
      }
    }
    if (current == null) {
      throw new IllegalArgumentException("This value is null, so its type is unknown");
    }
    throw new IllegalArgumentException("This value cannot be edited (" + current.getClass().getSimpleName() + ")");
  }

  private static Number parseNumber(Number current, String text) {
    if (current instanceof Integer) {
      return Integer.valueOf(Integer.parseInt(text));
    }
    if (current instanceof Long) {
      return Long.valueOf(Long.parseLong(text));
    }
    if (current instanceof Short) {
      return Short.valueOf(Short.parseShort(text));
    }
    if (current instanceof Byte) {
      return Byte.valueOf(Byte.parseByte(text));
    }
    if (current instanceof Float) {
      float f = Float.parseFloat(text);
      if (Float.isNaN(f) || Float.isInfinite(f)) {
        throw new NumberFormatException(text);
      }
      return Float.valueOf(f);
    }
    double d = Double.parseDouble(text);
    if (Double.isNaN(d) || Double.isInfinite(d)) {
      throw new NumberFormatException(text);
    }
    return Double.valueOf(d);
  }

  /** Removes one pair of surrounding quotes and undoes the escapes of {@link #quote}; unquoted text is kept as is. */
  private static String unquote(String typed) {
    String text = typed.trim();
    if (text.length() < 2 || text.charAt(0) != '"' || text.charAt(text.length() - 1) != '"') {
      return typed;
    }
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 1; i < text.length() - 1; i++) {
      char c = text.charAt(i);
      if (c == '\\' && i + 1 < text.length() - 1) {
        char next = text.charAt(++i);
        out.append(switch (next) {
          case 'n' -> '\n';
          case 'r' -> '\r';
          case 't' -> '\t';
          default -> next;
        });
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /**
   * Returns how many saves the debugger app can pick from, counting {@link Flixel#save} as entry 0.
   *
   * @return The number of listed saves, always at least 1.
   */
  public int getSaveCount() {
    FlixelSave main = Flixel.save;
    int count = 1;
    for (int i = 0; i < extras.getSize(); i++) {
      if (extras.get(i) != main) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns one save of the list.
   *
   * <p>A save that was added and later became {@link Flixel#save} is only listed once, as entry 0.
   *
   * @param index The position in the list. Index 0 is {@link Flixel#save}.
   * @return The save, or {@code null} when the index is out of range or entry 0 is asked for while
   *     {@link Flixel#save} is {@code null}.
   */
  @Nullable
  public FlixelSave getSaveAt(int index) {
    FlixelSave main = Flixel.save;
    if (index == 0) {
      return main;
    }
    int at = 0;
    for (int i = 0; i < extras.getSize(); i++) {
      FlixelSave entry = extras.get(i);
      if (entry != main && ++at == index) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Returns the position of the edited save in the list.
   *
   * @return The index, where 0 means {@link Flixel#save}.
   */
  public int getSelectedIndex() {
    FlixelSave main = Flixel.save;
    if (save == null || save == main) {
      return 0;
    }
    int at = 0;
    for (int i = 0; i < extras.getSize(); i++) {
      FlixelSave entry = extras.get(i);
      if (entry == main) {
        continue;
      }
      at++;
      if (entry == save) {
        return at;
      }
    }
    return 0;
  }

  /**
   * Returns the save that is edited: the selected entry of the list, or {@link Flixel#save} for entry 0.
   *
   * @return The save, or {@code null} when there is none.
   */
  @Nullable
  public FlixelSave getSave() {
    return save != null ? save : Flixel.save;
  }

  /**
   * Edits another save instead of {@link Flixel#save}, for example a second slot kept by the game.
   *
   * <p>The save is {@link #add(FlixelSave) added} to the list when it is not listed yet, then selected, and the panel
   * refreshes at once. Passing {@link Flixel#save} or {@code null} selects entry 0, which keeps following
   * {@link Flixel#save} when the game replaces it.
   *
   * @param save The save to edit, or {@code null} to go back to {@link Flixel#save}.
   */
  public void setSave(@Nullable FlixelSave save) {
    if (save == null || save == Flixel.save) {
      choose(null);
      return;
    }
    add(save);
    choose(save);
  }

  /**
   * Returns how many rows the last refresh produced.
   *
   * @return The row count, at most {@value #MAX_ROWS}.
   */
  public int getRowCount() {
    return rows.count;
  }

  public int getUnsavedCount() {
    return unsavedCount;
  }

  /** One row of the save, or one removed path. Rows are reused between refreshes. */
  private static final class Row {

    long hash;
    final FlixelString path = new FlixelString(32);
    final FlixelString display = new FlixelString(16);
    int type;
    int kind;
    boolean unsaved;
  }

  /** A reusable list of rows. */
  private static final class RowList {

    final FlixelArray<Row> items = new FlixelArray<>();
    int count;
    boolean truncated;

    Row next() {
      if (count == items.getSize()) {
        items.add(new Row());
      }
      return items.get(count++);
    }

    void clear() {
      count = 0;
      truncated = false;
    }
  }
}
