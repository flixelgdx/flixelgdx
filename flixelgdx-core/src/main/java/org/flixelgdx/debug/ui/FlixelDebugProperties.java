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
import org.flixelgdx.debug.inspector.FlixelDebugSection;
import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.inspector.FlixelPropertyBuilder;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Named values in foldable sections, each with an editor that fits its type, like the inspector of a game engine.
 *
 * <p>A property grid is a form the game fills in: one row per value, a checkbox for a boolean, a number field for a
 * number, and a text field for text. Rows that are read-only are just shown. When the person edits a row, the new
 * value comes back to the game.
 *
 * <h2>Live rows from getters</h2>
 *
 * <p>The easiest way is to describe each section once with getters (and setters for editable rows). The grid reads
 * the getters every frame while the panel is open and sends only the values that changed:
 *
 * <pre>{@code
 * FlixelDebugProperties grid = new FlixelDebugProperties()
 *     .section("Player", s -> s
 *         .floatProp("X", player::getX, player::setX)
 *         .intProp("Health", player::getHealth, player::setHealth))
 *     .section("World", s -> s
 *         .intProp("Enemies", enemies::countLiving));
 * }</pre>
 *
 * <h2>Rows written by hand</h2>
 *
 * <p>For rows that change shape (such as the properties of whatever object is selected), write the sections
 * yourself and handle edits with {@link #onEdit(FlixelDebugEditListener)}:
 *
 * <pre>{@code
 * grid.beginSections();
 * grid.beginSection("Stats", true);
 * grid.row("Kills", kills, false);
 * grid.row("Name", name, true);
 * grid.endSections();
 * }</pre>
 *
 * <p>Either way, writing rows does not allocate once the grid's buffers are big enough, and the rows are only sent
 * when their bytes changed.
 */
public class FlixelDebugProperties extends FlixelDebugWidget<FlixelDebugProperties> implements FlixelDebugBound {

  private final FlixelDebugListBuffer sections = new FlixelDebugListBuffer();
  private final FlixelArray<FlixelPropertyBuilder> bound = new FlixelArray<>();
  private final FlixelArray<String> boundTitles = new FlixelArray<>();
  private FlixelDebugMessageWriter builder;
  private FlixelDebugEditListener onEdit;
  private int sectionCount;
  private int rowCount;

  private boolean building;
  private boolean inSection;

  /** Creates an empty property grid. */
  public FlixelDebugProperties() {
    super(FlixelDebugProtocol.WIDGET_PROPERTIES);
  }

  /**
   * Adds a section of live rows described with getters and setters.
   *
   * @param title The section's title.
   * @param build Adds the rows, for example {@code s -> s.intProp("Health", player::getHealth)}.
   * @return This grid, for chaining.
   */
  @NotNull
  public FlixelDebugProperties section(@NotNull String title, @NotNull Consumer<FlixelPropertyBuilder> build) {
    FlixelPropertyBuilder rows = new FlixelPropertyBuilder();
    build.accept(rows);
    bound.add(rows);
    boundTitles.add(title);
    refresh();
    return this;
  }

  /** Makes rows marked {@code once()} in the live sections read their getters again. */
  public void reread() {
    for (int i = 0, n = bound.getSize(); i < n; i++) {
      bound.get(i).refresh();
    }
    refresh();
  }

  @Override
  public void refresh() {
    if (bound.getSize() == 0) {
      return;
    }
    beginSections();
    for (int i = 0, n = bound.getSize(); i < n; i++) {
      beginSection(boundTitles.get(i), true);
      rowCount += bound.get(i).writeItems(builder);
    }
    endSections();
  }

  /** Starts writing all sections by hand. Add them with {@link #beginSection(CharSequence, boolean)}. */
  public void beginSections() {
    builder = sections.begin();
    sectionCount = 0;
    rowCount = 0;
    inSection = false;
    building = true;
  }

  /**
   * Starts a section. The previous section is finished automatically.
   *
   * @param title The section's title.
   * @param expanded Whether the section is open the first time the app shows it.
   * @throws IllegalStateException When {@link #beginSections()} was not called first.
   */
  public void beginSection(@NotNull CharSequence title, boolean expanded) {
    if (!building) {
      throw new IllegalStateException("Call beginSections() before beginSection(...).");
    }
    finishSection();
    builder.beginList().writeValue(title).writeValue(expanded).beginList();
    rowCount = 0;
    inSection = true;
    sectionCount++;
  }

  /**
   * Adds a true or false row to the current section.
   *
   * @param name The row's name.
   * @param value The value.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, boolean value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a whole-number row to the current section.
   *
   * @param name The row's name.
   * @param value The value.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, int value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a whole-number row to the current section.
   *
   * @param name The row's name.
   * @param value The value.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, long value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a number row to the current section.
   *
   * @param name The row's name.
   * @param value The value.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, float value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a number row to the current section.
   *
   * @param name The row's name.
   * @param value The value.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, double value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a text row to the current section.
   *
   * @param name The row's name.
   * @param value The text; it is copied.
   * @param editable Whether the person may change it.
   */
  public void row(@NotNull CharSequence name, @Nullable CharSequence value, boolean editable) {
    openRow(name).writeValue(value);
    closeRow(editable);
  }

  /**
   * Adds a row that shows an error message in the error tone, for a value that could not be read.
   *
   * @param name The row's name.
   * @param message The error message.
   */
  public void errorRow(@NotNull CharSequence name, @Nullable CharSequence message) {
    openRow(name).writeErrorValue(message);
    closeRow(false);
  }

  /**
   * Adds the rows of an inspector section, read from an object, to the current section.
   *
   * @param section The inspector section that describes the rows.
   * @param target The object to read from.
   * @param epoch The read epoch for rows marked {@code once()}.
   */
  public void rows(@NotNull FlixelDebugSection section, @NotNull Object target, int epoch) {
    if (!inSection) {
      throw new IllegalStateException("Call beginSection(...) before adding rows.");
    }
    rowCount += section.writeListItems(builder, target, epoch);
  }

  /**
   * Returns the writer of the current section's rows, for code that writes rows on its own (such as a feed). Write
   * each row as a {@code LIST [STRING name, value, BOOL editable]} value, then report how many with
   * {@link #addRawRows(int)}.
   *
   * @return The writer, positioned inside the current section's rows.
   * @throws IllegalStateException When no section is open.
   */
  @NotNull
  public FlixelDebugMessageWriter getRowWriter() {
    if (!inSection) {
      throw new IllegalStateException("Call beginSection(...) before writing rows.");
    }
    return builder;
  }

  /**
   * Counts rows that were written straight into {@link #getRowWriter()}.
   *
   * @param count How many rows were written.
   */
  public void addRawRows(int count) {
    rowCount += count;
  }

  /**
   * Finishes writing the sections. They are only sent when they changed.
   *
   * @return {@code true} when the sections changed.
   */
  public boolean endSections() {
    finishSection();
    building = false;
    if (sections.end(sectionCount)) {
      markDirty(FlixelDebugProtocol.PROP_SECTIONS);
      return true;
    }
    return false;
  }

  /**
   * Sets what runs when the person edits a row that was written by hand. Rows of live sections apply their setters
   * on their own.
   *
   * @param onEdit The listener, or {@code null} for nothing.
   * @return This grid, for chaining.
   */
  @NotNull
  public FlixelDebugProperties onEdit(@Nullable FlixelDebugEditListener onEdit) {
    this.onEdit = onEdit;
    return this;
  }

  @Override
  protected long getPropMask() {
    return super.getPropMask() | bit(FlixelDebugProtocol.PROP_SECTIONS);
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    if (key == FlixelDebugProtocol.PROP_SECTIONS) {
      sections.write(writer);
    } else {
      super.writeProp(writer, key);
    }
  }

  @Override
  protected void handleEvent(int event, @NotNull FlixelDebugEventArgs args) {
    if (event != FlixelDebugProtocol.EVENT_EDIT) {
      return;
    }
    int section = args.nextInt(-1);
    int row = args.nextInt(-1);
    FlixelDebugValue value = args.next();
    if (section < 0 || row < 0 || value == null) {
      return;
    }
    if (bound.getSize() > 0) {
      if (section < bound.getSize()) {
        bound.get(section).apply(row, value);
      }
      // Sends the value the setter really kept, which may differ from what the person typed.
      resend(FlixelDebugProtocol.PROP_SECTIONS);
      refresh();
    } else if (onEdit != null) {
      onEdit.onEdit(section, row, value);
    }
  }

  private FlixelDebugMessageWriter openRow(CharSequence name) {
    if (!inSection) {
      throw new IllegalStateException("Call beginSection(...) before adding rows.");
    }
    rowCount++;
    return builder.beginList().writeValue(name);
  }

  private void closeRow(boolean editable) {
    builder.writeValue(editable).endList(3);
  }

  private void finishSection() {
    if (inSection) {
      builder.endList(rowCount).endList(3);
      inSection = false;
    }
  }

  /**
   * Returns how many sections the grid holds.
   *
   * @return The count.
   */
  public int getSectionCount() {
    return sections.getCount();
  }
}
