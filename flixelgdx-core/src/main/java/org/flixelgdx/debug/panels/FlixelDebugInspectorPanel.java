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

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.inspector.FlixelDebugSection;
import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugExpander;
import org.flixelgdx.debug.ui.FlixelDebugImage;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugProperties;
import org.flixelgdx.debug.ui.FlixelDebugSlider;
import org.flixelgdx.debug.ui.FlixelDebugStack;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Inspector" panel: the properties of the selected object, grouped into one section per class, with
 * editors for the properties that can be changed, plus a picture of what a selected sprite shows.
 *
 * <p>The sections come from the {@linkplain FlixelDebugManager#inspector inspector feed}, so classes registered with
 * {@code Flixel.debug.inspector.register(...)} show up here on their own. The person can also select the parent of
 * the selection, move the camera to it, clear it, or type a scene path such as {@code state.3.0}. When an AI agent
 * made the selection, a badge says so. A game can replace the panel under the id
 * {@value FlixelDebugDefaultPanels#INSPECTOR}.
 */
public class FlixelDebugInspectorPanel extends FlixelDebugPanel {

  /** The hint shown while nothing is selected. */
  public static final String EMPTY_HINT = "Nothing is selected. Pick an object in the scene or the viewport, use"
      + " Alt + Click in the game, or type a path below.";

  /** Seconds between two reads of the selected object's properties. */
  public static final float REFRESH_INTERVAL = 1f / 30f;

  private final FlixelDebugInspector inspector;
  private final FlixelArray<FlixelDebugSection> sections = new FlixelArray<>();
  private final FlixelDebugProperties grid = new FlixelDebugProperties();
  private final FlixelDebugTextBox path = new FlixelDebugTextBox();
  private final FlixelDebugLabel empty = FlixelDebugUi.hint(EMPTY_HINT);
  private final FlixelDebugLabel agentBadge = new FlixelDebugLabel(" An AI agent selected this object ");
  private final FlixelDebugStack selectionBar;
  private final FlixelDebugExpander preview;
  private final FlixelDebugImage image = new FlixelDebugImage();
  private final FlixelDebugLabel previewStatus = new FlixelDebugLabel();
  private float timer = REFRESH_INTERVAL;
  private int shownId = -1;
  private int previewId = -1;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose inspector feed the panel shows.
   */
  public FlixelDebugInspectorPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.INSPECTOR, "Inspector");
    this.inspector = manager.inspector;
    setDock(FlixelDebugProtocol.DOCK_CENTER);

    agentBadge.setStyle(FlixelDebugProtocol.TONE_SUCCESS | FlixelDebugProtocol.STYLE_BOLD).setVisible(false);
    path.setReadOnly(true).setStyle(FlixelDebugProtocol.STYLE_MONO).setGrow(1f).setName("path");
    selectionBar = FlixelDebugUi.row(path,
        new FlixelDebugButton("Parent", inspector::selectParent).setName("parent"),
        new FlixelDebugButton("Go to", inspector::goTo).setName("goto"),
        new FlixelDebugButton("Clear", inspector::clearSelection).setName("clear"));
    selectionBar.setVisible(false);

    FlixelDebugTextBox pathEntry = FlixelDebugUi.textBox("Select by path (for example state.3.0) and press Enter")
        .setClearOnSubmit(true)
        .setStyle(FlixelDebugProtocol.STYLE_MONO)
        .setName("select")
        .onSubmit(inspector::selectPath);

    grid.setGrow(1f).setName("properties").onEdit(this::edit);

    FlixelDebugSlider zoom = FlixelDebugUi.slider(0.5, 4.0, () -> image.getScale(), v -> image.setScale((float) v));
    zoom.setStep(0.25).setGrow(1f).setName("preview.zoom");
    image.setScale(2f).setName("preview.image");
    previewStatus.setStyle(FlixelDebugProtocol.TONE_MUTED).setWrap(true);
    preview = new FlixelDebugExpander("Preview", FlixelDebugUi.column(
        FlixelDebugUi.row(FlixelDebugUi.label("Zoom"), zoom,
            new FlixelDebugButton("Refresh", this::requestPreview).setName("preview.refresh")),
        previewStatus,
        FlixelDebugUi.scroll(image).setHeight(320f)));
    preview.setExpanded(false).setVisible(false).onChange(open -> {
      if (open) {
        requestPreview();
      }
    });

    inspector.getThumbnails().setListener((id, width, height, png, length) -> {
      if (id == previewId) {
        image.setPng(png, 0, length);
        previewStatus.setText("");
      }
    });

    setContent(FlixelDebugUi.column(empty, agentBadge, selectionBar, pathEntry,
        FlixelDebugUi.scroll(FlixelDebugUi.column(grid, preview)).setGrow(1f)));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    int id = inspector.getSelectedId();
    boolean selected = inspector.getSelected() != null;
    empty.setVisible(!selected);
    selectionBar.setVisible(selected);
    grid.setVisible(selected);
    agentBadge.setVisible(selected && inspector.isAgentSelection());
    boolean hasPreview = selected && inspector.hasPreview();
    preview.setVisible(hasPreview);
    if (id != shownId) {
      shownId = id;
      timer = REFRESH_INTERVAL;
      image.clearPng();
      previewId = -1;
      if (hasPreview && preview.isExpanded()) {
        requestPreview();
      }
    }
    timer += elapsed;
    if (timer < REFRESH_INTERVAL) {
      return;
    }
    timer = 0f;
    path.setText(inspector.getPath());
    grid.beginSections();
    Object current = inspector.getSelected();
    if (current != null) {
      inspector.collectSections(sections);
      int epoch = inspector.getEpoch();
      for (int i = 0, n = sections.getSize(); i < n; i++) {
        FlixelDebugSection section = sections.get(i);
        Object target = section.resolve(current);
        grid.beginSection(section.getDisplayName(), true);
        if (target != null) {
          grid.rows(section, target, epoch);
        }
      }
    }
    grid.endSections();
  }

  @Override
  protected void onOpen() {
    shownId = -1;
    timer = REFRESH_INTERVAL;
  }

  private void edit(int section, int row, FlixelDebugValue value) {
    if (section < 0 || section >= sections.getSize()) {
      return;
    }
    inspector.apply(sections.get(section), row, value);
    // Reads the properties again right away, so the grid shows the value the setter really kept.
    timer = REFRESH_INTERVAL;
    grid.resend(FlixelDebugProtocol.PROP_SECTIONS);
  }

  private void requestPreview() {
    previewId = inspector.getSelectedId();
    if (inspector.requestPreview()) {
      previewStatus.setText("Reading the picture from the GPU...");
    } else {
      previewStatus.setText("The picture cannot be read right now. Try Refresh in a moment.");
    }
  }
}
