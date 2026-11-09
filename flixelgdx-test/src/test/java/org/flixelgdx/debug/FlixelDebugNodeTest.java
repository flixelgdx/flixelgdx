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
import org.flixelgdx.FlixelBasic;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.FlixelState;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.util.FlixelString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugNodeTest {

  private FlixelLogger originalLog;
  private FlixelDebugManager debug;
  private FlixelCamera cam;
  private FlixelState state;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    Flixel.log = log;
    debug = new FlixelDebugManager();
    state = new TestState();
    Flixel.state = state;
    Flixel.cameras.clear();
    cam = new FlixelCamera(400, 300);
    cam.getViewport().setScreenBounds(0, 0, 400, 300);
    Flixel.cameras.add(cam);
  }

  @AfterEach
  void tearDown() {
    debug.controls.reset();
    Flixel.log = originalLog;
    Flixel.state = null;
    Flixel.cameras.clear();
  }

  @Test
  void theSceneDescendsIntoNodesAndLabelsChildrenByIndex() {
    Widget panel = new Widget(10f, 10f, 100f, 80f);
    Widget button = new Widget(20f, 20f, 30f, 10f);
    panel.children.add(null);
    panel.children.add(button);
    state.add(panel);

    FlixelDebugMessageReader r = DebugTestSupport.payload(debug.scene);
    int count = r.readCount();
    List<String> labels = new ArrayList<>();
    List<Integer> parents = new ArrayList<>();
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ids.add(r.readI32());
      parents.add(r.readI32());
      DebugTestSupport.str(r);
      labels.add(DebugTestSupport.str(r));
      r.readBool();
    }
    assertEquals(List.of("state", "0", "1"), labels, "the empty slot keeps its index and is not sent");
    assertEquals(ids.get(1), parents.get(2), "the button is a child of the panel");
    assertEquals(debug.ids.peek(button), ids.get(2));
  }

  @Test
  void pathsAndSelectionWorkThroughNodes() {
    Widget panel = new Widget(10f, 10f, 100f, 80f);
    Widget button = new Widget(20f, 20f, 30f, 10f);
    panel.children.add(button);
    state.add(panel);

    assertSame(button, FlixelDebugPaths.resolve(debug.inspector, "state.0.0"));
    assertNull(FlixelDebugPaths.resolve(debug.inspector, "state.0.1"));
    FlixelString path = new FlixelString();
    assertTrue(FlixelDebugPaths.build(debug.inspector, button, path));
    assertEquals("state.0.0", path.toString());
  }

  @Test
  void aNodeHasABoxAndIsPickedByIt() {
    Widget panel = new Widget(10f, 10f, 100f, 80f);
    Widget button = new Widget(20f, 20f, 30f, 10f);
    Widget hidden = new Widget(200f, 200f, 30f, 10f);
    hidden.onCamera = false;
    panel.children.add(button);
    panel.children.add(hidden);
    state.add(panel);

    debug.viewport.sample();
    assertEquals(2, debug.viewport.getBoxCount(), "the hidden node has no box");
    assertEquals(debug.ids.peek(panel), debug.viewport.getBoxId(0));
    assertEquals(debug.ids.peek(button), debug.viewport.getBoxId(1), "children are drawn on top");
    assertEquals(FlixelDebugProtocol.VIEWPORT_BOX_GROUP, debug.viewport.getBoxFlags(0));
    assertEquals(0, debug.viewport.getBoxFlags(1));

    assertSame(button, debug.viewport.pick(25f, 25f));
    assertSame(panel, debug.viewport.pick(90f, 80f));
    assertNull(debug.viewport.pick(300f, 250f));
  }

  @Test
  void childCountIgnoresPlainObjects() {
    assertEquals(0, FlixelDebugNodes.childCount(new Object()));
    assertFalse(FlixelDebugNodes.isContainer(new Object()));
    assertNull(FlixelDebugNodes.child(new Object(), 0));
    assertEquals(0, FlixelDebugNodes.childCount(null));
  }

  private static final class Widget extends FlixelBasic implements FlixelDebugNode {

    final FlixelArray<Widget> children = new FlixelArray<>();
    final float x;
    final float y;
    final float w;
    final float h;
    boolean onCamera = true;

    Widget(float x, float y, float w, float h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    @Override
    public int getDebugChildCount() {
      return children.getSize();
    }

    @Override
    public Object getDebugChild(int index) {
      return children.get(index);
    }

    @Override
    public boolean getDebugBounds(FlixelCamera camera, float[] out) {
      if (!onCamera) {
        return false;
      }
      out[0] = camera.worldToViewX(x, 1f);
      out[1] = camera.worldToViewY(y, 1f);
      out[2] = w;
      out[3] = h;
      return true;
    }
  }

  private static final class TestState extends FlixelState {
  }
}
