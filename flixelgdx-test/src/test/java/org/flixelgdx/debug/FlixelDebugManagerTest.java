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

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugManagerTest {

  private FlixelDebugManager manager;

  @BeforeEach
  void setUp() {
    manager = new FlixelDebugManager();
  }

  @Test
  void builtInPanelsAreRegisteredInOrder() {
    String[] ids = { "stats", "logs", "watches", "trackers", "controls", "console", "inspector" };
    int[] kinds = {
        FlixelDebugProtocol.KIND_STATS, FlixelDebugProtocol.KIND_LOGS, FlixelDebugProtocol.KIND_WATCHES,
        FlixelDebugProtocol.KIND_TRACKERS, FlixelDebugProtocol.KIND_CONTROLS, FlixelDebugProtocol.KIND_CONSOLE,
        FlixelDebugProtocol.KIND_INSPECTOR };
    assertEquals(ids.length, manager.getPanelCount());
    for (int i = 0; i < ids.length; i++) {
      assertEquals(ids[i], manager.getPanel(i).getId());
      assertEquals(kinds[i], manager.getPanel(i).getKind());
    }
    assertSame(manager.stats, manager.get("stats"));
    assertSame(manager.logs, manager.get("logs"));
    assertSame(manager.watches, manager.get("watches"));
    assertSame(manager.trackers, manager.get("trackers"));
    assertSame(manager.controls, manager.get("controls"));
    assertSame(manager.console, manager.get("console"));
    assertSame(manager.inspector, manager.get("inspector"));
  }

  @Test
  void registerReturnsThePanelAndGetFindsIt() {
    CountingPanel panel = manager.register(new CountingPanel("custom.count"));
    assertSame(panel, manager.get("custom.count"));
    assertEquals(FlixelDebugProtocol.KIND_CUSTOM, panel.getKind());
    assertSame(panel, manager.getPanel(manager.getPanelCount() - 1));
  }

  @Test
  void getWithTypeReturnsNullForTheWrongTypeOrUnknownId() {
    CountingPanel panel = manager.register(new CountingPanel("custom.count"));
    assertSame(panel, manager.get("custom.count", CountingPanel.class));
    assertSame(manager.stats, manager.get("stats", FlixelDebugStatsPanel.class));
    assertNull(manager.get("custom.count", FlixelDebugStatsPanel.class));
    assertNull(manager.get("nope", CountingPanel.class));
    assertNull(manager.get("nope"));
  }

  @Test
  void duplicateIdsAreRejected() {
    manager.register(new CountingPanel("custom.count"));
    assertThrows(IllegalArgumentException.class, () -> manager.register(new CountingPanel("custom.count")));
    assertThrows(IllegalArgumentException.class, () -> manager.register(new CountingPanel("stats")));
    assertEquals(8, manager.getPanelCount());
  }

  @Test
  void unregisterRemovesThePanelAndKeepsTheOrderOfTheRest() {
    CountingPanel a = manager.register(new CountingPanel("custom.a"));
    CountingPanel b = manager.register(new CountingPanel("custom.b"));
    CountingPanel c = manager.register(new CountingPanel("custom.c"));
    assertSame(b, manager.unregister("custom.b"));
    assertNull(manager.get("custom.b"));
    assertNull(manager.unregister("custom.b"));
    int n = manager.getPanelCount();
    assertSame(a, manager.getPanel(n - 2));
    assertSame(c, manager.getPanel(n - 1));
  }

  @Test
  void panelRevisionChangesWhenPanelsAreAddedOrRemoved() {
    long start = manager.getPanelRevision();
    manager.register(new CountingPanel("custom.a"));
    long afterAdd = manager.getPanelRevision();
    assertTrue(afterAdd > start);
    manager.unregister("custom.a");
    assertTrue(manager.getPanelRevision() > afterAdd);
    long afterRemove = manager.getPanelRevision();
    manager.unregister("custom.a");
    assertEquals(afterRemove, manager.getPanelRevision(), "removing an unknown panel changes nothing");
  }

  @Test
  void updateReachesOnlyEnabledPanels() {
    CountingPanel on = manager.register(new CountingPanel("custom.on"));
    CountingPanel off = manager.register(new CountingPanel("custom.off"));
    off.setEnabled(false);
    manager.update(0.25f);
    manager.update(0.25f);
    assertEquals(2, on.updates);
    assertEquals(0.5f, on.total, 1e-6f);
    assertEquals(0, off.updates);
    assertFalse(off.isEnabled());
  }

  @Test
  void disposeResetsEveryPanel() {
    CountingPanel panel = manager.register(new CountingPanel("custom.a"));
    manager.dispose();
    assertEquals(1, panel.resets);
  }

  private static final class CountingPanel extends FlixelDebugPanelBase {

    float total;
    int updates;
    int resets;

    CountingPanel(String id) {
      super(id, id, FlixelDebugProtocol.KIND_CUSTOM);
    }

    @Override
    public void update(float elapsed) {
      updates++;
      total += elapsed;
    }

    @Override
    public void write(@NotNull FlixelDebugMessageWriter writer) {}

    @Override
    public void reset() {
      resets++;
    }
  }
}
