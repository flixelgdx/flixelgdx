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
package org.flixelgdx.debug.panels;

import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugSave;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugDropdown;
import org.flixelgdx.debug.ui.FlixelDebugGrid;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugStack;
import org.flixelgdx.debug.ui.FlixelDebugTable;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Save" panel: a save editor for {@code Flixel.save} and the other saves a game lists with
 * {@code Flixel.debug.save.add(...)}.
 *
 * <p>Every plain value of the save is one row, named by its dotted path such as {@code player.coins}. Rows that differ
 * from what is in storage are highlighted, and removed paths are listed in a muted tone. Clicking a row copies its
 * path and value into the fields below, where the person can change the value and press Set, remove the key, write the
 * save, or erase everything (after a confirmation). All of it runs in the game through the
 * {@linkplain FlixelDebugManager#save save feed}, and nothing reaches storage until "Save now". A game can replace the
 * panel under the id {@value FlixelDebugDefaultPanels#SAVE}.
 */
public class FlixelDebugSavePanel extends FlixelDebugPanel {

  /** The row ids of removed paths start here, so they never collide with the ids of present rows. */
  private static final long REMOVED_BASE = 1L << 32;

  private long seenRevision = -1L;

  private final FlixelDebugSave save;
  private final FlixelDebugDropdown saves = new FlixelDebugDropdown();
  private final FlixelDebugStack savePicker;
  private final FlixelDebugLabel status = new FlixelDebugLabel();
  private final FlixelDebugTable table = new FlixelDebugTable();
  private final FlixelDebugTextBox path = new FlixelDebugTextBox();
  private final FlixelDebugTextBox value = new FlixelDebugTextBox();
  private final FlixelString scratch = new FlixelString(64);

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose save feed the panel edits.
   */
  public FlixelDebugSavePanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.SAVE, "Save");
    this.save = manager.save;
    setDock(FlixelDebugProtocol.DOCK_CENTER);

    saves.onChange(save::select).setGrow(1f).setName("save").setTooltip("The save this panel shows and edits");
    savePicker = FlixelDebugUi.row(FlixelDebugUi.label("Save"), saves);
    status.setStyle(FlixelDebugProtocol.STYLE_MONO).setName("status");

    FlixelDebugTextBox filter = FlixelDebugUi.textBox("Filter keys").setName("filter");
    table.column("Path", 0f, FlixelDebugProtocol.COLUMN_SORTABLE | FlixelDebugProtocol.COLUMN_MONO)
        .column("Value", 0f, FlixelDebugProtocol.COLUMN_SORTABLE | FlixelDebugProtocol.COLUMN_MONO)
        .setFilter(filter)
        .setMenu("Reset key")
        .setGrow(1f)
        .setName("rows");
    table.onSelect(this::pick);
    table.onMenu((id, item) -> {
      if (item == 0 && id >= 0 && id < save.getRowCount()) {
        save.resetKey(save.getRowPath((int) id));
      }
    });

    // Both fields are live, so the game always knows what the person typed when they press a button.
    path.setLive(true).setStyle(FlixelDebugProtocol.STYLE_MONO).setName("path");
    value.setLive(true).setStyle(FlixelDebugProtocol.STYLE_MONO).setName("value").onSubmit(text -> set());
    FlixelDebugGrid fields = new FlixelDebugGrid("auto", "*").add(
        FlixelDebugUi.label("Path"), path,
        FlixelDebugUi.label("Value"), value);

    setContent(FlixelDebugUi.column(
        savePicker,
        status,
        filter,
        table,
        fields,
        FlixelDebugUi.wrap(
            new FlixelDebugButton("Set", this::set).setName("set")
                .setTooltip("Changes the value at the path and keeps its type"),
            new FlixelDebugButton("Reset key", () -> save.resetKey(path.getText())).setName("reset")
                .setTooltip("Removes the value (or a whole branch) from the data"),
            new FlixelDebugButton("Save now", save::saveNow).setName("savenow")
                .setStyle(FlixelDebugProtocol.TONE_ACCENT),
            new FlixelDebugButton("Reset all", save::resetAll).setName("resetall")
                .setStyle(FlixelDebugProtocol.TONE_ERROR)
                .setConfirm("Erase all data of this save, in memory and in storage? This cannot be undone."))));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    long revision = save.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    int count = save.getSaveCount();
    FlixelDebugMessageWriter items = saves.beginItems();
    for (int i = 0; i < count; i++) {
      items.writeValue(save.getSaveLabel(i));
    }
    saves.endItems(count);
    saves.setSelected(save.getSelectedIndex());
    savePicker.setVisible(count > 1);
    status.setText(save.getStatus());
    status.setStyle(FlixelDebugProtocol.STYLE_MONO
        | (save.isStatusError() ? FlixelDebugProtocol.TONE_ERROR : FlixelDebugProtocol.TONE_NORMAL));

    table.beginRows();
    for (int i = 0, n = save.getRowCount(); i < n; i++) {
      int style = save.isRowUnsaved(i) ? FlixelDebugProtocol.TONE_ACCENT | FlixelDebugProtocol.STYLE_BOLD : 0;
      table.row(i, style).cell(save.getRowPath(i)).cell(save.getRowDisplay(i));
    }
    for (int i = 0, n = save.getRemovedCount(); i < n; i++) {
      table.row(REMOVED_BASE + i, FlixelDebugProtocol.TONE_MUTED | FlixelDebugProtocol.STYLE_ITALIC)
          .cell(save.getRemovedPath(i))
          .cell("(removed)");
    }
    table.endRows();
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }

  private void pick(long id) {
    if (id >= REMOVED_BASE) {
      int index = (int) (id - REMOVED_BASE);
      if (index < save.getRemovedCount()) {
        path.setText(save.getRemovedPath(index));
        value.setText("");
      }
      return;
    }
    if (id < 0 || id >= save.getRowCount()) {
      return;
    }
    path.setText(save.getRowPath((int) id));
    value.setText(save.getRowDisplay((int) id));
  }

  private void set() {
    scratch.set(path.getText());
    save.set(scratch, value.getText());
  }
}
