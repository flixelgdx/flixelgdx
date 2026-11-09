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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.FlixelSubState;
import org.flixelgdx.debug.feeds.FlixelDebugInspector;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.group.FlixelSpriteGroup;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugPathsTest {

  private FlixelDebugInspector inspector;
  private FlixelString path;
  private FlixelState state;

  @BeforeEach
  void setUp() {
    inspector = new FlixelDebugInspector(new FlixelDebugObjectIds());
    path = new FlixelString();
    state = new TestState();
    Flixel.state = state;
  }

  @AfterEach
  void tearDown() {
    Flixel.state = null;
  }

  @Test
  void theStateItselfIsJustState() {
    assertTrue(FlixelDebugPaths.build(inspector, state, path));
    assertEquals("state", path.toString());
    assertSame(state, FlixelDebugPaths.resolve(inspector, "state"));
  }

  @Test
  void membersAreNamedByTheirIndexAtEveryLevel() {
    FlixelSprite first = new FlixelSprite();
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    FlixelSprite inner = new FlixelSprite();
    FlixelSprite second = new FlixelSprite();
    group.add(inner);
    group.add(second);
    state.add(first);
    state.add(group);

    assertTrue(FlixelDebugPaths.build(inspector, first, path));
    assertEquals("state.0", path.toString());
    assertTrue(FlixelDebugPaths.build(inspector, second, path));
    assertEquals("state.1.1", path.toString());
    assertSame(second, FlixelDebugPaths.resolve(inspector, "state.1.1"));
    assertSame(group, FlixelDebugPaths.resolve(inspector, "state.1"));
  }

  @Test
  void aFailedBuildLeavesTheStringEmptyAndReportsFalse() {
    path.concat("junk");
    assertFalse(FlixelDebugPaths.build(inspector, new FlixelSprite(), path));
    assertEquals("", path.toString());
    assertFalse(FlixelDebugPaths.build(inspector, null, path));
  }

  @Test
  void subStatesStartTheirOwnPaths() {
    FlixelSprite inState = new FlixelSprite();
    state.add(inState);
    TestSubState sub = new TestSubState();
    FlixelSprite inSub = new FlixelSprite();
    sub.add(new FlixelSprite());
    sub.add(inSub);
    state.openSubState(sub);
    TestSubState nested = new TestSubState();
    FlixelSprite inNested = new FlixelSprite();
    nested.add(inNested);
    sub.openSubState(nested);

    assertTrue(FlixelDebugPaths.build(inspector, inState, path));
    assertEquals("state.0", path.toString());
    assertTrue(FlixelDebugPaths.build(inspector, sub, path));
    assertEquals("substate", path.toString());
    assertTrue(FlixelDebugPaths.build(inspector, inSub, path));
    assertEquals("substate.1", path.toString());
    assertTrue(FlixelDebugPaths.build(inspector, inNested, path));
    assertEquals("substate.substate.0", path.toString());

    assertSame(inSub, FlixelDebugPaths.resolve(inspector, "substate.1"));
    assertSame(inNested, FlixelDebugPaths.resolve(inspector, "substate.substate.0"));
    assertSame(nested, FlixelDebugPaths.resolve(inspector, "substate.substate"));
    assertSame(sub, FlixelDebugPaths.resolve(inspector, "state.substate"));
    assertSame(inNested, FlixelDebugPaths.resolve(inspector, "state.substate.substate.0"));
  }

  @Test
  void trackedRootsStartAtTheirLabel() {
    FlixelSpriteGroup manager = new FlixelSpriteGroup();
    FlixelSprite child = new FlixelSprite();
    manager.add(child);
    inspector.track(manager, "managers.enemies");

    assertTrue(FlixelDebugPaths.build(inspector, manager, path));
    assertEquals("managers.enemies", path.toString());
    assertTrue(FlixelDebugPaths.build(inspector, child, path));
    assertEquals("managers.enemies.0", path.toString());
    assertSame(child, FlixelDebugPaths.resolve(inspector, "managers.enemies.0"));
    assertSame(manager, FlixelDebugPaths.resolve(inspector, "managers.enemies"));
    assertNull(FlixelDebugPaths.resolve(inspector, "managers.enemie"));
  }

  @Test
  void badPathsResolveToNull() {
    state.add(new FlixelSprite());
    assertNull(FlixelDebugPaths.resolve(inspector, null));
    assertNull(FlixelDebugPaths.resolve(inspector, ""));
    assertNull(FlixelDebugPaths.resolve(inspector, "nothing"));
    assertNull(FlixelDebugPaths.resolve(inspector, "states"));
    assertNull(FlixelDebugPaths.resolve(inspector, "state.5"), "index out of range");
    assertNull(FlixelDebugPaths.resolve(inspector, "state.0.0"), "a sprite is not a group");
    assertNull(FlixelDebugPaths.resolve(inspector, "state.x"));
    assertNull(FlixelDebugPaths.resolve(inspector, "state.0x"));
    assertNull(FlixelDebugPaths.resolve(inspector, "state."));
    assertNull(FlixelDebugPaths.resolve(inspector, "state.-1"));
    assertNull(FlixelDebugPaths.resolve(inspector, "substate"), "no sub state is open");
    assertNull(FlixelDebugPaths.resolve(inspector, "state.substate"));
  }

  @Test
  void withoutAStateOnlyTrackedRootsResolve() {
    Flixel.state = null;
    assertNull(FlixelDebugPaths.resolve(inspector, "state"));
    assertFalse(FlixelDebugPaths.build(inspector, new FlixelSprite(), path));
  }

  @Test
  void selectPathSelectsTheObjectAndIgnoresBadPaths() {
    FlixelSprite sprite = new FlixelSprite();
    state.add(sprite);

    assertTrue(inspector.selectPath("state.0"));
    assertSame(sprite, inspector.getSelected());
    assertFalse(inspector.selectPath("state.9"));
    assertSame(sprite, inspector.getSelected(), "a failed select keeps the selection");

    sprite.kill();
    inspector.clearSelection();
    assertTrue(inspector.selectPath("state.0"), "a killed object that is still in the scene can be selected");
    assertSame(sprite, inspector.getSelected());
  }

  @Test
  void theInspectorStateCarriesThePathAndFollowsTheObject() {
    FlixelSprite a = new FlixelSprite();
    FlixelSprite b = new FlixelSprite();
    FlixelSpriteGroup group = new FlixelSpriteGroup();
    group.add(b);
    state.add(a);
    state.add(group);

    inspector.select(b);
    assertEquals("state.1.0", InspectorTestSupport.state(inspector).path());

    group.remove(b);
    state.add(b);
    assertEquals("state.2", InspectorTestSupport.state(inspector).path(), "a moved object gets a new path");
  }

  @Test
  void selectPathActionSelectsTheObject() {
    FlixelSprite sprite = new FlixelSprite();
    state.add(new FlixelSprite());
    state.add(sprite);

    DebugTestSupport.send(inspector, FlixelDebugProtocol.INSPECTOR_SELECT_PATH,
        w -> w.writeString("state.1"));

    assertSame(sprite, inspector.getSelected());
  }

  private static final class TestState extends FlixelState {
  }

  private static final class TestSubState extends FlixelSubState {
  }
}
