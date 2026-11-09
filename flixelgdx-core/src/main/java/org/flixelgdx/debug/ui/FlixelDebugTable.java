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
package org.flixelgdx.debug.ui;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongConsumer;

/**
 * Rows of cells under column headers, such as a list of textures, save values, or log lines.
 *
 * <p>Every row has an id that the game chooses (for example a texture id or a log sequence number). Clicks,
 * double clicks, and menu picks come back with that id, so the game always knows which thing the person meant, no
 * matter how the app sorted or filtered the rows.
 *
 * <h2>Two ways to fill a table</h2>
 *
 * <p><b>Rebuild</b> the rows whenever you like, even every frame. The table compares the new rows with the ones the
 * app already has, byte by byte, and only sends them when something changed:
 *
 * <pre>{@code
 * table.beginRows();
 * for (Texture t : textures) {
 *   table.row(t.id).cell(t.name).cell(t.width + " x " + t.height, t.width * t.height).cell(t.users);
 * }
 * table.endRows();
 * }</pre>
 *
 * <p><b>Append</b> rows for things that only ever grow, such as log lines. Only the new rows travel, and with a
 * {@linkplain #setLimit(int) limit} the oldest rows fall off the top:
 *
 * <pre>{@code
 * table.setLimit(500).setFollow(true);
 * table.appendRow(line.seq, style).cell(line.time).cell(line.message).detail(line.stack);
 * }</pre>
 *
 * <p>A row is finished automatically when the next one starts, so there is no "end row" call to forget. Writing rows
 * does not allocate once the table's buffers are big enough.
 *
 * <h2>What the app does on its own</h2>
 *
 * <p>Clicking a {@linkplain FlixelDebugProtocol#COLUMN_SORTABLE sortable} header sorts the rows, and a
 * {@linkplain #setFilter(FlixelDebugTextBox) filter box} hides rows that do not contain its text. Both happen inside
 * the app without a round trip, because they only change what is shown. The right-click menu always has
 * {@code Copy row}; your own {@linkplain #setMenu(String...) items} come first and are handled by the game.
 */
public class FlixelDebugTable extends FlixelDebugWidget<FlixelDebugTable> {

  private static final int IDLE = 0;
  private static final int BUILDING = 1;
  private static final int APPENDING = 2;

  private long selected = -1L;

  private final FlixelArray<Column> columnList = new FlixelArray<>();
  private final FlixelDebugListBuffer columns = new FlixelDebugListBuffer();
  private final FlixelDebugListBuffer menu = new FlixelDebugListBuffer();
  private final FlixelDebugMessageWriter appended = new FlixelDebugMessageWriter(256);
  private final Row row = new Row();
  private FlixelDebugMessageWriter rows = new FlixelDebugMessageWriter(256);
  private FlixelDebugMessageWriter building = new FlixelDebugMessageWriter(256);
  private FlixelDebugMessageWriter target;
  private int[] rowEnds = new int[32];
  private int[] buildEnds = new int[32];
  private FlixelDebugTextBox filter;
  private LongConsumer onSelect;
  private LongConsumer onActivate;
  private FlixelDebugMenuListener onMenu;
  private int rowCount;
  private int buildCount;
  private int appendedCount;
  private int appendStart;
  private int limit;
  private int sort;
  private int mode = IDLE;

  private boolean follow;

  /** Creates a table with no columns and no rows. */
  public FlixelDebugTable() {
    super(FlixelDebugProtocol.WIDGET_TABLE);
  }

  /**
   * Adds a sortable column that shares the leftover width.
   *
   * @param title The header text.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable column(@NotNull String title) {
    return column(title, 0f, FlixelDebugProtocol.COLUMN_SORTABLE);
  }

  /**
   * Adds a column.
   *
   * @param title The header text.
   * @param width The width in pixels, or {@code 0} to share the leftover width.
   * @param flags {@link FlixelDebugProtocol#COLUMN_SORTABLE}, {@link FlixelDebugProtocol#COLUMN_RIGHT}, and
   *   {@link FlixelDebugProtocol#COLUMN_MONO}, combined with {@code |}.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable column(@NotNull String title, float width, int flags) {
    columnList.add(new Column(title, width, flags));
    FlixelDebugMessageWriter writer = columns.begin();
    for (int i = 0, n = columnList.getSize(); i < n; i++) {
      Column c = columnList.get(i);
      writer.beginList().writeValue(c.title()).writeValue(c.width()).writeValue(c.flags()).endList(3);
    }
    if (columns.end(columnList.getSize())) {
      markDirty(FlixelDebugProtocol.PROP_COLUMNS);
    }
    return this;
  }

  /** Starts rebuilding all rows. Add them with {@link #row(long)}, then call {@link #endRows()}. */
  public void beginRows() {
    finishRow();
    building.clear();
    buildCount = 0;
    mode = BUILDING;
  }

  /**
   * Starts a new row while rebuilding. The previous row is finished automatically.
   *
   * @param id The row's id, which events report.
   * @return The row, to add cells to.
   * @throws IllegalStateException When {@link #beginRows()} was not called first.
   */
  @NotNull
  public Row row(long id) {
    return row(id, 0);
  }

  /**
   * Starts a new row with a style while rebuilding. The previous row is finished automatically.
   *
   * @param id The row's id, which events report.
   * @param style A {@linkplain FlixelDebugWidget#setStyle(int) style} for the whole row, such as
   *   {@link FlixelDebugProtocol#TONE_ERROR}.
   * @return The row, to add cells to.
   * @throws IllegalStateException When {@link #beginRows()} was not called first.
   */
  @NotNull
  public Row row(long id, int style) {
    if (mode != BUILDING) {
      throw new IllegalStateException("Call beginRows() before row(...).");
    }
    finishRow();
    target = building;
    row.start(building, id, style);
    return row;
  }

  /**
   * Finishes rebuilding. The rows are sent only when they differ from the ones the app has.
   *
   * @return {@code true} when the rows changed.
   */
  public boolean endRows() {
    finishRow();
    mode = IDLE;
    if (buildCount == rowCount && building.contentEquals(rows)) {
      return false;
    }
    FlixelDebugMessageWriter swap = rows;
    rows = building;
    building = swap;
    int[] ends = rowEnds;
    rowEnds = buildEnds;
    buildEnds = ends;
    rowCount = buildCount;
    appended.clear();
    appendedCount = 0;
    markDirty(FlixelDebugProtocol.PROP_ITEMS);
    return true;
  }

  /** Removes every row. */
  public void clearRows() {
    beginRows();
    endRows();
  }

  /**
   * Adds a row to the end of the table. Only the new row is sent, and when the table has a {@linkplain #setLimit(int)
   * limit}, the oldest rows are dropped.
   *
   * @param id The row's id, which events report.
   * @param style A style for the whole row, or {@code 0}.
   * @return The row, to add cells to.
   * @throws IllegalStateException While the rows are being rebuilt.
   */
  @NotNull
  public Row appendRow(long id, int style) {
    if (mode == BUILDING) {
      throw new IllegalStateException("Call endRows() before appendRow(...).");
    }
    finishRow();
    mode = APPENDING;
    // While the whole list is going to be sent anyway (or nobody is listening yet), the row only goes into the
    // stored rows; otherwise it also waits in the append buffer for the next patch.
    target = getPanel() == null || isDirty(FlixelDebugProtocol.PROP_ITEMS) ? rows : appended;
    appendStart = appended.getSize();
    row.start(target, id, style);
    return row;
  }

  /**
   * Replaces the items of the right-click menu that every row gets.
   *
   * @param items The item texts, in order. Pass none to remove the menu.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setMenu(@NotNull String... items) {
    FlixelDebugMessageWriter writer = menu.begin();
    for (String item : items) {
      writer.writeValue(item);
    }
    if (menu.end(items.length)) {
      markDirty(FlixelDebugProtocol.PROP_MENU);
    }
    return this;
  }

  /**
   * Sets what runs when the person selects a row or clears the selection.
   *
   * @param onSelect Receives the row id, or {@code -1} when nothing is selected; {@code null} for nothing.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable onSelect(@Nullable LongConsumer onSelect) {
    this.onSelect = onSelect;
    return this;
  }

  /**
   * Sets what runs when the person double-clicks a row or presses Enter on it.
   *
   * @param onActivate Receives the row id, or {@code null} for nothing.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable onActivate(@Nullable LongConsumer onActivate) {
    this.onActivate = onActivate;
    return this;
  }

  /**
   * Sets what runs when the person picks an item of the right-click menu.
   *
   * @param onMenu The listener, or {@code null} for nothing.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable onMenu(@Nullable FlixelDebugMenuListener onMenu) {
    this.onMenu = onMenu;
    return this;
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_ITEMS);
    if (columns.getCount() > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_COLUMNS);
    }
    if (selected != -1L) {
      mask |= bit(FlixelDebugProtocol.PROP_SELECTED);
    }
    if (menu.getCount() > 0) {
      mask |= bit(FlixelDebugProtocol.PROP_MENU);
    }
    if (limit != 0) {
      mask |= bit(FlixelDebugProtocol.PROP_LIMIT);
    }
    if (follow) {
      mask |= bit(FlixelDebugProtocol.PROP_FOLLOW);
    }
    if (filter != null) {
      mask |= bit(FlixelDebugProtocol.PROP_FILTER);
    }
    if (sort != 0) {
      mask |= bit(FlixelDebugProtocol.PROP_SORT);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_ITEMS -> {
        finishRow();
        writer.beginList().writeRaw(rows).endList(rowCount);
      }
      case FlixelDebugProtocol.PROP_COLUMNS -> columns.write(writer);
      case FlixelDebugProtocol.PROP_SELECTED -> writer.writeValue(selected);
      case FlixelDebugProtocol.PROP_MENU -> menu.write(writer);
      case FlixelDebugProtocol.PROP_LIMIT -> writer.writeValue(limit);
      case FlixelDebugProtocol.PROP_FOLLOW -> writer.writeValue(follow);
      case FlixelDebugProtocol.PROP_FILTER -> writer.writeValue(filter != null ? filter.getId() : 0);
      case FlixelDebugProtocol.PROP_SORT -> writer.writeValue(sort);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected boolean hasPending() {
    finishRow();
    return appendedCount > 0;
  }

  @Override
  protected int writeAppends(@NotNull FlixelDebugMessageWriter writer) {
    finishRow();
    if (appendedCount == 0) {
      return 0;
    }
    writer.writeI32(getId()).writeU8(FlixelDebugProtocol.PATCH_APPEND).writeU8(FlixelDebugProtocol.PROP_ITEMS);
    writer.beginList().writeRaw(appended).endList(appendedCount);
    appended.clear();
    appendedCount = 0;
    return 1;
  }

  @Override
  protected void clearPending() {
    finishRow();
    appended.clear();
    appendedCount = 0;
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    switch (event) {
      case FlixelDebugProtocol.EVENT_SELECT -> {
        FlixelDebugValue value = args.next();
        selected = value != null && value.isNumber() ? value.asLong() : -1L;
        if (onSelect != null) {
          onSelect.accept(selected);
        }
      }
      case FlixelDebugProtocol.EVENT_ACTIVATE -> {
        long id = args.nextLong(-1L);
        if (onActivate != null) {
          onActivate.accept(id);
        }
      }
      case FlixelDebugProtocol.EVENT_MENU -> {
        long id = args.nextLong(-1L);
        int item = args.nextInt(-1);
        if (onMenu != null && item >= 0 && item < menu.getCount()) {
          onMenu.onMenu(id, item);
        }
      }
      default -> {
      }
    }
  }

  private void finishRow() {
    if (!row.isOpen()) {
      return;
    }
    row.close();
    if (target == building) {
      buildEnds = grow(buildEnds, buildCount);
      buildEnds[buildCount++] = building.getSize();
      return;
    }
    if (target == appended) {
      rows.writeRaw(appended, appendStart, appended.getSize());
      appendedCount++;
      markPending();
    }
    rowEnds = grow(rowEnds, rowCount);
    rowEnds[rowCount++] = rows.getSize();
    if (limit > 0 && rowCount > limit) {
      dropOldest(rowCount - limit);
    }
    if (limit > 0 && appendedCount > limit) {
      // So many rows piled up since the last patch (for example while the panel was closed) that sending the whole
      // list is cheaper than sending them all as appends.
      appended.clear();
      appendedCount = 0;
      markDirty(FlixelDebugProtocol.PROP_ITEMS);
    }
    mode = IDLE;
  }

  private void dropOldest(int count) {
    int bytes = rowEnds[count - 1];
    rows.dropFront(bytes);
    for (int i = count; i < rowCount; i++) {
      rowEnds[i - count] = rowEnds[i] - bytes;
    }
    rowCount -= count;
  }

  private static int[] grow(int[] array, int used) {
    if (used < array.length) {
      return array;
    }
    int[] bigger = new int[array.length * 2];
    System.arraycopy(array, 0, bigger, 0, used);
    return bigger;
  }

  /**
   * Returns how many rows the table holds.
   *
   * @return The row count.
   */
  public int getRowCount() {
    finishRow();
    return rowCount;
  }

  public long getSelected() {
    return selected;
  }

  /**
   * Selects a row, or clears the selection.
   *
   * @param selected The row id, or {@code -1} for none.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setSelected(long selected) {
    if (this.selected != selected) {
      this.selected = selected;
      markDirty(FlixelDebugProtocol.PROP_SELECTED);
    }
    return this;
  }

  public int getLimit() {
    return limit;
  }

  /**
   * Sets the most rows the table keeps when rows are appended. The oldest rows are dropped first.
   *
   * @param limit The most rows, or {@code 0} for no limit.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setLimit(int limit) {
    if (this.limit != limit) {
      this.limit = limit;
      markDirty(FlixelDebugProtocol.PROP_LIMIT);
    }
    return this;
  }

  public boolean isFollow() {
    return follow;
  }

  /**
   * Sets whether the app keeps the newest row in view after rows are appended, like a terminal. The app stops
   * following while the person scrolls up to read older rows.
   *
   * @param follow {@code true} to follow new rows.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setFollow(boolean follow) {
    if (this.follow != follow) {
      this.follow = follow;
      markDirty(FlixelDebugProtocol.PROP_FOLLOW);
    }
    return this;
  }

  @Nullable
  public FlixelDebugTextBox getFilter() {
    return filter;
  }

  /**
   * Sets a text box whose text filters the rows. The app hides every row in which no cell contains the text,
   * ignoring case. The box must be in the same panel.
   *
   * @param filter The text box, or {@code null} for no filter.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setFilter(@Nullable FlixelDebugTextBox filter) {
    if (this.filter != filter) {
      this.filter = filter;
      markDirty(FlixelDebugProtocol.PROP_FILTER);
    }
    return this;
  }

  public int getSort() {
    return sort;
  }

  /**
   * Sets the column the app sorts the rows by at first.
   *
   * @param sort The column, counting from 1; a negative number sorts that column descending; {@code 0} keeps the
   *   game's order.
   * @return This table, for chaining.
   */
  @NotNull
  public FlixelDebugTable setSort(int sort) {
    if (this.sort != sort) {
      this.sort = sort;
      markDirty(FlixelDebugProtocol.PROP_SORT);
    }
    return this;
  }

  /**
   * One row being written. Add cells in column order; the row is finished when the next row starts.
   *
   * <p>The same object is reused for every row, so do not keep it.
   */
  public static final class Row {

    private final FlixelString detail = new FlixelString(0);
    private FlixelDebugMessageWriter out;
    private int cells;

    private boolean open;

    private Row() {}

    /**
     * Adds a text cell.
     *
     * @param text The text; it is copied.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(@Nullable CharSequence text) {
      out.writeValue(text);
      cells++;
      return this;
    }

    /**
     * Adds a cell that shows text but sorts by a number, for example {@code "64 x 64"} sorted by its pixel count.
     *
     * @param text The text to show.
     * @param sortKey The number to sort by.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(@Nullable CharSequence text, double sortKey) {
      out.beginList().writeValue(text).writeValue(sortKey).endList(2);
      cells++;
      return this;
    }

    /**
     * Adds a whole-number cell.
     *
     * @param value The number.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(int value) {
      out.writeValue(value);
      cells++;
      return this;
    }

    /**
     * Adds a whole-number cell.
     *
     * @param value The number.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(long value) {
      out.writeValue(value);
      cells++;
      return this;
    }

    /**
     * Adds a number cell.
     *
     * @param value The number.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(float value) {
      out.writeValue(value);
      cells++;
      return this;
    }

    /**
     * Adds a number cell.
     *
     * @param value The number.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(double value) {
      out.writeValue(value);
      cells++;
      return this;
    }

    /**
     * Adds a cell holding one value that was written ahead of time, such as a value read by a feed.
     *
     * <p>The writer must hold exactly one value. Its bytes are copied, so it can be reused for the next cell.
     *
     * @param value The writer holding the value.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(@NotNull FlixelDebugMessageWriter value) {
      out.writeRaw(value);
      cells++;
      return this;
    }

    /**
     * Adds a true or false cell.
     *
     * @param value The flag.
     * @return This row, for chaining.
     */
    @NotNull
    public Row cell(boolean value) {
      out.writeValue(value);
      cells++;
      return this;
    }

    /**
     * Sets extra text for the row (such as a stack trace) that the app shows as a tooltip and can copy.
     *
     * @param text The text; it is copied.
     * @return This row, for chaining.
     */
    @NotNull
    public Row detail(@Nullable CharSequence text) {
      if (text == null) {
        detail.clear();
      } else {
        detail.set(text);
      }
      return this;
    }

    void start(FlixelDebugMessageWriter out, long id, int style) {
      this.out = out;
      cells = 0;
      detail.clear();
      open = true;
      out.beginList().writeValue(id).writeValue(style).beginList();
    }

    void close() {
      out.endList(cells);
      out.writeValue(detail);
      out.endList(4);
      open = false;
    }

    boolean isOpen() {
      return open;
    }
  }

  private record Column(String title, float width, int flags) {
  }
}
