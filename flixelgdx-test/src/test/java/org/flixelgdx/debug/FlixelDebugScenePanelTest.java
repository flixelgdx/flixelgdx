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
package org.flixelgdx.debug;

import com.sun.management.ThreadMXBean;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.debug.panels.FlixelDebugInspectorPanel;
import org.flixelgdx.debug.panels.FlixelDebugScenePanel;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugScenePanelTest {

  private record Node(int id, int parent, String type, String label, boolean tracked) {
  }

  private FlixelDebugObjectIds ids;
  private FlixelDebugInspectorPanel inspector;
  private FlixelDebugScenePanel scene;
  private FlixelState state;

  @BeforeEach
  void setUp() {
    ids = new FlixelDebugObjectIds();
    inspector = new FlixelDebugInspectorPanel(ids);
    scene = new FlixelDebugScenePanel(ids, inspector);
    state = new TestState();
    Flixel.state = state;
  }

  @AfterEach
  void tearDown() {
    Flixel.state = null;
  }

  private List<Node> nodes() {
    FlixelDebugMessageReader r = DebugTestSupport.payload(scene);
    int count = r.readCount();
    List<Node> out = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      int id = r.readI32();
      int parent = r.readI32();
      String type = DebugTestSupport.str(r);
      String label = DebugTestSupport.str(r);
      out.add(new Node(id, parent, type, label, r.readBool()));
    }
    assertEquals(0, r.getRemaining());
    return out;
  }

  @Test
  void hasTheSceneKindAndId() {
    assertEquals("scene", scene.getId());
    assertEquals(FlixelDebugProtocol.KIND_SCENE, scene.getKind());
  }

  @Test
  void anEmptyStateIsOneRootNode() {
    List<Node> nodes = nodes();
    assertEquals(1, nodes.size());
    assertEquals(-1, nodes.get(0).parent());
    assertEquals("state", nodes.get(0).label());
    assertEquals("TestState", nodes.get(0).type());
    assertFalse(nodes.get(0).tracked());
  }

  @Test
  void noStateGivesAnEmptyTree() {
    Flixel.state = null;
    assertEquals(0, nodes().size());
  }

  @Test
  void parentsComeBeforeTheirChildrenAndLabelsAreScenePathSteps() {
    FlixelSprite first = new FlixelSprite();
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite inner = new FlixelSprite();
    group.add(inner);
    state.add(first);
    state.add(group);
    TestSubState sub = new TestSubState();
    sub.add(new FlixelSprite());
    state.openSubState(sub);

    List<Node> nodes = nodes();
    assertEquals(6, nodes.size());
    assertEquals("state", nodes.get(0).label());
    assertEquals("0", nodes.get(1).label());
    assertEquals(nodes.get(0).id(), nodes.get(1).parent());
    assertEquals(ids.idOf(first), nodes.get(1).id());
    assertEquals("1", nodes.get(2).label());
    assertEquals(ids.idOf(group), nodes.get(2).id());
    assertEquals("0", nodes.get(3).label());
    assertEquals(nodes.get(2).id(), nodes.get(3).parent());
    assertEquals(ids.idOf(inner), nodes.get(3).id());
    assertEquals("substate", nodes.get(4).label());
    assertEquals("TestSubState", nodes.get(4).type());
    assertEquals(nodes.get(0).id(), nodes.get(4).parent());
    assertEquals(nodes.get(4).id(), nodes.get(5).parent());
    assertEquals(ids.idOf(state), nodes.get(0).id());

    Set<Integer> seen = new HashSet<>();
    for (Node node : nodes) {
      assertTrue(node.parent() == -1 || seen.contains(node.parent()), "the parent of " + node + " comes first");
      seen.add(node.id());
    }
  }

  @Test
  void emptySlotsKeepTheirIndexes() {
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite a = new FlixelSprite();
    FlixelSprite b = new FlixelSprite();
    group.add(a);
    group.add(b);
    state.add(group);
    group.remove(a);

    List<Node> nodes = nodes();
    Node last = nodes.get(nodes.size() - 1);
    assertEquals(ids.idOf(b), last.id());
    assertEquals(String.valueOf(group.getMembers().indexOf(b, true)), last.label());
  }

  @Test
  void trackedRootsAreFlaggedAndAddedAfterTheState() {
    FlixelSpriteGroup managers = new FlixelSpriteGroup();
    managers.add(new FlixelSprite());
    FlixelSprite inState = new FlixelSprite();
    state.add(inState);
    inspector.track(managers, "managers");
    inspector.track(inState, "player");

    List<Node> nodes = nodes();
    assertEquals(4, nodes.size(), "an object that is already in the state is not listed twice");
    Node player = nodes.get(1);
    assertEquals(ids.idOf(inState), player.id());
    assertTrue(player.tracked(), "the copy inside the state is flagged");
    Node root = nodes.get(2);
    assertEquals("managers", root.label());
    assertEquals(-1, root.parent());
    assertTrue(root.tracked());
    assertEquals("0", nodes.get(3).label());
    assertEquals(root.id(), nodes.get(3).parent());
    assertFalse(nodes.get(3).tracked());
  }

  @Test
  void theRevisionOnlyChangesWhenTheShapeChanges() {
    scene.refresh();
    long first = scene.getRevision();
    scene.refresh();
    scene.refresh();
    assertEquals(first, scene.getRevision(), "an unchanged tree is not resent");

    FlixelSprite sprite = new FlixelSprite();
    state.add(sprite);
    scene.refresh();
    long added = scene.getRevision();
    assertTrue(added > first);

    sprite.setX(50f);
    scene.refresh();
    assertEquals(added, scene.getRevision(), "moving an object does not change the shape");

    state.remove(sprite);
    scene.refresh();
    assertTrue(scene.getRevision() > added);
  }

  @Test
  void openingASubStateOrTrackingChangesTheRevision() {
    scene.refresh();
    long before = scene.getRevision();
    state.openSubState(new TestSubState());
    scene.refresh();
    long open = scene.getRevision();
    assertTrue(open > before);
    inspector.track(new FlixelSprite(), "extra");
    scene.refresh();
    assertTrue(scene.getRevision() > open);
  }

  @Test
  void updateChecksAtTheCheckInterval() {
    scene.update(0.01f);
    long first = scene.getRevision();
    state.add(new FlixelSprite());
    scene.update(0.1f);
    assertEquals(first, scene.getRevision(), "too early to look again");
    scene.update(FlixelDebugScenePanel.CHECK_INTERVAL);
    assertTrue(scene.getRevision() > first);
  }

  @Test
  void requestTreeForcesAResend() {
    scene.refresh();
    long before = scene.getRevision();
    DebugTestSupport.send(scene, FlixelDebugProtocol.SCENE_REQUEST_TREE, w -> {
    });
    assertTrue(scene.getRevision() > before);
    DebugTestSupport.send(scene, 99, w -> {
    });
  }

  @Test
  void walkingAnUnchangedTreeDoesNotAllocate() {
    for (int i = 0; i < 50; i++) {
      state.add(new FlixelSprite());
    }
    for (int i = 0; i < 200; i++) {
      scene.refresh();
    }
    ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    long id = Thread.currentThread().threadId();
    long before = bean.getThreadAllocatedBytes(id);
    for (int i = 0; i < 200; i++) {
      scene.refresh();
    }
    long used = bean.getThreadAllocatedBytes(id) - before;
    assertTrue(used < 2048, "allocated " + used + " bytes");
  }

  private static final class TestState extends FlixelState {
  }

  private static final class TestSubState extends FlixelSubState {
  }
}
