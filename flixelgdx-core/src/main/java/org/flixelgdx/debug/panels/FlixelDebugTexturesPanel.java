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
import org.flixelgdx.debug.feeds.FlixelDebugTextures;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugImage;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugSlider;
import org.flixelgdx.debug.ui.FlixelDebugTable;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Textures" panel: every loaded texture with its size, format, GPU memory, and users, plus a preview of
 * the selected one.
 *
 * <p>The list comes from the {@linkplain FlixelDebugManager#textures textures feed}, which rebuilds it about once a
 * second. The person can sort by any column, filter by name, and step through the textures with the arrow buttons;
 * selecting a texture reads its pixels back from the GPU for the preview. Textures loaded from the same file more
 * than once are marked as duplicates. A game can replace the panel under the id
 * {@value FlixelDebugDefaultPanels#TEXTURES}.
 */
public class FlixelDebugTexturesPanel extends FlixelDebugPanel {

  private static final float BYTES_PER_MB = 1024f * 1024f;

  private long seenRevision = -1L;

  private final FlixelDebugTextures textures;
  private final FlixelDebugTable table = new FlixelDebugTable();
  private final FlixelDebugLabel count = value();
  private final FlixelDebugLabel memory = value();
  private final FlixelDebugLabel duplicates = value();
  private final FlixelDebugLabel page = value();
  private final FlixelDebugImage image = new FlixelDebugImage();
  private final FlixelDebugLabel status = new FlixelDebugLabel("Select a texture to see it.");
  private final FlixelString text = new FlixelString(32);
  private int previewId = -1;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose textures feed the panel shows.
   */
  public FlixelDebugTexturesPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.TEXTURES, "Textures");
    this.textures = manager.textures;
    setDock(FlixelDebugProtocol.DOCK_CENTER);

    FlixelDebugTextBox filter = FlixelDebugUi.textBox("Filter by name").setName("filter");
    int sortable = FlixelDebugProtocol.COLUMN_SORTABLE;
    int number = FlixelDebugProtocol.COLUMN_SORTABLE | FlixelDebugProtocol.COLUMN_RIGHT;
    table.column("Id", 56f, number)
        .column("Name", 0f, sortable)
        .column("Size", 90f, number)
        .column("Format", 80f, sortable)
        .column("GPU MB", 80f, number)
        .column("Users", 60f, number)
        .column("Duplicate", 90f, sortable)
        .setFilter(filter)
        .setSort(-5)
        .setGrow(1f)
        .setName("textures");
    table.onSelect(this::preview);

    status.setStyle(FlixelDebugProtocol.TONE_MUTED).setWrap(true);
    image.setScale(1f).setName("preview");
    FlixelDebugSlider zoom = FlixelDebugUi.slider(0.5, 4.0, () -> image.getScale(), v -> image.setScale((float) v));
    zoom.setStep(0.25).setGrow(1f).setName("zoom");

    textures.getThumbnails().setListener((id, width, height, png, length) -> {
      if (id == previewId) {
        image.setPng(png, 0, length);
        status.setText(text.setLength(0).concat(width).concat(" x ").concat(height).concat(" preview"));
      }
    });

    setContent(FlixelDebugUi.column(
        FlixelDebugUi.row(count, memory, duplicates).setSpacing(16f),
        filter,
        table,
        FlixelDebugUi.separator(),
        FlixelDebugUi.row(
            new FlixelDebugButton("<", () -> step(-1)).setName("previous"),
            page,
            new FlixelDebugButton(">", () -> step(1)).setName("next"),
            FlixelDebugUi.label("Zoom"),
            zoom),
        status,
        FlixelDebugUi.scroll(image).setHeight(180f)));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    long revision = textures.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    int n = textures.getTextureCount();
    count.setText(text.setLength(0).concat(n).concat(n == 1 ? " texture" : " textures"));
    memory.setText(
        text.setLength(0).concatFloatRounded(textures.getTotalBytes() / BYTES_PER_MB, 1).concat(" MB on the GPU"));
    int dupes = textures.getDuplicateCount();
    duplicates.setText(text.setLength(0).concat(dupes).concat(dupes == 1 ? " duplicate" : " duplicates"));
    duplicates.setStyle(FlixelDebugProtocol.STYLE_MONO
        | (dupes > 0 ? FlixelDebugProtocol.TONE_WARNING : FlixelDebugProtocol.TONE_NORMAL));
    table.beginRows();
    for (int i = 0; i < n; i++) {
      boolean duplicate = textures.isDuplicate(i);
      int width = textures.getWidth(i);
      int height = textures.getHeight(i);
      text.setLength(0).concat(width).concat(" x ").concat(height);
      FlixelDebugTable.Row row = table.row(textures.getId(i), duplicate ? FlixelDebugProtocol.TONE_WARNING : 0)
          .cell(textures.getId(i))
          .cell(textures.getName(i))
          .cell(text, (double) width * height)
          .cell(textures.getFormat(i));
      text.setLength(0).concatFloatRounded(textures.getBytes(i) / BYTES_PER_MB, 2);
      row.cell(text, (double) textures.getBytes(i))
          .cell(textures.getUsers(i))
          .cell(duplicate ? "duplicate" : "");
    }
    table.endRows();
    updatePage();
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }

  private void preview(long id) {
    previewId = (int) id;
    image.clearPng();
    updatePage();
    if (id < 0) {
      status.setText("Select a texture to see it.");
    } else if (textures.requestPreview((int) id)) {
      status.setText("Reading the picture from the GPU...");
    } else {
      status.setText("The picture cannot be read right now. Pick the texture again in a moment.");
    }
  }

  private void step(int direction) {
    int n = textures.getTextureCount();
    if (n == 0) {
      return;
    }
    int index = indexOf(previewId);
    int next = index < 0 ? 0 : (index + direction + n) % n;
    int id = textures.getId(next);
    table.setSelected(id);
    preview(id);
  }

  private void updatePage() {
    int index = indexOf(previewId);
    int n = textures.getTextureCount();
    page.setText(text.setLength(0).concat(index < 0 ? 0 : index + 1).concat(" / ").concat(n));
  }

  private int indexOf(int id) {
    for (int i = 0, n = textures.getTextureCount(); i < n; i++) {
      if (textures.getId(i) == id) {
        return i;
      }
    }
    return -1;
  }

  private static FlixelDebugLabel value() {
    return new FlixelDebugLabel().setStyle(FlixelDebugProtocol.STYLE_MONO);
  }
}
