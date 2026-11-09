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
package org.flixelgdx.debug.feeds;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugNode;
import org.flixelgdx.debug.FlixelDebugNodes;
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.FlixelDebugPaths;
import org.flixelgdx.debug.FlixelDebugThumbnails;
import org.flixelgdx.debug.inspector.FlixelDebugSection;
import org.flixelgdx.debug.inspector.FlixelDebugValue;
import org.flixelgdx.debug.inspector.FlixelSectionBuilder;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.functional.FlixelExistable;
import org.flixelgdx.functional.FlixelPositional;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphicsManager;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The inspector panel of the remote debugger: pick one object in the running game and see (and edit) its
 * properties.
 *
 * <p>Think of a doctor's chart. When you select a patient, the chart opens with one page for every specialty that
 * applies: the general page, the page for athletes, the page for sprinters. The inspector does the same for a game
 * object. It looks at the object's class and every class it extends, and shows one <em>section</em> for each class
 * that has been registered, most specific first. A {@code Player} that extends {@code FlixelSprite} shows the
 * {@code Player} section, then the sprite section, then the object section, then the basic section.
 *
 * <h2>Registering a section</h2>
 *
 * <p>The framework already registers sections for {@code FlixelBasic}, {@code FlixelObject}, {@code FlixelSprite},
 * {@code FlixelText}, {@code FlixelCamera}, and the groups and states. Add your own for your classes:
 *
 * <pre>{@code
 * Flixel.debug.inspector.register(Player.class, "Player", b -> b
 *     .intProp("Health", Player::getHealth, Player::setHealth)
 *     .floatProp("Speed", Player::getSpeed)
 *     .boolProp("On ground", Player::isOnGround)
 *     .enumProp("State", Player::getState));
 * }</pre>
 *
 * <p>Registering a class again replaces its section. Classes in the middle of the family tree that are not
 * registered are simply skipped, and the family tree of each class is worked out once and remembered.
 *
 * <h2>Selecting</h2>
 *
 * <pre>{@code
 * Flixel.debug.inspector.select(player);    // The app opens the player's sections.
 * Flixel.debug.inspector.selectParent();    // Moves up to the group or state that holds the player.
 * Flixel.debug.inspector.goTo();            // Centers the main camera on the player.
 * Flixel.debug.inspector.clearSelection();
 * }</pre>
 *
 * <p>An object whose {@code exists} flag is off can still be selected and edited while it is anywhere in the scene,
 * so a killed or recyclable object can be inspected and switched back on. The selection clears itself, and the
 * object's id is forgotten, once the object no longer exists and is no longer anywhere in the scene (it was
 * destroyed or removed). The scene is everything reachable from {@link Flixel#state}, its sub states, and the
 * objects added with {@link #track(Object, String)}, through every {@link FlixelGroupable} and {@link FlixelDebugNode}.
 *
 * <p>Each selected object has a stable numeric id from {@link FlixelDebugManager#ids}, and the app refers to
 * objects by that number. The path sent with the selection is its address in the object tree, such as
 * {@code state.3.0} (see {@link FlixelDebugPaths}). An object that is not in the tree, for example a tracked
 * object that is not a group, gets {@code TypeName#id} instead. The same paths work with
 * {@link #selectPath(CharSequence)}, the {@code SELECT_PATH} action, and the {@code select} console command.
 *
 * <p>Reading values never allocates. Values are re-read about ten times a second while an object is selected.
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelSectionBuilder
 * @see FlixelDebugProtocol#KIND_INSPECTOR
 */
public class FlixelDebugInspector extends FlixelDebugFeedBase {

  /** Seconds between revision bumps while an object is selected, which is how often the app is offered fresh values. */
  private static final float REFRESH_INTERVAL = 0.1f;

  /** Seconds between cleanups of ids of objects that no longer exist and are no longer in the scene. */
  private static final float SWEEP_INTERVAL = 1f;

  /** How deep a parent search goes, which also protects against a group that contains itself. */
  private static final int MAX_DEPTH = 64;

  private static final String TAG = "Debugger/Inspector";

  private float refreshTimer;
  private float sweepTimer;
  private float pathTimer;

  private int epoch;

  private final FlixelMap<Class<?>, FlixelDebugSection> sections = new FlixelMap<>();
  private final FlixelMap<Class<?>, Chain> chains = new FlixelMap<>();
  private final FlixelArray<FlixelDebugSection> sent = new FlixelArray<>();
  private final FlixelArray<SelectionListener> listeners = new FlixelArray<>();
  private final FlixelArray<Object> trackedObjects = new FlixelArray<>();
  private final FlixelArray<String> trackedLabels = new FlixelArray<>();
  private final FlixelDebugValue value = new FlixelDebugValue();
  private final FlixelString pathText = new FlixelString(64);
  private final FlixelString pathInput = new FlixelString(64);
  private final FlixelDebugThumbnails thumbnails = new FlixelDebugThumbnails();
  private final FlixelDebugObjectIds ids;

  @Nullable
  private FocusHandler focusHandler;

  @Nullable
  private Object selected;

  @Nullable
  private Object pathObject;

  private boolean pathFound;

  /** Whether the selected object was already checked for being in the scene since its exists flag went off. */
  private boolean goneChecked;

  private boolean agentSelection;

  /**
   * Creates the inspector panel.
   *
   * @param ids The registry that gives objects their ids.
   */
  public FlixelDebugInspector(@NotNull FlixelDebugObjectIds ids) {
    super("inspector", FlixelDebugProtocol.KIND_INSPECTOR);
    this.ids = ids;
  }

  /**
   * Registers the rows to show for a class, replacing any earlier registration for that exact class.
   *
   * <p>The section appears whenever the selected object is an instance of {@code type} (or of a subclass).
   *
   * @param type The class the section is for.
   * @param displayName The title of the section in the app.
   * @param build Adds the rows to the builder.
   * @param <T> The class type, so getters and setters can use method references such as {@code Player::getHealth}.
   */
  public <T> void register(@NotNull Class<T> type, @NotNull String displayName,
      @NotNull Consumer<FlixelSectionBuilder<T>> build) {
    FlixelDebugSection section = new FlixelDebugSection(type, displayName);
    build.accept(new FlixelSectionBuilder<>(section));
    sections.put(type, section);
    chains.clear();
    markChanged();
  }

  /**
   * Removes the section registered for a class.
   *
   * @param type The class to remove the section of.
   * @return {@code true} when a section was removed.
   */
  public boolean unregister(@NotNull Class<?> type) {
    if (sections.remove(type) == null) {
      return false;
    }
    chains.clear();
    markChanged();
    return true;
  }

  /**
   * Returns whether a section is registered for exactly this class.
   *
   * @param type The class to look for.
   * @return {@code true} when a section is registered.
   */
  public boolean isRegistered(@NotNull Class<?> type) {
    return sections.containsKey(type);
  }

  /**
   * Selects an object.
   *
   * <p>An object that does not exist (see {@link FlixelExistable}) can be selected too, so a killed object can be
   * inspected and switched back on. When it is not anywhere in the scene either, the next {@link #update(float)}
   * clears the selection again.
   *
   * @param object The object to select, or {@code null} to clear the selection.
   */
  public void select(@Nullable Object object) {
    if (object == null) {
      clearSelection();
      return;
    }
    boolean changed = selected != object;
    selected = object;
    agentSelection = false;
    goneChecked = false;
    ids.idOf(object);
    epoch++;
    refreshTimer = 0f;
    markChanged();
    if (changed) {
      notifyListeners(object);
    }
  }

  /** Clears the selection. */
  public void clearSelection() {
    if (selected == null) {
      return;
    }
    selected = null;
    agentSelection = false;
    sent.clear();
    epoch++;
    markChanged();
    notifyListeners(null);
  }

  /**
   * Selects the object a path points to.
   *
   * @param path A path in the format of {@link FlixelDebugPaths}, such as {@code state.3.0}.
   * <p>Objects whose {@code exists} flag is off are found as well, since a path always points into the scene.
   *
   * @return {@code true} when the path led to an object and it was selected. Otherwise nothing changes.
   */
  public boolean selectPath(@Nullable CharSequence path) {
    Object object = FlixelDebugPaths.resolve(this, path);
    if (object == null) {
      return false;
    }
    select(object);
    return true;
  }

  /**
   * Moves the selection to the group or state that holds the selected object.
   *
   * <p>The search starts at the current state (including its sub state) and at every object registered with
   * {@link #track(Object, String)}, and looks through every {@link FlixelGroupable} and {@link FlixelDebugNode} it finds.
   *
   * @return {@code true} when a parent was found and selected. When the selected object is a root, or nothing is
   *     selected, nothing changes.
   */
  public boolean selectParent() {
    Object current = getSelected();
    if (current == null) {
      return false;
    }
    Object parent = findParent(Flixel.state, current, 0);
    for (int i = 0, n = trackedObjects.getSize(); parent == null && i < n; i++) {
      parent = findParent(trackedObjects.get(i), current, 0);
    }
    if (parent == null) {
      return false;
    }
    select(parent);
    return true;
  }

  /**
   * Centers the main camera on the selected object.
   *
   * <p>When the debug manager created this panel, the move goes through the free camera, which stops the camera
   * from following its target so the move is not undone on the next frame.
   *
   * @return {@code true} when the camera moved. It does nothing when the selection has no position (it is not a
   *     {@link FlixelPositional}), nothing is selected, or there is no camera.
   */
  public boolean goTo() {
    Object current = getSelected();
    if (!(current instanceof FlixelPositional positional) || Flixel.cameras.getSize() == 0) {
      return false;
    }
    FocusHandler handler = focusHandler;
    if (handler != null) {
      return handler.focus(positional.getMidpointX(), positional.getMidpointY());
    }
    FlixelCamera camera = Flixel.cameras.first();
    camera.focusOn(positional.getMidpointX(), positional.getMidpointY());
    return true;
  }

  /**
   * Adds a listener that is told whenever the selected object changes.
   *
   * <p>The planned use is "selecting an object in the scene opens the inspector tab".
   *
   * @param listener The listener to add. Adding the same listener twice does nothing.
   */
  public void addSelectionListener(@NotNull SelectionListener listener) {
    if (!listeners.contains(listener, true)) {
      listeners.add(listener);
    }
  }

  /**
   * Removes a listener added with {@link #addSelectionListener(SelectionListener)}.
   *
   * @param listener The listener to remove.
   */
  public void removeSelectionListener(@NotNull SelectionListener listener) {
    listeners.removeValue(listener, true);
  }

  /**
   * Tells the scene panel about an object outside the scene tree, such as a manager, so it can be listed as an
   * extra root and be selected.
   *
   * <p>Tracking an object again only changes its label. Objects stay tracked until {@link #untrack(Object)}
   * is called, so untrack them when they go away.
   *
   * @param object The object to list.
   * @param label The name shown for it in the scene.
   */
  public void track(@NotNull Object object, @NotNull String label) {
    int index = trackedObjects.indexOf(object, true);
    if (index >= 0) {
      trackedLabels.set(index, label);
    } else {
      trackedObjects.add(object);
      trackedLabels.add(label);
    }
    ids.idOf(object);
    markChanged();
  }

  /**
   * Stops listing an object that was added with {@link #track(Object, String)}. Its id is released, and the
   * selection is cleared when the object was selected.
   *
   * @param object The object to remove.
   * @return {@code true} when the object was tracked.
   */
  public boolean untrack(@NotNull Object object) {
    int index = trackedObjects.indexOf(object, true);
    if (index < 0) {
      return false;
    }
    trackedObjects.removeIndex(index);
    trackedLabels.removeIndex(index);
    if (selected == object) {
      clearSelection();
    }
    ids.release(object);
    markChanged();
    return true;
  }

  /**
   * Refreshes the selection and cleans up ids.
   *
   * <p>The frame the selected object stops existing, the scene is searched once for it, and the selection is
   * cleared when it is no longer there. About once a second the whole scene is walked and marked (see
   * {@link FlixelDebugObjectIds#beginMark()}), the selection is cleared when it no longer exists and was not
   * reached, and {@link FlixelDebugObjectIds#sweep()} forgets every other such object.
   *
   * @param elapsed Seconds since the last frame.
   */
  @Override
  public void update(float elapsed) {
    Object current = selected;
    if (current != null && isGone(current)) {
      if (!goneChecked) {
        goneChecked = true;
        if (!isInScene(current)) {
          clearSelection();
          current = null;
        }
      }
    } else {
      goneChecked = false;
    }
    if (current != null) {
      refreshTimer += elapsed;
      if (refreshTimer >= REFRESH_INTERVAL) {
        refreshTimer = 0f;
        markChanged();
      }
    }
    pathTimer += elapsed;
    if (pathTimer >= SWEEP_INTERVAL) {
      pathTimer = 0f;
      if (!pathFound) {
        // An object that was not in the tree may have been added to it since the last search.
        pathObject = null;
      }
    }
    sweepTimer += elapsed;
    if (sweepTimer >= SWEEP_INTERVAL) {
      sweepTimer = 0f;
      markScene();
      Object stale = selected;
      if (stale != null && isGone(stale) && !ids.isMarked(stale)) {
        clearSelection();
      }
      ids.sweep();
    }
  }

  /**
   * Writes the inspector state: the selected id, its path, one section per registered class in the object's family
   * tree, and whether the object has a texture.
   *
   * <p>The thumbnail of the texture is a separate {@code THUMBNAIL} payload that the app requests with
   * {@code REQUEST_THUMBNAIL}. This method only reports whether there is something to ask for.
   *
   * @param writer The writer positioned after the panel index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeU8(FlixelDebugProtocol.INSPECTOR_STATE);
    Object current = getSelected();
    if (current == null) {
      sent.clear();
      writer.writeI32(FlixelDebugObjectIds.NONE).writeString("");
      writer.beginArray().endArray(0);
      writer.writeBool(false);
      return;
    }
    int id = ids.idOf(current);
    Chain chain = chainFor(current.getClass());
    writer.writeI32(id).writeString(pathOf(current, chain, id));

    sent.clear();
    writer.beginArray();
    FlixelArray<FlixelDebugSection> found = chain.sections;
    for (int i = 0, n = found.getSize(); i < n && sent.getSize() < FlixelDebugProtocol.MAX_ARRAY_COUNT; i++) {
      FlixelDebugSection section = found.get(i);
      Object target = section.resolve(current);
      if (target == null) {
        continue;
      }
      writer.writeString(section.getTypeName()).writeString(section.getDisplayName());
      section.writeRows(writer, target, epoch);
      sent.add(section);
    }
    writer.endArray(sent.getSize());
    writer.writeBool(hasTexture(current));
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    // Every feed action comes from a tool such as an AI agent, so what it selects or edits is marked for the panels.
    switch (action) {
      case FlixelDebugProtocol.INSPECTOR_SELECT -> {
        Object object = ids.get(reader.readI32());
        if (object != null) {
          select(object);
          markAgentSelection();
        }
      }
      case FlixelDebugProtocol.INSPECTOR_SELECT_PARENT -> {
        selectParent();
        markAgentSelection();
      }
      case FlixelDebugProtocol.INSPECTOR_CLEAR -> clearSelection();
      case FlixelDebugProtocol.INSPECTOR_GO_TO -> goTo();
      case FlixelDebugProtocol.INSPECTOR_SET_PROPERTY -> {
        setProperty(reader);
        ids.markAgentTouched(selected);
      }
      case FlixelDebugProtocol.INSPECTOR_SELECT_PATH -> {
        reader.readString(pathInput);
        selectPath(pathInput);
        markAgentSelection();
      }
      case FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL -> requestThumbnail(reader.readI32());
      default -> {
      }
    }
  }

  /**
   * Marks the current selection as made by a tool (such as an AI agent), so the inspector panel can say so. Feed
   * actions call it; any later selection made another way clears the mark.
   */
  public void markAgentSelection() {
    if (selected != null) {
      agentSelection = true;
      ids.markAgentTouched(selected);
    }
  }

  /**
   * Collects the inspector sections that describe the selected object, in the order they are shown: the sections of
   * its own class first, then those of its parent classes, then sections that point at a related object. Sections
   * whose related object is missing are left out.
   *
   * <p>A panel reads each section's rows with
   * {@link FlixelDebugSection#writeListItems(FlixelDebugMessageWriter, Object, int)}, passing
   * {@code section.resolve(getSelected())} and {@link #getEpoch()}.
   *
   * @param out Receives the sections; it is cleared first. Nothing else is allocated.
   * @return How many sections were collected.
   */
  public int collectSections(@NotNull FlixelArray<FlixelDebugSection> out) {
    out.clear();
    Object current = getSelected();
    if (current == null) {
      return 0;
    }
    FlixelArray<FlixelDebugSection> found = chainFor(current.getClass()).sections;
    for (int i = 0, n = found.getSize(); i < n; i++) {
      FlixelDebugSection section = found.get(i);
      if (section.resolve(current) != null) {
        out.add(section);
      }
    }
    return out.getSize();
  }

  /**
   * Changes one property of the selected object, as if the person edited it.
   *
   * @param section A section returned by {@link #collectSections(FlixelArray)}.
   * @param row The position of the property in the section.
   * @param value The new value. Numbers are converted like a Java cast; other kinds must match.
   * @return {@code true} when the property accepted the value.
   */
  public boolean apply(@NotNull FlixelDebugSection section, int row, @NotNull FlixelDebugValue value) {
    Object current = getSelected();
    if (current == null) {
      return false;
    }
    Object target = section.resolve(current);
    if (target == null || !section.apply(row, target, value)) {
      return false;
    }
    markChanged();
    return true;
  }

  /**
   * Starts reading a picture of what the selected sprite shows, for the listener of {@link #getThumbnails()}. It is
   * not sent to tools.
   *
   * @return {@code true} when the read started.
   */
  public boolean requestPreview() {
    Object current = getSelected();
    if (current instanceof FlixelSprite sprite) {
      FlixelFrame frame = sprite.getFrame();
      if (frame != null) {
        return thumbnails.requestPreview(ids.idOf(current), frame.getTexture(), frame.getRegionX(),
            frame.getRegionY(), frame.getRegionWidth(), frame.getRegionHeight());
      }
    }
    return false;
  }

  /**
   * Returns whether the selected object can be shown as a picture (it is a sprite with a frame and the graphics
   * backend can read pixels back).
   *
   * @return {@code true} when {@link #requestPreview()} can work.
   */
  public boolean hasPreview() {
    Object current = getSelected();
    return current != null && hasTexture(current);
  }

  /**
   * Returns the address of the selected object in the scene, such as {@code state.3.0}, or {@code TypeName#id} for
   * an object that is not in the scene tree.
   *
   * @return The reused path text, empty when nothing is selected.
   */
  @NotNull
  public CharSequence getPath() {
    Object current = getSelected();
    if (current == null) {
      pathText.clear();
      pathObject = null;
      return pathText;
    }
    return pathOf(current, chainFor(current.getClass()), ids.idOf(current));
  }

  @Override
  public boolean hasPendingPayload() {
    return thumbnails.hasPending();
  }

  /**
   * Writes the oldest finished thumbnail as a {@code THUMBNAIL} payload.
   *
   * @param writer The writer positioned after the panel index.
   */
  @Override
  public void writePendingPayload(@NotNull FlixelDebugMessageWriter writer) {
    thumbnails.write(writer, FlixelDebugProtocol.INSPECTOR_THUMBNAIL);
  }

  @Override
  public void reset() {
    clearSelection();
    sent.clear();
    ids.clear();
    thumbnails.clear();
  }

  /**
   * Returns whether the object has a texture whose thumbnail the app can request.
   *
   * <p>This is the hook for the thumbnail feature: override it (or extend it) when more kinds of objects can
   * provide a picture. The default is {@code true} for a sprite that has a frame, when the graphics backend can read
   * textures back (see {@link FlixelGraphicsManager#supportsReadPixels()}).
   *
   * @param object The selected object.
   * @return {@code true} when a thumbnail can be requested.
   */
  protected boolean hasTexture(@NotNull Object object) {
    return object instanceof FlixelSprite sprite && sprite.getFrame() != null
        && Flixel.graphics.supportsReadPixels();
  }

  /**
   * Starts making a thumbnail of what a sprite shows: the part of its texture that its current frame covers.
   *
   * <p>The result arrives a few frames later through {@link #writePendingPayload(FlixelDebugMessageWriter)}. An
   * unknown id or an object that is not a sprite with a frame (text, for example) gets an empty thumbnail right
   * away, so the app does not wait for a picture that never comes.
   */
  private void requestThumbnail(int id) {
    if (ids.get(id) instanceof FlixelSprite sprite) {
      FlixelFrame frame = sprite.getFrame();
      if (frame != null) {
        thumbnails.request(id, frame.getTexture(), frame.getRegionX(), frame.getRegionY(), frame.getRegionWidth(),
            frame.getRegionHeight());
        return;
      }
    }
    thumbnails.reject(id);
  }

  /**
   * Returns the path text of the selected object, searching the tree only when the cached path no longer leads to
   * the object.
   */
  private FlixelString pathOf(Object current, Chain chain, int id) {
    if (pathObject == current && (!pathFound || FlixelDebugPaths.resolve(this, pathText) == current)) {
      return pathText;
    }
    pathObject = current;
    pathFound = FlixelDebugPaths.build(this, current, pathText);
    if (!pathFound) {
      pathText.clear();
      pathText.concat(chain.typeName).concat('#').concat(id);
    }
    return pathText;
  }

  private void setProperty(FlixelDebugMessageReader reader) {
    int id = reader.readI32();
    int sectionIndex = reader.readU16();
    int propertyIndex = reader.readU16();
    value.read(reader);
    Object current = getSelected();
    if (current == null || ids.peek(current) != id || sectionIndex >= sent.getSize()) {
      return;
    }
    FlixelDebugSection section = sent.get(sectionIndex);
    Object target = section.resolve(current);
    if (target == null) {
      return;
    }
    if (section.apply(propertyIndex, target, value)) {
      markChanged();
    }
  }

  private Chain chainFor(Class<?> type) {
    Chain chain = chains.get(type);
    if (chain != null) {
      return chain;
    }
    chain = new Chain(type);
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      FlixelDebugSection section = sections.get(c);
      if (section != null) {
        chain.sections.add(section);
      }
    }
    chains.put(type, chain);
    return chain;
  }

  private void notifyListeners(@Nullable Object object) {
    for (int i = 0; i < listeners.getSize(); i++) {
      try {
        listeners.get(i).onSelectionChanged(object);
      } catch (Exception e) {
        Flixel.log.tagged(TAG).warn("A selection listener failed: {}", e.getMessage());
      }
    }
  }

  private static boolean isGone(Object object) {
    return object instanceof FlixelExistable existable && !existable.isExists();
  }

  /** Returns whether an object is the current state, a tracked root, or somewhere below one of them. */
  private boolean isInScene(Object target) {
    if (target == Flixel.state || trackedObjects.contains(target, true)) {
      return true;
    }
    if (findParent(Flixel.state, target, 0) != null) {
      return true;
    }
    for (int i = 0, n = trackedObjects.getSize(); i < n; i++) {
      if (findParent(trackedObjects.get(i), target, 0) != null) {
        return true;
      }
    }
    return false;
  }

  /** Starts a mark pass and marks every object reachable from the current state and the tracked roots. */
  private void markScene() {
    ids.beginMark();
    markTree(Flixel.state, 0);
    for (int i = 0, n = trackedObjects.getSize(); i < n; i++) {
      markTree(trackedObjects.get(i), 0);
    }
  }

  private void markTree(@Nullable Object node, int depth) {
    if (node == null || depth > MAX_DEPTH) {
      return;
    }
    ids.mark(node);
    for (int i = 0, n = FlixelDebugNodes.childCount(node); i < n; i++) {
      markTree(FlixelDebugNodes.child(node, i), depth + 1);
    }
    if (node instanceof FlixelState state) {
      markTree(state.getSubState(), depth + 1);
    }
  }

  @Nullable
  private static Object findParent(@Nullable Object node, Object target, int depth) {
    if (node == null || depth > MAX_DEPTH) {
      return null;
    }
    int n = FlixelDebugNodes.childCount(node);
    for (int i = 0; i < n; i++) {
      if (FlixelDebugNodes.child(node, i) == target) {
        return node;
      }
    }
    for (int i = 0; i < n; i++) {
      Object member = FlixelDebugNodes.child(node, i);
      if (FlixelDebugNodes.isContainer(member) || member instanceof FlixelState) {
        Object found = findParent(member, target, depth + 1);
        if (found != null) {
          return found;
        }
      }
    }
    if (node instanceof FlixelState state) {
      FlixelSubState sub = state.getSubState();
      if (sub == target) {
        return state;
      }
      return findParent(sub, target, depth + 1);
    }
    return null;
  }

  /**
   * Returns how many extra roots were added with {@link #track(Object, String)}.
   *
   * @return The tracked count.
   */
  public int getTrackedCount() {
    return trackedObjects.getSize();
  }

  /**
   * Returns a tracked object. Together with {@link #getTrackedCount()} this walks the extra roots without
   * creating an iterator.
   *
   * @param index A position from 0 to {@link #getTrackedCount()} minus 1.
   * @return The object.
   */
  @NotNull
  public Object getTracked(int index) {
    return trackedObjects.get(index);
  }

  /**
   * Returns the label of a tracked object.
   *
   * @param index A position from 0 to {@link #getTrackedCount()} minus 1.
   * @return The label given to {@link #track(Object, String)}.
   */
  @NotNull
  public String getTrackedLabel(int index) {
    return trackedLabels.get(index);
  }

  /**
   * Returns the selected object, or {@code null} when nothing is selected.
   *
   * <p>The object may have its {@code exists} flag off while it is still in the scene. One that was destroyed or
   * removed is dropped from the selection by {@link #update(float)}.
   *
   * @return The selected object, or {@code null}.
   */
  @Nullable
  public Object getSelected() {
    return selected;
  }

  /**
   * Returns the id of the selected object.
   *
   * @return The id, or {@link FlixelDebugObjectIds#NONE} when nothing is selected.
   */
  public int getSelectedId() {
    Object current = getSelected();
    return current != null ? ids.idOf(current) : FlixelDebugObjectIds.NONE;
  }

  /**
   * Replaces how {@link #goTo()} moves the camera.
   *
   * @param focusHandler The handler, or {@code null} to center the main camera directly.
   */
  /**
   * Returns whether the current selection was made by a tool (such as an AI agent) rather than by a person or the
   * game.
   *
   * @return {@code true} for a selection made through a feed action.
   */
  public boolean isAgentSelection() {
    return agentSelection;
  }

  /**
   * Returns the read epoch: it goes up whenever rows marked {@code once()} should read their getters again.
   *
   * @return The epoch.
   */
  public int getEpoch() {
    return epoch;
  }

  /**
   * Returns the thumbnail reader of the inspector. A panel sets its listener to receive preview pictures.
   *
   * @return The thumbnail reader.
   */
  @NotNull
  public FlixelDebugThumbnails getThumbnails() {
    return thumbnails;
  }

  public void setFocusHandler(@Nullable FocusHandler focusHandler) {
    this.focusHandler = focusHandler;
  }

  /**
   * Returns the registry that gives objects their ids.
   *
   * @return The registry.
   */
  @NotNull
  public FlixelDebugObjectIds getIds() {
    return ids;
  }

  private static final class Chain {

    final FlixelArray<FlixelDebugSection> sections = new FlixelArray<>();
    final String typeName;

    Chain(Class<?> type) {
      String simple = type.getSimpleName();
      typeName = simple.isEmpty() ? type.getName() : simple;
    }
  }

  /** Moves the main camera to a world point for {@link #goTo()}. */
  @FunctionalInterface
  public interface FocusHandler {

    /**
     * Centers the main camera on a world point.
     *
     * @param worldX The world X to center on.
     * @param worldY The world Y to center on.
     * @return {@code true} when the camera moved.
     */
    boolean focus(float worldX, float worldY);
  }

  /** Listens for changes of the inspector's selection. */
  @FunctionalInterface
  public interface SelectionListener {

    /**
     * Called after the selection changed.
     *
     * @param selected The newly selected object, or {@code null} when the selection was cleared.
     */
    void onSelectionChanged(@Nullable Object selected);
  }
}
