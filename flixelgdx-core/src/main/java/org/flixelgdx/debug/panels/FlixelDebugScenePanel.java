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
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.feeds.FlixelDebugScene;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugTree;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Scene" panel: the tree of the current state, its open sub states, their groups, and the objects added
 * with the inspector's {@code track()}.
 *
 * <p>Selecting a node selects its object in the inspector, and selecting an object anywhere else (with Alt + Click,
 * the viewport, or a tool) selects its node here. Nodes added with {@code track()} carry a "tracked" badge and objects
 * an AI agent touched carry an "agent" badge. The tree is rebuilt when the scene's shape changes, which the
 * {@linkplain FlixelDebugManager#scene scene feed} checks twice a second. A game can replace the panel under the id
 * {@value FlixelDebugDefaultPanels#SCENE}.
 */
public class FlixelDebugScenePanel extends FlixelDebugPanel {

  private long seenRevision = -1L;

  private final FlixelDebugScene scene;
  private final FlixelDebugInspector inspector;
  private final FlixelDebugObjectIds ids;
  private final FlixelDebugTree tree = new FlixelDebugTree();
  private final FlixelDebugLabel empty =
      FlixelDebugUi.hint("The scene is empty. Connect a game to see its objects.");
  private final FlixelDebugScene.NodeVisitor builder = this::addNode;
  private int seenAgentRevision = -1;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose scene feed the panel shows.
   */
  public FlixelDebugScenePanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.SCENE, "Scene");
    this.scene = manager.scene;
    this.inspector = manager.inspector;
    this.ids = manager.ids;
    setDock(FlixelDebugProtocol.DOCK_CENTER);
    FlixelDebugTextBox filter = FlixelDebugUi.textBox("Filter objects").setGrow(1f).setName("filter");
    tree.setFilter(filter).setGrow(1f).setName("tree");
    tree.onSelect(id -> {
      Object object = id >= 0 ? ids.get((int) id) : null;
      if (object != null) {
        inspector.select(object);
      }
    });
    tree.onActivate(id -> {
      Object object = ids.get((int) id);
      if (object != null) {
        inspector.select(object);
        inspector.goTo();
      }
    });
    setContent(FlixelDebugUi.column(
        FlixelDebugUi.row(new FlixelDebugButton("Refresh", this::refresh).setName("refresh"), filter),
        empty,
        tree));
  }

  /** Builds the tree again from the current scene. */
  public void refresh() {
    scene.refresh();
    seenRevision = -1L;
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    long revision = scene.getRevision();
    int agentRevision = ids.getAgentRevision();
    if (revision != seenRevision || agentRevision != seenAgentRevision) {
      seenRevision = revision;
      seenAgentRevision = agentRevision;
      tree.beginNodes();
      int count = scene.visit(builder);
      tree.endNodes();
      empty.setVisible(count == 0);
    }
    tree.setSelected(inspector.getSelectedId());
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }

  private void addNode(int id, int parent, String type, CharSequence label, boolean trackedRoot) {
    FlixelDebugTree.Node node = tree.node(id, parent, type).detail(label);
    if (parent < 0) {
      node.style(FlixelDebugProtocol.STYLE_EXPANDED);
    }
    if (trackedRoot) {
      node.badge("tracked", FlixelDebugProtocol.TONE_ACCENT);
    }
    if (ids.isAgentTouched(id)) {
      node.badge("agent", FlixelDebugProtocol.TONE_SUCCESS);
    }
  }
}
