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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelObject;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelObjectIntMap;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugNode;
import org.flixelgdx.debug.FlixelDebugNodes;
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.FlixelDebugPicker;
import org.flixelgdx.debug.remote.FlixelDebugAgent;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.group.FlixelGroupable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The viewport panel of the remote debugger: a live map of what one camera shows, plus the buttons to fly that
 * camera, pick objects, and drag them around from the debugger app.
 *
 * <p>Think of it as a remote control with a little map of the screen on it. The map shows a box for every object
 * the camera draws, right where it is drawn, so the app can draw the same picture. Tapping a box on the map
 * selects that object, dragging a box moves the object, and dragging or pinching the empty map flies the camera.
 * This is what makes the free camera and picking usable on a phone, where the game has no keyboard or mouse for
 * the {@linkplain FlixelDebugControlsPanel controls panel} to read.
 *
 * <h2>What the map holds</h2>
 *
 * <p>Everything is in the <strong>view space</strong> of the chosen camera, the space objects are drawn in:
 * {@code 0, 0} is the top left corner of what the camera shows, and the view is as wide and tall as the camera's
 * world size divided by its zoom. Because each object is placed with its own scroll factor, parallax layers and
 * screen-fixed objects land exactly where they appear. Each box has the object's
 * {@linkplain FlixelDebugObjectIds id} (the same id the scene and inspector panels use), its hitbox (measured
 * by {@link FlixelDebugPicker#measure(Object, FlixelCamera)} with {@linkplain #hitboxes hitbox mode} on,
 * so a box is exactly what a tap hits), its type name, and a few flags.
 *
 * <p>A box is the object's collision box, the same one the game outlines when hitboxes are on, not the area its
 * graphic covers. It does not change when a sprite is scaled, rotated, offset, or plays an animation, only when
 * the object moves or its hitbox is resized (for example with {@link FlixelSprite#updateHitbox()}). The selected
 * object is the exception: its box is the area its graphic covers, the same one the game outlines in yellow, so
 * the map and the game window show the same selection outline. Boxes come in
 * draw order, bottom first, and only objects that touch the view are listed. At most {@value #MAX_BOXES} boxes
 * are sent; when more objects are on screen, the ones drawn on top are kept.
 *
 * <p>A group ({@link FlixelGroupable}) has the box around its drawn members instead of a hitbox of its own, so
 * every group, from a sprite group to a game's custom group, is outlined around what it holds.
 *
 * <p>Objects that are neither a {@link FlixelObject} nor a group can still have a box: a
 * {@link FlixelDebugNode} measures itself in the camera's view space, and its children are walked like group
 * members. Picking, the selection outline, and moving (when the node has a position) use the same box.
 *
 * <h2>Sampling</h2>
 *
 * <p>The panel looks at the camera at the sample rate of the controls panel (see
 * {@link FlixelDebugControlsPanel#setRate(int)}) and right after each action. It only has new content, and only
 * gets sent, when something on the map changed, which it checks with a cheap checksum. Sampling does not allocate
 * once every object and type on screen has been seen once. While no debugger app is connected, the timed samples
 * are skipped, so the panel costs nothing when nobody is watching.
 *
 * <h2>Flying the camera</h2>
 *
 * <p>{@link #pan(float, float)} and {@link #zoom(float, float, float)} go through the free camera of the controls
 * panel ({@link FlixelDebugControlsPanel#panFreeCam(float, float)} and
 * {@link FlixelDebugControlsPanel#zoomFreeCam(float, float, float)}). They turn it on and point it at the camera
 * this panel shows, so following a target does not undo the move. {@link #resetCamera()} turns the free camera
 * off, which puts the camera back exactly as the game had it.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FlixelDebugViewportPanel viewport = Flixel.debug.viewport;
 * viewport.setCameraIndex(1);          // Show the second camera, for example a HUD camera.
 * viewport.pick(40f, 30f);             // Select whatever is drawn at that spot.
 * viewport.move(viewport.getBoxId(0), 16f, 0f); // Push the bottom object 16 units to the right.
 * viewport.pan(0f, -100f);             // Look 100 units higher.
 * viewport.resetCamera();              // Back to the game's camera.
 * }</pre>
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_VIEWPORT
 * @see FlixelDebugPicker
 */
public class FlixelDebugViewportPanel extends FlixelDebugPanelBase {

  /** The most boxes one state message holds. */
  public static final int MAX_BOXES = 1024;

  /** The highest camera index the protocol can carry in its {@code u8} field. */
  private static final int MAX_CAMERA = 255;

  private final FlixelObjectIntMap<Class<?>> typeIndices = new FlixelObjectIntMap<>();
  private final FlixelArray<String> typeNames = new FlixelArray<>();
  private final FlixelDebugPicker picker = new FlixelDebugPicker();
  /** Measures the selected object by the area its graphic covers, to match the game's yellow outline. */
  private final FlixelDebugPicker graphicPicker = new FlixelDebugPicker();
  private final FlixelDebugPicker.Visitor collector = this::collect;
  private final FlixelDebugControlsPanel controls;
  private final FlixelDebugInspectorPanel inspector;
  private final FlixelDebugObjectIds ids;

  @Nullable
  private Object walkSelected;

  private int[] boxIds;
  private float[] boxX;
  private float[] boxY;
  private float[] boxW;
  private float[] boxH;
  private short[] boxTypes;
  private byte[] boxFlags;

  private float timer;
  private float viewWidth;
  private float viewHeight;
  private float zoom = 1f;
  private float scrollX;
  private float scrollY;
  private int cameraIndex;
  private int shownIndex;
  private int cameraCount;
  private int boxCount;
  private int selectedId = FlixelDebugObjectIds.NONE;
  private int signature;

  private boolean freeCam;
  private boolean paused;
  private boolean hitboxes;
  private boolean sampled;
  private boolean dirty;

  /**
   * Creates the viewport panel.
   *
   * @param controls The controls panel whose free camera, pause, hitboxes, and sample rate this panel uses.
   * @param inspector The inspector that picked objects are selected on. Its id registry names the boxes.
   */
  public FlixelDebugViewportPanel(@NotNull FlixelDebugControlsPanel controls,
      @NotNull FlixelDebugInspectorPanel inspector) {
    super("viewport", "Viewport", FlixelDebugProtocol.KIND_VIEWPORT);
    this.controls = controls;
    this.inspector = inspector;
    this.ids = inspector.getIds();
    picker.hitboxes = true;
  }

  @Override
  public void update(float elapsed) {
    if (!dirty && !isAppLive()) {
      // Nobody is looking at the map, so walking the scene would be wasted work.
      timer = 0f;
      return;
    }
    timer += elapsed;
    float interval = 1f / controls.getRate();
    if (dirty || timer >= interval) {
      timer = dirty ? 0f : timer - interval;
      if (timer > interval) {
        timer = 0f;
      }
      dirty = false;
      sample();
    }
  }

  /**
   * Looks at the chosen camera now and stores a fresh map of it.
   *
   * <p>{@link #update(float)} calls this at the sample rate and right after an action. When the map differs from
   * the last one, the panel is marked as changed so the network agent sends it. It is public so tools and tests can
   * take a sample on demand. It only allocates the first time it sees an object (for its id) or a class (for its
   * type name).
   */
  public void sample() {
    if (takeSample()) {
      markChanged();
    }
  }

  /**
   * Selects the topmost object drawn at a point of the chosen camera, or clears the selection when nothing is
   * there.
   *
   * <p>It searches the same way an Alt click does (see {@link FlixelDebugPicker#pickInCamera(FlixelCamera, float,
   * float)}), so an open sub state is searched first. Unlike an Alt click, it tests each object's hitbox instead of
   * the area its graphic covers, so it hits exactly the boxes on the map.
   *
   * @param viewX The horizontal position in the camera's view space.
   * @param viewY The vertical position in the camera's view space.
   * @return The selected object, or {@code null} when nothing was there and the selection was cleared.
   */
  @Nullable
  public Object pick(float viewX, float viewY) {
    dirty = true;
    FlixelCamera cam = shownCamera();
    Object hit = cam != null && Float.isFinite(viewX) && Float.isFinite(viewY)
        ? picker.pickInCamera(cam, viewX, viewY) : null;
    if (hit != null) {
      inspector.select(hit);
    } else {
      inspector.clearSelection();
    }
    return hit;
  }

  /**
   * Moves an object by a distance in world units.
   *
   * <p>The move is a distance, not a new position, so drag updates that arrive late or close together add up
   * instead of fighting each other. Any object that has a position (see {@link FlixelPositional}) can be moved,
   * selected or not. A group moves its members along with it.
   *
   * @param id The object's id from {@link FlixelDebugObjectIds}.
   * @param dx How far to move right, in world units. Negative moves left.
   * @param dy How far to move down, in world units. Negative moves up.
   * @return {@code true} when the object moved. It is {@code false} for an unknown id, an object without a
   *     position, or a distance that is not a finite number.
   */
  public boolean move(int id, float dx, float dy) {
    dirty = true;
    if (!Float.isFinite(dx) || !Float.isFinite(dy) || !(ids.get(id) instanceof FlixelPositional positional)) {
      return false;
    }
    positional.changeX(dx);
    positional.changeY(dy);
    return true;
  }

  /**
   * Turns the free camera on for the chosen camera and moves it by a distance in view space.
   *
   * @param dx How far to move the camera right, in view units. Negative moves it left.
   * @param dy How far to move the camera down, in view units. Negative moves it up.
   * @return {@code true} when the camera moved.
   * @see FlixelDebugControlsPanel#panFreeCam(float, float)
   */
  public boolean pan(float dx, float dy) {
    dirty = true;
    if (shownCamera() == null) {
      return false;
    }
    controls.setFreeCamIndex(shownIndex);
    return controls.panFreeCam(dx, dy);
  }

  /**
   * Turns the free camera on for the chosen camera and zooms it around a point that stays still.
   *
   * @param factor How much to multiply the zoom by, for example {@code 1.25} to zoom in one wheel notch.
   * @param anchorViewX The horizontal position of the point that stays still, in view space before the zoom.
   * @param anchorViewY The vertical position of the point that stays still, in view space before the zoom.
   * @return {@code true} when the zoom changed.
   * @see FlixelDebugControlsPanel#zoomFreeCam(float, float, float)
   */
  public boolean zoom(float factor, float anchorViewX, float anchorViewY) {
    dirty = true;
    if (shownCamera() == null) {
      return false;
    }
    controls.setFreeCamIndex(shownIndex);
    return controls.zoomFreeCam(factor, anchorViewX, anchorViewY);
  }

  /**
   * Turns the free camera off, which restores the camera's scroll, zoom, bounds, and follow target exactly, and
   * points the free camera back at the main camera.
   */
  public void resetCamera() {
    dirty = true;
    controls.setFreeCam(false);
    controls.setFreeCamIndex(0);
  }

  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    if (!sampled) {
      // The agent is sending right now, so there is no need to mark the panel as changed and send it again.
      takeSample();
    }
    writer.writeU8(shownIndex)
        .writeU8(cameraCount)
        .writeF32(viewWidth)
        .writeF32(viewHeight)
        .writeF32(zoom)
        .writeF32(scrollX)
        .writeF32(scrollY)
        .writeBool(freeCam)
        .writeBool(paused)
        .writeBool(hitboxes)
        .writeI32(selectedId);
    int types = typeNames.getSize();
    writer.beginArray();
    for (int i = 0; i < types; i++) {
      writer.writeString(typeNames.get(i));
    }
    writer.endArray(types);
    writer.beginArray();
    for (int i = 0; i < boxCount; i++) {
      writer.writeI32(boxIds[i])
          .writeF32(boxX[i])
          .writeF32(boxY[i])
          .writeF32(boxW[i])
          .writeF32(boxH[i])
          .writeU16(boxTypes[i] & 0xFFFF)
          .writeU8(boxFlags[i] & 0xFF);
    }
    writer.endArray(boxCount);
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    switch (action) {
      case FlixelDebugProtocol.VIEWPORT_SET_CAMERA -> setCameraIndex(reader.readU8());
      case FlixelDebugProtocol.VIEWPORT_PICK -> pick(reader.readF32(), reader.readF32());
      case FlixelDebugProtocol.VIEWPORT_MOVE -> move(reader.readI32(), reader.readF32(), reader.readF32());
      case FlixelDebugProtocol.VIEWPORT_PAN -> pan(reader.readF32(), reader.readF32());
      case FlixelDebugProtocol.VIEWPORT_ZOOM -> zoom(reader.readF32(), reader.readF32(), reader.readF32());
      case FlixelDebugProtocol.VIEWPORT_RESET_CAMERA -> resetCamera();
      default -> {
      }
    }
  }

  @Override
  public void reset() {
    timer = 0f;
    cameraIndex = 0;
    shownIndex = 0;
    boxCount = 0;
    sampled = false;
    dirty = false;
    walkSelected = null;
    markChanged();
  }

  /** Takes a sample and returns whether it differs from the last one, without marking the panel as changed. */
  private boolean takeSample() {
    FlixelCamera cam = shownCamera();
    cameraCount = Math.min(Flixel.cameras.getSize(), MAX_CAMERA);
    freeCam = controls.isFreeCam();
    paused = controls.isPaused();
    hitboxes = controls.isHitboxes();
    selectedId = inspector.getSelectedId();
    boxCount = 0;
    if (cam != null) {
      zoom = cam.getZoom();
      viewWidth = cam.getWorldWidth() / zoom;
      viewHeight = cam.getWorldHeight() / zoom;
      scrollX = cam.scrollX;
      scrollY = cam.scrollY;
      ensureBoxes();
      walkSelected = inspector.getSelected();
      // Walk from the top down so that, when there are too many objects, the ones drawn on top are kept.
      picker.forEachDrawn(cam, true, collector);
      walkSelected = null;
      reverseBoxes();
    } else {
      zoom = 1f;
      viewWidth = 0f;
      viewHeight = 0f;
      scrollX = 0f;
      scrollY = 0f;
    }
    int sum = computeSignature();
    boolean changed = !sampled || sum != signature;
    sampled = true;
    signature = sum;
    return changed;
  }

  /** Returns the camera the map shows, and remembers its index: the chosen one, or the main camera instead. */
  @Nullable
  private FlixelCamera shownCamera() {
    FlixelArray<FlixelCamera> cameras = Flixel.cameras;
    int size = cameras.getSize();
    shownIndex = cameraIndex < size ? cameraIndex : 0;
    return size > 0 ? cameras.get(shownIndex) : null;
  }

  private boolean collect(Object member, FlixelCamera cam) {
    if (boxCount >= MAX_BOXES) {
      return true;
    }
    // The selected box covers the graphic, like the yellow outline in the game. Every other box is a hitbox.
    FlixelDebugPicker measurer = member == walkSelected ? graphicPicker : picker;
    if (!measurer.measure(member, cam)) {
      return false;
    }
    float left = measurer.getBoundsLeft();
    float top = measurer.getBoundsTop();
    float width = measurer.getBoundsWidth();
    float height = measurer.getBoundsHeight();
    if (left > viewWidth || top > viewHeight || left + width < 0f || top + height < 0f) {
      return false;
    }
    int flags = 0;
    if (member == walkSelected) {
      flags |= FlixelDebugProtocol.VIEWPORT_BOX_SELECTED;
    }
    if (member instanceof FlixelGroupable<?> || FlixelDebugNodes.childCount(member) > 0) {
      flags |= FlixelDebugProtocol.VIEWPORT_BOX_GROUP;
    }
    if (member instanceof FlixelObject object && !object.isActive()) {
      flags |= FlixelDebugProtocol.VIEWPORT_BOX_INACTIVE;
    }
    int i = boxCount++;
    boxIds[i] = ids.idOf(member);
    boxX[i] = left;
    boxY[i] = top;
    boxW[i] = width;
    boxH[i] = height;
    boxTypes[i] = (short) typeIndex(member.getClass());
    boxFlags[i] = (byte) flags;
    return false;
  }

  /** Returns the position of a class in the type table, adding its simple name the first time it is seen. */
  private int typeIndex(Class<?> type) {
    int index = typeIndices.get(type, -1);
    if (index >= 0) {
      return index;
    }
    if (typeNames.getSize() >= FlixelDebugProtocol.MAX_ARRAY_COUNT) {
      return 0;
    }
    String simple = type.getSimpleName();
    index = typeNames.getSize();
    typeNames.add(simple.isEmpty() ? type.getName() : simple);
    typeIndices.put(type, index);
    return index;
  }

  private void ensureBoxes() {
    if (boxIds != null) {
      return;
    }
    // Made on the first sample, so a game that never shows this panel pays nothing for it.
    boxIds = new int[MAX_BOXES];
    boxX = new float[MAX_BOXES];
    boxY = new float[MAX_BOXES];
    boxW = new float[MAX_BOXES];
    boxH = new float[MAX_BOXES];
    boxTypes = new short[MAX_BOXES];
    boxFlags = new byte[MAX_BOXES];
  }

  /** Turns the boxes, collected from the top down, into draw order. */
  private void reverseBoxes() {
    for (int a = 0, b = boxCount - 1; a < b; a++, b--) {
      int id = boxIds[a];
      boxIds[a] = boxIds[b];
      boxIds[b] = id;
      float x = boxX[a];
      boxX[a] = boxX[b];
      boxX[b] = x;
      float y = boxY[a];
      boxY[a] = boxY[b];
      boxY[b] = y;
      float w = boxW[a];
      boxW[a] = boxW[b];
      boxW[b] = w;
      float h = boxH[a];
      boxH[a] = boxH[b];
      boxH[b] = h;
      short type = boxTypes[a];
      boxTypes[a] = boxTypes[b];
      boxTypes[b] = type;
      byte flag = boxFlags[a];
      boxFlags[a] = boxFlags[b];
      boxFlags[b] = flag;
    }
  }

  private int computeSignature() {
    int sum = shownIndex;
    sum = sum * 31 + cameraCount;
    sum = sum * 31 + Float.floatToIntBits(viewWidth);
    sum = sum * 31 + Float.floatToIntBits(viewHeight);
    sum = sum * 31 + Float.floatToIntBits(zoom);
    sum = sum * 31 + Float.floatToIntBits(scrollX);
    sum = sum * 31 + Float.floatToIntBits(scrollY);
    sum = sum * 31 + (freeCam ? 1 : 0) + (paused ? 2 : 0) + (hitboxes ? 4 : 0);
    sum = sum * 31 + selectedId;
    sum = sum * 31 + typeNames.getSize();
    sum = sum * 31 + boxCount;
    for (int i = 0; i < boxCount; i++) {
      sum = sum * 31 + boxIds[i];
      sum = sum * 31 + Float.floatToIntBits(boxX[i]);
      sum = sum * 31 + Float.floatToIntBits(boxY[i]);
      sum = sum * 31 + Float.floatToIntBits(boxW[i]);
      sum = sum * 31 + Float.floatToIntBits(boxH[i]);
      sum = sum * 31 + boxTypes[i];
      sum = sum * 31 + boxFlags[i];
    }
    return sum;
  }

  private static boolean isAppLive() {
    FlixelDebugManager debug = Flixel.debug;
    FlixelDebugAgent agent = debug != null ? debug.getAgent() : null;
    return agent != null && agent.isLive();
  }

  /**
   * Returns the camera the map shows right now: the chosen one, or the main camera while the chosen index is past
   * the end of the camera list.
   *
   * @return The camera, or {@code null} when the game has no camera.
   */
  @Nullable
  public FlixelCamera getShownCamera() {
    return shownCamera();
  }

  /**
   * Returns the type name a box index from the type table stands for.
   *
   * @param typeIndex A type index of a box.
   * @return The simple class name.
   */
  @NotNull
  public String getTypeName(int typeIndex) {
    return typeNames.get(typeIndex);
  }

  /**
   * Returns how many boxes the latest sample holds.
   *
   * @return A count from 0 to {@value #MAX_BOXES}.
   */
  public int getBoxCount() {
    return boxCount;
  }

  /**
   * Returns the object id of a box from the latest sample.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1, in draw order.
   * @return The id from {@link FlixelDebugObjectIds}.
   */
  public int getBoxId(int index) {
    return boxIds[index];
  }

  /**
   * Returns the left edge of a box, in view space.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The left edge.
   */
  public float getBoxX(int index) {
    return boxX[index];
  }

  /**
   * Returns the top edge of a box, in view space.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The top edge.
   */
  public float getBoxY(int index) {
    return boxY[index];
  }

  /**
   * Returns the width of a box, in view units.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The width.
   */
  public float getBoxWidth(int index) {
    return boxW[index];
  }

  /**
   * Returns the height of a box, in view units.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The height.
   */
  public float getBoxHeight(int index) {
    return boxH[index];
  }

  /**
   * Returns the position of a box's type name in the type table (see {@link #getTypeName(int)}).
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The type index.
   */
  public int getBoxType(int index) {
    return boxTypes[index] & 0xFFFF;
  }

  /**
   * Returns the flags of a box: a mix of the {@code VIEWPORT_BOX_*} bits of {@link FlixelDebugProtocol}.
   *
   * @param index A box index from 0 to {@link #getBoxCount()} minus 1.
   * @return The flags.
   */
  public int getBoxFlags(int index) {
    return boxFlags[index] & 0xFF;
  }

  public int getCameraIndex() {
    return cameraIndex;
  }

  /**
   * Chooses which camera the map shows and the camera actions act on.
   *
   * <p>While the index is past the end of {@link Flixel#cameras}, the main camera (index {@code 0}) is shown
   * instead, and the state message says so.
   *
   * @param cameraIndex A position in {@link Flixel#cameras}, clamped to {@code 0} to {@code 255}.
   */
  public void setCameraIndex(int cameraIndex) {
    this.cameraIndex = Math.max(0, Math.min(MAX_CAMERA, cameraIndex));
    dirty = true;
  }
}
