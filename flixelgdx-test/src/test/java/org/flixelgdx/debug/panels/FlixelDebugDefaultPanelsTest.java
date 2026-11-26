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
package org.flixelgdx.debug.ui;

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.panels.FlixelDebugDefaultPanels;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.UiTestSupport.Node;
import org.flixelgdx.debug.ui.UiTestSupport.Op;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.flixelgdx.debug.ui.UiTestSupport.event;
import static org.flixelgdx.debug.ui.UiTestSupport.opened;
import static org.flixelgdx.debug.ui.UiTestSupport.patch;
import static org.flixelgdx.debug.ui.UiTestSupport.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugDefaultPanelsTest {

  private FlixelLogger originalLog;
  private FlixelDebugManager manager;
  private FlixelDefaultLogger root;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    Flixel.log = root;
    manager = new FlixelDebugManager();
    root.addSink(manager.logs.getSink());
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  private FlixelDebugPanel panel(String id) {
    FlixelDebugPanel panel = manager.panels.get(id);
    assertNotNull(panel, id);
    return panel;
  }

  @Test
  void everyBuiltInPanelOpensUpdatesAndWritesWithoutAGame() {
    for (int i = 0; i < manager.panels.getCount(); i++) {
      FlixelDebugPanel panel = manager.panels.get(i);
      panel.setOpen(true);
      for (int frame = 0; frame < 10; frame++) {
        manager.update(0.05f);
      }
      Node root = tree(panel);
      assertTrue(root.type() > 0, panel.getId());
      manager.update(0.05f);
      patch(panel);
    }
  }

  @Test
  void theControlsPanelDrivesTheControlsFeed() {
    FlixelDebugPanel controls = opened(panel(FlixelDebugDefaultPanels.CONTROLS));
    FlixelDebugWidget<?> hitboxes = controls.find("hitboxes");
    assertNotNull(hitboxes);
    event(controls, hitboxes, FlixelDebugProtocol.EVENT_CHANGE, true);
    assertTrue(manager.controls.isHitboxes());

    FlixelDebugWidget<?> warn = controls.find("level.warn");
    assertNotNull(warn);
    event(controls, warn, FlixelDebugProtocol.EVENT_CHANGE, true);
    assertEquals(FlixelDebugProtocol.LEVEL_WARN, manager.controls.getLogLevel());

    manager.controls.setHitboxes(false);
    manager.update(0.016f);
    boolean sawHitboxes = false;
    for (Op op : patch(controls)) {
      if (op.widget() == hitboxes.getId() && op.key() == FlixelDebugProtocol.PROP_VALUE) {
        sawHitboxes = !((Boolean) op.value());
      }
    }
    assertTrue(sawHitboxes, "a change made by the game shows up in the panel");
  }

  @Test
  void theLogsPanelAppendsNewLinesAndFiltersByLevel() {
    FlixelDebugPanel logs = opened(panel(FlixelDebugDefaultPanels.LOGS));
    manager.update(0.016f);
    patch(logs);
    root.tagged("Game").info("hello");
    root.tagged("Game").warn("careful");
    manager.update(0.016f);
    List<Op> ops = patch(logs);
    Op append = ops.stream().filter(op -> op.code() == FlixelDebugProtocol.PATCH_APPEND).findFirst().orElseThrow();
    assertEquals(2, ((List<?>) append.value()).size());

    FlixelDebugWidget<?> info = logs.find("show.info");
    assertNotNull(info);
    event(logs, info, FlixelDebugProtocol.EVENT_CHANGE, false);
    manager.update(0.016f);
    FlixelDebugWidget<?> lines = logs.find("lines");
    assertNotNull(lines);
    List<?> rows = null;
    for (Op op : patch(logs)) {
      if (op.widget() == lines.getId() && op.key() == FlixelDebugProtocol.PROP_ITEMS) {
        rows = (List<?>) op.value();
      }
    }
    assertNotNull(rows);
    assertEquals(1, rows.size(), "only the warning is left");
  }

  @Test
  void theConsolePanelRunsWhatThePersonSubmits() {
    FlixelDebugPanel console = opened(panel(FlixelDebugDefaultPanels.CONSOLE));
    manager.update(0.016f);
    FlixelDebugWidget<?> command = console.find("command");
    assertNotNull(command);
    Node node = tree(console);
    assertNotNull(node);
    int before = manager.console.getResultCount();
    event(console, command, FlixelDebugProtocol.EVENT_SUBMIT, "hitboxes on");
    assertEquals(before + 1, manager.console.getResultCount());
    assertTrue(manager.controls.isHitboxes());
  }

  @Test
  void theConsoleCanShowAndCloseAppPanels() {
    manager.console.run("show inspector");
    manager.console.run("close Trackers");
    manager.console.run("layout.reset");
    int requests = 0;
    while (manager.panels.hasRequest()) {
      manager.panels.writeRequest(new FlixelDebugMessageWriter());
      requests++;
    }
    assertEquals(3, requests);
  }

  @Test
  void theWatchesPanelListsEveryWatchInATable() {
    FlixelDebugPanel watches = opened(panel(FlixelDebugDefaultPanels.WATCHES));
    manager.watches.addInt("Score", () -> 42);
    manager.watches.addInt("Broken", () -> {
      throw new IllegalStateException("no level");
    });
    manager.update(1f);
    FlixelDebugWidget<?> values = watches.find("values");
    assertNotNull(values);
    assertInstanceOf(FlixelDebugTable.class, values, "the watches show as a flat table, not a folding section");
    List<?> rows = null;
    for (Op op : patch(watches)) {
      if (op.widget() == values.getId() && op.key() == FlixelDebugProtocol.PROP_ITEMS) {
        rows = (List<?>) op.value();
      }
    }
    assertNotNull(rows);
    assertEquals(2, rows.size());
    List<?> score = (List<?>) rows.get(0);
    assertEquals(FlixelDebugProtocol.TONE_NORMAL, score.get(1));
    assertEquals(List.of("Score", 42), score.get(2));
    List<?> broken = (List<?>) rows.get(1);
    assertEquals(FlixelDebugProtocol.TONE_ERROR, broken.get(1));
    assertEquals(List.of("Broken", "error: no level"), broken.get(2));
  }

  @Test
  void theInspectorPanelFollowsTheSelection() {
    FlixelDebugPanel inspector = opened(panel(FlixelDebugDefaultPanels.INSPECTOR));
    manager.inspector.select(new Object());
    manager.update(1f);
    FlixelDebugWidget<?> path = inspector.find("path");
    assertNotNull(path);
    assertTrue(path.getParent().isVisible(), "the selection bar shows while something is selected");
    manager.inspector.markAgentSelection();
    assertTrue(manager.inspector.isAgentSelection());
    manager.inspector.clearSelection();
    assertFalse(manager.inspector.isAgentSelection());
  }
}
