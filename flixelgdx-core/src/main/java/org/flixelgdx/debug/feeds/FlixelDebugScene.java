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
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelIdentityMap;
import org.flixelgdx.debug.FlixelDebugNode;
import org.flixelgdx.debug.FlixelDebugNodes;
import org.flixelgdx.debug.FlixelDebugObjectIds;
import org.flixelgdx.debug.FlixelDebugPaths;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.group.FlixelGroupable;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

/**
 * The scene panel of the remote debugger: a tree of every object in the current state, its open sub states,
 * and the extra roots added through the inspector.
 *
 * <p>Think of it as the floor plan of a building. The state is the building, each group is a room, and the
 * objects are the furniture inside. The app draws the plan, and a click on a piece of furniture selects it
 * in the inspector. The plan is a snapshot: it is sent whole, and sent again only when the building was
 * rearranged.
 *
 * <h2>Nodes</h2>
 *
 * <p>Each node carries the {@linkplain FlixelDebugObjectIds id} of its object, the id of its parent
 * ({@code -1} for a root), the type name, a label, and a flag that is true for objects added through
 * {@link FlixelDebugInspector#track(Object, String)}. Parents always come before their children.
 * The label is the step the object takes in a {@linkplain FlixelDebugPaths scene path}: {@code state} for
 * the state, {@code substate} for an open sub state, the member index for a member, and the tracked label
 * for a tracked root. Joining the labels from the root down with dots gives a valid scene path, for example
 * {@code state.3.0}.
 *
 * <h2>Change detection</h2>
 *
 * <p>Besides groups, the walk descends into every {@link FlixelDebugNode}, so objects that are neither a group
 * nor a sprite (for example user interface widgets) have their children listed too. A child of either kind is
 * labeled with its index.
 *
 * <p>About twice a second the panel walks the tree and adds up a checksum of the shape (each object, the
 * size of each group, the open sub states). When the checksum differs from the last one the revision goes
 * up, so the network agent sends a new snapshot. The walk does not allocate. A changed object property
 * does not change the shape, so it never triggers a resend. The app can also ask for a fresh snapshot with
 * {@link FlixelDebugProtocol#SCENE_REQUEST_TREE}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Flixel.debug.scene.refresh();          // Check for changes right now.
 * int nodes = Flixel.debug.scene.getNodeCount();
 * }</pre>
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugProtocol#KIND_SCENE
 * @see FlixelDebugPaths
 */
public class FlixelDebugScene extends FlixelDebugFeedBase {

  /** Seconds between two checks for a changed tree. */
  public static final float CHECK_INTERVAL = 0.5f;

  private final FlixelIdentityMap<Class<?>, String> typeNames = new FlixelIdentityMap<>();
  private final FlixelString label = new FlixelString();
  private final FlixelDebugObjectIds ids;
  private final FlixelDebugInspector roots;

  private FlixelDebugMessageWriter writer;
  private NodeVisitor visitor;
  private boolean[] trackedSeen = new boolean[4];
  private float timer;
  private int checksum;
  private int nodeCount;
  private boolean checked;

  /**
   * Creates the scene panel.
   *
   * @param ids The registry that gives objects their ids.
   * @param roots The inspector that knows the tracked roots.
   */
  public FlixelDebugScene(@NotNull FlixelDebugObjectIds ids, @NotNull FlixelDebugInspector roots) {
    super("scene", FlixelDebugProtocol.KIND_SCENE);
    this.ids = ids;
    this.roots = roots;
  }

  @Override
  public void update(float elapsed) {
    timer += elapsed;
    if (timer >= CHECK_INTERVAL) {
      timer = 0f;
      refresh();
    }
  }

  /**
   * Walks the tree now and, when its shape differs from the last walk, makes the panel send a new snapshot.
   *
   * <p>{@link #update(float)} calls this twice a second. It does not allocate.
   */
  public void refresh() {
    writer = null;
    int before = checksum;
    walkAll();
    if (!checked || checksum != before) {
      checked = true;
      markChanged();
    }
  }

  /**
   * Writes the whole tree as an {@code array<node>}.
   *
   * @param out The writer positioned right after the panel index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter out) {
    writer = out;
    out.beginArray();
    walkAll();
    out.endArray(nodeCount);
    writer = null;
    checked = true;
  }

  /**
   * Walks the current scene and shows every node to a visitor, parents before their children, in the same order and
   * with the same ids and labels as the feed's payload. It does not change what the feed considers already sent.
   *
   * @param visitor Receives each node.
   * @return How many nodes were visited.
   */
  public int visit(@NotNull NodeVisitor visitor) {
    int savedChecksum = checksum;
    int savedCount = nodeCount;
    this.visitor = visitor;
    try {
      walkAll();
      return nodeCount;
    } finally {
      this.visitor = null;
      checksum = savedChecksum;
      nodeCount = savedCount;
    }
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    if (action == FlixelDebugProtocol.SCENE_REQUEST_TREE) {
      markChanged();
    }
  }

  @Override
  public void reset() {
    checked = false;
    timer = 0f;
  }

  private void walkAll() {
    checksum = 0;
    nodeCount = 0;
    int tracked = roots.getTrackedCount();
    if (trackedSeen.length < tracked) {
      trackedSeen = new boolean[Math.max(tracked, trackedSeen.length * 2)];
    }
    for (int i = 0; i < tracked; i++) {
      trackedSeen[i] = false;
    }
    FlixelState state = Flixel.state;
    if (state != null) {
      walk(state, FlixelDebugObjectIds.NONE, FlixelDebugPaths.STATE, -1, 0);
    }
    for (int i = 0; i < tracked; i++) {
      if (!trackedSeen[i]) {
        walk(roots.getTracked(i), FlixelDebugObjectIds.NONE, roots.getTrackedLabel(i), -1, 0);
      }
    }
  }

  private void walk(Object node, int parent, String step, int index, int depth) {
    if (nodeCount >= FlixelDebugProtocol.MAX_ARRAY_COUNT) {
      return;
    }
    nodeCount++;
    checksum = checksum * 31 + System.identityHashCode(node);
    boolean tracked = false;
    for (int i = 0, n = roots.getTrackedCount(); i < n; i++) {
      if (roots.getTracked(i) == node) {
        trackedSeen[i] = true;
        tracked = true;
      }
    }
    int id = FlixelDebugObjectIds.NONE;
    if (writer != null || visitor != null) {
      id = ids.idOf(node);
      if (step != null) {
        label.set(step);
      } else {
        label.set(index);
      }
      String type = typeName(node.getClass());
      if (writer != null) {
        writer.writeI32(id).writeI32(parent).writeString(type).writeString(label).writeBool(tracked);
      }
      if (visitor != null) {
        visitor.visit(id, parent, type, label, tracked);
      }
    }
    if (depth < FlixelDebugPaths.MAX_DEPTH) {
      int size = FlixelDebugNodes.childCount(node);
      if (size > 0 || node instanceof FlixelGroupable<?>) {
        checksum = checksum * 31 + size;
        for (int i = 0; i < size; i++) {
          Object member = FlixelDebugNodes.child(node, i);
          if (member == null) {
            checksum = checksum * 31 + 7;
          } else {
            walk(member, id, null, i, depth + 1);
          }
        }
      }
      if (node instanceof FlixelState owner && owner.getSubState() != null) {
        walk(owner.getSubState(), id, FlixelDebugPaths.SUBSTATE, -1, depth + 1);
      }
    }
    checksum = checksum * 31 + 13;
  }

  private String typeName(Class<?> type) {
    String name = typeNames.get(type);
    if (name == null) {
      String simple = type.getSimpleName();
      name = simple.isEmpty() ? type.getName() : simple;
      typeNames.put(type, name);
    }
    return name;
  }

  /**
   * Returns how many nodes the most recent walk visited.
   *
   * @return The node count, at most 65535.
   */
  public int getNodeCount() {
    return nodeCount;
  }

  /**
   * Receives the nodes of the scene from {@link FlixelDebugScene#visit(NodeVisitor)}.
   */
  @FunctionalInterface
  public interface NodeVisitor {

    /**
     * Receives one node.
     *
     * @param id The object's id, shared with the inspector and the viewport.
     * @param parent The parent's id, or {@code -1} for a root.
     * @param type The simple class name of the object.
     * @param label The node's step in a scene path, such as {@code state} or {@code 3}; a reused buffer.
     * @param trackedRoot Whether the object was added with the inspector's {@code track()}.
     */
    void visit(int id, int parent, @NotNull String type, @NotNull CharSequence label, boolean trackedRoot);
  }
}
