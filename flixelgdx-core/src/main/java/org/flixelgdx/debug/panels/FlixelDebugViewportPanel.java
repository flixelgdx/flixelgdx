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
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.feeds.FlixelDebugControls;
import org.flixelgdx.debug.feeds.FlixelDebugViewport;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugCanvas;
import org.flixelgdx.debug.ui.FlixelDebugDropdown;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugToggle;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Viewport" panel: a live map of every object a camera draws, where the person can click an object to
 * select it, drag it to move it, drag with the right button to pan the camera, and turn the wheel to zoom.
 *
 * <p>It works without the device's keyboard and mouse, which a phone does not have. The boxes come from the
 * {@linkplain FlixelDebugManager#viewport viewport feed}; the selected object is outlined, groups are dashed, and
 * inactive objects are dimmed. Panning or zooming turns the free camera on, and "Reset camera" gives the camera back
 * to the game exactly as it was. A game can replace the panel under the id {@value FlixelDebugDefaultPanels#VIEWPORT}.
 */
public class FlixelDebugViewportPanel extends FlixelDebugPanel {

  /** The help line under the map. */
  public static final String HELP = "Click an object to select it and drag it to move it. Drag with the right mouse"
      + " button to pan and turn the wheel to zoom; both fly the free camera.";

  private long seenRevision = -1L;

  private final FlixelDebugViewport viewport;
  private final FlixelDebugControls controls;
  private final FlixelDebugObjectIds ids;
  private final FlixelDebugCanvas canvas = new FlixelDebugCanvas();
  private final FlixelDebugDropdown cameras = new FlixelDebugDropdown();
  private final FlixelDebugLabel freeCamBadge = new FlixelDebugLabel(" Free cam ");
  private final FlixelDebugButton pause = new FlixelDebugButton("Pause");
  private final FlixelDebugLabel readout = new FlixelDebugLabel();
  private final FlixelDebugLabel noCamera = FlixelDebugUi.hint("Connect a game to see what its camera shows.");
  private final FlixelString text = new FlixelString(48);
  private final FlixelString label = new FlixelString(32);
  private int shownCameraCount = -1;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose viewport feed the panel shows.
   */
  public FlixelDebugViewportPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.VIEWPORT, "Viewport");
    this.viewport = manager.viewport;
    this.controls = manager.controls;
    this.ids = manager.ids;
    setDock(FlixelDebugProtocol.DOCK_CENTER);

    cameras.onChange(viewport::setCameraIndex).setName("camera")
        .setTooltip("The camera the map shows and the camera buttons fly");
    freeCamBadge.setStyle(FlixelDebugProtocol.TONE_ACCENT | FlixelDebugProtocol.STYLE_BOLD)
        .setTooltip("The debugger flies this camera. Reset camera gives it back.");
    pause.onClick(() -> controls.setPaused(!controls.isPaused())).setName("pause");
    readout.setStyle(FlixelDebugProtocol.TONE_MUTED | FlixelDebugProtocol.STYLE_MONO);
    canvas.setInteract(FlixelDebugProtocol.INTERACT_PICK | FlixelDebugProtocol.INTERACT_DRAG
        | FlixelDebugProtocol.INTERACT_PAN | FlixelDebugProtocol.INTERACT_ZOOM)
        .setGrow(1f)
        .setName("map")
        .setListener(new FlixelDebugCanvas.Listener() {
          @Override
          public void onPick(float x, float y) {
            viewport.pick(x, y);
          }

          @Override
          public void onDrag(long id, float dx, float dy) {
            viewport.move((int) id, dx, dy);
          }

          @Override
          public void onPan(float dx, float dy) {
            // The person drags the world, so the camera moves the other way.
            viewport.pan(-dx, -dy);
          }

          @Override
          public void onZoom(float factor, float x, float y) {
            viewport.zoom(factor, x, y);
          }
        });

    setContent(FlixelDebugUi.column(
        FlixelDebugUi.wrap(
            cameras,
            freeCamBadge,
            new FlixelDebugButton("Reset camera", viewport::resetCamera).setName("reset")
                .setTooltip("Turn the free camera off and restore the camera exactly"),
            pause,
            new FlixelDebugButton("Step", controls::stepFrame).setName("step")
                .setTooltip("Advance one frame while paused"),
            new FlixelDebugToggle("Hitboxes", controls::isHitboxes, controls::setHitboxes).setName("hitboxes"),
            readout),
        noCamera,
        canvas,
        FlixelDebugUi.hint(HELP)));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    pause.setText(controls.isPaused() ? "Resume" : "Pause");
    long revision = viewport.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    int count = viewport.getCameraCount();
    noCamera.setVisible(count == 0);
    canvas.setVisible(count > 0);
    if (count != shownCameraCount) {
      shownCameraCount = count;
      FlixelDebugMessageWriter items = cameras.beginItems();
      for (int i = 0; i < count; i++) {
        items.writeValue(text.setLength(0).concat("Camera ").concat(i));
      }
      cameras.endItems(count);
    }
    cameras.setVisible(count > 1).setSelected(count > 0 ? viewport.getShownIndex() : -1);
    freeCamBadge.setVisible(viewport.isFreeCam());
    readout.setText(text.setLength(0).concat("zoom ").concatFloatRounded(viewport.getZoom(), 2)
        .concat("  scroll ").concatFloatRounded(viewport.getScrollX(), 1).concat(", ")
        .concatFloatRounded(viewport.getScrollY(), 1));
    canvas.setView(viewport.getViewWidth(), viewport.getViewHeight());
    canvas.beginShapes();
    for (int i = 0, n = viewport.getBoxCount(); i < n; i++) {
      int id = viewport.getBoxId(i);
      int flags = viewport.getBoxFlags(i);
      int style = FlixelDebugProtocol.TONE_NORMAL;
      if ((flags & FlixelDebugProtocol.VIEWPORT_BOX_SELECTED) != 0) {
        style = FlixelDebugProtocol.TONE_WARNING | FlixelDebugProtocol.SHAPE_SELECTED;
      } else if (ids.isAgentTouched(id)) {
        style = FlixelDebugProtocol.TONE_SUCCESS;
      }
      if ((flags & FlixelDebugProtocol.VIEWPORT_BOX_GROUP) != 0) {
        style |= FlixelDebugProtocol.SHAPE_DASHED;
      }
      if ((flags & FlixelDebugProtocol.VIEWPORT_BOX_INACTIVE) != 0) {
        style |= FlixelDebugProtocol.SHAPE_DIM;
      }
      label.setLength(0).concat(viewport.getTypeName(viewport.getBoxType(i))).concat(" #").concat(id);
      canvas.shape(id, viewport.getBoxX(i), viewport.getBoxY(i), viewport.getBoxWidth(i), viewport.getBoxHeight(i),
          style, label);
    }
    canvas.endShapes();
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }
}
